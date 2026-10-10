// Open Construct Demo -> Portal Constructs. All three pages unlock automatically.
// These pages describe structures; they do not register portal mechanics.
import mods.thaumcraft.Research;
import minetweaker.item.IItemStack;

Research.addTab("TD_CONSTRUCT_DEMO", "minecraft", "textures/items/ender_eye.png");
game.setLocalization("en_US", "tc.research_category.TD_CONSTRUCT_DEMO", "Construct Demo");
Research.addResearch("TD_PORTAL_CONSTRUCTS", "TD_CONSTRUCT_DEMO", "", 0, 0, 0, <minecraft:obsidian>);
Research.setAutoUnlock("TD_PORTAL_CONSTRUCTS", true);
game.setLocalization("en_US", "tc.research_name.TD_PORTAL_CONSTRUCTS", "Portal Constructs");
game.setLocalization("en_US", "tc.research_text.TD_PORTAL_CONSTRUCTS", "Doors to other worlds");

val empty = null as IItemStack;
val obsidian = <minecraft:obsidian>;
val glowstone = <minecraft:glowstone>;

// Each layer is a list of rows. Layers are listed from bottom to top.
// Nether: 4 wide, 5 high, 1 deep; flint and steel activates the frame.
Research.addConstructPage("TD_PORTAL_CONSTRUCTS", [
    [[obsidian, obsidian, obsidian, obsidian]],
    [[obsidian, empty, empty, obsidian]],
    [[obsidian, empty, empty, obsidian]],
    [[obsidian, empty, empty, obsidian]],
    [[obsidian, obsidian, obsidian, obsidian]]
], <minecraft:flint_and_steel>);

// Aether: same frame, made from glowstone; activated with a water bucket.
Research.addConstructPage("TD_PORTAL_CONSTRUCTS", [
    [[glowstone, glowstone, glowstone, glowstone]],
    [[glowstone, empty, empty, glowstone]],
    [[glowstone, empty, empty, glowstone]],
    [[glowstone, empty, empty, glowstone]],
    [[glowstone, glowstone, glowstone, glowstone]]
], <minecraft:water_bucket>);

// Twilight Forest: 2x2 water pool in a grass border, flowers above, diamond activation.
val grass = <minecraft:grass>;
val water = <minecraft:water>;
val flower = <minecraft:red_flower:*>;
Research.addConstructPage("TD_PORTAL_CONSTRUCTS", [
    [
        [grass, grass, grass, grass],
        [grass, water, water, grass],
        [grass, water, water, grass],
        [grass, grass, grass, grass]
    ],
    [
        [flower, flower, flower, flower],
        [flower, empty, empty, flower],
        [flower, empty, empty, flower],
        [flower, flower, flower, flower]
    ]
], <minecraft:diamond>);
