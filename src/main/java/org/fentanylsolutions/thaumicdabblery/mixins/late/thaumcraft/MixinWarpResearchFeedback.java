package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumcraft;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.IChatComponent;

import org.fentanylsolutions.thaumicdabblery.feature.warpevents.WarpCommandSender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;

import thaumcraft.common.lib.events.CommandThaumcraft;

@Mixin(value = CommandThaumcraft.class, remap = false)
public abstract class MixinWarpResearchFeedback {

    @WrapWithCondition(
        method = { "giveAspect", "setWarp", "addWarp", "giveResearch", "giveAllResearch", "resetResearch" },
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/EntityPlayerMP;addChatMessage(Lnet/minecraft/util/IChatComponent;)V",
            remap = true),
        require = 7)
    private boolean td$quietCommand(EntityPlayerMP player, IChatComponent message,
        @Local(argsOnly = true) ICommandSender sender) {
        return !(sender instanceof WarpCommandSender);
    }
}
