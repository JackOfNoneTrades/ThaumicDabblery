package org.fentanylsolutions.thaumicdabblery.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.launchwrapper.Launch;

import org.fentanylsolutions.thaumicdabblery.ThaumicDabblery;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

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
            mixins.add("thaumcraft.MixinItemWandCasting");
            mixins.add("thaumcraft.MixinWandManager");
            mixins.add("thaumcraft.MixinEventHandlerRunic");
            mixins.add("thaumcraft.MixinInfusionRunicAugmentRecipe");
            mixins.add("thaumcraft.ResearchItemAccessor");
            mixins.add("thaumcraft.MixinResearchManager");
            mixins.add("thaumcraft.MixinScanManager");
            if (FMLLaunchHandler.side()
                .isClient()) {
                mixins.add("thaumcraft.MixinGuiResearchBrowser");
            }
            if (loadedMods.contains("modtweaker2")) {
                mixins.add("thaumcraft.MixinResearchCategories");
                mixins.add("thaumcraft.MixinTileFocalManipulator");
                if (FMLLaunchHandler.side()
                    .isClient()) {
                    mixins.add("thaumcraft.MixinGuiFocalManipulator");
                }
                mixins.add("modtweaker.MixinAddPage");
                mixins.add("modtweaker.MixinAddPrereq");
                mixins.add("modtweaker.MixinClearPrereqs");
                if (hasModTweakerMoveResearchHelper()) {
                    mixins.add("modtweaker.MixinMoveResearch");
                } else {
                    mixins.add("modtweaker.MixinMoveResearchLegacy");
                }
                mixins.add("modtweaker.MixinOrphanResearch");
                mixins.add("modtweaker.MixinRemoveResearch");
                mixins.add("modtweaker.MixinRemoveTab");
                mixins.add("modtweaker.MixinSetResearch");
            }
            if (loadedMods.contains("tc4tweak")) {
                mixins.add("tc4tweaks.MixinGetResearch");
            }
            if (loadedMods.contains("thaumicbases")) {
                mixins.add("thaumicbases.MixinItemCastingBracelet");
            }
            if (loadedMods.contains("salisarcana") && FMLLaunchHandler.side()
                .isClient()) {
                mixins.add("salisarcana.MixinWandPartTooltipEventHandler");
            }
            if (loadedMods.contains("WitchingGadgets") && FMLLaunchHandler.side()
                .isClient()) {
                mixins.add("witchinggadgets.MixinClientEventHandler");
                mixins.add("witchinggadgets.MixinPacketClientNotifier");
                mixins.add("witchinggadgets.MixinMessageClientNotifierHandler");
            }
        }
        if (loadedMods.contains("ThaumicHorizons") && loadedMods.contains("witchery")) {
            mixins.add("thaumichorizons.MixinItemInfusionSelfCheat");
            mixins.add("witchery.MixinItemMysticBranch");
            mixins.add("witchery.MixinPacketSpellPreparedHandler");
            if (FMLLaunchHandler.side()
                .isClient()) {
                mixins.add("thaumichorizons.MixinGuiVat");
                mixins.add("thaumichorizons.MixinItemInfusionSelfCheatClient");
            }
        }
        return mixins;
    }

    private static boolean hasModTweakerMoveResearchHelper() {
        String resource = "modtweaker2/mods/thaumcraft/research/MoveResearch.class";
        try (InputStream input = Launch.classLoader.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalStateException("Could not inspect ModTweaker MoveResearch bytecode");
            }

            final boolean[] found = { false };
            new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM5) {

                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                    String[] exceptions) {
                    if ("moveResearchItem".equals(name)) {
                        found[0] = true;
                    }
                    return null;
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            ThaumicDabblery.LOG
                .info("Detected {} ModTweaker MoveResearch layout", found[0] ? "helper-based" : "legacy inline");
            return found[0];
        } catch (IOException exception) {
            throw new IllegalStateException("Could not inspect ModTweaker MoveResearch bytecode", exception);
        }
    }
}
