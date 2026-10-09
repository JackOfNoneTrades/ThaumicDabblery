package org.fentanylsolutions.thaumicdabblery.feature.vatfacing;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraftforge.event.entity.player.EntityInteractEvent;

import com.kentington.thaumichorizons.common.tiles.TileVat;
import com.kentington.thaumichorizons.common.tiles.TileVatSlave;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Server-selected targets and frame controls. Never ticks or changes the contained creature's AI. */
public final class VatFacing {

    public static final Map<String, Rule> RULES = new HashMap<>();
    public static ItemStack rotationItem;
    private static final String TAG = "thaumicdabblery:vatFacing";

    public interface Holder {

        State thaumicdabblery$facing();
    }

    public static State state(TileVat vat) {
        return ((Holder) vat).thaumicdabblery$facing();
    }

    public static final class Rule {

        public final String mode;
        public final double range;
        public final float yawLimit, pitchLimit, speed;

        public Rule(String mode, double range, float yawLimit, float pitchLimit, float speed) {
            this.mode = mode;
            this.range = range;
            this.yawLimit = yawLimit;
            this.pitchLimit = pitchLimit;
            this.speed = speed;
        }
    }

    public static final class State {

        public boolean active;
        public boolean customBobbing;
        public float bobAmplitude, yOffset;
        public int bobPeriod = 360;
        public float scale = 1;
        public float body, head, pitch, prevBody, prevHead, prevPitch, goalBody, goalHead, goalPitch;
        public float speed = 6, yawLimit = 60, pitchLimit = 30;
        public UUID frame;
        public int target = -1;
        private boolean initialized;
        private final Map<UUID, Integer> frames = new HashMap<>();

        public void step() {
            prevBody = body;
            prevHead = head;
            prevPitch = pitch;
            body = approach(body, goalBody, speed);
            head = MathHelper.clamp_float(approach(head, goalHead, speed), -yawLimit, yawLimit);
            pitch = MathHelper.clamp_float(approach(pitch, goalPitch, speed), -pitchLimit, pitchLimit);
            prevHead = MathHelper.clamp_float(prevHead, -yawLimit, yawLimit);
            prevPitch = MathHelper.clamp_float(prevPitch, -pitchLimit, pitchLimit);
        }

        public void write(NBTTagCompound root) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setBoolean("active", active);
            tag.setBoolean("customBobbing", customBobbing);
            tag.setFloat("bobAmplitude", bobAmplitude);
            tag.setInteger("bobPeriod", bobPeriod);
            tag.setFloat("yOffset", yOffset);
            tag.setFloat("scale", scale);
            tag.setFloat("body", body);
            tag.setFloat("head", head);
            tag.setFloat("pitch", pitch);
            tag.setFloat("goalBody", goalBody);
            tag.setFloat("goalHead", goalHead);
            tag.setFloat("goalPitch", goalPitch);
            tag.setFloat("speed", speed);
            tag.setFloat("yawLimit", yawLimit);
            tag.setFloat("pitchLimit", pitchLimit);
            if (frame != null) tag.setString("frame", frame.toString());
            root.setTag(TAG, tag);
        }

