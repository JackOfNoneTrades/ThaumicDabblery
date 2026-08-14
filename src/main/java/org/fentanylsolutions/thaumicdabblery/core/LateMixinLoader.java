package org.fentanylsolutions.thaumicdabblery.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.fentanylsolutions.thaumicdabblery.ThaumicDabblery;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

import cpw.mods.fml.relauncher.FMLLaunchHandler;
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
        List<String> mixins = new ArrayList<>();
        if (loadedMods.contains("Thaumcraft")) {
            mixins.add("thaumcraft.MixinWandManager");
            mixins.add("thaumcraft.ResearchItemAccessor");
            mixins.add("thaumcraft.MixinResearchManager");
            mixins.add("thaumcraft.MixinScanManager");
        }
        if (loadedMods.contains("ThaumicHorizons") && loadedMods.contains("witchery")) {
            mixins.add("witchery.MixinItemMysticBranch");
            mixins.add("witchery.MixinPacketSpellPreparedHandler");
            if (FMLLaunchHandler.side()
                .isClient()) {
                mixins.add("thaumichorizons.MixinGuiVat");
            }
        }
        return mixins;
    }
}
