package org.fentanylsolutions.thaumicdabblery.feature.aurapylon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.potion.Potion;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

import org.fentanylsolutions.thaumicdabblery.compat.tc4tweaks.AuraResearchCacheCompat;
import org.fentanylsolutions.thaumicdabblery.mixins.late.gadomancy.AuraEffectHandlerInvoker;

import makeo.gadomancy.api.AuraEffect;
import makeo.gadomancy.common.aura.AuraEffectHandler;
import makeo.gadomancy.common.aura.AuraEffects.PotionDistributionEffect;
import makeo.gadomancy.common.aura.AuraResearchManager;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategoryList;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.playerdata.PacketResearchComplete;
import thaumcraft.common.lib.research.ResearchManager;

/** Loaded only when Gadomancy is installed. Each action retains an immutable prior state for rollback. */
public final class AuraPylonRegistry {

    private static final Map<Aspect, State> STATES = new LinkedHashMap<>();
    private static final Map<Aspect, AuraEffect> PUBLISHED = new LinkedHashMap<>();
    private static final Map<Aspect, ResearchItem> OWNED_RESEARCH = new LinkedHashMap<>();

    private AuraPylonRegistry() {}

    public static Aspect requireAspect(String tag) {
        Aspect aspect = tag == null ? null
            : Aspect.getAspect(
                tag.trim()
                    .toLowerCase(Locale.ROOT));
        if (aspect == null) throw new IllegalArgumentException("Unknown Aura Pylon aspect: " + tag);
        return aspect;
    }

    public static String requireTarget(String target) {
        if (target != null) {
            target = target.trim();
            if ("players".equals(target) || "living".equals(target) || "undead".equals(target)) return target;
            if (target.startsWith("entity:")) {
                Object type = EntityList.stringToClassMapping.get(target.substring(7));
                if (type instanceof Class && EntityLivingBase.class.isAssignableFrom((Class<?>) type)) return target;
            }
        }
        throw new IllegalArgumentException(
            "Aura Pylon target must be players, living, undead, or entity:<registered living entity ID>: " + target);
    }

    public static Potion requirePotion(int id) {
        if (id < 0 || id >= Potion.potionTypes.length || Potion.potionTypes[id] == null) {
            throw new IllegalArgumentException("Unknown Aura Pylon potion ID: " + id);
        }
        if (Potion.potionTypes[id].isInstant())
            throw new IllegalArgumentException("Aura Pylon duration rules do not support instant potions: " + id);
        return Potion.potionTypes[id];
    }

    public static Rule rule(String target, int potionId, int amplifier, int addedTicks, int maxTicks, int intervalTicks,
        double range) {
        String selector = requireTarget(target);
        Potion potion = requirePotion(potionId);
        if (amplifier < 0 || amplifier > 127) throw new IllegalArgumentException("Aura Pylon amplifier must be 0..127");
        if (addedTicks <= 0 || maxTicks < addedTicks || intervalTicks <= 0) {
            throw new IllegalArgumentException(
                "Aura Pylon durations/interval must be positive and maxTicks >= addedTicks");
        }
        if (Double.isNaN(range) || range <= 0 || range > 64)
            throw new IllegalArgumentException("Aura Pylon range must be > 0 and <= 64");
        return new Rule(selector, potion, amplifier, addedTicks, maxTicks, intervalTicks, range);
    }

    public static synchronized Runnable add(String tag, Rule rule) {
        Aspect aspect = requireAspect(tag);
        State previous = STATES.get(aspect);
        AuraEffect original = previous == null ? AuraEffectHandler.registeredEffects.get(aspect) : previous.original;
        List<Rule> rules = previous == null ? new ArrayList<>() : new ArrayList<>(previous.rules);
        // Snapshot undo keeps duplicate declarations independently undoable without applying twice.
        if (rules.stream()
            .noneMatch(existing -> existing.key.equals(rule.key))) rules.add(rule);
        return edit(aspect, new State(original, previous == null || previous.keepOriginal, rules));
    }

