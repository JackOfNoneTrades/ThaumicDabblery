package org.fentanylsolutions.thaumicdabblery.compat.tc4tweaks;

import java.util.Map;

import net.glease.tc4tweak.modules.FlushableCache;
import net.glease.tc4tweak.modules.getResearch.GetResearch;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.ReflectionHelper;
import thaumcraft.api.research.ResearchItem;

/** Update only our research entry without enabling caches that MineTweaker temporarily disabled. */
public final class AuraResearchCacheCompat {

    private AuraResearchCacheCompat() {}

    public static void update(String key, ResearchItem research) {
        if (Loader.isModLoaded("tc4tweak")) TC4Tweaks.update(key, research);
    }

    private static final class TC4Tweaks {

        private static final FlushableCache<Map<String, ResearchItem>> CACHE = ReflectionHelper
            .getPrivateValue(GetResearch.class, null, "cache");

        private static void update(String key, ResearchItem research) {
            Map<String, ResearchItem> entries = CACHE.getCache();
            if (entries == null) return;
            if (research == null) entries.remove(key);
            else entries.put(key, research);
        }
    }
}
