package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumictinkerer;

import org.fentanylsolutions.thaumicdabblery.feature.osmotic.OsmoticPackets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import thaumic.tinkerer.common.block.tile.TileEnchanter;
import thaumic.tinkerer.common.network.packet.PacketEnchanterStartWorking;
import thaumic.tinkerer.common.network.packet.PacketTile;

@Mixin(value = PacketEnchanterStartWorking.class, remap = false)
public abstract class MixinPacketEnchanterStartWorking extends PacketTile<TileEnchanter>
    implements OsmoticPackets.Request {

    public int[] thaumicdabblery$request() {
        return new int[] { dim, x, y, z };
    }

    @Inject(
        method = "onMessage(Lthaumic/tinkerer/common/network/packet/PacketEnchanterStartWorking;Lcpw/mods/fml/common/network/simpleimpl/MessageContext;)Lcpw/mods/fml/common/network/simpleimpl/IMessage;",
        at = @At("HEAD"),
        cancellable = true,
        require = 1)
    private void thaumicdabblery$validate(PacketEnchanterStartWorking message, MessageContext context,
        CallbackInfoReturnable<IMessage> cir) {
        OsmoticPackets.enqueue((OsmoticPackets.Request) message, context);
        cir.setReturnValue(null);
    }
}
