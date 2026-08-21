package org.fentanylsolutions.thaumicdabblery.mixins.late.witchinggadgets;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import thaumcraft.api.research.ResearchCategories;

/** Covers the SimpleImpl client notifier used by current Witching Gadgets releases. */
@Pseudo
@Mixin(targets = "witchinggadgets.common.util.network.message.MessageClientNotifier$HandlerClient", remap = false)
public abstract class MixinMessageClientNotifierHandler {

    @Inject(
        method = "onMessage(Lwitchinggadgets/common/util/network/message/MessageClientNotifier;"
            + "Lcpw/mods/fml/common/network/simpleimpl/MessageContext;)"
            + "Lcpw/mods/fml/common/network/simpleimpl/IMessage;",
        at = @At("HEAD"),
        cancellable = true,
        require = 1)
    private void thaumicdabblery$skipMissingResearchCategory(@Coerce Object message, MessageContext context,
        CallbackInfoReturnable<IMessage> cir) {
        if (ResearchCategories.getResearchList("WITCHGADG") == null) {
            cir.setReturnValue(null);
        }
    }
}
