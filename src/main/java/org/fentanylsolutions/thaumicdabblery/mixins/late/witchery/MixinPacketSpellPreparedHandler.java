package org.fentanylsolutions.thaumicdabblery.mixins.late.witchery;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import org.fentanylsolutions.thaumicdabblery.ThaumicDabblery;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.emoniph.witchery.Witchery;
import com.emoniph.witchery.network.PacketSpellPrepared;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

@Mixin(value = PacketSpellPrepared.Handler.class, remap = false)
public abstract class MixinPacketSpellPreparedHandler {

    @Inject(
        method = "onMessage(Lcom/emoniph/witchery/network/PacketSpellPrepared;"
            + "Lcpw/mods/fml/common/network/simpleimpl/MessageContext;)"
            + "Lcpw/mods/fml/common/network/simpleimpl/IMessage;",
        at = @At("RETURN"))
    private void thaumicdabblery$logPreparedSpell(PacketSpellPrepared message, MessageContext context,
        CallbackInfoReturnable<IMessage> cir) {
        if (!ThaumicDabblery.isDebugMode()) {
            return;
        }
        EntityPlayer player = Witchery.proxy.getPlayer(context);
        NBTTagCompound data = player.getEntityData();
        ThaumicDabblery.debug(
            "[Mystic Branch/Witchery] Server handled PacketSpellPrepared for " + player.getCommandSenderName()
                + ": effectID="
                + data.getInteger("WITCSpellEffectID")
                + ", enhancedLevel="
                + data.getInteger("WITCSpellEffectEnhanced"));
    }
}
