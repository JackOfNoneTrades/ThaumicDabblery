package org.fentanylsolutions.thaumicdabblery.feature.witcherybranch;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.fentanylsolutions.thaumicdabblery.ThaumicDabblery;

import com.emoniph.witchery.Witchery;
import com.emoniph.witchery.item.ItemMysticBranch;
import com.kentington.thaumichorizons.common.lib.EntityInfusionProperties;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;

public final class WitcheryBranchVirtualUse {

    static final WitcheryBranchVirtualUse INSTANCE = new WitcheryBranchVirtualUse();

    private static final Map<UUID, EntityPlayerMP> ACTIVE_PLAYERS = new HashMap<>();

    private WitcheryBranchVirtualUse() {}

    static synchronized void begin(EntityPlayerMP player) {
        debugState("START requested", player);
        cancel(player);
        if (!canUse(player) || ItemUseMethods.isUsingItem(player)) {
            ThaumicDabblery.LOG.warn("Rejected virtual Mystic Branch start from {}", player.getGameProfile());
            debugState("START rejected", player);
            return;
        }

        ItemStack branch = new ItemStack(Witchery.Items.MYSTIC_BRANCH);
        VirtualItemUseState.begin(player, branch);
        ((ItemMysticBranch) Witchery.Items.MYSTIC_BRANCH).onItemRightClick(branch, player.worldObj, player);
        ACTIVE_PLAYERS.put(player.getUniqueID(), player);
        debugState("START completed", player);
    }

    static synchronized void finish(EntityPlayerMP player) {
        debugState("FINISH requested", player);
        if (ACTIVE_PLAYERS.remove(player.getUniqueID()) == null) {
            debugState("FINISH ignored because player was not active", player);
            return;
        }
        if (!canUse(player)) {
            debugState("FINISH converted to clear because validation failed", player);
            clearVirtualUse(player);
            return;
        }

        finishVirtualUse(player);
        VirtualItemUseState.end(player);
        if (ItemUseMethods.isUsingItem(player)) {
            ItemUseMethods.clearItemInUse(player);
        }
        debugState("FINISH completed", player);
    }

    private static void finishVirtualUse(EntityPlayer player) {
        ItemStack inUse = ItemUseMethods.getItemInUse(player);
        if (inUse != null && inUse.getItem() == Witchery.Items.MYSTIC_BRANCH) {
            ThaumicDabblery.debug("[Mystic Branch/server] Stopping Witchery branch normally");
            ItemUseMethods.stopUsingItem(player);
        } else {
            ThaumicDabblery.debug(
                "[Mystic Branch/server] Clearing unexpected item in use instead of stopping: " + itemName(inUse));
            ItemUseMethods.clearItemInUse(player);
        }
    }

    static synchronized void cancel(EntityPlayer player) {
        if (player != null && ACTIVE_PLAYERS.remove(player.getUniqueID()) != null) {
            debugState("CANCEL requested", player);
            clearVirtualUse(player);
            debugState("CANCEL completed", player);
        }
    }

    private static boolean canUse(EntityPlayer player) {
        if (!WitcheryBranchFeature.isActive() || !WitcheryBranchCompat.isRegistered() || player == null) {
            return false;
        }
        EntityInfusionProperties properties = (EntityInfusionProperties) player
            .getExtendedProperties(EntityInfusionProperties.EXT_PROP_NAME);
        return properties != null && properties.hasPlayerInfusion(WitcheryBranchFeature.INFUSION_ID);
    }

    private static void clearVirtualUse(EntityPlayer player) {
        ItemStack inUse = ItemUseMethods.getItemInUse(player);
        VirtualItemUseState.end(player);
        if (inUse != null && inUse.getItem() == Witchery.Items.MYSTIC_BRANCH) {
            ItemUseMethods.clearItemInUse(player);
        }
        player.getEntityData()
            .removeTag("WITCSpellEffectID");
        player.getEntityData()
            .removeTag("WITCSpellEffectEnhanced");
    }

