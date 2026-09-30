package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumcraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.feature.scanall.ScanAll;
import org.fentanylsolutions.thaumicdabblery.feature.scanall.ScanAllSources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.client.gui.GuiResearchRecipe;
import thaumcraft.common.CommonProxy;

@Mixin(value = GuiResearchRecipe.class, remap = false)
public abstract class MixinAllScanSources {

    @Shadow
    HashMap<Aspect, ArrayList<ItemStack>> aspectItems;

    @Redirect(
        method = "<init>",
        at = @At(value = "INVOKE", target = "Lthaumcraft/common/CommonProxy;getScannedObjects()Ljava/util/Map;"))
    private Map<String, ArrayList<String>> td$deferFullCatalog(CommonProxy proxy) {
        return ScanAll.itemsComplete(Minecraft.getMinecraft().thePlayer.getCommandSenderName()) ? Collections.emptyMap()
            : proxy.getScannedObjects();
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void td$useBatchedCatalog(ResearchItem research, int page, double x, double y, CallbackInfo ci) {
        String name = Minecraft.getMinecraft().thePlayer.getCommandSenderName();
        if ("ASPECTS".equals(research.key) && ScanAll.itemsComplete(name)) aspectItems = ScanAllSources.sources(name);
    }
}
