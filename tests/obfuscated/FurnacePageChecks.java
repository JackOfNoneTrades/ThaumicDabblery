package tdtest;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.tileentity.TileEntityFurnace;
import minetweaker.MineTweakerImplementationAPI;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.*;

public final class FurnacePageChecks {
    public int checks;
    public void check(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
    public static ItemStack stack(String name) { return new ItemStack((Item) Item.field_150901_e.func_82594_a("minecraft:" + name)); }
    private ResearchItem item(String key) { return ResearchCategories.getResearch(key); }
    private ResearchPage[] pages(String key) { return item(key).getPages(); }
    private void script(String source) throws Exception {
        Files.write(Paths.get("scripts/zz-furnace-validation.zs"), source.getBytes(StandardCharsets.UTF_8));
        MineTweakerImplementationAPI.reload();
    }
    private String page(String input) { return "mods.thaumcraft.Research.addFurnacePage(\"TD_FURNACE_BASE\", " + input + ");\n"; }
    public void baseline() {
        check(item("TD_BAKED_TREATS") != null, "script compiled against Research expansion");
        ResearchPage[] p = pages("TD_BAKED_TREATS");
        check(p.length == 2 && p[0].type == ResearchPage.PageType.TEXT && p[1].type == ResearchPage.PageType.SMELTING, "text and smelting page order");
        check(((ItemStack)p[1].recipe).func_77969_a(stack("bowl")), "page uses input");
        check(p[1].recipeOutput.func_77969_a(stack("cookie")) && p[1].recipeOutput.field_77994_a == 2, "custom output and count");
        check(FurnaceRecipes.func_77602_a().func_151395_a(stack("bowl")).field_77994_a == 2, "real smelting recipe exists");
        check(item("TD_BAKED_TREATS").isAutoUnlock(), "manual demo is automatically unlocked");
    }
    public void run() throws Exception {
        check(Boolean.FALSE.equals(Launch.blackboard.get("fml.deobfuscatedEnvironment")), "obfuscated environment");
        baseline();
        TileEntityFurnace furnace = new TileEntityFurnace();
        furnace.func_70299_a(0, stack("bowl")); furnace.func_145949_j();
        check(furnace.func_70301_a(0) == null, "native furnace consumes bowl");
        check(furnace.func_70301_a(2).func_77969_a(stack("cookie")) && furnace.func_70301_a(2).field_77994_a == 2, "native furnace produces two cookies");
        ResearchPage[] nativePages = pages("CRUCIBLE");
        ResearchPage[] original = {new ResearchPage("original")};
        ResearchItem base = new ResearchItem("TD_FURNACE_BASE", "BASICS", new AspectList(), 100, 100, 0, stack("bowl")).setPages(original).registerResearchItem();
        ResearchItem empty = new ResearchItem("TD_FURNACE_NULL", "BASICS", new AspectList(), 102, 100, 0, stack("bowl")).registerResearchItem();
        Path demo = Paths.get("scripts/manual-furnace-pages.zs");
        byte[] demoText = Files.readAllBytes(demo);
        try {
            script(page("<minecraft:iron_ore>") + page("<Thaumcraft:ItemShard:6>") + page("<minecraft:bowl>"));
            check(base.getPages().length == 4 && base.getPages()[0] == original[0], "append multiple pages retaining identity");
            check(base.getPages()[1].recipeOutput.func_77969_a(stack("iron_ingot")), "vanilla smelting");
            check(base.getPages()[2].recipeOutput.func_77960_j() == 14, "Salis Mundus recipe");
            baseline();
            for (int i=0;i<3;i++) { MineTweakerImplementationAPI.reload(); check(base.getPages().length == 4, "reload does not duplicate pages"); }
            script("");
            check(base.getPages() == original, "undo restores original array");
            script("mods.thaumcraft.Research.addFurnacePage(\"TD_FURNACE_NULL\", <minecraft:iron_ore>);");
            check(empty.getPages().length == 1, "append to null pages");
            script(""); check(empty.getPages() == null, "undo restores null pages");
            script("mods.thaumcraft.Research.clearPages(\"TD_FURNACE_BASE\");\n" + page("<minecraft:iron_ore>"));
            check(base.getPages().length == 1, "clear then append");
            script(""); check(base.getPages() == original, "clear then append undo");
            script(page("<minecraft:iron_ore>") + "mods.thaumcraft.Research.clearPages(\"TD_FURNACE_BASE\");");
            check(base.getPages().length == 0, "append then clear");
            script(""); check(base.getPages() == original, "append then clear undo");
            script(page("<minecraft:iron_ore>") + "mods.thaumcraft.Research.moveResearch(\"TD_FURNACE_BASE\", \"ALCHEMY\", 100, 100);");
            check("ALCHEMY".equals(base.category) && base.getPages().length == 2, "moving research retains furnace page");
            script(""); check("BASICS".equals(base.category) && base.getPages() == original, "moved research undo");
            script(page("<minecraft:iron_ore>") + "mods.thaumcraft.Research.removeResearch(\"TD_FURNACE_BASE\");");
            check(item("TD_FURNACE_BASE") == null, "remove research with furnace page");
            script(""); check(item("TD_FURNACE_BASE") == base && base.getPages() == original, "removed research undo");
            script(page("null") + page("<minecraft:iron_ore> * 2") + page("<minecraft:log:*>") + page("<minecraft:diamond>")
                + "mods.thaumcraft.Research.addFurnacePage(\"TD_DOES_NOT_EXIST\", <minecraft:iron_ore>);\n"
                + "mods.thaumcraft.Research.addFurnacePage(\"\", <minecraft:iron_ore>);\n");
            check(base.getPages() == original, "invalid inputs cause no mutation");
            script("furnace.addRecipe(<minecraft:cookie>, <minecraft:wool:14>);\n"
                + "furnace.addRecipe(<minecraft:cookie>, <minecraft:wool:11>);\n"
                + page("<minecraft:wool:14>") + page("<minecraft:wool:11>"));
            check(((ItemStack)base.getPages()[1].recipe).func_77960_j() == 14 && ((ItemStack)base.getPages()[2].recipe).func_77960_j() == 11, "same output with distinct metadata inputs");
            script(page("<minecraft:bowl>") + "furnace.remove(<minecraft:cookie>, <minecraft:bowl>);\nfurnace.addRecipe(<minecraft:diamond>, <minecraft:bowl>);");
            check(base.getPages()[1].recipeOutput.func_77969_a(stack("diamond")), "refresh output after later replacement");
            check(pages("TD_BAKED_TREATS")[1].recipeOutput.func_77969_a(stack("diamond")), "all managed output links refreshed");
            script(page("<minecraft:bowl>") + "furnace.remove(<minecraft:cookie>, <minecraft:bowl>);");
            check(base.getPages() == original, "later removed recipe removes blank page");
            check(pages("TD_BAKED_TREATS").length == 1, "all invalid final pages removed");
            script(""); baseline();
            script(page("<minecraft:iron_ore>"));
            ResearchPage foreign = new ResearchPage("foreign");
            base.setPages(base.getPages()[0], base.getPages()[1], foreign);
            script(""); check(base.getPages().length == 2 && base.getPages()[1] == foreign, "undo preserves unrelated page changes");
            base.setPages(original);
            script(page("<minecraft:iron_ore>"));
            ResearchItem replacement = new ResearchItem("TD_FURNACE_BASE", "BASICS", new AspectList(), 100, 100, 0, stack("bowl")).setPages(foreign);
            ResearchCategories.getResearchList("BASICS").research.put("TD_FURNACE_BASE", replacement);
            script("");
            check(replacement.getPages().length == 1 && replacement.getPages()[0] == foreign, "undo does not touch replacement research");
            check(base.getPages() == original, "undo cleans detached original object");
            ResearchCategories.getResearchList("BASICS").research.put("TD_FURNACE_BASE", base);
            Files.delete(demo); MineTweakerImplementationAPI.reload();
            check(item("TD_BAKED_TREATS") == null, "removing demo removes research");
            check(FurnaceRecipes.func_77602_a().func_151395_a(stack("bowl")) == null, "removing demo removes smelting recipe");
            Files.write(demo,demoText); MineTweakerImplementationAPI.reload(); baseline();
            check(pages("CRUCIBLE") == nativePages, "native Basic Alchemy pages unchanged");
        } finally {
            Files.write(demo,demoText); Files.deleteIfExists(Paths.get("scripts/zz-furnace-validation.zs"));
            MineTweakerImplementationAPI.reload();
            ResearchCategories.getResearchList("BASICS").research.remove("TD_FURNACE_BASE");
            ResearchCategories.getResearchList("BASICS").research.remove("TD_FURNACE_NULL");
        }
    }
}
