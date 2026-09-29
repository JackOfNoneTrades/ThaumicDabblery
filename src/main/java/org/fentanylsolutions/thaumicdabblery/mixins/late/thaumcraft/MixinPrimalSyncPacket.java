package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumcraft;

import net.minecraft.entity.player.EntityPlayer;

import org.fentanylsolutions.thaumicdabblery.feature.customaspects.PrimalDiscovery;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.lib.network.playerdata.PacketSyncAspects;
import thaumcraft.common.lib.research.ResearchManager;

@Mixin(value = PacketSyncAspects.class, remap = false)
public abstract class MixinPrimalSyncPacket {

    @Redirect(
        method = "onMessage(Lthaumcraft/common/lib/network/playerdata/PacketSyncAspects;Lcpw/mods/fml/common/network/simpleimpl/MessageContext;)Lcpw/mods/fml/common/network/simpleimpl/IMessage;",
        at = @At(
            value = "INVOKE",
            target = "Lthaumcraft/common/lib/research/ResearchManager;completeAspect(Lnet/minecraft/entity/player/EntityPlayer;Lthaumcraft/api/aspects/Aspect;S)V"))
    private void td$serverKnowledge(ResearchManager manager, EntityPlayer player, Aspect aspect, short amount) {
        PrimalDiscovery.grant(player.getCommandSenderName(), aspect, () -> {
            manager.completeAspect(player, aspect, amount);
            return null;
        });
    }
}
