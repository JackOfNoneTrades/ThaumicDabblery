package org.fentanylsolutions.thaumicdabblery.feature.witcherybranch;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;

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
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent event) {
        if (event.phase != Phase.START) {
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        EntityClientPlayerMP player = minecraft.thePlayer;
        boolean keyDown = KEY.getIsKeyPressed();

        if (virtualUseActive && (!WitcheryBranchFeature.isActive() || player == null
            || !hasInfusion(player)
            || minecraft.currentScreen != null)) {
            cancel(player);
        } else if (keyDown && !keyWasDown && canStart(minecraft, player)) {
            begin(player);
        } else if (!keyDown && keyWasDown && virtualUseActive) {
            finish(player);
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
        ItemStack branch = new ItemStack(Witchery.Items.MYSTIC_BRANCH);
        VirtualItemUseState.begin(player, branch);
        ((ItemMysticBranch) Witchery.Items.MYSTIC_BRANCH).onItemRightClick(branch, player.worldObj, player);
        virtualUseActive = true;
        WitcheryBranchNetwork.send(WitcheryBranchNetwork.Action.START);
    }

    private void finish(EntityClientPlayerMP player) {
        if (player != null && isUsingVirtualBranch(player)) {
            player.stopUsingItem();
        }
        VirtualItemUseState.end(player);
        virtualUseActive = false;
        WitcheryBranchNetwork.send(WitcheryBranchNetwork.Action.FINISH);
    }

    private void cancel(EntityClientPlayerMP player) {
        if (player != null && isUsingVirtualBranch(player)) {
            player.clearItemInUse();
            player.getEntityData()
                .removeTag("Strokes");
            player.getEntityData()
                .removeTag("startYaw");
            player.getEntityData()
                .removeTag("startPitch");
        }
        VirtualItemUseState.end(player);
        if (virtualUseActive) {
            WitcheryBranchNetwork.send(WitcheryBranchNetwork.Action.CANCEL);
        }
        virtualUseActive = false;
    }

    private static boolean isUsingVirtualBranch(EntityClientPlayerMP player) {
        return VirtualItemUseState.isActive(player);
    }
}
