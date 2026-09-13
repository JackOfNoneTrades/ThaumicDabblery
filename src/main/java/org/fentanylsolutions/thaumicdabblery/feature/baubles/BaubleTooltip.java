package org.fentanylsolutions.thaumicdabblery.feature.baubles;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public final class BaubleTooltip {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void tooltip(ItemTooltipEvent event) {
        if (!BaubleRules.hasRule(event.itemStack)) return;
        // Remove only the standard Expanded slot-information block, not addon descriptions.
        String header = StatCollector.translateToLocal("tooltip.compatibleslots");
        int start = event.toolTip.indexOf(header);
        if (start >= 0) {
            Set<String> nativeNames = new HashSet<>();
            for (String type : baubles.common.BaublesConfig
                .getDisplayTypesWithUniversalOverrides(BaubleRules.nativeTypes(event.itemStack))) {
                String label = StatCollector.translateToLocal("slot." + type);
                nativeNames.add(label);
                nativeNames.add(label + ",");
            }
            event.toolTip.remove(start);
            while (start < event.toolTip.size() && nativeNames.contains(event.toolTip.get(start)))
                event.toolTip.remove(start);
        }
        event.toolTip.remove(StatCollector.translateToLocal("tooltip.shiftprompt"));
        List<String> labels = new ArrayList<>();
        for (String type : baubles.common.BaublesConfig
            .getDisplayTypesWithUniversalOverrides(BaubleRules.types(event.itemStack))) {
            String key = "slot." + type;
            labels.add(StatCollector.canTranslate(key) ? StatCollector.translateToLocal(key) : type);
        }
        event.toolTip.add(
            EnumChatFormatting.GRAY + (labels.isEmpty()
                ? StatCollector.translateToLocal("thaumicdabblery.baubles.disabled")
                : StatCollector.translateToLocalFormatted("thaumicdabblery.baubles.slots", String.join(", ", labels))));
    }
}
