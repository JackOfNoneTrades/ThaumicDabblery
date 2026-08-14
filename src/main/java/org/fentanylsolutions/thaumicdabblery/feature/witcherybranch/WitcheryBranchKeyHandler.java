package org.fentanylsolutions.thaumicdabblery.feature.witcherybranch;

import java.util.Arrays;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.fentanylsolutions.thaumicdabblery.ThaumicDabblery;
import org.lwjgl.input.Keyboard;

import com.emoniph.witchery.Witchery;
import com.emoniph.witchery.item.ItemMysticBranch;
import com.kentington.thaumichorizons.common.lib.EntityInfusionProperties;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;

public final class WitcheryBranchKeyHandler {

    private static final WitcheryBranchKeyHandler INSTANCE = new WitcheryBranchKeyHandler();
    private static final KeyBinding KEY = new KeyBinding(
        "key.thaumicdabblery.mysticBranch",
        Keyboard.KEY_B,
        "key.categories.thaumicdabblery");

    private static boolean registered;

    private boolean keyWasDown;
    private boolean virtualUseActive;
    private int debugHeartbeatTicks;

    private WitcheryBranchKeyHandler() {}

    static void register() {
        if (registered) {
            return;
        }
        ClientRegistry.registerKeyBinding(KEY);
        FMLCommonHandler.instance()
            .bus()
            .register(INSTANCE);
        registered = true;
        ThaumicDabblery.debug("[Mystic Branch/client] Registered key handler");
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent event) {
        if (event.phase != Phase.START) {
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        EntityClientPlayerMP player = minecraft.thePlayer;
        boolean keyDown = KEY.getIsKeyPressed();

        if (keyDown != keyWasDown) {
            debugState(keyDown ? "key pressed" : "key released", minecraft, player);
        }

        if (virtualUseActive && (!WitcheryBranchFeature.isActive() || player == null
            || !hasInfusion(player)
            || minecraft.currentScreen != null)) {
            cancel(player);
        } else if (keyDown && !keyWasDown) {
            if (canStart(minecraft, player)) {
                begin(player);
            } else {
                debugState("start rejected", minecraft, player);
            }
        } else if (!keyDown && keyWasDown && virtualUseActive) {
            finish(player);
        }

        if (virtualUseActive && ThaumicDabblery.isDebugMode() && ++debugHeartbeatTicks >= 20) {
            debugHeartbeatTicks = 0;
            debugState("held for another 20 ticks", minecraft, player);
        }

        keyWasDown = keyDown;
    }

    private static boolean canStart(Minecraft minecraft, EntityClientPlayerMP player) {
        return WitcheryBranchFeature.isActive() && WitcheryBranchCompat.isRegistered()
            && player != null
            && minecraft.currentScreen == null
            && minecraft.inGameHasFocus
            && !player.isUsingItem()
            && hasInfusion(player);
    }

    private static boolean hasInfusion(EntityClientPlayerMP player) {
        EntityInfusionProperties properties = (EntityInfusionProperties) player
            .getExtendedProperties(EntityInfusionProperties.EXT_PROP_NAME);
        return properties != null && properties.hasPlayerInfusion(WitcheryBranchFeature.INFUSION_ID);
    }

    private void begin(EntityClientPlayerMP player) {
        ThaumicDabblery.debug("[Mystic Branch/client] Beginning virtual use");
        ItemStack branch = new ItemStack(Witchery.Items.MYSTIC_BRANCH);
        VirtualItemUseState.begin(player, branch);
        ((ItemMysticBranch) Witchery.Items.MYSTIC_BRANCH).onItemRightClick(branch, player.worldObj, player);
        virtualUseActive = true;
        debugHeartbeatTicks = 0;
        debugState("after Witchery onItemRightClick", Minecraft.getMinecraft(), player);
        WitcheryBranchNetwork.send(WitcheryBranchNetwork.Action.START);
    }

    private void finish(EntityClientPlayerMP player) {
        debugState("finishing virtual use", Minecraft.getMinecraft(), player);
        boolean wasVirtualUse = player != null && isUsingVirtualBranch(player);
        if (wasVirtualUse && player.isUsingItem()) {
            player.stopUsingItem();
        }
        VirtualItemUseState.end(player);
        if (wasVirtualUse && player.isUsingItem()) {
            player.clearItemInUse();
        }
        virtualUseActive = false;
        debugHeartbeatTicks = 0;
        WitcheryBranchNetwork.send(WitcheryBranchNetwork.Action.FINISH);
    }

    private void cancel(EntityClientPlayerMP player) {
        debugState("cancelling virtual use", Minecraft.getMinecraft(), player);
        boolean wasVirtualUse = player != null && isUsingVirtualBranch(player);
        boolean wasUsingBranch = wasVirtualUse && player.getItemInUse() != null
            && player.getItemInUse()
                .getItem() == Witchery.Items.MYSTIC_BRANCH;
        if (wasVirtualUse) {
            player.getEntityData()
                .removeTag("Strokes");
            player.getEntityData()
                .removeTag("startYaw");
            player.getEntityData()
                .removeTag("startPitch");
        }
        VirtualItemUseState.end(player);
        if (wasUsingBranch) {
            player.clearItemInUse();
        }
        if (virtualUseActive) {
            WitcheryBranchNetwork.send(WitcheryBranchNetwork.Action.CANCEL);
        }
        virtualUseActive = false;
        debugHeartbeatTicks = 0;
    }

    private static boolean isUsingVirtualBranch(EntityClientPlayerMP player) {
        return VirtualItemUseState.isActive(player);
    }

    private static void debugState(String stage, Minecraft minecraft, EntityClientPlayerMP player) {
        if (!ThaumicDabblery.isDebugMode()) {
            return;
        }
        if (player == null) {
            ThaumicDabblery.debug(
                "[Mystic Branch/client] " + stage
                    + ": player=null, featureActive="
                    + WitcheryBranchFeature.isActive()
                    + ", compatRegistered="
                    + WitcheryBranchCompat.isRegistered());
            return;
        }

        NBTTagCompound data = player.getEntityData();
        ItemStack itemInUse = player.getItemInUse();
        ThaumicDabblery.debug(
            "[Mystic Branch/client] " + stage
                + ": featureActive="
                + WitcheryBranchFeature.isActive()
                + ", compatRegistered="
                + WitcheryBranchCompat.isRegistered()
                + ", hasInfusion="
                + hasInfusion(player)
                + ", screen="
                + (minecraft.currentScreen == null ? "none"
                    : minecraft.currentScreen.getClass()
                        .getName())
                + ", focused="
                + minecraft.inGameHasFocus
                + ", virtualActive="
                + VirtualItemUseState.isActive(player)
                + ", isUsingItem="
                + player.isUsingItem()
                + ", itemInUse="
                + itemName(itemInUse)
                + ", useCount="
                + player.getItemInUseCount()
                + ", strokes="
                + Arrays.toString(data.getByteArray("Strokes"))
                + ", preparedEffect="
                + (data.hasKey("WITCSpellEffectID") ? data.getInteger("WITCSpellEffectID") : "missing")
                + ", yaw="
                + player.rotationYawHead
                + ", pitch="
                + player.rotationPitch);
    }

    private static String itemName(ItemStack stack) {
        return stack == null ? "null" : stack.getUnlocalizedName() + ":" + stack.getItemDamage();
    }
}