    private static void debugState(String stage, EntityPlayer player) {
        if (!ThaumicDabblery.isDebugMode()) {
            return;
        }
        if (player == null) {
            ThaumicDabblery.debug("[Mystic Branch/server] " + stage + ": player=null");
            return;
        }
        EntityInfusionProperties properties = (EntityInfusionProperties) player
            .getExtendedProperties(EntityInfusionProperties.EXT_PROP_NAME);
        NBTTagCompound data = player.getEntityData();
        ItemStack inUse = ItemUseMethods.getItemInUse(player);
        ThaumicDabblery.debug(
            "[Mystic Branch/server] " + stage
                + ": player="
                + player.getCommandSenderName()
                + ", featureActive="
                + WitcheryBranchFeature.isActive()
                + ", compatRegistered="
                + WitcheryBranchCompat.isRegistered()
                + ", hasInfusion="
                + (properties != null && properties.hasPlayerInfusion(WitcheryBranchFeature.INFUSION_ID))
                + ", trackedActive="
                + ACTIVE_PLAYERS.containsKey(player.getUniqueID())
                + ", virtualActive="
                + VirtualItemUseState.isActive(player)
                + ", itemInUse="
                + itemName(inUse)
                + ", strokes="
                + Arrays.toString(data.getByteArray("Strokes"))
                + ", preparedEffect="
                + (data.hasKey("WITCSpellEffectID") ? data.getInteger("WITCSpellEffectID") : "missing")
                + ", enhancedLevel="
                + (data.hasKey("WITCSpellEffectEnhanced") ? data.getInteger("WITCSpellEffectEnhanced") : "missing"));
    }

    private static String itemName(ItemStack stack) {
        return stack == null ? "null" : stack.getUnlocalizedName() + ":" + stack.getItemDamage();
    }

    @SubscribeEvent
    public void onLogout(PlayerLoggedOutEvent event) {
        cancel(event.player);
    }

    @SubscribeEvent
    public void onChangedDimension(PlayerChangedDimensionEvent event) {
        cancel(event.player);
    }

    @SubscribeEvent
    public void onRespawn(PlayerRespawnEvent event) {
        cancel(event.player);
    }

    /**
     * getItemInUse is stripped by its client-only annotation on dedicated servers, even though the
     * backing field and the item-use lifecycle are server-side. Forge's reflection helper handles
     * its MCP, SRG, and Notch field names for both dev and reobfuscated jars.
     */
    private static final class ItemUseMethods {

        private static final Field ITEM_IN_USE = ReflectionHelper
            .findField(EntityPlayer.class, "itemInUse", "field_71074_e", "f");
        private static final Method STOP_USING_ITEM = ReflectionHelper
            .findMethod(EntityPlayer.class, null, new String[] { "stopUsingItem", "func_71034_by", "bA" });
        private static final Method CLEAR_ITEM_IN_USE = ReflectionHelper
            .findMethod(EntityPlayer.class, null, new String[] { "clearItemInUse", "func_71041_bz", "bB" });

        private ItemUseMethods() {}

        private static ItemStack getItemInUse(EntityPlayer player) {
            try {
                return (ItemStack) ITEM_IN_USE.get(player);
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException("Unable to read EntityPlayer itemInUse", exception);
            }
        }

        private static boolean isUsingItem(EntityPlayer player) {
            return getItemInUse(player) != null;
        }

        private static void stopUsingItem(EntityPlayer player) {
            invoke(STOP_USING_ITEM, player);
        }

        private static void clearItemInUse(EntityPlayer player) {
            invoke(CLEAR_ITEM_IN_USE, player);
        }

        private static Object invoke(Method method, EntityPlayer player) {
            try {
                return method.invoke(player);
            } catch (IllegalAccessException | InvocationTargetException exception) {
                throw new IllegalStateException("Unable to invoke EntityPlayer." + method.getName(), exception);
            }
        }
    }
}
