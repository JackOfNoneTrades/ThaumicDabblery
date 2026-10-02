package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumcraft;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentTranslation;

import org.fentanylsolutions.thaumicdabblery.feature.customaspects.PrimalDiscovery;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.ReflectionHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.lib.network.playerdata.PacketAspectCombinationToServer;
import thaumcraft.common.lib.research.ResearchManager;

@Mixin(value = PacketAspectCombinationToServer.class, remap = false)
public abstract class MixinAspectCombination {

    @Inject(
        method = "onMessage(Lthaumcraft/common/lib/network/playerdata/PacketAspectCombinationToServer;Lcpw/mods/fml/common/network/simpleimpl/MessageContext;)Lcpw/mods/fml/common/network/simpleimpl/IMessage;",
        at = @At("HEAD"),
        cancellable = true)
    private void td$requireDiscovery(PacketAspectCombinationToServer message, MessageContext context,
        CallbackInfoReturnable<IMessage> cir) {
        Aspect first = ReflectionHelper.getPrivateValue(PacketAspectCombinationToServer.class, message, "aspect1");
        Aspect second = ReflectionHelper.getPrivateValue(PacketAspectCombinationToServer.class, message, "aspect2");
        if (first == null || second == null) return;
        EntityPlayerMP player = context.getServerHandler().playerEntity;
        if (player == null) return;
        Aspect result = ResearchManager.getCombinationResult(first, second);
        if (!PrimalDiscovery.allowed(Thaumcraft.proxy.getPlayerKnowledge(), player.getCommandSenderName(), result)) {
            player.addChatMessage(new ChatComponentTranslation("thaumicdabblery.aspect.scanBeforeCombining"));
            // Cancel before Thaumcraft consumes either personal points or research-table bonus points.
            cir.setReturnValue(null);
        }
    }
}
