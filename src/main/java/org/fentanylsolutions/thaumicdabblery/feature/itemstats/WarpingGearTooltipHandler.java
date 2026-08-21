package org.fentanylsolutions.thaumicdabblery.feature.itemstats;

import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

final class WarpingGearTooltipHandler {

    static final WarpingGearTooltipHandler INSTANCE = new WarpingGearTooltipHandler();

    private WarpingGearTooltipHandler() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onItemTooltip(ItemTooltipEvent event) {
        if (!ThaumcraftItemStatsFeature.isEnabled()) {
            return;
        }

        Integer warp = WarpingGearRegistry.get(event.itemStack);
        if (warp != null && warp < 0) {
            event.toolTip
                .add(EnumChatFormatting.DARK_PURPLE + StatCollector.translateToLocal("item.warping") + " " + warp);
        }
    }
}
