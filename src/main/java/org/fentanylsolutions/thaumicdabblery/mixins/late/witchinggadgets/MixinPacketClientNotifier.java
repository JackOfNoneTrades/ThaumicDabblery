package org.fentanylsolutions.thaumicdabblery.mixins.late.witchinggadgets;

import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import thaumcraft.api.research.ResearchCategories;

/** Covers the legacy Witching Gadgets client notifier used by 1.2.x releases. */
@Pseudo
@Mixin(targets = "witchinggadgets.common.util.network.PacketClientNotifier", remap = false)
public abstract class MixinPacketClientNotifier {

    @Inject(method = "handleClientSide", at = @At("HEAD"), cancellable = true, require = 1)
    private void thaumicdabblery$skipMissingResearchCategory(EntityPlayer player, CallbackInfo ci) {
        if (ResearchCategories.getResearchList("WITCHGADG") == null) {
            ci.cancel();
        }
    }
}
