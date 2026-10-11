import mods.witchery.Cauldron;
Cauldron.addRecipe("custom:diamond", [<minecraft:nether_wart>, <minecraft:dirt>],
    <minecraft:cookie>, <minecraft:diamond>.withTag({cauldron: 1}) * 2, 100);
Cauldron.addRecipe("custom:neutral", [<minecraft:bedrock>], <minecraft:wooden_hoe>, <minecraft:emerald> * 2);
Cauldron.setOutput("native:minecraft:porkchop/0/0", <minecraft:cookie> * 2);
Cauldron.setPower("native:minecraft:porkchop/0/0", 100);
Cauldron.removeRecipe("native:minecraft:beef/0/0");
