package org.fentanylsolutions.thaumicdabblery.feature.researcheditor;

/** Geometry comes from the actual stock browser, including resized books. */
public interface ResearchBrowserAccess {

    int thaumicdabblery$width();

    int thaumicdabblery$height();

    double thaumicdabblery$mapX();

    double thaumicdabblery$mapY();

    void thaumicdabblery$pan(double x, double y);
}
