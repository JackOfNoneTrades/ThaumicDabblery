import mods.witchery.Distillery;
Distillery.addRecipe("custom:diamond", [<minecraft:dirt>, <minecraft:cookie>], [<minecraft:diamond>.withTag({distillery:1}) * 2, <minecraft:emerald>, <minecraft:coal> * 3, <minecraft:redstone> * 4], 2);
Distillery.addRecipe("custom:solo", [<minecraft:stick>], [<minecraft:emerald>]);
Distillery.addRecipe("custom:double", [<minecraft:cookie>, <minecraft:cookie>], [<minecraft:coal>], 1);
