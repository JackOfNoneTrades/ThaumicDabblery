package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumcraft;

import java.util.LinkedHashMap;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import org.fentanylsolutions.thaumicdabblery.compat.modtweaker.ResearchTabGateRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import thaumcraft.api.research.ResearchCategoryList;
import thaumcraft.client.gui.GuiResearchBrowser;

@Mixin(value = GuiResearchBrowser.class, remap = false)
public abstract class MixinGuiResearchBrowser {

    @Shadow
    private static String selectedCategory;

    @Inject(method = "updateResearch", at = @At("HEAD"))
    private void thaumicdabblery$selectVisibleCategory(CallbackInfo ci) {
        Set<String> visible = ResearchTabGateRegistry
            .getVisibleCategories(thaumicdabblery$getCategoryKeys(), thaumicdabblery$getPlayerName());
        if (!visible.isEmpty() && !visible.contains(selectedCategory)) {
            selectedCategory = visible.iterator()
                .next();
        }
    }

    @Redirect(
        method = { "updateResearch", "drawScreen", "func_73863_a", "genResearchBackground", "mouseClicked",
            "func_73864_a" },
        at = @At(value = "INVOKE", target = "Ljava/util/LinkedHashMap;keySet()Ljava/util/Set;"),
        require = 4)
    private Set<String> thaumicdabblery$hideResearchCategories(LinkedHashMap<String, ResearchCategoryList> categories) {
        return ResearchTabGateRegistry.getVisibleCategories(categories.keySet(), thaumicdabblery$getPlayerName());
    }

    private static Set<String> thaumicdabblery$getCategoryKeys() {
        return thaumcraft.api.research.ResearchCategories.researchCategories.keySet();
    }

    private static String thaumicdabblery$getPlayerName() {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        return player == null ? null : player.getCommandSenderName();
    }
}
