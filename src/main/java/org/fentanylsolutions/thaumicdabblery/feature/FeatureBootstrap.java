package org.fentanylsolutions.thaumicdabblery.feature;

import org.fentanylsolutions.thaumicdabblery.feature.itemstats.ThaumcraftItemStatsFeature;
import org.fentanylsolutions.thaumicdabblery.feature.researchscangates.ResearchScanGatesFeature;
import org.fentanylsolutions.thaumicdabblery.feature.visdiscount.VisDiscountFeature;
import org.fentanylsolutions.thaumicdabblery.feature.witcherybranch.WitcheryBranchFeature;

/** Central registration point for feature modules. */
final class FeatureBootstrap {

    private FeatureBootstrap() {}

    static void registerFeatures() {
        FeatureManager.register(new VisDiscountFeature());
        FeatureManager.register(new ThaumcraftItemStatsFeature());
        FeatureManager.register(new ResearchScanGatesFeature());
        FeatureManager.register(new WitcheryBranchFeature());
    }
}
