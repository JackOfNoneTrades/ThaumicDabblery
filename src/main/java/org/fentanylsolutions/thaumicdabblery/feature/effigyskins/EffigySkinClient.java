package org.fentanylsolutions.thaumicdabblery.feature.effigyskins;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.INetHandler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.kentington.thaumichorizons.common.tiles.TileVat;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;

/** Client-only skin cache. Profile lookup never performs network work on the render thread. */
public final class EffigySkinClient {

    private static volatile EffigySkinNetwork.Binding binding;
    private static final Cache<String, Skin> SKINS = CacheBuilder.newBuilder()
        .maximumSize(128)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build();
    private static final ThreadPoolExecutor LOOKUPS = new ThreadPoolExecutor(
        0,
        2,
        30,
        TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(32),
        task -> {
            Thread thread = new Thread(task, "Dabblery effigy skins");
            thread.setDaemon(true);
            return thread;
        });
    private static final ModelBiped LEGACY = new EffigySkinModel(false, false);
    private static final EffigySkinModel CLASSIC = new EffigySkinModel(true, false),
        SLIM = new EffigySkinModel(true, true);

    private EffigySkinClient() {}

    public static void register() {
        FMLCommonHandler.instance()
            .bus()
            .register(new EffigySkinClient());
    }

    public static void receive(EffigySkinNetwork.Binding next, INetHandler source) {
        Minecraft client = Minecraft.getMinecraft();
        client.func_152344_a(() -> { if (client.getNetHandler() == source) binding = next; });
    }

    @SubscribeEvent
    public void disconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        binding = null;
        SKINS.invalidateAll();
        LOOKUPS.getQueue()
            .clear();
    }

    public static GameProfile profileFor(TileVat vat) {
        EntityPlayer viewer = Minecraft.getMinecraft().thePlayer;
        return EffigySkins.select(vat, viewer == null ? null : viewer.getGameProfile(), binding);
    }

    private static String key(GameProfile profile) {
        StringBuilder key = new StringBuilder(
            profile.getId()
                .toString());
        for (Property property : profile.getProperties()
            .get("textures"))
            key.append(':')
                .append(property.getValue());
        return key.toString();
    }

    public static Skin resolve(GameProfile profile, World world) {
        if (profile == null || profile.getId() == null) return null;
        String key = key(profile);
        Skin skin = SKINS.getIfPresent(key);
        if (skin == null) {
            skin = new Skin();
            SKINS.put(key, skin);
            Skin pending = skin;
            GameProfile copied = EffigySkins.copy(profile);
            try {
                LOOKUPS.execute(() -> load(copied, pending));
            } catch (RejectedExecutionException ignored) { /*
                                                            * Bounded queue: use the effigy fallback until a later
                                                            * retry.
                                                            */ }
        }
        // Honor the skin already displayed for online players, including the local player.
        if (world != null) for (Object value : world.playerEntities) {
            if (value instanceof AbstractClientPlayer) {
                AbstractClientPlayer player = (AbstractClientPlayer) value;
                if (profile.getId()
                    .equals(
                        player.getGameProfile()
                            .getId())
                    && player.func_152123_o()) {
                    skin.texture = player.getLocationSkin();
                    break;
                }
            }
        }
        return skin.texture == null ? null : skin;
    }

    private static void load(GameProfile profile, Skin skin) {
        try {
            Minecraft client = Minecraft.getMinecraft();
            if (!profile.getProperties()
                .containsKey("textures"))
                profile = client.func_152347_ac()
                    .fillProfileProperties(profile, true);
            if (profile == null) return;
            MinecraftProfileTexture texture = client.func_152347_ac()
                .getTextures(profile, false)
                .get(MinecraftProfileTexture.Type.SKIN);
            if (texture == null) return;
            skin.slim = "slim".equals(texture.getMetadata("model"));
            // Like vanilla player skulls, allow saved texture properties to outlive their signature timestamp.
            client.func_152342_ad()
                .func_152790_a(
                    profile,
                    (type, location) -> { if (type == MinecraftProfileTexture.Type.SKIN) skin.texture = location; },
                    false);
        } catch (RuntimeException ignored) { /* Offline/missing profiles retain the original effigy texture. */ }
    }

    public static boolean render(TileEntity interior, float scale) {
        return render(interior, scale, 0, 0);
    }

    public static boolean render(TileEntity interior, float scale, float yaw, float pitch) {
        TileEntity above = interior.getWorldObj()
            .getTileEntity(interior.xCoord, interior.yCoord + 1, interior.zCoord);
        if (!(above instanceof TileVat)) return false;
        Skin skin = resolve(profileFor((TileVat) above), interior.getWorldObj());
        if (skin == null) return false;
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(skin.texture);
        int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
        int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
        ModelBiped model = width > 0 && height == width ? (skin.slim ? SLIM : CLASSIC) : LEGACY;
        model.render(null, 0, 0, 0, yaw, pitch, scale);
        return true;
    }

    public static final class Skin {

        public volatile ResourceLocation texture;
        public volatile boolean slim;
    }
}
