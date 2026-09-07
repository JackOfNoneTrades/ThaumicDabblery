package tdtest;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import org.fentanylsolutions.thaumicdabblery.compat.modtweaker.CustomAspectsZen;
import org.fentanylsolutions.thaumicdabblery.feature.customaspects.CustomAspectRegistry;
import org.fentanylsolutions.thaumicdabblery.feature.customaspects.CustomAspectRegistry.Definition;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.lib.network.playerdata.PacketSyncAspects;
import thaumcraft.common.lib.research.ResearchManager;
import thaumcraft.common.tiles.TileCentrifuge;
import thaumcraft.common.tiles.TileJarFillable;

/** Shared checks run inside real obfuscated Forge clients/servers, never on a development classpath. */
public final class CustomAspectChecks {
    public int checks;
    public final Aspect time = Aspect.getAspect("tdtempus");
    public final Aspect moment = Aspect.getAspect("tdmoment");
    public final Aspect echo = Aspect.getAspect("tdecho");

    public void registry() {
        check(Boolean.FALSE.equals(Launch.blackboard.get("fml.deobfuscatedEnvironment")), "production environment");
        check(time != null && moment != null && echo != null, "all definitions present at init");
        check("Tdtempus".equals(time.getName()), "display name");
        check(time.getColor() == 0xB68CFF && echo.getColor() == 0, "RGB values including black");
        check("thaumcraft:textures/aspects/ordo.png".equals(time.getImage().toString()), "resource location");
        check(time.getComponents()[0] == Aspect.ORDER && time.getComponents()[1] == Aspect.VOID, "component order");
        check(moment.getComponents()[0] == time && echo.getComponents()[0] == time && echo.getComponents()[1] == time, "forward and repeated components");
        check(Aspect.getPrimalAspects().size() == 6, "no new primals");
        check(!time.isPrimal() && Aspect.getCompoundAspects().contains(time), "genuine compound aspect");
        check(ResearchManager.getCombinationResult(Aspect.ORDER, Aspect.VOID) == time, "native combination");
        check(ResearchManager.getCombinationResult(Aspect.VOID, Aspect.ORDER) == time, "reversed combination");
        check(ResearchManager.getCombinationResult(time, time) == echo, "repeated-component combination");
        check(ResearchManager.getCombinationResult(time, Aspect.ORDER) == moment, "custom-on-custom combination");
        AspectList primals = ResearchManager.reduceToPrimals(new AspectList().add(time, 1));
        check(primals.getAmount(Aspect.ORDER) == 1 && primals.getAmount(Aspect.AIR) == 1 && primals.getAmount(Aspect.ENTROPY) == 1, "native primal decomposition");
        CustomAspectRegistry.verifyRegistrations();
    }

    public void scripted() {
        check(Aspect.getAspect("tdtempus") == time, "identity survives reload");
        AspectList tags = ThaumcraftApiHelper.getObjectAspects(new ItemStack(Items.field_151113_aN));
        check(tags != null && tags.getAmount(time) == 4, "scripted clock aspects");
        check(ResearchCategories.getResearch("TD_ASPECT_PROBE") != null, "research created");
        check(ResearchCategories.getResearch("TD_ASPECT_PROBE").tags.getAmount(time) == 3, "research uses custom aspects");
        CrucibleRecipe recipe = ThaumcraftApi.getCrucibleRecipe(new ItemStack(Items.field_151045_i));
        check(recipe != null && recipe.aspects.getAmount(time) == 4 && recipe.aspects.getAmount(moment) == 2, "scripted crucible costs");
        check(recipe.matches(new AspectList().add(time, 4).add(moment, 2), new ItemStack(Items.field_151044_h)), "actual crucible match");
        check(!recipe.matches(new AspectList().add(time, 3).add(moment, 2), new ItemStack(Items.field_151044_h)), "actual crucible shortage");
    }

