package org.fentanylsolutions.thaumicdabblery;

import org.fentanylsolutions.thaumicdabblery.compat.modtweaker.ResearchTabOrderClientHandler;
import org.fentanylsolutions.thaumicdabblery.feature.researcheditor.ResearchEditorClient;
import org.fentanylsolutions.thaumicdabblery.feature.scanall.ScanAllSources;
import org.fentanylsolutions.thaumicdabblery.feature.visdiscount.VisDiscountTooltipHandler;
import org.fentanylsolutions.thaumicdabblery.feature.wandcomponents.WandComponentStatsTooltipHandler;
import org.fentanylsolutions.thaumicdabblery.feature.witcherybranch.WitcheryBranchFeature;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInitializationEvent;

@SuppressWarnings("unused")
public class ClientProxy extends CommonProxy {

    @Override
    public boolean toggleResearchEditor() {
        if (!Loader.isModLoaded("MineTweaker3") || !Loader.isModLoaded("modtweaker2"))
            throw new IllegalArgumentException("Thaumonomicon editing requires MineTweaker and ModTweaker.");
        return ResearchEditorClient.toggle();
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        ScanAllSources.register();
        VisDiscountTooltipHandler.register();
        WandComponentStatsTooltipHandler.register();
        WitcheryBranchFeature.registerClientHandler();
        if (Loader.isModLoaded("MineTweaker3") && Loader.isModLoaded("modtweaker2")) {
            ResearchTabOrderClientHandler.register();
            ResearchEditorClient.register();
        }
    }
}
