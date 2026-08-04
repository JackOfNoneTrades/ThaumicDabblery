package org.fentanylsolutions.thaumicdabblery.feature;

import org.fentanylsolutions.thaumicdabblery.feature.visdiscount.VisDiscountFeature;

/** Central registration point for feature modules. */
final class FeatureBootstrap {

    private FeatureBootstrap() {}

    static void registerFeatures() {
        FeatureManager.register(new VisDiscountFeature());
    }
}
