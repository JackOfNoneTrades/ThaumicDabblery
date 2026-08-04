package org.fentanylsolutions.thaumicdabblery;

import org.fentanylsolutions.thaumicdabblery.feature.visdiscount.VisDiscountTooltipHandler;

import cpw.mods.fml.common.event.FMLInitializationEvent;

@SuppressWarnings("unused")
public class ClientProxy extends CommonProxy {

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        VisDiscountTooltipHandler.register();
    }
}
