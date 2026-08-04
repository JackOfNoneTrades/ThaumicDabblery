package org.fentanylsolutions.thaumicdabblery.feature.visdiscount;

import java.util.Iterator;
import java.util.Map;

import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import thaumcraft.api.aspects.Aspect;

public final class VisDiscountTooltipHandler {

    private static final VisDiscountTooltipHandler INSTANCE = new VisDiscountTooltipHandler();

    private VisDiscountTooltipHandler() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onItemTooltip(ItemTooltipEvent event) {
        if (!VisDiscountFeature.isEnabled()) {
            return;
        }

        VisDiscountRegistry.TooltipRule rule = VisDiscountRegistry.getTooltipRule(event.itemStack);
        if (rule.isEmpty()) {
            return;
        }

        String label = StatCollector.translateToLocal("tc.visdiscount");
        removeOverriddenNativeLines(event, label, rule);

        if (rule.getUniversal() != null) {
            event.toolTip.add(format(label, null, rule.getUniversal()));
        }
        for (Map.Entry<Aspect, Integer> entry : rule.getAspects()
            .entrySet()) {
            event.toolTip.add(format(label, entry.getKey(), entry.getValue()));
        }
    }

    private static void removeOverriddenNativeLines(ItemTooltipEvent event, String label,
        VisDiscountRegistry.TooltipRule rule) {
        Iterator<String> lines = event.toolTip.iterator();
        while (lines.hasNext()) {
            String line = EnumChatFormatting.getTextWithoutFormattingCodes(lines.next());
            if (line == null) {
                continue;
            }

            if (rule.getUniversal() != null && isVisDiscountLine(line, label)) {
                lines.remove();
                continue;
            }

            for (Aspect aspect : rule.getAspects()
                .keySet()) {
                if (line.startsWith(label + " (" + aspect.getName() + "):")) {
                    lines.remove();
                    break;
                }
            }
        }
    }

    private static boolean isVisDiscountLine(String line, String label) {
        return line.startsWith(label + ":") || line.startsWith(label + " (");
    }

    private static String format(String label, Aspect aspect, int discount) {
        String aspectSuffix = aspect == null ? "" : " (" + aspect.getName() + ")";
        return EnumChatFormatting.DARK_PURPLE + label + aspectSuffix + ": " + discount + "%";
    }
}
