package org.fentanylsolutions.thaumicdabblery.core;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.fentanylsolutions.thaumicdabblery.ThaumicDabblery;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

@SuppressWarnings("unused")
@LateMixin
@IFMLLoadingPlugin.MCVersion("1.7.10")
public class LateMixinLoader implements ILateMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins." + ThaumicDabblery.MODID + ".late.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        if (loadedMods.contains("Thaumcraft")) {
            return Collections.singletonList("thaumcraft.MixinWandManager");
        }
        return Collections.emptyList();
    }
}
