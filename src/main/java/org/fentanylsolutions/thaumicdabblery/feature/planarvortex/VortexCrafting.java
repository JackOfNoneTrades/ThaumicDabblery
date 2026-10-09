package org.fentanylsolutions.thaumicdabblery.feature.planarvortex;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.kentington.thaumichorizons.common.ThaumicHorizons;
import com.kentington.thaumichorizons.common.tiles.TileVortex;

/** A short, uncommitted windup. Normal crafting/payment runs exactly once at the release beat. */
public final class VortexCrafting {

    public static final int RELEASE = 28, DURATION = 36, EXPAND = 22;
    public static final float MIN_SCALE = .04F;

    public interface Holder {

        State thaumicdabblery$crafting();

        void thaumicdabblery$craftInput(EntityItem input);
    }

    public static final class State {

        public long started = -1;
        public boolean executing, released;
        private EntityItem input;
        public int inputId = -1;
        public VortexSuction.Path suction;
        private EntityPlayer player;
        private ItemStack wand;
        private Object head;
    }

    public static State state(TileVortex tile) {
        return ((Holder) tile).thaumicdabblery$crafting();
    }

    public static boolean ready(TileVortex tile) {
        return !tile.collapsing && !tile.createdDimension
            && tile.count >= 50
            && (tile.cheat || tile.beams >= 6
                || tile.getWorldObj().provider.dimensionId == ThaumicHorizons.dimensionPocketId);
    }

    public static boolean gateInput(TileVortex tile, EntityItem input) {
        if (tile.getWorldObj().isRemote) return false;
        State state = state(tile);
        if (state.executing) return false;
        if (state.started >= 0) return true;
        if (VortexSuction.active(input) != null) return true;
        if (!ready(tile) || !VortexRecipes.animatesInput(tile, input)) return false;
        state.input = input;
        begin(tile, state);
        return true;
    }

    public static boolean gateWand(TileVortex tile, ItemStack wand, EntityPlayer player) {
        State state = state(tile);
        if (state.executing) return false;
        if (state.started >= 0) return true;
        if (!ready(tile) || !VortexRecipes.canComplete(tile, wand, player)) return false;
        if (tile.getWorldObj().isRemote) return true;
        state.player = player;
        state.wand = wand;
        state.head = head(tile);
        begin(tile, state);
        return true;
    }

    private static Object head(TileVortex tile) {
        if (!tile.items.isEmpty()) return tile.items.get(0);
        return VortexRecipes.pending(tile)
            .isEmpty() ? null
                : VortexRecipes.pending(tile)
                    .get(0);
    }

    private static void begin(TileVortex tile, State state) {
        state.started = tile.getWorldObj()
            .getTotalWorldTime();
        state.released = false;
        if (state.input != null) {
            state.inputId = state.input.getEntityId();
            state.suction = new VortexSuction.Path(
                state.started,
                state.input.posX,
                state.input.posY,
                state.input.posZ,
                tile.xCoord + .5,
                tile.yCoord + .5,
                tile.zCoord + .5);
            ((VortexSuction.Holder) state.input).thaumicdabblery$suction(state.suction);
        }
        tile.getWorldObj()
            .markBlockForUpdate(tile.xCoord, tile.yCoord, tile.zCoord);
        VortexFeedback.animate(tile, state.started);
        tile.getWorldObj()
            .playSoundEffect(tile.xCoord + .5, tile.yCoord + .5, tile.zCoord + .5, "thaumcraft:craftstart", .25F, .65F);
    }

    public static void tick(TileVortex tile) {
        State state = state(tile);
        if (tile.getWorldObj().isRemote) {
            if (state.suction != null && state.inputId >= 0) {
                net.minecraft.entity.Entity entity = tile.getWorldObj()
                    .getEntityByID(state.inputId);
                if (entity instanceof EntityItem) {
                    state.input = (EntityItem) entity;
                    ((VortexSuction.Holder) state.input).thaumicdabblery$suction(state.suction);
                }
            }
            return;
        }
        if (state.started < 0) return;
        long age = tile.getWorldObj()
            .getTotalWorldTime() - state.started;
        if (!ready(tile) || age < 0) {
            clear(tile);
            return;
        }
        if (!state.released && age >= RELEASE) {
            state.released = true;
            state.executing = true;
            try {
                if (state.input != null) {
                    if (!state.input.isDead && state.input.worldObj == tile.getWorldObj()
                        && tile.getDistanceTo(state.input.posX, state.input.posY, state.input.posZ) <= 12)
                        ((Holder) tile).thaumicdabblery$craftInput(state.input);
                } else if (state.player != null && !state.player.isDead
                    && state.player.worldObj == tile.getWorldObj()
                    && tile.getWorldObj().playerEntities.contains(state.player)
                    && head(tile) == state.head
                    && ownsWand(state)) {
                        tile.onWandRightClick(
                            tile.getWorldObj(),
                            state.wand,
                            state.player,
                            tile.xCoord,
                            tile.yCoord,
                            tile.zCoord,
                            0,
                            0);
                    }
            } finally {
                state.executing = false;
                releaseInput(tile);
                state.player = null;
                state.wand = null;
                state.head = null;
            }
        }
        if (age >= DURATION) clear(tile);
    }

    private static boolean ownsWand(State state) {
        for (ItemStack stack : state.player.inventory.mainInventory) if (stack == state.wand) return true;
        return false;
    }

    public static void releaseInput(TileVortex tile) {
        State state = state(tile);
        if (state.input != null && ((VortexSuction.Holder) state.input).thaumicdabblery$suction() == state.suction)
            ((VortexSuction.Holder) state.input).thaumicdabblery$suction(null);
        state.input = null;
        state.inputId = -1;
        state.suction = null;
    }

    private static void clear(TileVortex tile) {
        State state = state(tile);
        state.started = -1;
        releaseInput(tile);
        state.player = null;
        state.wand = null;
        state.head = null;
        VortexFeedback.animate(tile, -1);
    }

    public static void cancel(TileVortex tile) {
        if (tile.getWorldObj() != null && !tile.getWorldObj().isRemote && state(tile).started >= 0) clear(tile);
    }

    private static float smooth(float t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    public static float scale(float age) {
        if (age < 0 || age >= DURATION) return 1;
        if (age < 12) return 1 - (1 - MIN_SCALE) * smooth(age / 12);
        if (age < EXPAND) return MIN_SCALE;
        if (age < RELEASE) {
            float t = (age - EXPAND) / (RELEASE - EXPAND);
            return MIN_SCALE + (1.12F - MIN_SCALE) * (1 - (float) Math.pow(1 - t, 3));
        }
        return 1.12F - .12F * smooth((age - RELEASE) / (DURATION - RELEASE));
    }

    public static float rays(float age) {
        return smooth((age - 4) / 8) * (1 - smooth((age - 14) / 6));
    }

    public static float brightness(float age) {
        if (age < 0 || age >= DURATION) return 1;
        return age < EXPAND ? 1 - .4F * smooth(age / 12) : .6F + .4F * smooth((age - EXPAND) / 4);
    }
}
