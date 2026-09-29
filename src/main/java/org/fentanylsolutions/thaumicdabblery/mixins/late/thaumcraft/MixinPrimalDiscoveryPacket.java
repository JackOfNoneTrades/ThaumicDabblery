package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumcraft;

import org.fentanylsolutions.thaumicdabblery.feature.customaspects.PrimalDiscovery;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.lib.network.playerdata.PacketAspectDiscovery;
import thaumcraft.common.lib.research.PlayerKnowledge;

@Mixin(value = PacketAspectDiscovery.class, remap = false)
public abstract class MixinPrimalDiscoveryPacket {

    @Redirect(
        method = "onMessage(Lthaumcraft/common/lib/network/playerdata/PacketAspectDiscovery;Lcpw/mods/fml/common/network/simpleimpl/MessageContext;)Lcpw/mods/fml/common/network/simpleimpl/IMessage;",
        at = @At(
            value = "INVOKE",
            target = "Lthaumcraft/common/lib/research/PlayerKnowledge;addDiscoveredAspect(Ljava/lang/String;Lthaumcraft/api/aspects/Aspect;)Z"))
    private boolean td$serverDiscovery(PlayerKnowledge knowledge, String player, Aspect aspect) {
        return PrimalDiscovery.grant(player, aspect, () -> knowledge.addDiscoveredAspect(player, aspect));
    }
}
