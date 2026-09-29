package org.fentanylsolutions.thaumicdabblery.feature.champions;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.entity.EntityList;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import org.fentanylsolutions.thaumicdabblery.ThaumicDabblery;
import org.fentanylsolutions.thaumicdabblery.feature.Feature;
import org.fentanylsolutions.thaumicdabblery.feature.FeatureConfig;

import cpw.mods.fml.common.event.FMLServerStartingEvent;
import thaumcraft.common.config.Config;
import thaumcraft.common.config.ConfigEntities;
import thaumcraft.common.entities.monster.boss.EntityThaumcraftBoss;
import thaumcraft.common.entities.monster.mods.ChampionModifier;
import thaumcraft.common.lib.utils.EntityUtils;

/** Server-side policy only; Thaumcraft still supplies champion effects, attributes, names and drops. */
public final class ChampionMobsFeature implements Feature {

    public static final String ID = "championMobs";
    private static volatile Settings settings = new Settings(
        false,
        false,
        true,
        1,
        Collections.emptySet(),
        Collections.emptySet());
    private static final Set<Class<?>> WARNED_CLASSES = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Override
    public String id() {
        return ID;
    }

    public static boolean isEnabled() {
        return settings.enabled;
    }

    @Override
    public void configure(Configuration config) {
        String category = FeatureConfig.category(ID);
        String languageKey = ThaumicDabblery.MODID + ".config.feature." + ID;
        config.setCategoryLanguageKey(category, languageKey);
        config.getCategory(category)
            .remove("enabled");
        Property rulesProperty = config.get(
            category,
            "applyCustomRules",
            false,
            "Apply custom champion mob rules from this section.\n"
                + "false: Thaumcraft controls champions normally; champions can still spawn.\n"
                + "true: Apply the eligibility lists and chance settings below.\n"
                + "Only affects mobs that have not been checked yet. Existing mobs are unchanged.\n"
                + "Uses the server's configuration, including in singleplayer.");
        rulesProperty.setLanguageKey(languageKey + ".applyCustomRules");
        boolean enabled = rulesProperty.getBoolean(false);
        Property modeProperty = config.get(
            category,
            "mode",
            "whitelist",
            "whitelist: listed mobs (plus optional existing whitelist). blacklist: all supported mobs except listed IDs.");
        modeProperty.setValidValues(new String[] { "whitelist", "blacklist" });
        String mode = modeProperty.getString()
            .trim()
            .toLowerCase(Locale.ROOT);
        if (!"whitelist".equals(mode) && !"blacklist".equals(mode)) {
            ThaumicDabblery.LOG.warn("Unknown champion mode '{}'; using whitelist.", mode);
            mode = "whitelist";
        }
        modeProperty.set(mode);
        boolean includeExisting = config
            .get(
                category,
                "includeExistingWhitelist",
                true,
                "In whitelist mode, also allow Thaumcraft/addon whitelist entries, including their subclasses.")
            .getBoolean(true);
        Property chanceProperty = config.get(
            category,
            "baseChancePercent",
            1.0,
            "Base chance in percent (0..100, fractions allowed), before native difficulty/location and mob-weight bonuses. "
                + "0 can still produce champions where bonuses apply. Use alwaysChampions for an unconditional chance.",
            0.0,
            100.0);
        double chance = chanceProperty.getDouble(1.0);
        if (!Double.isFinite(chance)) chance = 1.0;
        chance = Math.max(0.0, Math.min(100.0, chance));
        chanceProperty.set(chance);
        Set<String> entities = names(
            config.get(
                category,
                "entities",
                new String[0],
                "Exact, case-sensitive 1.7.10 entity IDs, e.g. Creeper or WitherBoss. Only EntityMob subclasses are supported.")
                .getStringList());
        Set<String> always = names(
            config.get(
                category,
                "alwaysChampions",
                new String[0],
                "Exact entity IDs guaranteed on their first champion check. Overrides the list filter, chance, minimum-health "
                    + "threshold and Thaumcraft's champion_mobs switch, but not structural safety checks. No reroll of saved mobs.")
                .getStringList());
        settings = new Settings(enabled, "blacklist".equals(mode), includeExisting, chance, entities, always);
    }

    private static Set<String> names(String[] values) {
        Set<String> result = new HashSet<>();
        for (String value : values) if (!value.trim()
            .isEmpty()) result.add(value.trim());
        return Collections.unmodifiableSet(result);
    }

    @Override
    public void serverStarting(FMLServerStartingEvent event) {
        validateEntries();
    }

