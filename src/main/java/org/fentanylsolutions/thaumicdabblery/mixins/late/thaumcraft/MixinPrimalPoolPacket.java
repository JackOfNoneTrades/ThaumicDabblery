package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumcraft;

import org.fentanylsolutions.thaumicdabblery.feature.customaspects.PrimalDiscovery;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.lib.network.playerdata.PacketAspectPool;
import thaumcraft.common.lib.research.PlayerKnowledge;

@Mixin(value = PacketAspectPool.class, remap = false)
public abstract class MixinPrimalPoolPacket {

    @Redirect(
        method = "onMessage(Lthaumcraft/common/lib/network/playerdata/PacketAspectPool;Lcpw/mods/fml/common/network/simpleimpl/MessageContext;)Lcpw/mods/fml/common/network/simpleimpl/IMessage;",
        at = @At(
            value = "INVOKE",
            target = "Lthaumcraft/common/lib/research/PlayerKnowledge;setAspectPool(Ljava/lang/String;Lthaumcraft/api/aspects/Aspect;S)Z"))
    private boolean td$serverPool(PlayerKnowledge knowledge, String player, Aspect aspect, short amount) {
        // Denied generic grants can still send a zero-total packet. Do not discover an unknown aspect from it.
        return amount > 0 ? PrimalDiscovery.grant(player, aspect, () -> knowledge.setAspectPool(player, aspect, amount))
            : knowledge.setAspectPool(player, aspect, amount);
    }
}