    public static synchronized Runnable clear(String tag) {
        Aspect aspect = requireAspect(tag);
        State previous = STATES.get(aspect);
        AuraEffect original = previous == null ? AuraEffectHandler.registeredEffects.get(aspect) : previous.original;
        return edit(aspect, new State(original, false, Collections.emptyList()));
    }

    public static synchronized Runnable removeCustom(String tag, String target, int potionId) {
        Aspect aspect = requireAspect(tag);
        State previous = STATES.get(aspect);
        if (previous == null) return () -> {};
        List<Rule> rules = new ArrayList<>(previous.rules);
        rules.removeIf(rule -> rule.target.equals(target) && rule.potionId == potionId);
        return edit(aspect, new State(previous.original, previous.keepOriginal, rules));
    }

    private static Runnable edit(Aspect aspect, State next) {
        checkOwnership(aspect);
        // Ordinary scripts execute after Gadomancy's post-init. Fail before touching any state if misused early.
        if (ResearchCategories.getResearchList("gadomancy") == null) {
            throw new IllegalStateException(
                "Aura Pylon rules must run in ordinary scripts after Gadomancy initialization");
        }
        State previous = STATES.put(aspect, next);
        publish(aspect, next);
        return new Runnable() {

            private boolean undone;

            @Override
            public void run() {
                synchronized (AuraPylonRegistry.class) {
                    if (undone) return;
                    checkOwnership(aspect);
                    if (STATES.get(aspect) != next)
                        throw new IllegalStateException("Aura Pylon actions must be undone in reverse order");
                    if (previous == null) {
                        putEffect(aspect, next.original);
                        STATES.remove(aspect);
                        PUBLISHED.remove(aspect);
                        removeOwnedResearch(aspect);
                    } else {
                        STATES.put(aspect, previous);
                        publish(aspect, previous);
                    }
                    undone = true;
                }
            }
        };
    }

    public static synchronized void refresh() {
        for (Map.Entry<Aspect, State> entry : STATES.entrySet()) {
            checkOwnership(entry.getKey());
            publish(entry.getKey(), entry.getValue());
        }
    }

    private static void checkOwnership(Aspect aspect) {
        if (PUBLISHED.containsKey(aspect) && AuraEffectHandler.registeredEffects.get(aspect) != PUBLISHED.get(aspect)) {
            throw new IllegalStateException(
                "Another mod replaced the managed Aura Pylon effect for " + aspect.getTag());
        }
    }

    private static void publish(Aspect aspect, State state) {
        AuraEffect effect = !AuraPylonFeature.isEnabled() ? state.original
            : state.rules.isEmpty() ? (state.keepOriginal ? state.original : null) : state;
        putEffect(aspect, effect);
        PUBLISHED.put(aspect, effect);
        if (effect != null) {
            String key = String.format(AuraResearchManager.TC_AURA_RESEARCH_STR, aspect.getTag());
            if (ResearchCategories.getResearch(key) == null) {
                ResearchItem research = new ResearchItem(key, "gadomancy");
                research.registerResearchItem();
                OWNED_RESEARCH.put(aspect, research);
                AuraResearchCacheCompat.update(key, research);
            }
        } else {
            removeOwnedResearch(aspect);
        }
    }

    private static void putEffect(Aspect aspect, AuraEffect effect) {
        if (effect == null) AuraEffectHandler.registeredEffects.remove(aspect);
        else AuraEffectHandler.registeredEffects.put(aspect, effect);
    }

    private static void removeOwnedResearch(Aspect aspect) {
        ResearchItem owned = OWNED_RESEARCH.remove(aspect);
        ResearchCategoryList category = ResearchCategories.getResearchList("gadomancy");
        if (owned != null && category != null && category.research.get(owned.key) == owned) {
            category.research.remove(owned.key);
            AuraResearchCacheCompat.update(owned.key, null);
        }
    }