    @Override
    public void onConfigReload() {
        validateEntries();
    }

    private static void validateEntries() {
        Settings policy = settings;
        if (!policy.enabled) return;
        Set<String> entries = new HashSet<>(policy.entities);
        entries.addAll(policy.always);
        for (String name : entries) {
            Object type = EntityList.stringToClassMapping.get(name);
            if (!(type instanceof Class)) {
                ThaumicDabblery.LOG.warn("Unknown champion entity ID '{}'; entry has no effect.", name);
            } else if (!EntityMob.class.isAssignableFrom((Class<?>) type)) {
                ThaumicDabblery.LOG
                    .warn("Unsupported champion entity '{}': not an EntityMob; entry has no effect.", name);
            }
        }
    }

    public static void checkSpawn(EntityMob mob, boolean dangerousLocation) {
        Settings policy = settings;
        if (!policy.enabled || mob.worldObj.isRemote) return;
        IAttributeInstance champion = mob.getEntityAttribute(EntityUtils.CHAMPION_MOD);
        if (champion == null) {
            warnUnsafe(mob);
            return;
        }
        // -2 = never checked, -1 = checked ordinary mob, >= 0 = champion. This is persisted by vanilla attributes.
        if (champion.getAttributeValue() >= -1.0) return;
        boolean nativeAllowed = false;
        int weightBonus = 0;
        for (Map.Entry<Class, Integer> entry : ConfigEntities.championModWhitelist.entrySet()) {
            if (!entry.getKey()
                .isInstance(mob)) continue;
            nativeAllowed = true;
            if (Config.championMobs || mob instanceof EntityThaumcraftBoss)
                weightBonus = Math.max(weightBonus, entry.getValue() - 1);
        }
        String name = EntityList.getEntityString(mob);
        boolean always = policy.always.contains(name);
        boolean allowed = policy.blacklist ? !policy.entities.contains(name)
            : policy.entities.contains(name) || (policy.includeExisting && nativeAllowed);
        if (!mob.isDead && (always || allowed)) {
            IAttributeInstance health = mob.getEntityAttribute(SharedMonsterAttributes.maxHealth);
            if (health == null || mob.getEntityAttribute(SharedMonsterAttributes.attackDamage) == null
                || mob.getEntityAttribute(SharedMonsterAttributes.movementSpeed) == null) {
                warnUnsafe(mob);
            } else if (always || (health.getBaseValue() >= 10.0 && mob.worldObj.rand.nextDouble() * 100.0 < Math
                .max(0.0, Math.min(100.0, policy.chance + weightBonus + environmentBonus(mob, dangerousLocation))))) {
                    EntityUtils.makeChampion(mob, false);
                    return;
                }
        }
        champion.removeModifier(ChampionModifier.ATTRIBUTE_MOD_NONE);
        champion.applyModifier(ChampionModifier.ATTRIBUTE_MOD_NONE);
    }

    private static double environmentBonus(EntityMob mob, boolean dangerousLocation) {
        double bonus = 0;
        if (mob.worldObj.difficultySetting == EnumDifficulty.EASY || !Config.championMobs) bonus -= 2;
        if (mob.worldObj.difficultySetting == EnumDifficulty.HARD && Config.championMobs) bonus += 2;
        if (mob.worldObj.provider.dimensionId == Config.dimensionOuterId) bonus += 3;
        BiomeGenBase biome = mob.worldObj
            .getBiomeGenForCoords(MathHelper.floor_double(mob.posX), MathHelper.floor_double(mob.posZ));
        if (BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.SPOOKY)
            || BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.NETHER)
            || BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.END)) bonus += Config.championMobs ? 2 : 1;
        if (dangerousLocation) bonus += Config.championMobs ? 10 : 3;
        return bonus;
    }

    private static void warnUnsafe(EntityMob mob) {
        if (WARNED_CLASSES.add(mob.getClass())) ThaumicDabblery.LOG.warn(
            "Cannot safely make '{}' ({}) a champion: missing required attributes.",
            EntityList.getEntityString(mob),
            mob.getClass()
                .getName());
    }

    private static final class Settings {

        final boolean enabled, blacklist, includeExisting;
        final double chance;
        final Set<String> entities, always;

        Settings(boolean enabled, boolean blacklist, boolean includeExisting, double chance, Set<String> entities,
            Set<String> always) {
            this.enabled = enabled;
            this.blacklist = blacklist;
            this.includeExisting = includeExisting;
            this.chance = chance;
            this.entities = entities;
            this.always = always;
        }
    }
}
