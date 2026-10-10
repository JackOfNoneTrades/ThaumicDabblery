package org.fentanylsolutions.thaumicdabblery.mixins.early;

import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.ServerCommandManager;
import net.minecraft.util.ChatComponentTranslation;

import org.fentanylsolutions.thaumicdabblery.feature.warpevents.WarpCommandSender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerCommandManager.class)
public abstract class MixinWarpCommandFeedback {

    @Inject(method = "func_152372_a", at = @At("HEAD"), cancellable = true, require = 1)
    private void td$quietWarp(ICommandSender sender, ICommand command, int flags, String key, Object[] arguments,
        CallbackInfo ci) {
        if (sender instanceof WarpCommandSender) {
            sender.addChatMessage(new ChatComponentTranslation(key, arguments));
            ci.cancel();
        }
    }
}
