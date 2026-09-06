package org.fentanylsolutions.thaumicdabblery.feature.wandcomponents;

import java.text.NumberFormat;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.oredict.OreDictionary;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.wands.WandRod;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandRodPrimalOnUpdate;

public final class WandComponentStatsTooltipHandler {

    private WandComponentStatsTooltipHandler() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new WandComponentStatsTooltipHandler());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTooltip(ItemTooltipEvent event) {
        if (!WandComponentStatsFeature.isEnabled() || event.itemStack == null) return;
        ItemStack stack = event.itemStack;
        WandRod rod;
        Integer potency;
        Map<Aspect, WandComponentStatsRegistry.Regeneration> regeneration;
        if (stack.getItem() instanceof ItemWandCasting) {
            rod = ((ItemWandCasting) stack.getItem()).getRod(stack);
            potency = WandComponentStatsRegistry.getPotency(stack);
            regeneration = WandComponentStatsRegistry.getRegeneration(stack);
        } else {
            rod = findCore(stack);
            if (rod == null) return;
            potency = WandComponentStatsRegistry.getCorePotency(rod.getTag());
            regeneration = WandComponentStatsRegistry.getCoreRegeneration(rod.getTag());
        }
        if (potency != null) {
            event.toolTip.remove(StatCollector.translateToLocal("salisarcana:wand_rod.runes"));
            if (potency > 0) {
                event.toolTip.add(StatCollector.translateToLocalFormatted("thaumicdabblery.wand.potency", potency));
            }
        }
        if (regeneration == null) return;
        if (rod != null && rod.getOnUpdate() != null
            && rod.getOnUpdate()
                .getClass() == WandRodPrimalOnUpdate.class) {
            removeTranslation(event.toolTip, "salisarcana:wand_rod.special." + rod.getTag());
        }
        if (regeneration.isEmpty()) {
            event.toolTip.add(StatCollector.translateToLocal("thaumicdabblery.wand.regeneration.disabled"));
        }
        for (Map.Entry<Aspect, WandComponentStatsRegistry.Regeneration> entry : regeneration.entrySet()) {
            WandComponentStatsRegistry.Regeneration rule = entry.getValue();
            event.toolTip.add(
                StatCollector.translateToLocalFormatted(
                    "thaumicdabblery.wand.regeneration",
                    entry.getKey()
                        .getName(),
                    NumberFormat.getNumberInstance()
                        .format(rule.amount / 100.0),
                    rule.interval,
                    rule.ceilingPercent));
        }
    }

    private static WandRod findCore(ItemStack stack) {
        for (WandRod rod : WandRod.rods.values()) {
            ItemStack component = rod.getItem();
            if (component != null && component.getItem() == stack.getItem()
                && (component.getItemDamage() == stack.getItemDamage()
                    || component.getItemDamage() == OreDictionary.WILDCARD_VALUE))
                return rod;
        }
        return null;
    }

    private static void removeTranslation(List<String> tooltip, String key) {
        if (StatCollector.canTranslate(key)) tooltip.remove(StatCollector.translateToLocal(key));
        for (int line = 1; StatCollector.canTranslate(key + "." + line); line++) {
            tooltip.remove(StatCollector.translateToLocal(key + "." + line));
        }
    }
}
