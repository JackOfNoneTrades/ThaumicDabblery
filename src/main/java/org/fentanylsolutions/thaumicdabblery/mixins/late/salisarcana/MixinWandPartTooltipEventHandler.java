package org.fentanylsolutions.thaumicdabblery.mixins.late.salisarcana;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.oredict.OreDictionary;

import org.fentanylsolutions.thaumicdabblery.feature.wandcomponents.WandComponentVisDiscountFeature;
import org.fentanylsolutions.thaumicdabblery.feature.wandcomponents.WandComponentVisDiscountRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.wands.WandCap;

@Pseudo
@Mixin(targets = "dev.rndmorris.salisarcana.lib.WandPartTooltipEventHandler", remap = false)
public abstract class MixinWandPartTooltipEventHandler {

    @Unique
    private static final Aspect[] PRIMALS = { Aspect.AIR, Aspect.EARTH, Aspect.FIRE, Aspect.WATER, Aspect.ORDER,
        Aspect.ENTROPY };

    @Inject(method = "renderTooltip", at = @At("RETURN"), require = 1)
    private void thaumicdabblery$showScriptedCapDiscounts(ItemTooltipEvent event, CallbackInfo callbackInfo) {
        if (!WandComponentVisDiscountFeature.isEnabled() || event == null || event.itemStack == null) {
            return;
        }

        WandCap cap = findCap(event.itemStack);
        if (cap == null || !hasScriptedDiscount(cap)) {
            return;
        }

        List<String> originalLines = createNativeDiscountLines(cap);
        int insertionIndex = findLines(event.toolTip, originalLines);
        if (insertionIndex < 0) {
            return;
        }

        event.toolTip.subList(insertionIndex, insertionIndex + originalLines.size())
            .clear();
        event.toolTip.addAll(insertionIndex, createEffectiveDiscountLines(cap));
    }

    @Unique
    private static WandCap findCap(ItemStack stack) {
        for (WandCap cap : WandCap.caps.values()) {
            if (cap == null || cap.getItem() == null) {
                continue;
            }

            ItemStack capStack = cap.getItem();
            if (capStack.getItem() == stack.getItem() && (capStack.getItemDamage() == stack.getItemDamage()
                || capStack.getItemDamage() == OreDictionary.WILDCARD_VALUE)) {
                return cap;
            }
        }
        return null;
    }

    @Unique
    private static boolean hasScriptedDiscount(WandCap cap) {
        for (Aspect aspect : PRIMALS) {
            if (WandComponentVisDiscountRegistry.getCapDiscount(cap.getTag(), aspect) != null) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private static int findLines(List<String> tooltip, List<String> lines) {
        int lastStart = tooltip.size() - lines.size();
        for (int start = 0; start <= lastStart; start++) {
            boolean matches = true;
            for (int offset = 0; offset < lines.size(); offset++) {
                if (!tooltip.get(start + offset)
                    .equals(lines.get(offset))) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return start;
            }
        }
        return -1;
    }

    @Unique
    private static List<String> createNativeDiscountLines(WandCap cap) {
        List<String> lines = new ArrayList<>();
        float baseModifier = cap.getBaseCostModifier();
        float specialModifier = cap.getSpecialCostModifier();
        List<Aspect> specialAspects = cap.getSpecialCostModifierAspects();

        if (specialModifier == 0.0F || specialAspects == null || specialAspects.isEmpty()) {
            lines.add(createGenericLine(baseModifier));
            return lines;
        }

        StringJoiner specials = new StringJoiner(", ");
        StringJoiner normals = new StringJoiner(", ");
        for (Aspect aspect : PRIMALS) {
            (specialAspects.contains(aspect) ? specials : normals).add(formatAspect(aspect));
        }
        lines.add(createAspectLine(specials.toString(), specialModifier));
        lines.add(createAspectLine(normals.toString(), baseModifier));
        return lines;
    }

    @Unique
    private static List<String> createEffectiveDiscountLines(WandCap cap) {
        Map<String, AspectGroup> groups = new LinkedHashMap<>();
        for (Aspect aspect : PRIMALS) {
            float modifier = getEffectiveModifier(cap, aspect);
            String formattedDiscount = formatDiscount(modifier);
            AspectGroup group = groups.get(formattedDiscount);
            if (group == null) {
                group = new AspectGroup(modifier, formattedDiscount);
                groups.put(formattedDiscount, group);
            }
            group.aspects.add(formatAspect(aspect));
        }

        List<String> lines = new ArrayList<>();
        if (groups.size() == 1) {
            lines.add(
                createGenericLine(
                    groups.values()
                        .iterator()
                        .next().modifier));
            return lines;
        }

        for (AspectGroup group : groups.values()) {
            StringJoiner aspects = new StringJoiner(", ");
            for (String aspect : group.aspects) {
                aspects.add(aspect);
            }
            lines.add(
                StatCollector.translateToLocalFormatted(
                    "salisarcana:wand_cap.vis_discount.aspect",
                    aspects.toString(),
                    group.formattedDiscount));
        }
        return lines;
    }

    @Unique
    private static float getEffectiveModifier(WandCap cap, Aspect aspect) {
        Integer scriptedDiscount = WandComponentVisDiscountRegistry.getCapDiscount(cap.getTag(), aspect);
        if (scriptedDiscount != null) {
            return 1.0F - scriptedDiscount / 100.0F;
        }

        List<Aspect> specialAspects = cap.getSpecialCostModifierAspects();
        if (specialAspects != null && specialAspects.contains(aspect)) {
            return cap.getSpecialCostModifier();
        }
        return cap.getBaseCostModifier();
    }

    @Unique
    private static String createGenericLine(float modifier) {
        return StatCollector
            .translateToLocalFormatted("salisarcana:wand_cap.vis_discount.generic", formatDiscount(modifier));
    }

    @Unique
    private static String createAspectLine(String aspects, float modifier) {
        return StatCollector
            .translateToLocalFormatted("salisarcana:wand_cap.vis_discount.aspect", aspects, formatDiscount(modifier));
    }

    @Unique
    private static String formatDiscount(float modifier) {
        return (modifier > 1.0F ? "\u00a74" : "\u00a72") + NumberFormat.getPercentInstance()
            .format(1.0F - modifier);
    }

    @Unique
    private static String formatAspect(Aspect aspect) {
        return "\u00a7" + aspect.getChatcolor() + aspect.getName();
    }

    private static final class AspectGroup {

        private final float modifier;
        private final String formattedDiscount;
        private final List<String> aspects = new ArrayList<>();

        private AspectGroup(float modifier, String formattedDiscount) {
            this.modifier = modifier;
            this.formattedDiscount = formattedDiscount;
        }
    }
}
