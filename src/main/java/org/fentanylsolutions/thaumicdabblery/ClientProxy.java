package org.fentanylsolutions.thaumicdabblery;

import net.minecraft.network.INetHandler;

import org.fentanylsolutions.thaumicdabblery.compat.modtweaker.ResearchTabOrderClientHandler;
import org.fentanylsolutions.thaumicdabblery.feature.effigyskins.EffigySkinClient;
import org.fentanylsolutions.thaumicdabblery.feature.effigyskins.EffigySkinNetwork;
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
    public void receiveEffigyBinding(EffigySkinNetwork.Binding binding, INetHandler source) {
        EffigySkinClient.receive(binding, source);
    }

    @Override
    public void receiveVatFacing(org.fentanylsolutions.thaumicdabblery.feature.vatfacing.VatFacingNetwork.Pose pose,
        INetHandler source) {
        org.fentanylsolutions.thaumicdabblery.feature.vatfacing.VatFacingClient.receive(pose, source);
    }

    @Override
    public void receiveVortexPreview(net.minecraft.nbt.NBTTagCompound data, INetHandler source) {
        org.fentanylsolutions.thaumicdabblery.feature.planarvortex.VortexFeedbackClient.receive(data, source);
    }

    @Override
    public boolean toggleResearchEditor() {
        if (!Loader.isModLoaded("MineTweaker3") || !Loader.isModLoaded("modtweaker2"))
            throw new IllegalArgumentException("Thaumonomicon editing requires MineTweaker and ModTweaker.");
        return ResearchEditorClient.toggle();
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        if (Loader.isModLoaded("ThaumicHorizons")) {
            EffigySkinClient.register();
            org.fentanylsolutions.thaumicdabblery.feature.planarvortex.VortexFeedbackClient.register();
        }
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
