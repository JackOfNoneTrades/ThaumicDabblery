# Thaumic Dabblery

![logo](images/logo_small.png)

## Features

* Extends ModTweaker Thaumcraft 4 compatibility
    * Custom primal and compound aspects with scripted names, icons, colors, components, and descriptions
    * Custom warp events with warp ranges, command sequences, player targeting and an operator testing command
    * Gadomancy Aura Pylon potion effects for custom or existing aspects, with multiple entity targets and reload-safe replacement of native effects
    * Vis discount modification for equippables (armor and baubles)
    * Per-aspect wand cap and fixed casting item Vis discount customization, including Thaumic Bases bracelets
    * Wand cap/core assembly cost customization
    * Wand core capacity, Vis regeneration, and innate Potency customization, including individual Thaumic Bases and Thaumic Concilium bracelets
    * Equipped item warp and runic shielding modification
    * Entity and item scanning based prerequisites for research
    * Safe research movement and removal; ordinary tab removal waits for editor moves and preserves entries moved out
    * Reload-safe Thaumonomicon tab name, icon, and background overrides
    * Reload-safe Thaumonomicon tab ordering, including compatibility with TC4Tweaks
    * Focal Manipulator upgrade path customization
    * Thaumic Horizons player and creature vat recipe modification, custom mob transformations and configurable Thaumonomicon display items
* Scripted head/body tracking for vat creatures and effigies, with configurable item-frame rotation controls
* Scripted Planar Vortex item and creature recipes, with NBT support and native recipe removal
* Scripted Osmotic Enchanter enchantments, vis costs, icons and research requirements, with paginated selection
* Scripted Witchery rite offerings, living sacrifices, circles, initial altar power, time and weather conditions
* Thaumic Horizons Corpse Effigies display bound player skins, with personal appearances for shared Soul Beacons
* Single-player Thaumonomicon editor with drag placement, parent editing, cross-tab moves and swaps, properties, deletion, undo/redo and compact script saving through `/td edit`
* Operator command `/td scanall [player]` to complete item/entity scans, discover aspects and reveal scan-dependent research without awarding research points
* Configurable minimum Vis cost for casting and crafting, including Thaumic Bases bracelets
* Configurable champion mob whitelist/blacklist, base spawn chance and guaranteed champions
* Scriptable Baubles Expanded slot assignments for ordinary items and existing baubles, including multiple allowed slot types and GTNH/original Witching Gadgets compatibility
* Salis Arcana compatibility preventing wand cap/core replacement from changing Thaumic Bases and Thaumic Concilium bracelet variants
* Thaumic Horizons self-infusion allowing the player to cast Witchery Mystic Branch spells using a keybind, with a creative grant item.

