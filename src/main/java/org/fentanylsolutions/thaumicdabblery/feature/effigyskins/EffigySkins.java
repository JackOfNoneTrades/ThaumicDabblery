package org.fentanylsolutions.thaumicdabblery.feature.effigyskins;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;

import com.kentington.thaumichorizons.common.tiles.TileSoulBeacon;
import com.kentington.thaumichorizons.common.tiles.TileVat;
import com.mojang.authlib.GameProfile;

/** Cosmetic identity only. Native beacon eligibility and resurrection remain entirely Horizons' responsibility. */
public final class EffigySkins {

    private static final String TAG = "thaumicdabblery:effigySkin";

    private EffigySkins() {}

    public static GameProfile copy(GameProfile profile) {
        if (profile == null || profile.getId() == null) return null;
        GameProfile result = new GameProfile(profile.getId(), profile.getName());
        result.getProperties()
            .putAll(
                "textures",
                profile.getProperties()
                    .get("textures"));
        return result;
    }

    public static void write(EffigySkinHolder holder, NBTTagCompound tag) {
        GameProfile profile = holder.thaumicdabblery$getEffigyProfile();
        if (profile != null) {
            NBTTagCompound data = new NBTTagCompound();
            NBTUtil.func_152460_a(data, profile);
            tag.setTag(TAG, data);
        } else tag.removeTag(TAG);
    }

    public static void read(EffigySkinHolder holder, NBTTagCompound tag) {
        GameProfile profile = null;
        if (tag.hasKey(TAG, 10)) {
            try {
                profile = NBTUtil.func_152459_a(tag.getCompoundTag(TAG));
            } catch (IllegalArgumentException ignored) { /*
                                                          * Invalid old/world-edited identity falls back to the effigy.
                                                          */ }
        }
        holder.thaumicdabblery$setEffigyProfile(profile);
    }

    public static void bind(TileSoulBeacon beacon, EntityPlayer player) {
        if (beacon.getWorldObj().isRemote) return;
        update(beacon, player.getGameProfile());
        TileEntity below = beacon.getWorldObj()
            .getTileEntity(beacon.xCoord, beacon.yCoord - 1, beacon.zCoord);
        if (below instanceof TileVat) update(below, player.getGameProfile());
        if (player instanceof EntityPlayerMP) EffigySkinNetwork.sync((EntityPlayerMP) player);
    }

    private static void update(TileEntity tile, GameProfile profile) {
        ((EffigySkinHolder) tile).thaumicdabblery$setEffigyProfile(profile);
        tile.markDirty();
        tile.getWorldObj()
            .markBlockForUpdate(tile.xCoord, tile.yCoord, tile.zCoord);
    }

    /** A beacon can be bound before an effigy is grown. Copy once when that body becomes visible. */
    public static void inherit(TileVat vat) {
        if (vat.getWorldObj().isRemote || ((EffigySkinHolder) vat).thaumicdabblery$getEffigyProfile() != null
            || !(vat.mode == 3 || vat.mode == 4 || vat.mode == 2 && vat.recipeType == 1)) return;
        TileEntity above = vat.getWorldObj()
            .getTileEntity(vat.xCoord, vat.yCoord + 1, vat.zCoord);
        if (above instanceof TileSoulBeacon) {
            GameProfile profile = ((EffigySkinHolder) above).thaumicdabblery$getEffigyProfile();
            if (profile != null) update(vat, profile);
        }
    }

    public static GameProfile select(TileVat vat, GameProfile viewer, EffigySkinNetwork.Binding binding) {
        if (viewer != null && binding != null && binding.matches(vat)) return viewer;
        return ((EffigySkinHolder) vat).thaumicdabblery$getEffigyProfile();
    }
}
