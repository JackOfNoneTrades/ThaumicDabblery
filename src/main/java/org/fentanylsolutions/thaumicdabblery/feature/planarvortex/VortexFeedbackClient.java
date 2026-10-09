package org.fentanylsolutions.thaumicdabblery.feature.planarvortex;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.INetHandler;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.lib.UtilsFX;
import thaumcraft.common.items.wands.ItemWandCasting;

public final class VortexFeedbackClient {

    private static NBTTagCompound preview = new NBTTagCompound();
    private static INetHandler connection;
    private static long received;

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new VortexFeedbackClient());
    }

    public static void receive(NBTTagCompound data, INetHandler source) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.func_152344_a(() -> {
            if (mc.getNetHandler() != source) return;
            if (data != null && data.hasKey("craftStarted")) {
                VortexCraftingClient.receive(mc, data);
                return;
            }
            preview = data == null ? new NBTTagCompound() : data;
            connection = source;
            received = System.nanoTime();
        });
    }

    public static final class Cost {

        public final Aspect aspect;
        public final int amount;
        public final boolean missing;

        Cost(Aspect aspect, int amount, boolean missing) {
            this.aspect = aspect;
            this.amount = amount;
            this.missing = missing;
        }
    }

    public static List<Cost> visibleCosts(Minecraft mc) {
        List<Cost> costs = new ArrayList<>();
        if (mc.theWorld == null || mc.thePlayer == null
            || mc.currentScreen != null
            || mc.gameSettings.hideGUI
            || connection != mc.getNetHandler()
            || System.nanoTime() - received > 2000000000L
            || preview.hasNoTags()
            || preview.getInteger("dimension") != mc.theWorld.provider.dimensionId) return costs;
        ItemStack held = mc.thePlayer.getHeldItem();
        MovingObjectPosition hit = mc.objectMouseOver;
        if (held == null || !(held.getItem() instanceof ItemWandCasting)
            || hit == null
            || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK
            || hit.blockX != preview.getInteger("x")
            || hit.blockY != preview.getInteger("y")
            || hit.blockZ != preview.getInteger("z")) return costs;
        NBTTagCompound amounts = preview.getCompoundTag("costs"), missing = preview.getCompoundTag("missing");
        for (Object key : amounts.func_150296_c()) {
            String tag = (String) key;
            Aspect aspect = Aspect.getAspect(tag);
            if (aspect != null) costs.add(new Cost(aspect, amounts.getInteger(tag), missing.getBoolean(tag)));
        }
        costs.sort(Comparator.comparing(c -> c.aspect.getTag()));
        return costs;
    }

    public static String formatCost(int hundredths) {
        return BigDecimal.valueOf(hundredths, 2)
            .stripTrailingZeros()
            .toPlainString();
    }

    public static float alpha(boolean missing, float ticks) {
        return missing ? 0.45F + 0.25F * (float) Math.sin(ticks * 0.3F) : 1;
    }

    @SubscribeEvent
    public void render(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft mc = Minecraft.getMinecraft();
        List<Cost> costs = visibleCosts(mc);
        if (costs.isEmpty()) return;
        int columns = Math.min(6, costs.size()), rows = (costs.size() + columns - 1) / columns;
        int x = event.resolution.getScaledWidth() / 2 - (columns * 36 - 20) / 2;
        int y = Math.max(
            4,
            Math.min(event.resolution.getScaledHeight() / 2 + 22, event.resolution.getScaledHeight() - rows * 32 - 4));
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        try {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            for (int i = 0; i < costs.size(); i++) {
                Cost cost = costs.get(i);
                int cx = x + i % columns * 36, cy = y + i / columns * 32;
                float opacity = alpha(cost.missing, mc.thePlayer.ticksExisted + event.partialTicks);
                UtilsFX.drawTag(cx, cy, cost.aspect, 0, 0, 0, GL11.GL_ONE_MINUS_SRC_ALPHA, opacity, false);
                // Thaumcraft's helper re-enables lighting and disables blending on return.
                GL11.glDisable(GL11.GL_LIGHTING);
                GL11.glEnable(GL11.GL_BLEND);
                String number = formatCost(cost.amount);
                mc.fontRenderer.drawStringWithShadow(
                    number,
                    cx + 8 - mc.fontRenderer.getStringWidth(number) / 2,
                    cy + 18,
                    0xffffff);
            }
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }
}