Documentation can be found [here](https://github.com/JackOfNoneTrades/ThaumicDabblery/wiki/Documentation).

### Gadomancy Aura Pylon scripting

With Gadomancy installed, put rules in ordinary `scripts/*.zs` files:

```zenscript
import mods.gadomancy.AuraPylon;

AuraPylon.clear("aqua"); // Remove all native and earlier scripted behavior.
AuraPylon.add("aqua", "players", 13); // Water breathing.
AuraPylon.add("aqua", "players", 16); // Night vision, alongside water breathing.
AuraPylon.add("aqua", "entity:Squid", 13);
AuraPylon.removeCustom("aqua", "players", 16);
```

Targets are `players`, `living`, `undead`, or `entity:<registered ID>` (case sensitive).
Aspect tags include custom aspects registered at startup. `add` preserves existing native
behavior unless cleared first; `remove(aspect)` is an alias for `clear(aspect)`.
`removeCustom` removes all scripted rules matching the aspect, target, and potion ID.
Reloading after removing scripts restores the original effects.

The extended overload is `add(aspect, target, potionId, amplifier, addedTicks, maxTicks,
intervalTicks, range)`. Defaults are `0, 10, 1200, 4, 8`: level I, adding 10 duration
ticks every 4 ticks up to 60 seconds, using Gadomancy's box range of 8 blocks.
Amplifiers must be 0–127; range must be greater than 0 and at most 64. Instant potions
are unsupported. Native potion immunity and same-potion merging still apply. Exact
duplicate rules apply once; different overlapping rules can both add duration.

Clearing stops future behavior, including native block effects; existing potions expire
normally and past world changes remain. Native Lux spawn suppression can linger for
up to 16 ticks. Gadomancy's aspect blacklist still applies. Disable the feature through
`features.auraPylon.enabled` to restore native behavior. For new or changed research
descriptions, supply `gadomancy.aura.effect.<aspect tag>` in a language resource pack.

[![hub](images/badges/github.png)](https://github.com/JackOfNoneTrades/ThaumicDabblery/releases)
[![modrinth](images/badges/modrinth.png)](https://modrinth.com/mod/thaumic-dabblery/settings/versions)
[![curse](images/badges/curse.png)](https://www.curseforge.com/minecraft/mc-mods/thaumic-dabblery)
[![67](images/badges/67.png)](https://67.fentanylsolutions.org/mod/thaumic-dabblery)
[![maven](images/badges/maven.png)](https://maven.fentanylsolutions.org/#/releases/org/fentanylsolutions/thaumicdabblery/ThaumicDabblery)
![forge](images/badges/forge.png)

## Dependencies

* [Thaumcraft 4](https://www.curseforge.com/minecraft/mc-mods/thaumcraft) [![curse](images/icons/curse.png)](https://www.curseforge.com/minecraft/mc-mods/thaumcraft)
* [Modtweaker](https://www.curseforge.com/minecraft/mc-mods/modtweaker) [![curse](images/icons/curse.png)](https://www.curseforge.com/minecraft/mc-mods/modtweaker) [![modrinth](images/icons/modrinth.png)](https://modrinth.com/mod/modtweaker/versions) 
* [UniMixins](https://modrinth.com/mod/unimixins) [![curse](images/icons/curse.png)](https://www.curseforge.com/minecraft/mc-mods/unimixins) [![modrinth](images/icons/modrinth.png)](https://modrinth.com/mod/unimixins/versions) [![git](images/icons/git.png)](https://github.com/LegacyModdingMC/UniMixins/releases)

## Building

`./gradlew build`.

## Credits

* [GT:NH buildscript](https://github.com/GTNewHorizons/ExampleMod1.7.10)
* [AlexSocol's StatTweaker](https://bitbucket.org/AlexSocol/stattweaker), whose Thaumcraft integration inspired the item warp and runic shielding scripting support

## License

`CC BY 4.0`.

<br>

![license](images/license_small.png)


### Primal aspects and component edits

Startup scripts in `config/thaumicdabblery/aspects/` can use
`mods.thaumcraft.CustomAspects.registerPrimal(tag, color, image, description, hiddenUntilScanned)` and
`CustomAspects.setComponents(aspect, first, second)`. Optional scan-gated primals remain unknown until a successful
item scan; discovery persists and synchronizes. Component edits preserve the aspect object and affect combination,
scanning and decomposition. Cycles and ambiguous final pairs are rejected before applying the batch.

Both operations require a client/server restart. Native wand recipe costs retain their original primals; explicit
extra vis costs are shown on additional arcane-workbench pages. See the
[custom-aspects guide](https://github.com/JackOfNoneTrades/ThaumicDabblery/wiki/Custom-Aspects) for examples and limits.

### Furnace research pages

With either supported ModTweaker fork, add native smelting pages in ordinary `scripts/*.zs` files:

```zenscript
// The research must exist. Pass the furnace input.
mods.thaumcraft.Research.addFurnacePage("MY_RESEARCH", <minecraft:iron_ore>);
```

Register custom furnace recipes with `furnace.addRecipe(output, input, xp)` before adding their pages.
Page changes support `/mt reload`; smelting itself does not require research.
See the [research wiki](https://github.com/JackOfNoneTrades/ThaumicDabblery/wiki/Research#furnace-recipe-pages)
and the runnable [demo script](tests/obfuscated/manual-furnace-pages.zs).

### Mystical Construct pages

Add display-only, layered structure diagrams with optional activation items and vis costs:

```zenscript
mods.thaumcraft.Research.addConstructPage("MY_RESEARCH", [
    [[<minecraft:bookshelf>]]
], <Thaumcraft:WandCasting>);
```

Layers are supplied bottom to top. Use `null` for empty cells; metadata, NBT and wildcard variants
are supported. Works with GTNH and CurseForge ModTweaker, without Thaumic Horizons.
See the [research wiki](https://github.com/JackOfNoneTrades/ThaumicDabblery/wiki/Research#mystical-construct-pages)
and the [portal demo](tests/obfuscated/construct-pages.zs).

### Custom warp events

```zenscript
import mods.thaumcraft.WarpEvents;

WarpEvents.register("weakness", 50, 100, [
    "effect @w 18 99 4",
    "playsound mob.endermen.stare @w ~ ~ ~"
]);
```

Ranges include permanent, normal, temporary and equipped warp. `@w` targets the affected player;
commands run server-side with command-block permissions. By default, 25% of successful native
warp checks choose one eligible custom event. Native protections and research progression remain
active. Test with `/td warp trigger weakness [player]`, and list events with `/td warp list`.
See the [warp event documentation](https://github.com/JackOfNoneTrades/ThaumicDabblery/wiki/Warp-Events)
for configuration, command behavior and reload details. An optional fifth argument supplies a
private chat message in Thaumcraft's warp-message style: `register(name, minWarp, maxWarp, commands, message)`.

### Witchery rite requirements

Edit existing Witchery rites with `mods.witchery.Rites`: mandatory and optional offerings,
living sacrifices, native circles, initial altar power, time, weather and Overworld restrictions.
Rite IDs and effects are preserved, and edits support script reload/undo. Both CurseForge and
GTNH ModTweaker are supported without requiring WitcheryExtras.

Run `/mt witcheryRites` for IDs and requirements. See the
[Witchery rite documentation](https://github.com/JackOfNoneTrades/ThaumicDabblery/wiki/Witchery-Rites)
for syntax and native behavior.
