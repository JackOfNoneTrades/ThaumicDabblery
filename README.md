# Thaumic Dabblery

![logo](images/logo_small.png)

## Features

* Extends ModTweaker Thaumcraft 4 compatibility
    * Vis discount modification for equippables (armor and baubles)
    * Entity and item scanning based prerequisites for research
    * Thaumic Horizons player vat infusion recipe modification
* Thaumic Horizons self-infusion allowing the player to cast Witchery Mystic Branch spells using a keybind (WIP)

Documentation can be found [here](https://github.com/JackOfNoneTrades/ThaumicDabblery/wiki/Documentation)

Thaumic Horizons vat recipes can be replaced by infusion ID:

```zenscript
mods.thaumichorizons.SelfInfusion.removeRecipe(11);
mods.thaumichorizons.SelfInfusion.addRecipe(
    "thaumicdabbleryMysticBranch",
    11,
    10,
    "corpus 24, spiritus 32, humanus 16, motus 16, praecantatio 64",
    [<minecraft:cactus>]
);
```

The arguments to `addRecipe` are the research key, infusion effect ID, instability,
essentia costs, and pedestal items. An empty research key removes the research
requirement. Adding a new ID only defines its recipe; Java code must still implement
the corresponding player effect. Existing Thaumonomicon recipe pages are synchronized
with scripted removals and replacements.

[![hub](images/badges/github.png)](https://github.com/JackOfNoneTrades/ThaumicDabblery/releases)
[![modrinth](images/badges/modrinth.png)](https://modrinth.com/mod/thaumic-dabblery/settings/versions)
[![curse](images/badges/curse.png)](https://www.curseforge.com/minecraft/mc-mods/thaumic-dabblery)
![forge](images/badges/forge.png)

<!--
[![modrinth](images/badges/modrinth.png)](https://modrinth.com/mod/thaumic-dabblery)
[![curse](images/badges/curse.png)](https://www.curseforge.com/minecraft/mc-mods/thaumic-dabblery)
[![67](images/badges/67.png)](https://67.fentanylsolutions.org/mod/thaumic-dabblery)
[![maven](images/badges/maven.png)](https://maven.fentanylsolutions.org/#/releases/org/fentanylsolutions/thaumicdabblery/ThaumicDabblery)
-->

## Dependencies

* [Thaumcraft 4](https://www.curseforge.com/minecraft/mc-mods/thaumcraft) [![curse](images/icons/curse.png)](https://www.curseforge.com/minecraft/mc-mods/thaumcraft)
* [Modtweaker](https://www.curseforge.com/minecraft/mc-mods/modtweaker) [![curse](images/icons/curse.png)](https://www.curseforge.com/minecraft/mc-mods/modtweaker) [![modrinth](images/icons/modrinth.png)](https://modrinth.com/mod/modtweaker/versions) 
* [UniMixins](https://modrinth.com/mod/unimixins) [![curse](images/icons/curse.png)](https://www.curseforge.com/minecraft/mc-mods/unimixins) [![modrinth](images/icons/modrinth.png)](https://modrinth.com/mod/unimixins/versions) [![git](images/icons/git.png)](https://github.com/LegacyModdingMC/UniMixins/releases)

## Building

`./gradlew build`.

## Credits

* [GT:NH buildscript](https://github.com/GTNewHorizons/ExampleMod1.7.10)

## License

`CC BY 4.0`.

<br>

![license](images/license_small.png)
