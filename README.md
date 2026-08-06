# Thaumic Dabblery

![logo](images/logo_small.png)

Extended ModTweaker Thaumcraft 4 compat.


[![hub](images/badges/github.png)](https://github.com/JackOfNoneTrades/ThaumicDabblery/releases)
![forge](images/badges/forge.png)

<!--
[![modrinth](images/badges/modrinth.png)](https://modrinth.com/mod/thaumic-dabblery)
[![curse](images/badges/curse.png)](https://www.curseforge.com/minecraft/mc-mods/thaumic-dabblery)
[![67](images/badges/67.png)](https://67.fentanylsolutions.org/mod/thaumic-dabblery)
[![maven](images/badges/maven.png)](https://maven.fentanylsolutions.org/#/releases/org/fentanylsolutions/thaumicdabblery/ThaumicDabblery)
-->


## Dependencies

* [Thaumcraft 4](https://www.curseforge.com/minecraft/mc-mods/thaumcraft) [![curse](images/icons/curse.png)](https://www.curseforge.com/minecraft/mc-mods/thaumcraft)
* [UniMixins](https://modrinth.com/mod/unimixins) [![curse](images/icons/curse.png)](https://www.curseforge.com/minecraft/mc-mods/unimixins) [![modrinth](images/icons/modrinth.png)](https://modrinth.com/mod/unimixins/versions) [![git](images/icons/git.png)](https://github.com/LegacyModdingMC/UniMixins/releases)

## ModTweaker

Vis discounts are integer percentage points. A scripted value replaces an item's native discount; the two values are
never added together. Aspect-specific values take precedence over the universal value for that aspect.

```js
// Set a universal 5% vis discount.
mods.thaumcraft.VisDiscount.set(<minecraft:golden_helmet>, 5);

// Override only the Aer discount with 12%.
mods.thaumcraft.VisDiscount.set(<minecraft:golden_helmet>, <aspect:aer>, 12);
```

Only equipped armor and Baubles contribute. Expanded Baubles slots beyond the original four are supported.

### Research scan gates

Research can be hidden until the player completes one or more Thaumometer scans. Every `require...` call adds an
independent requirement, so calls for the same research key are combined with **AND**.

```js
// BONEBOW is revealed only after scanning both a bow and a bone.
mods.thaumcraft.ResearchScanGates.requireItem("BONEBOW", <minecraft:bow>);
mods.thaumcraft.ResearchScanGates.requireItem("BONEBOW", <minecraft:bone>);

// MIRROR is revealed only after scanning an Enderman and an ender pearl.
mods.thaumcraft.ResearchScanGates.requireEntity("MIRROR", "Enderman");
mods.thaumcraft.ResearchScanGates.requireItem("MIRROR", <minecraft:ender_pearl>);

// Wildcards work too: any planks AND a cow.
mods.thaumcraft.ResearchScanGates.requireItem("TINYGLASSES", <minecraft:planks:*>);
mods.thaumcraft.ResearchScanGates.requireEntity("TINYGLASSES", "Cow");
```

Use `requireAnyItem` or `requireAnyEntity` to make one requirement accept alternatives. Alternatives within that one
call are combined with **OR**, while separate calls are still combined with **AND**.

```zenscript
// (red wool OR blue wool) AND (Cow OR Mooshroom)
mods.thaumcraft.ResearchScanGates.requireAnyItem(
    "BANNERS",
    [<minecraft:wool:14>, <minecraft:wool:11>]
);
mods.thaumcraft.ResearchScanGates.requireAnyEntity(
    "BANNERS",
    ["Cow", "MushroomCow"]
);
```

Clear every scripted requirement for an entry without deleting or recreating the research:

```zenscript
mods.thaumcraft.ResearchScanGates.clear("BANNERS");
```

While a gate is active, it replaces that entry's native item, entity, and aspect discovery triggers and makes the
entry strictly scan-discovered. Clearing or undoing the gate restores its original triggers and visibility flags.
Progress is stored per player, and a target scanned before a gate was installed may be scanned once more to recover
the missing requirement without awarding aspects twice. Entities still need registered Thaumcraft aspects to be
valid scan targets. Fully qualified calls such as the examples require no import. In a dedicated pack, distribute the
gate scripts to both client and server; the client copy is needed to recognize recovery scans of previously scanned
targets.

## Building

`./gradlew build`.

## Credits

* [GT:NH buildscript](https://github.com/GTNewHorizons/ExampleMod1.7.10)
* Catalogue Vintage banner by [u/RShotZz](https://www.reddit.com/user/RShotZz/) ([source](https://www.reddit.com/media?url=https%3A%2F%2Fi.redd.it%2Flz64ouwgxaw61.png))

## License

`CC BY 4.0`.

<br>

![license](images/license_small.png)
