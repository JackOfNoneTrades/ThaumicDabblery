package org.fentanylsolutions.thaumicdabblery.feature.planarvortex;

import net.minecraft.entity.item.EntityItem;

/** Transient item flight shared by server physics and client prediction; no optional mod classes. */
public final class VortexSuction {

    public static final int ABSORBED = 12;

    public interface Holder {

        Path thaumicdabblery$suction();

        void thaumicdabblery$suction(Path path);
    }

    public static final class Path {

        public final long started;
        public final double fromX, fromY, fromZ, x, y, z;

        public Path(long started, double fromX, double fromY, double fromZ, double x, double y, double z) {
            this.started = started;
            this.fromX = fromX;
            this.fromY = fromY;
            this.fromZ = fromZ;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public long age(EntityItem item) {
            return item.worldObj.getTotalWorldTime() - started;
        }

        public void move(EntityItem item) {
            double t = Math.max(0, Math.min(1, age(item) / (double) ABSORBED));
            t = t * t * (3 - 2 * t);
            item.setPosition(fromX + (x - fromX) * t, fromY + (y - fromY) * t, fromZ + (z - fromZ) * t);
            item.motionX = item.motionY = item.motionZ = 0;
            item.onGround = false;
            if (!item.worldObj.isRemote) item.velocityChanged = true;
        }
    }

    public static Path active(EntityItem item) {
        Path path = ((Holder) item).thaumicdabblery$suction();
        if (path == null || item.isDead || path.age(item) < 0 || path.age(item) >= VortexCrafting.DURATION) return null;
        return path;
    }

    public static boolean absorbed(EntityItem item) {
        Path path = active(item);
        return path != null && path.age(item) >= ABSORBED;
    }
}
