package org.fentanylsolutions.thaumicdabblery.compat.modtweaker;

import org.fentanylsolutions.thaumicdabblery.feature.customaspects.CustomAspectRegistry.Definition;

import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenClass("mods.thaumcraft.CustomAspects")
public final class CustomAspectsZen {

    private CustomAspectsZen() {}

    @ZenMethod
    public static void register(String tag, int color, String image, String first, String second, String description) {
        CustomAspectScriptLoader.queue(new Definition(tag, color, image, first, second, description));
    }
}
