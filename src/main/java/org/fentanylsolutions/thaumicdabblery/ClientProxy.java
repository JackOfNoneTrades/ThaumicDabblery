package org.fentanylsolutions.thaumicdabblery;

import org.fentanylsolutions.thaumicdabblery.feature.visdiscount.VisDiscountTooltipHandler;
import org.fentanylsolutions.thaumicdabblery.feature.witcherybranch.WitcheryBranchFeature;

import cpw.mods.fml.common.event.FMLInitializationEvent;

@SuppressWarnings("unused")
public class ClientProxy extends CommonProxy {

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        VisDiscountTooltipHandler.register();
        WitcheryBranchFeature.registerClientHandler();
    }
}
