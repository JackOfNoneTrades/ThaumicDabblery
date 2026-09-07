// this must compile on the first start, not just after a reload
val time = <aspect:tdtempus>;
print("TD_CUSTOM_ASPECT_BRACKET " + time.name);
mods.thaumcraft.Aspects.set(<minecraft:clock>, "tdtempus 4");
mods.thaumcraft.Research.addResearch("TD_ASPECT_PROBE", "BASICS", "tdtempus 3, tdmoment 1", 12, 12, 1, <minecraft:clock>);
mods.thaumcraft.Crucible.addRecipe("TD_ASPECT_PROBE", <minecraft:diamond>, <minecraft:coal>, "tdtempus 4, tdmoment 2");
game.setLocalization("en_US", "tc.aspect.tdtempus", "time from script");
