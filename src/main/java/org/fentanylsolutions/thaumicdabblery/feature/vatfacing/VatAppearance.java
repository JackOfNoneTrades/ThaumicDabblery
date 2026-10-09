package org.fentanylsolutions.thaumicdabblery.feature.vatfacing;

import java.util.HashMap;
import java.util.Map;

/** Script settings are resolved on the server and sent with the vat's visual pose. */
public final class VatAppearance {

    public static Bobbing globalBobbing;
    public static final Map<String, Bobbing> BOBBING = new HashMap<>();
    public static final Map<String, Float> Y_OFFSETS = new HashMap<>();
    public static final Map<String, Float> SCALES = new HashMap<>();

    public static final class Bobbing {

        public final float amplitude;
        public final int period;

        public Bobbing(float amplitude, int period) {
            this.amplitude = amplitude;
            this.period = period;
        }
    }

    public static boolean apply(VatFacing.State state, String entity, boolean eligible) {
        Bobbing bobbing = eligible ? BOBBING.getOrDefault(entity, globalBobbing) : null;
        boolean custom = bobbing != null;
        float amplitude = custom ? bobbing.amplitude : 0;
        int period = custom ? bobbing.period : 360;
        float offset = eligible ? Y_OFFSETS.getOrDefault(entity, 0F) : 0;
        float scale = eligible ? SCALES.getOrDefault(entity, 1F) : 1;
        boolean changed = state.customBobbing != custom || state.bobAmplitude != amplitude
            || state.bobPeriod != period
            || state.yOffset != offset
            || state.scale != scale;
        state.customBobbing = custom;
        state.bobAmplitude = amplitude;
        state.bobPeriod = period;
        state.yOffset = offset;
        state.scale = scale;
        return changed;
    }

    /** Cosine is the native bob's phase of the sine wave; reduce time before converting to floating point. */
    public static float bob(VatFacing.State state, long ticks, float partial) {
        return state.bobAmplitude
            * (float) Math.cos(2 * Math.PI * (Math.floorMod(ticks, state.bobPeriod) + partial) / state.bobPeriod);
    }
}
