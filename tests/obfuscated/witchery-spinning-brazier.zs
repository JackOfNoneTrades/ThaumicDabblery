import mods.witchery.SpinningWheel;
import mods.witchery.Brazier;

SpinningWheel.addRecipe("custom:thread", <minecraft:wool> * 2,
    [<minecraft:redstone>, <minecraft:glowstone_dust>],
    <minecraft:string>.withTag({spinning: 1}) * 4);
SpinningWheel.addRecipe("custom:solo", <minecraft:stick> * 3, [], <minecraft:diamond>);

Brazier.addSummoningRecipe("custom:guardian",
    [<minecraft:bone>, <minecraft:iron_ingot>, <minecraft:diamond>],
    "Pig", 6, {CustomName: "Crypt Guardian", PersistenceRequired: 1});
Brazier.setPowerPerTick("custom:guardian", 0.5);
Brazier.setBookText("custom:guardian", "Crypt Guardian", "Summons a pig when the offerings finish burning.");
Brazier.copyRecipe("custom:war_incense", "builtin:strong");
Brazier.setItems("custom:war_incense", [<minecraft:coal>, <minecraft:redstone>, <minecraft:bone>]);
Brazier.setBurnTicks("custom:war_incense", 120);
Brazier.setPowerPerTick("custom:war_incense", 0);
Brazier.setBookText("custom:war_incense", "War Incense", "Strengthens creatures near the brazier.");