    public void storage(World world, EntityPlayer player) throws Exception {
        AspectList input = new AspectList().add(time, 17).add(moment, 5);
        NBTTagCompound tags = new NBTTagCompound();
        input.writeToNBT(tags);
        AspectList output = new AspectList();
        output.readFromNBT(tags);
        check(output.getAmount(time) == 17 && output.getAmount(moment) == 5, "aspect list NBT round trip");

        TileJarFillable jar = new TileJarFillable();
        jar.func_145834_a(world);
        jar.aspectFilter = time;
        check(jar.addEssentia(time, 24, ForgeDirection.UP) == 24, "essentia input");
        check(jar.getEssentiaType(ForgeDirection.UP) == time && jar.amount == 24, "jar contents");
        check(jar.takeEssentia(time, 7, ForgeDirection.UP) == 7 && jar.amount == 17, "essentia output");
        NBTTagCompound saved = new NBTTagCompound();
        jar.writeCustomNBT(saved);
        TileJarFillable restored = new TileJarFillable();
        restored.readCustomNBT(saved);
        check(restored.aspect == time && restored.aspectFilter == time && restored.amount == 17, "jar and label NBT");
        File disk = new File("custom-aspect-jar.dat");
        if (disk.exists()) {
            TileJarFillable previous = new TileJarFillable();
            previous.readCustomNBT(CompressedStreamTools.func_74797_a(disk));
            check(previous.aspect == time && previous.amount == 17, "jar survives full process restart");
            System.out.println("TD_ASPECT_DISK_RESTART_PASS");
        }
        CompressedStreamTools.func_74795_b(saved, disk);

        TileCentrifuge centrifuge = new TileCentrifuge();
        centrifuge.func_145834_a(world);
        Method process = TileCentrifuge.class.getDeclaredMethod("processEssentia");
        process.setAccessible(true);
        for (int i = 0; i < 20; i++) {
            check(centrifuge.addEssentia(time, 1, ForgeDirection.DOWN) == 1, "centrifuge accepts custom compound");
            process.invoke(centrifuge);
            check(centrifuge.aspectOut == Aspect.ORDER || centrifuge.aspectOut == Aspect.VOID, "centrifuge outputs native component");
            centrifuge.aspectOut = null;
        }

        String name = player.func_70005_c_();
        Thaumcraft.proxy.playerKnowledge.addDiscoveredAspect(name, time);
        Thaumcraft.proxy.playerKnowledge.setAspectPool(name, time, (short) 11);
        check(Thaumcraft.proxy.playerKnowledge.hasDiscoveredAspect(name, time), "discovery");
        NBTTagCompound knowledge = new NBTTagCompound();
        ResearchManager.saveAspectNBT(knowledge, player);
        Thaumcraft.proxy.playerKnowledge.aspectsDiscovered.remove(name);
        ResearchManager.loadAspectNBT(knowledge, player);
        check(Thaumcraft.proxy.playerKnowledge.getAspectPoolFor(name, time) == 11, "player knowledge save and load");
        ByteBuf buffer = Unpooled.buffer();
        try {
            new PacketSyncAspects(player).toBytes(buffer);
            PacketSyncAspects decoded = new PacketSyncAspects();
            decoded.fromBytes(buffer);
            Field data = PacketSyncAspects.class.getDeclaredField("data");
            data.setAccessible(true);
            check(((AspectList) data.get(decoded)).getAmount(time) == 11, "actual TC packet serialization");
        } finally {
            buffer.release();
        }
    }

    public void validation() {
        int size = Aspect.aspects.size();
        rejects(() -> definition(null, "aer", "terra"), "null ID");
        rejects(() -> definition("bad id", "aer", "terra"), "invalid ID");
        rejects(() -> definition(String.join("", Collections.nCopies(65, "a")), "aer", "terra"), "long ID");
        rejects(() -> new Definition("test", -1, "a:b.png", "aer", "terra", "test"), "negative RGB");
        rejects(() -> new Definition("test", 0x1000000, "a:b.png", "aer", "terra", "test"), "oversized RGB");
        rejects(() -> new Definition("test", 0, "a:../b.png", "aer", "terra", "test"), "traversal icon");
        rejects(() -> new Definition("test", 0, "https://example.com/b.png", "aer", "terra", "test"), "remote icon");
        rejects(() -> new Definition("test", 0, "a:b.png", "aer", "terra", " "), "empty description");
        rejects(() -> validate(definition("aer", "aer", "terra")), "existing ID");
        rejects(() -> validate(definition("test", "aer", "terra"), definition("test", "ignis", "terra")), "duplicate ID");
        rejects(() -> validate(definition("test", "terra", "aqua")), "native duplicate pair reversed");
        rejects(() -> validate(definition("test", "vacuos", "ordo")), "custom duplicate pair reversed");
        rejects(() -> validate(definition("test", "missingaspect", "terra")), "missing component");
        rejects(() -> validate(definition("test", "test", "terra")), "self cycle");
        rejects(() -> validate(definition("testa", "testb", "terra"), definition("testb", "testa", "ordo")), "indirect cycle");
        rejects(() -> validate(definition("testa", "aer", "terra"), definition("testb", "terra", "aer")), "duplicate new pair");
        List<Definition> ordered = CustomAspectRegistry.validate(Arrays.asList(definition("testa", "testb", "terra"), definition("testb", "aer", "aer")));
        check(ordered.get(0).tag.equals("testb") && ordered.get(1).tag.equals("testa"), "topological registration order");
        rejects(() -> CustomAspectsZen.register("test", 0, "a:b.png", "aer", "terra", "test"), "normal script cannot register late");
        rejects(() -> CustomAspectRegistry.registerAll(Collections.emptyList()), "registration is frozen");
        check(Aspect.aspects.size() == size, "validation never modifies the registry");
    }

    private static Definition definition(String tag, String first, String second) {
        return new Definition(tag, 0xFFFFFF, "thaumcraft:textures/aspects/ordo.png", first, second, "test");
    }

    private static void validate(Definition... definitions) {
        CustomAspectRegistry.validate(Arrays.asList(definitions));
    }

    private void rejects(Runnable action, String message) {
        try {
            action.run();
        } catch (IllegalArgumentException | IllegalStateException expected) {
            check(true, message);
            return;
        }
        throw new AssertionError("Expected rejection: " + message);
    }

    public void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
}
