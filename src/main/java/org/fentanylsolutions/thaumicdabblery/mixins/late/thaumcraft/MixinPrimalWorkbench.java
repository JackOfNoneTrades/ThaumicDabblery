package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumcraft;

import java.util.ArrayList;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.util.StatCollector;

import org.fentanylsolutions.thaumicdabblery.feature.customaspects.PrimalDiscovery;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.client.gui.GuiArcaneWorkbench;
import thaumcraft.client.lib.UtilsFX;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import thaumcraft.common.tiles.TileArcaneWorkbench;

/** Retain the native six cost positions, paging additional explicit primal costs. */
@Mixin(value = GuiArcaneWorkbench.class, remap = false)
public abstract class MixinPrimalWorkbench extends GuiContainer {

    @Shadow
    private TileArcaneWorkbench tileEntity;
    @Shadow
    ArrayList<Aspect> primals;
    @Unique
    private int td$page;
    @Unique
    private int td$pages = 1;

    protected MixinPrimalWorkbench(Container container) {
        super(container);
    }

    @Inject(method = "drawGuiContainerBackgroundLayer", remap = true, at = @At("HEAD"))
    private void td$costPages(float ticks, int mouseX, int mouseY, CallbackInfo ci) {
        ArrayList<Aspect> all = PrimalDiscovery.nativePrimals();
        AspectList cost = ThaumcraftCraftingManager.findMatchingArcaneRecipeAspects(tileEntity, mc.thePlayer);
        if (cost != null) for (Aspect aspect : Aspect.getPrimalAspects())
            if (!all.contains(aspect) && cost.getAmount(aspect) > 0) all.add(aspect);
        td$pages = (all.size() + 5) / 6;
        td$page = Math.min(td$page, td$pages - 1);
        primals = new ArrayList<>(all.subList(td$page * 6, Math.min(all.size(), (td$page + 1) * 6)));
    }

    @Inject(method = "drawGuiContainerBackgroundLayer", remap = true, at = @At("RETURN"))
    private void td$pageLabel(float ticks, int mouseX, int mouseY, CallbackInfo ci) {
        if (td$pages > 1) {
            int x = (width - xSize) / 2 + 40, y = (height - ySize) / 2 + 4;
            String label = StatCollector.translateToLocalFormatted("thaumicdabblery.vis.page", td$page + 1, td$pages);
            drawRect(x - 2, y - 1, x + fontRendererObj.getStringWidth(label) + 2, y + 9, 0xB0000000);
            GL11.glColor4f(1, 1, 1, 1);
            fontRendererObj.drawStringWithShadow(label, x, y, 0xFFFFFF);
        }
    }

    @Override
    protected void mouseClicked(int x, int y, int button) {
        int left = (width - xSize) / 2, top = (height - ySize) / 2;
        if (td$pages > 1 && x >= left + 40 && x < left + 150 && y >= top && y < top + 16) {
            td$page = (td$page + (button == 1 ? td$pages - 1 : 1)) % td$pages;
            return;
        }
        super.mouseClicked(x, y, button);
    }

    @Redirect(
        method = "drawGuiContainerBackgroundLayer",
        remap = true,
        at = @At(
            value = "INVOKE",
            target = "Lthaumcraft/client/lib/UtilsFX;drawTag(IILthaumcraft/api/aspects/Aspect;FIDIFZ)V",
            remap = false))
    private void td$unknownCost(int x, int y, Aspect aspect, float amount, int bonus, double z, int blend, float alpha,
        boolean bw) {
        if (PrimalDiscovery.allowed(Thaumcraft.proxy.getPlayerKnowledge(), mc.thePlayer.getCommandSenderName(), aspect))
            UtilsFX.drawTag(x, y, aspect, amount, bonus, z, blend, alpha, bw);
        else {
            GL11.glColor4f(1, 1, 1, 1);
            fontRendererObj.drawStringWithShadow("?", x + 4, y, 0xDDDDDD);
            fontRendererObj.drawStringWithShadow(Float.toString(amount), x, y + 9, 0xDDDDDD);
        }
    }
}