    /** Return true only for our marker. The native path handles untouched/disabled aspects. */
    public static boolean distribute(Aspect aspect, World world, double x, double y, double z, int tick) {
        AuraEffect current = AuraEffectHandler.registeredEffects.get(aspect);
        if (!(current instanceof State)) return false;
        if (world.isRemote || AuraResearchManager.isBlacklisted(aspect)) return true;
        State state = (State) current;
        double researchRange = -1;
        if (state.keepOriginal && state.original != null && tick % state.original.getTickInterval() == 0) {
            dispatch(state.original, world, x, y, z);
            researchRange = state.original.getRange();
        }
        for (Rule rule : state.rules) {
            if (tick % rule.getTickInterval() != 0) continue;
            dispatch(rule, world, x, y, z);
            researchRange = Math.max(researchRange, rule.getRange());
        }
        if (researchRange >= 0) {
            AxisAlignedBB box = AxisAlignedBB.getBoundingBox(x - .5, y - .5, z - .5, x + .5, y + .5, z + .5)
                .expand(researchRange, researchRange, researchRange);
            for (Object entity : world.getEntitiesWithinAABB(EntityPlayer.class, box)) {
                EntityPlayer player = (EntityPlayer) entity;
                String key = String.format(AuraResearchManager.TC_AURA_RESEARCH_STR, aspect.getTag());
                boolean known = ResearchManager.isResearchComplete(player.getCommandSenderName(), key);
                AuraResearchManager.tryUnlockAuraEffect(player, aspect);
                // Gadomancy sends a notification but does not send Thaumcraft's completion packet.
                if (!known && player instanceof EntityPlayerMP
                    && ResearchManager.isResearchComplete(player.getCommandSenderName(), key)) {
                    PacketHandler.INSTANCE.sendTo(new PacketResearchComplete(key), (EntityPlayerMP) player);
                }
            }
        }
        return true;
    }

    private static void dispatch(AuraEffect effect, World world, double x, double y, double z) {
        if (effect.getEffectType() != AuraEffect.EffectType.BLOCK_EFFECT)
            AuraEffectHandlerInvoker.td$entities(effect, world, x, y, z);
        if (effect.getEffectType() != AuraEffect.EffectType.ENTITY_EFFECT)
            AuraEffectHandlerInvoker.td$blocks(effect, world, x, y, z);
    }

    public static final class Rule extends PotionDistributionEffect {

        private final String target;
        private final int potionId;
        private final double range;
        private final String key;

        private Rule(String target, Potion potion, int amplifier, int addedTicks, int maxTicks, int intervalTicks,
            double range) {
            super(potion, intervalTicks, addedTicks, maxTicks, amplifier);
            this.target = target;
            this.potionId = potion.id;
            this.range = range;
            key = target + ":"
                + potionId
                + ":"
                + amplifier
                + ":"
                + addedTicks
                + ":"
                + maxTicks
                + ":"
                + intervalTicks
                + ":"
                + range;
        }

        @Override
        public boolean isEntityApplicable(Entity entity) {
            if (!(entity instanceof EntityLivingBase)) return false;
            if ("players".equals(target)) return entity instanceof EntityPlayer;
            if ("living".equals(target)) return true;
            if ("undead".equals(target)) return ((EntityLivingBase) entity).isEntityUndead();
            return target.substring(7)
                .equals(EntityList.getEntityString(entity));
        }

        @Override
        public double getRange() {
            return range;
        }
    }

    /** Registry marker; the dispatcher mixin preserves each child's own tick interval and range. */
    private static final class State extends AuraEffect.EntityAuraEffect {

        private final AuraEffect original;
        private final boolean keepOriginal;
        private final List<Rule> rules;

        private State(AuraEffect original, boolean keepOriginal, List<Rule> rules) {
            this.original = original;
            this.keepOriginal = keepOriginal;
            this.rules = Collections.unmodifiableList(new ArrayList<>(rules));
        }

        @Override
        public boolean isEntityApplicable(Entity entity) {
            return false;
        }

        @Override
        public void doEntityEffect(ChunkCoordinates origin, Entity entity) {}
    }
}
