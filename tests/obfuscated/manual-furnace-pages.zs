// Thaumic Dabblery furnace-page demo. Supports either ModTweaker fork.
// Open the Thaumonomicon's "Furnace Demo" tab, then "Baked Treats".
// Remove this script and /mt reload to remove the demo and its recipe.
import mods.thaumcraft.Research;

furnace.addRecipe(<minecraft:cookie> * 2, <minecraft:bowl>, 0.5);

Research.addTab("TD_FURNACE_DEMO", "minecraft", "textures/blocks/furnace_front_off.png");
game.setLocalization("en_US", "tc.research_category.TD_FURNACE_DEMO", "Furnace Demo");
Research.addResearch("TD_BAKED_TREATS", "TD_FURNACE_DEMO", "", 0, 0, 0, <minecraft:cookie>);
Research.setAutoUnlock("TD_BAKED_TREATS", true);
game.setLocalization("en_US", "tc.research_name.TD_BAKED_TREATS", "Baked Treats");
game.setLocalization("en_US", "tc.research_text.TD_BAKED_TREATS", "A furnace recipe page demonstration");
game.setLocalization("en_US", "td.research_page.baked_treats", "A small scripting demonstration: smelt one wooden bowl into two cookies.<BR>The facing page uses Thaumcraft's native smelting display. Try the recipe in an ordinary furnace.<BR>This research unlocks automatically. It documents the recipe; smelting does not require research.");
Research.addPage("TD_BAKED_TREATS", "td.research_page.baked_treats");
// Furnace pages take the INPUT, unlike other ModTweaker recipe-page helpers.
Research.addFurnacePage("TD_BAKED_TREATS", <minecraft:bowl>);
