# Thaumic Dabblery

Miscellaneous Thaumcraft 4 patches for Minecraft 1.7.10.


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
* [CraftTweaker](https://modrinth.com/mod/crafttweaker) (optional) [![curse](images/icons/curse.png)](https://www.curseforge.com/minecraft/mc-mods/crafttweaker) [![modrinth](images/icons/modrinth.png)](https://modrinth.com/mod/crafttweaker) [![git](images/icons/git.png)](https://github.com/GTNewHorizons/CraftTweaker)
* [ModTweaker](https://modrinth.com/mod/modtweaker) (optional) [![curse](images/icons/curse.png)](https://www.curseforge.com/minecraft/mc-mods/modtweaker) [![modrinth](images/icons/modrinth.png)](https://modrinth.com/mod/modtweaker) [![git](images/icons/git.png)](https://github.com/jaredlll08/ModTweaker)
* [ContentTweaker](https://modrinth.com/mod/contenttweaker) (optional) [![curse](images/icons/curse.png)](https://www.curseforge.com/minecraft/mc-mods/contenttweaker) [![modrinth](images/icons/modrinth.png)](https://modrinth.com/mod/contenttweaker) [![git](images/icons/git.png)](https://github.com/CraftTweaker/ContentTweaker)
* [UniMixins](https://modrinth.com/mod/unimixins) [![curse](images/icons/curse.png)](https://www.curseforge.com/minecraft/mc-mods/unimixins) [![modrinth](images/icons/modrinth.png)](https://modrinth.com/mod/unimixins/versions) [![git](images/icons/git.png)](https://github.com/LegacyModdingMC/UniMixins/releases)

## ModTweaker

Vis discounts are integer percentage points. A scripted value replaces an item's native discount; the two values are
never added together. Aspect-specific values take precedence over the universal value for that aspect.

```zenscript
// Set a universal 5% vis discount.
mods.thaumcraft.VisDiscount.set(<minecraft:golden_helmet>, 5);

// Override only the Aer discount with 12%.
mods.thaumcraft.VisDiscount.set(<minecraft:golden_helmet>, <aspect:aer>, 12);
```

Only equipped armor and Baubles contribute. Expanded Baubles slots beyond the original four are supported.

## Building

`./gradlew build`.

## Credits

* [GT:NH buildscript](https://github.com/GTNewHorizons/ExampleMod1.7.10)
* Catalogue Vintage banner by [u/RShotZz](https://www.reddit.com/user/RShotZz/) ([source](https://www.reddit.com/media?url=https%3A%2F%2Fi.redd.it%2Flz64ouwgxaw61.png))

## License

`CC BY 4.0`.

<br>

![license](images/license_small.png)
