import mods.thaumcraft.WarpEvents;

WarpEvents.register("weakness", 50, 100, [
    "/effect @w 18 99 4",
    "playsound mob.endermen.stare @w ~ ~ ~"
]);

// Research target for the command probe, registered before addon caches are populated.
mods.thaumcraft.Research.addResearch("TD_WARP_TEST", "BASICS", "", 101, 101, 0, <minecraft:diamond>);
