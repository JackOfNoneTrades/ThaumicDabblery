package org.fentanylsolutions.thaumicdabblery.compat.modtweaker;

import org.fentanylsolutions.thaumicdabblery.feature.customaspects.CustomAspectRegistry.Definition;

import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenClass("mods.thaumcraft.CustomAspects")
public final class CustomAspectsZen {

    private CustomAspectsZen() {}

    @ZenMethod
    public static void registerPrimal(String tag, int color, String image, String description,
        boolean hiddenUntilScanned) {
        CustomAspectScriptLoader.queue(new Definition(tag, color, image, description, hiddenUntilScanned));
    }

    @ZenMethod
    public static void setComponents(String tag, String first, String second) {
        CustomAspectScriptLoader.queue(
            new org.fentanylsolutions.thaumicdabblery.feature.customaspects.CustomAspectRegistry.ComponentEdit(
                tag,
                first,
                second));
    }

    @ZenMethod
    public static void register(String tag, int color, String image, String first, String second, String description) {
        CustomAspectScriptLoader.queue(new Definition(tag, color, image, first, second, description));
    }

    @ZenMethod
    public static void register(String tag, int color, String image, String first, String second, String description,
        boolean hiddenUntilScanned) {
        if (first == null || second == null) throw new IllegalArgumentException("Compound aspects need two components");
        CustomAspectScriptLoader
            .queue(new Definition(tag, color, image, first, second, description, hiddenUntilScanned));
    }

}
