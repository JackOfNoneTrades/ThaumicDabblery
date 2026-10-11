package org.fentanylsolutions.thaumicdabblery.feature.effigyskins;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;

import org.fentanylsolutions.thaumicdabblery.ThaumicDabblery;

import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.FMLCommonHandler;

/** Server-only cache of native bindings, including players whose old vats are unloaded. */
public final class EffigySkinBindings {

    private static final Map<MinecraftServer, Map<UUID, EffigySkinNetwork.Binding>> CACHE = new WeakHashMap<>();

    private EffigySkinBindings() {}

    private static Map<UUID, EffigySkinNetwork.Binding> bindings(MinecraftServer server) {
        return CACHE.computeIfAbsent(server, ignored -> new HashMap<>());
    }

    public static void remember(EntityPlayer player) {
        if (player.worldObj.isRemote) return;
        MinecraftServer server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server != null)
            bindings(server).put(player.getUniqueID(), EffigySkinNetwork.Binding.from(player.getEntityData()));
    }

    public static boolean permits(GameProfile profile, TileEntity vat) {
        MinecraftServer server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server == null || profile == null) return true;
        UUID id = profile.getId();
        Map<UUID, EffigySkinNetwork.Binding> bindings = bindings(server);
        for (Object entry : server.getConfigurationManager().playerEntityList) {
            EntityPlayer player = (EntityPlayer) entry;
            if (id.equals(player.getUniqueID())) {
                remember(player);
                break;
            }
        }
        if (!bindings.containsKey(id)) bindings.put(id, readOffline(server, id));
        EffigySkinNetwork.Binding binding = bindings.get(id);
        // Missing player data is not evidence of a different binding. Keep legacy appearances in that case.
        return binding == null || binding.matches(vat);
    }

    private static EffigySkinNetwork.Binding readOffline(MinecraftServer server, UUID id) {
        File file = new File(
            server.worldServerForDimension(0)
                .getSaveHandler()
                .getWorldDirectory(),
            "playerdata/" + id + ".dat");
        if (!file.isFile()) return null;
        try (FileInputStream input = new FileInputStream(file)) {
            return EffigySkinNetwork.Binding.from(
                CompressedStreamTools.readCompressed(input)
                    .getCompoundTag("ForgeData"));
        } catch (IOException exception) {
            ThaumicDabblery.LOG.warn("Could not check saved Soul Beacon binding for {}", id, exception);
            return null;
        }
    }
}