        public void read(NBTTagCompound root) {
            NBTTagCompound tag = root.getCompoundTag(TAG);
            active = tag.getBoolean("active");
            customBobbing = tag.getBoolean("customBobbing");
            bobAmplitude = MathHelper.clamp_float(finite(tag.getFloat("bobAmplitude"), 0), 0, 2);
            bobPeriod = tag.hasKey("bobPeriod") ? MathHelper.clamp_int(tag.getInteger("bobPeriod"), 2, 72000) : 360;
            yOffset = MathHelper.clamp_float(finite(tag.getFloat("yOffset"), 0), -4, 4);
            scale = tag.hasKey("scale") ? MathHelper.clamp_float(finite(tag.getFloat("scale"), 1), 0.05F, 8) : 1;
            goalBody = finite(tag.getFloat("goalBody"), 0);
            goalHead = finite(tag.getFloat("goalHead"), 0);
            goalPitch = finite(tag.getFloat("goalPitch"), 0);
            speed = MathHelper.clamp_float(finite(tag.getFloat("speed"), 6), 0.1F, 180);
            yawLimit = MathHelper.clamp_float(finite(tag.getFloat("yawLimit"), 60), 0, 85);
            pitchLimit = MathHelper.clamp_float(finite(tag.getFloat("pitchLimit"), 30), 0, 85);
            if (!initialized) {
                body = prevBody = finite(tag.getFloat("body"), 0);
                head = prevHead = finite(tag.getFloat("head"), 0);
                pitch = prevPitch = finite(tag.getFloat("pitch"), 0);
                initialized = true;
            }
            frame = null;
            try {
                if (tag.hasKey("frame")) frame = UUID.fromString(tag.getString("frame"));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private static float finite(float f, float fallback) {
        return Float.isNaN(f) || Float.isInfinite(f) ? fallback : f;
    }

    public static float approach(float from, float to, float speed) {
        return MathHelper.wrapAngleTo180_float(
            from + MathHelper.clamp_float(MathHelper.wrapAngleTo180_float(to - from), -speed, speed));
    }

    public static float interpolate(float previous, float current, float partial) {
        return previous + MathHelper.wrapAngleTo180_float(current - previous) * partial;
    }

    public static boolean matches(ItemStack stack) {
        return rotationItem != null && stack != null
            && rotationItem.getItem() == stack.getItem()
            && (rotationItem.getItemDamage() == 32767 || rotationItem.getItemDamage() == stack.getItemDamage());
    }

    public static TileVat owner(EntityItemFrame frame) {
        TileEntity tile = frame.worldObj
            .getTileEntity(frame.field_146063_b, frame.field_146064_c, frame.field_146062_d);
        if (tile instanceof TileVat) return (TileVat) tile;
        return tile instanceof TileVatSlave ? ((TileVatSlave) tile).getBoss(-1) : null;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void interact(EntityInteractEvent event) {
        if (event.entityPlayer.worldObj.isRemote || !(event.target instanceof EntityItemFrame)) return;
        EntityItemFrame frame = (EntityItemFrame) event.target;
        if (!matches(frame.getDisplayedItem()) && !matches(event.entityPlayer.getHeldItem())) return;
        TileVat vat = owner(frame);
        if (vat != null) {
            state(vat).frame = frame.getUniqueID();
            vat.markDirty();
        }
    }

    private static EntityItemFrame control(TileVat vat, State state) {
        UUID previousFrame = state.frame;
        List<EntityItemFrame> frames = vat.getWorldObj()
            .getEntitiesWithinAABB(
                EntityItemFrame.class,
                AxisAlignedBB.getBoundingBox(
                    vat.xCoord - 2,
                    vat.yCoord - 4,
                    vat.zCoord - 2,
                    vat.xCoord + 3,
                    vat.yCoord + 2,
                    vat.zCoord + 3));
        frames.sort(Comparator.comparingInt(Entity::getEntityId));
        Set<UUID> present = new HashSet<>();
        EntityItemFrame selected = null, fallback = null;
        boolean first = state.frames.isEmpty();
        for (EntityItemFrame frame : frames) {
            if (frame.isDead || !matches(frame.getDisplayedItem()) || owner(frame) != vat) continue;
            UUID id = frame.getUniqueID();
            present.add(id);
            Integer old = state.frames.put(id, frame.getRotation());
            if (!first && (old == null || old.intValue() != frame.getRotation())) state.frame = id;
            if (fallback == null) fallback = frame;
        }
        state.frames.keySet()
            .retainAll(present);
        for (EntityItemFrame frame : frames) if (present.contains(frame.getUniqueID()) && frame.getUniqueID()
            .equals(state.frame)) selected = frame;
        if (selected == null) selected = fallback;
        UUID chosen = selected == null ? null : selected.getUniqueID();
        if (!Objects.equals(chosen, previousFrame)) {
            state.frame = chosen;
            vat.markDirty();
        }
        return selected;
    }

    public static void tick(TileVat vat) {
        State s = state(vat);
        if (vat.getWorldObj().isRemote) {
            s.step();
            return;
        }
        EntityLivingBase mob = vat.getEntityContained();
        boolean effigy = mob == null && (vat.mode == 3 || vat.mode == 4 || vat.mode == 2 && vat.recipeType == 1);
        String key = effigy ? "effigy" : mob == null ? null : EntityList.getEntityString(mob);
        boolean appearanceChanged = VatAppearance
            .apply(s, key, effigy || mob != null && !(mob instanceof EntityPlayer));
        Rule rule = RULES.get(key);
        EntityItemFrame frame = control(vat, s);
        boolean active = (effigy || mob != null && !(mob instanceof EntityPlayer))
            && (frame != null || rule != null && !"none".equals(rule.mode));
        float oldBody = s.goalBody, oldHead = s.goalHead, oldPitch = s.goalPitch, oldSpeed = s.speed,
            oldYaw = s.yawLimit, oldLimit = s.pitchLimit;
        boolean oldActive = s.active;
        s.active = active;
        s.goalBody = effigy ? 180 : mob == null ? 0 : mob.renderYawOffset;
        s.goalHead = 0;
        s.goalPitch = 0;
        s.speed = rule == null ? 6 : rule.speed;
        s.yawLimit = rule == null ? 60 : rule.yawLimit;
        s.pitchLimit = rule == null ? 30 : rule.pitchLimit;
        if (frame != null)
            s.goalBody = MathHelper.wrapAngleTo180_float(frame.hangingDirection * 90 + frame.getRotation() * 90);
        EntityPlayer target = null;
        double best = rule == null ? 0 : rule.range * rule.range;
        if (active && rule != null && !"none".equals(rule.mode)) {
            for (Object o : vat.getWorldObj().playerEntities) {
                EntityPlayer p = (EntityPlayer) o;
                if (p.isDead) continue;
                double distance = p.getDistanceSq(vat.xCoord + 0.5, vat.yCoord - 1, vat.zCoord + 0.5);
                if (distance < best) {
                    best = distance;
                    target = p;
                }
            }
        }
        s.target = target == null ? -1 : target.getEntityId();
        if (target != null) {
            double dx = target.posX - (vat.xCoord + 0.5), dz = target.posZ - (vat.zCoord + 0.5);
            double eyes = effigy ? vat.yCoord - 0.6 : mob.posY + mob.getEyeHeight();
            double dy = target.posY + target.getEyeHeight() - eyes;
            float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90;
            if (dx * dx + dz * dz < 0.0001) yaw = s.goalBody;
            if ("body".equals(rule.mode) && frame == null) s.goalBody = yaw;
            if ("head".equals(rule.mode) || frame == null) {
                s.goalHead = MathHelper
                    .clamp_float(MathHelper.wrapAngleTo180_float(yaw - s.goalBody), -s.yawLimit, s.yawLimit);
                s.goalPitch = MathHelper.clamp_float(
                    (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))),
                    -s.pitchLimit,
                    s.pitchLimit);
            }
        }
        if (!s.initialized) {
            s.body = s.prevBody = effigy ? 180 : mob == null ? 0 : mob.renderYawOffset;
            s.initialized = true;
        }
        s.step();
        boolean changed = oldActive != active || oldBody != s.goalBody
            || oldHead != s.goalHead
            || oldPitch != s.goalPitch
            || oldSpeed != s.speed
            || oldYaw != s.yawLimit
            || oldLimit != s.pitchLimit;
        if (appearanceChanged || changed
            || (active || s.customBobbing || s.yOffset != 0 || s.scale != 1) && vat.getWorldObj()
                .getTotalWorldTime() % 20 == 0)
            VatFacingNetwork.send(vat);
    }
}
