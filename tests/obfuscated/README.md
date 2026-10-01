# Obfuscated production tests

These are disposable Forge test mods, **not** ordinary deobfuscated JUnit tests.
The probes assert that `fml.deobfuscatedEnvironment` is false. Do not install them in a player's instance:
server probes alter scripts/config temporarily, and probes stop their instance when finished.

Include the production `fentlib-04136bd-snapshot.jar` and GTNHLib 0.11.37 in automated instances.
Leave FentLib's `terminalDepLoaderProgress` enabled (the default) to send dependency-loader progress to
the terminal instead of opening popups. Never install its `-dev` artifact in these obfuscated runtimes.

## Thaumic Horizons creature infusions

Compile `CreatureInfusionChecks.java` with `CreatureInfusionServerProbe.java` or
`CreatureInfusionClientProbe.java`, adding the production Thaumic Horizons 1.8.24 jar to the SRG-first
classpath below. Client compilation also needs the normal client libraries. Use disposable instances only;
probes write `scripts/zz-creature-test.zs`, reload scripts, alter a test world and stop the game on completion.
Do not install both probes together. No separate script fixture is needed.

Run GTNH ModTweaker 0.14.0 and CurseForge ModTweaker 0.9.6, each with and without TC4Tweaks 1.5.47 and
Salis Arcana 1.1.71-GTNH. Require `TD_CREATURE_SERVER_PASS` or `TD_CREATURE_CLIENT_PASS` and no probe or
mixin failures. Invalid-script errors are intentional in validation cases. Process exit status alone is insufficient.

Coverage includes all 30 native recipe keys and their pages; preserved creature inputs, effect IDs and NBT;
research gating; per-pedestal stack normalization; repeated aspect costs; removal and redefinition;
exact recipe priority and page-array restoration; invalid-key/cost rejection; research/page removal ordering;
and the recipe-list command. Real vats start edited Diamond Skin and Chocolate Cow recipes, retain captured
costs through a script reload, and complete with their original native results. Completion is invoked directly
after recipe selection; this does not simulate the entire essentia-delivery and instability tick sequence.
The native Mooshroom recipe is checked for editing and page updates, not full vat completion.

Clients additionally render edited upgrade, transformation, both Sheeder and Nightmare pages, then removed
and restored pages. Screenshots are saved as `creature-page-*.png`. Common checks run on the integrated
server; this is not a remote dedicated-server networking test. A remapped MCP copy of the server probe can
also run in development, with its environment assertion inverted.

Verified on 2026-09-30 with Forge 1614, Java 8, Thaumcraft 4.2.3.5, CraftTweaker 3.4.8,
Thaumic Horizons 1.8.24 and GTNHLib 0.11.52:

| Runtime | ModTweaker | TC4Tweaks + Salis | Assertions |
| --- | --- | --- | --- |
| Production server | 0.14.0 / 0.9.6 | absent | 13,432 each |
| Production server | 0.14.0 / 0.9.6 | present | 13,628 each |
| Production client | 0.14.0 / 0.9.6 | absent | 13,712 each |
| Production client | 0.14.0 / 0.9.6 | present | 13,912 each |
| Development server | 0.14.0 | absent | 13,432 |

Separate startup probes also passed without Horizons, without MineTweaker/ModTweaker, and without either.
Raw logs and screenshots from this run are in `/tmp/td-creature-test/` on the development machine.

## Custom creature transformations

Compile `CreatureInfusionChecks.java` and `CustomCreatureChecks.java` with either
`CustomCreatureServerProbe.java` or `CustomCreatureClientProbe.java`. Use the same production classpath,
Horizons jar and four ModTweaker/addon combinations described above. These probes include the native
recipe-editing regression checks. Their optional `after:tc4tweak` dependency ensures TC4Tweaks has initialized
its networked infusion-matching configuration before tests run in `FMLServerStartedEvent`.

Require `TD_CUSTOM_CREATURE_SERVER_PASS` / `TD_CUSTOM_CREATURE_CLIENT_PASS`, with no probe or mixin
failures. Deliberately invalid recipes and one unavailable saved output generate expected errors. The
common probe writes `pending-custom-vat.dat` and temporarily changes scripts and the disposable world.

Custom checks cover registration, duplicate keys, multiple recipes for one mob pair, exact entity matching,
research gates, linked display metadata/NBT names, multiple linked pages, edits/removals and exact undo,
priority relative to native recipes, validation and missing-output recovery. They start recipes through
the real wand activation method, pay essentia through the vat's container API, consume pedestal items
through native crafting cycles, and verify vanilla/modded transformations and output attributes/name/costs.
Instability is set to zero for deterministic cycle checks; essentia transport from external jars is not simulated.
Undead inputs work only for matching custom transformations, and native upgrades stay blocked on undead.
A native upgrade is also completed in the same vat after a custom transformation to check output-label cleanup.

The client additionally places a real vat near its player and checks that the transformed mob and custom
name arrive through the normal tile update packets. It renders the chosen book icons, edited page and
removed page, saving `custom-creature-*.png`. This uses a real client/integrated-server connection, not a
remote dedicated-server connection.

For restart coverage, compile `CustomCreatureSavedVatProbe.java` alone. Copy `pending-custom-vat.dat` from
the server probe into another disposable production server, remove MineTweaker and ModTweaker, and
install only this probe. It must print `TD_CUSTOM_CREATURE_RESTART_PASS`: the saved transformation must
finish with the original output, consume its ingredient, retain the custom name and use the output's own
attributes. Both Horizons and Dabblery remain installed. The probe stops the server when finished.

Verified on 2026-09-30 with the same Forge/Thaumcraft/Horizons versions listed above:

| Runtime | ModTweaker | TC4Tweaks + Salis | Assertions |
| --- | --- | --- | --- |
| Production server | 0.14.0 / 0.9.6 | absent | 13,512 each |
| Production server | 0.14.0 / 0.9.6 | present | 13,708 each |
| Production client | 0.14.0 / 0.9.6 | absent | 13,519 each |
| Production client | 0.14.0 / 0.9.6 | present | 13,715 each |
| Development server | 0.14.0 | absent | 13,512 |

The separate restart without scripting mods and all three optional-dependency startup combinations also passed.
Raw logs, saved vat NBT and screenshots are in `/tmp/td-custom-creature-test/` on the development machine.

## Champion mobs

Compile `ChampionChecks.java` with either `ChampionServerProbe.java` or `ChampionClientProbe.java` using
the SRG-first production compilation classpath below (include LWJGL for the client). Install only the
matching probe in an isolated production instance. Do not install these probes in a player's world.
Leave `features.championmobs.applyCustomRules=false` (the default); the probe changes policy temporarily during its assertions.
No ZenScript fixtures are required. Both probes stop their instance when done.

Run both ModTweaker 0.14.0 and 0.9.6 with and without TC4Tweaks 1.5.47, on dedicated servers and clients.
Require `TD_CHAMPION_SERVER_PASS` / `TD_CHAMPION_CLIENT_PASS` and no failed assertions or mixin injection
failures. The intentional invalid-mode/ID and unsupported-entity warnings are expected. The client runs
the common checks on its integrated server thread and verifies champion name/health/modifier synchronization
over the actual local connection; this is not a remote dedicated-server multiplayer test.

Coverage: native disabled passthrough; exact IDs versus inherited native eligibility; whitelist/blacklist and
guaranteed precedence; fractional and 0/100 boundaries; difficulty, biome, Outer Lands and dangerous-room
bonuses; native reduced-chance switch; addon weights; minimum base health; native creeper restriction;
withers and a registered probe mob; missing-attribute rejection; disk NBT roundtrip and ordinary-mob NBT
reload without rerolls; feature disable; real Forge world insertion; and no client-side conversion.
The disk roundtrip is entity serialization, not a full world/server restart test.

For manual testing, copy `champions-manual.cfg`'s champion section into the instance's existing config and
restart. Spawn fresh zombies, creepers, silverfish and skeletons: the first three must be champions, while
skeletons must remain ordinary. Existing mobs are not retroactively changed. Creepers must be Bold.

## Salis Arcana bracelet replacements

Compile `BraceletReplacementChecks.java` with either `BraceletReplacementServerProbe.java` or
`BraceletReplacementClientProbe.java`, using the SRG-first production classpath below. Install the resulting
probe only in a disposable production instance; each probe stops its instance after finishing. Do not install
both probes together. Add `bracelet-replacement.zs` to `scripts/` when both bracelet mods are installed.

Cover Salis 1.1.71-GTNH with GTNH ModTweaker and Salis v2.6.0 with CurseForge ModTweaker, including GTNH and
original Thaumic Bases, Thaumic Concilium, and runs with and without TC4Tweaks. NEI checks additionally need
production NEI and Aspect Recipe Index (which requires TC4Tweaks' API); omit Aspect Recipe Index in clients
without TC4Tweaks to exercise the optional integration guard. Require `TD_BRACELET_SERVER_PASS` / `TD_BRACELET_CLIENT_PASS`,
no failed assertions, no script errors and no mixin application/injection failures; process exit status alone
is not sufficient. Probes require an obfuscated runtime.

Checks cover every registered bracelet variant in all nine recipe-input positions, direct recipe output/cost and
research queries, unchanged rejected-item metadata/NBT, regular wand/staff/sceptre replacement, bracelet power
slots, casting-item recognition, Vis storage/consumption, scripted capacity, and script reloads. Client checks
also exercise both actual NEI handlers, ensuring bracelet recipes disappear while ordinary replacements remain.

## Baubles Expanded slots

Legacy Witching Gadgets coverage includes original 1.1.10 and KryptonCaptain 1.2.9 with Traveller's Gear 1.16.6.
Also compile `WitchingBaubleScriptChecks.java` and `WitchingBaubleLegacyServerChecks.java` / `WitchingBaubleLegacyClientChecks.java` for the corresponding
probe, with the production Traveller's Gear jar on the compilation classpath. Those helpers are not invoked
in GTNH-only instances. Install `legacy-wg-Baubles.cfg` as `config/Baubles.cfg` in legacy test instances to
explicitly enable expanded slots, which those WG versions do not create themselves. Keep GTNH regression runs
without Traveller's Gear to check optional linkage. Use both ModTweaker forks, with and without TC4Tweaks.

Legacy checks additionally cover native Traveller's Gear jump/haste and cloak discovery/storage, same-metadata
native and moved cloaks, closing a moved bag after unequip, exact-item GUI selection and invalid slot rejection,
wolf-event dispatch and cloak tick/unequip callbacks, raven glide, expanded-slot Vis amulets, the native ability
wheel's IDs and packet serialization, and server-side activation including disabled/out-of-range cases.
No real remote-client packet delivery is claimed; the native packet and receiver paths are exercised separately.

Compile `BaubleSlotChecks.java` with `BaubleSlotServerProbe.java` and `WitchingBaubleSlotChecks.java` for
the dedicated server, or with `BaubleSlotClientProbe.java` and `WitchingBaubleClientChecks.java` for the client.
Use the SRG-first compilation classpath below, plus the production Witching Gadgets jar. Client compilation
also needs LWJGL, Botania's API and NEI's API; these compilation inputs are not installed as runtime mods.
The optional Witching Gadgets test classes are deliberately separate so the common probe can load without WG.
Never install both probes together, or either probe in a normal player's instance.

Install `bauble-slots.zs` in `scripts/`, plus `bauble-slots-wg.zs` only when WG is installed.
Use `bauble-slots-wg-krypton.zs` instead of the WG script for KryptonCaptain 1.2.9: its sniper ring
uses metadata 5, while original/GTNH WG use 6. Do not install both WG scripts. Run the production
jar against ModTweaker 0.14.0 and 0.9.6, each with and without TC4Tweaks. Test both dedicated server and
`runObfClient`; include a no-WG control and an older Baubles Expanded control. The probes require an
obfuscated runtime. Clients automatically create/join a disposable creative world and stop after checking it.

Require `TD_BAUBLE_SERVER_ALL_PASS` / `TD_BAUBLE_CLIENT_ALL_PASS`, and no failed assertions, script errors,
or mixin application/injection failures. Exit status alone is insufficient. The server exercises actual slots,
normal clicks, shift-click transfers, stack limits and count conservation, NBT, native callbacks/restrictions,
repeated reloads, script removal and full-inventory recovery. WG checks exercise moved haste/sniper effects,
cloak/kama discovery, storage GUI selection, and focus-pouch contents without overwriting other copies.
The client checks GUI hover and item tooltips, double jump, sniper zoom, cloak GUI creation and the actual
raven-kama tick method's outgoing glide message slot. That message is captured by the test, not sent over
a dedicated-server connection; manual multiplayer testing remains useful.

For manual testing, use `manual-bauble-slots.zs` with WG installed. It needs no test probe and deliberately
uses the same rules as the automated fixtures, plus a moved raven kama. Bind WG's activate key to test
storage/glide controls. Verify items retain their contents across unequip/re-equip and save/rejoin.

For original WG use `manual-legacy-witching-baubles.zs` and Traveller's Gear's active-abilities wheel.
For KryptonCaptain 1.2.9 change the sniper-ring metadata in that manual script from 6 to 5.

## Custom aspects

Compile `CustomAspectChecks.java` alongside `CustomAspectServerProbe.java` or `CustomAspectClientProbe.java`,
using the SRG-first production compilation classpath described below. Also include the production Minecraft
server jar (for Netty), and LWJGL for the client. Package the client translation fixture
`custom-aspect-client-en_US.lang` as `assets/tdaspectclientprobe/lang/en_US.lang` in its probe jar.
Never install both probes together.

Copy `custom-aspects/*.zs` to the instance's `config/thaumicdabblery/aspects/` and `custom-aspects-usage.zs` to
`scripts/`. The early files deliberately contain forward references, mixed-case IDs, a repeated-component aspect,
and black/hex RGB colors. The ordinary script uses a new aspect bracket on the first compilation, assigns clock
aspects, adds research and a crucible recipe, and overrides a description through localization.

Run both ModTweaker 0.14.0 and 0.9.6, with and without TC4Tweaks 1.5.47, on separate dedicated servers and clients.
The current fixtures include production IC2 2.2.828: CraftTweaker 3.4.8's generated class registry references
`CropCard` while loading its core bracket handlers, so without IC2 it can skip item brackets entirely. This is a
test-environment dependency, not a new dependency of Thaumic Dabblery or its aspect feature.

Also cover ContentTweaker 1.0.5 with FluxedCore 1.0.9, without IC2: copy `custom-aspects-content.zs` to
`contentScripts/`. Require its `tdaspecttoken` item registration before the custom-aspect pass, then all normal
client assertions. This checks that the isolated compiler leaves ContentTweaker and ordinary scripts intact.

Require `TD_ASPECT_EARLY_PASS` / `TD_ASPECT_CLIENT_EARLY_PASS`, then `TD_ASPECT_SERVER_ALL_PASS` /
`TD_ASPECT_CLIENT_ALL_PASS`, with no failed checks, script errors or mixin injection errors. The client probe
creates/joins a disposable creative world and checks resource loading, fallback descriptions, language overrides,
and resource/script reloads. The server checks research combinations, actual crucible matching, jar input/output,
label and aspect-list NBT, centrifuge decomposition, saved player knowledge, and TC's packet serialization.
Both validate bad definitions without mutating the registry. The server also parks a definition and the ordinary
script during `/mt reload`: aspects must retain identity while ordinary research is undone. Launch the same
server again and require `TD_ASPECT_DISK_RESTART_PASS` for its saved jar.

For a real dedicated-server connection, start the server probe with `-Dtd.aspects.network=true` on a free
loopback-only port, then run the client probe with `-Dtd.aspects.server=127.0.0.1:PORT`. Require
`TD_ASPECT_NETWORK_SENT` and `TD_ASPECT_NETWORK_RECEIVED`, plus the ordinary pass markers. The client must
receive a pool of 23 custom research points and a jar containing 17 custom essentia with its matching label.
The server stops after the client disconnects; use a timeout as a fallback.

Negative startup controls: add a bad `.zs` after otherwise valid definitions, separately testing syntax errors,
missing components, duplicate IDs, an existing/reversed component pair, and a dependency cycle. Each must report
`Could not load custom aspects`, identify the problem, and stop before creating/loading a world. Do not treat
Forge's process exit status alone as proof of success.

## Wand component server probe

`WandComponentStatsProbe.java` checks actual arcane recipe costs, capacity and charging, native and scripted
regeneration, independent aspect timing, fractional amounts, ceilings, Potency stacking, individual bracelet
variants, wildcard precedence, validation, feature toggling, and repeated MineTweaker rollback/replay.
Both probes also use `WandCraftingCostChecks.java`: compile that source alongside each probe. It checks costs
22/24/32, independent cap/core edits with Forbidden Magic's creative components unchanged, wand/staff versus
sceptre limits, unsafe recipe rejection, zero costs, external integer overflow, and item metadata save/load.
The server additionally checks the actual recipe `matches` methods with a researched fake player.

Prepare an isolated production Forge 1.7.10 server with the built **reobfuscated** Thaumic Dabblery jar,
CraftTweaker, one ModTweaker version, Thaumcraft, UniMixins, and their production dependencies.
Use a fresh superflat world, an ephemeral port (`server-port=0`), and `online-mode=false`.
Copy these scripts into that instance's `scripts/`:

- `wand-components.zs` in every run.
- `bracelets.zs` only when Thaumic Bases is installed.
- `concilium.zs` only when Thaumic Concilium and its bracelet integration are installed.

Compile with Java 8 bytecode and annotation processing disabled. Put SRG-named Minecraft classes **first**
on the compilation classpath, then Forge API classes, the built production mod, installed production mods,
and Forge's library jars. For this project's RetroFuturaGradle setup, the Minecraft input is
`build/rfg/srg_merged_minecraft.jar`; the cached `forgeSrc-1.7.10-10.13.4.1614-1.7.10.jar` supplies Forge APIs.
These compilation inputs are not the runtime: launch the normal production Forge universal server jar.

```sh
javac --release 8 -proc:none -cp "$probe_classpath" -d "$probe_classes" tests/obfuscated/WandComponentStatsProbe.java tests/obfuscated/WandCraftingCostChecks.java
jar cf "$probe_jar" -C "$probe_classes" .
# Install probe_jar in the isolated server's mods directory, then launch from that directory using Java 8:
java -Xmx1536m -Dmixin.debug.verbose=true -Dmixin.debug.export=true -jar forge-1.7.10-10.13.4.1614-1.7.10-universal.jar nogui
```

Require `TD_WAND_STATS_ALL_PASS` in the log, no `TD_WAND_STATS_FAILED`, no script errors, and no mixin
application/injection errors. Forge may exit successfully even after a failed assertion, so its exit code alone
is not sufficient. Never put development jars or the compilation classpath into the runtime mods directory.

Test matrix: ModTweaker 0.14.0 and 0.9.6, each without optional addons, with Thaumic Bases, and with both
Thaumic Bases and Concilium. Include TC4Tweaks and the corresponding Salis build in the addon-enabled runs.
The original and GTNH Thaumic Bases layouts must both be covered.

## Wand component client probe

`WandComponentClientProbe.java` is client-only. Compile it similarly, also including LWJGL 2 in the compilation
classpath. Use a separate jar/instance from the server probe. It waits for client ticks, forces expanded Salis
tooltips, and checks the actual Forge tooltip event pipeline, including baseline descriptions, scripted numeric
values, removal of obsolete descriptions, disabled regeneration, and assembled-item Potency. Zero Potency must
hide both our `+0` line and Salis's obsolete native `+1` description; undo must restore the positive custom
description on cores and assembled wands.
Disabled regeneration must remove obsolete Salis descriptions without adding a status line or changing other
tooltip information. Cover native and custom regeneration, core and casting-only rules, repeated undo, and
feature disable/re-enable; active custom regeneration must remain visible.

Run with production jars through `runObfClient`, in an isolated working directory. Require
`TD_WAND_CLIENT_ALL_PASS` and no `TD_WAND_CLIENT_FAILED`. Cover Salis Arcana 1.1.71-GTNH and v2.6.0,
plus a client startup with Concilium installed.

## Gadomancy Aura Pylon

Compile `AuraPylonChecks.java`, `AuraPylonNetwork.java`, and `AuraPylonServerProbe.java`
using the SRG-first production classpath described above, including original Gadomancy
1.0.7.3. The same probe runs on GTNH Gadomancy. For clients substitute
`AuraPylonClientProbe.java` for the server entry point and include LWJGL. Package
`aura-pylon-client-en_US.lang` as `assets/tdpylonclientprobe/lang/en_US.lang` in the client
probe jar. Never install both entry points together or use these probes in player worlds.

Copy `aura-pylon-aspect.zs` into `config/thaumicdabblery/aspects/` and `aura-pylon.zs`
into `scripts/`. Use only these fixtures in the disposable instance. For the no-Gadomancy
server control omit both scripts; it checks normal startup and the explicit missing-mod
error. Both entry points reject a deobfuscated runtime.

Run ModTweaker 0.14.0 and 0.9.6 crossed with Gadomancy 1.0.7.3 and 1.5.16, on dedicated
servers and clients. Cover TC4Tweaks 1.5.47 and its absence. Require
`TD_PYLON_SERVER_PASS`, `TD_PYLON_CLIENT_PASS`, or `TD_PYLON_ABSENT_PASS`, as applicable,
with no `TD_PYLON_*_FAILED`, pylon script errors, or mixin injection/application failures.
Launch a successful server again against the same world and additionally require
`TD_PYLON_DISK_RESTART_PASS`. Client probes create/join their own integrated world and exit.

Coverage includes real fueled and unfueled pylon tile updates; players/living/undead/exact
entity selectors; exclusions and range; potion duration accumulation, cap, amplifier,
immunity and overlapping rules; duplicate definitions; independent native/custom intervals
and ranges; native entity and block callbacks; wipe/redefine and selective custom removal;
validation; reverse-order/idempotent undo; third-party replacement detection; configuration
toggle; research ownership and TC4Tweaks caches; repeated script reload/removal; actual saved
world restart; and client potion/research synchronization and localization. Fake players in
common checks capture outgoing packets; live connection checks below exercise actual delivery.

For dedicated multiplayer, start the server with `-Dtd.pylon.network=true` and a free
loopback-only port. It stays alive after its checks and creates a fueled pylon when a player
joins. Start the client with `-Dtd.pylon.server=127.0.0.1:PORT`; require
`TD_PYLON_NETWORK_READY` on the server and `TD_PYLON_CLIENT_PASS ... remote=true` on the
client. The server stops when the client disconnects. Give both processes an external timeout.
The fixture holds the pylon's crystal heartbeat and delays granting research prerequisites
until initial player-data synchronization is complete; effect application uses normal world ticks.

For manual use, copy `manual-aura-pylon.zs` to an instance's ordinary scripts directory.
Supply Aqua, Exanimis, and Aer to assembled pylons; verify player/undead/cow targeting.
Remove the script and `/mt reload` to restore native behavior. Previously applied potions
expire normally; wiping does not undo past world changes.

## Primal aspects and component edits

Compile `PrimalAspectChecks.java`, `PrimalAspectNetwork.java` and either `PrimalAspectServerProbe.java` or
`PrimalAspectClientProbe.java` with the production SRG-first classpath described above. Client compilation also
needs LWJGL. Install only one probe in an isolated instance; probes modify knowledge and stop the game when done.
Copy `primal-aspects/*.zs` into `config/thaumicdabblery/aspects/` and `primal-aspects-usage.zs` into `scripts/`.
The fixtures intentionally change Vacuos and add two primals. Do not use them in a player's world.

Run both GTNH ModTweaker 0.14.0 and CurseForge ModTweaker 0.9.6, with and without TC4Tweaks/Salis Arcana.
Require `TD_PRIMAL_SERVER_PASS` / `TD_PRIMAL_CLIENT_PASS`, no probe failures and no mixin errors. Server restarts
read `primal-saved.dat` and require `TD_PRIMAL_RESTART_PASS`. The client creates a fresh test world for each run,
checks the hidden state before the server scans a clock, and verifies discovered knowledge afterwards. It clicks
through the actual arcane-workbench cost pages and saves screenshots of unknown/known costs in `screenshots/`.

For separate multiplayer checks, launch the server with `-Dtd.primal.network=true` and its matching client with
`-Dtd.primal.server=127.0.0.1:PORT`. Use a fresh player identity to check initial concealment; the server scans the
seed item after 180 ticks. Require `TD_PRIMAL_NETWORK_SCAN` on the server and `TD_PRIMAL_CLIENT_PASS ... remote=true`
on the client. The server stops on disconnect. Never expose this test server publicly.

Coverage includes early registration/forward references, preserved existing-aspect identity, final-batch pair reuse,
cycle rejection through existing aspects, failed-validation atomicity, edited combination/decomposition/centrifuge
behavior, hidden/visible knowledge, blocked generic grants and fragments, failed scans, item/dropped-item scans,
already-scanned-item recovery, per-player isolation, zero-point saved knowledge, wand storage and costs, native
wand cost preservation, energized nodes, repeated script reloads, and client vis-amulet tooltip concealment.
Also rerun the original compound-only probes to check existing behavior without any new primals.

## Furnace research pages

Use `FurnacePageChecks.java` with `FurnacePageServerProbe.java` for the dedicated-server probe, or with
`FurnacePageClientProbe.java` for the integrated/remote client probe. Put `manual-furnace-pages.zs` in the disposable
instance's `scripts/` directory using that exact filename. These tests rewrite a second fixture script, temporarily
remove/restore the demo script, and repeatedly reload scripts. Do not run the probes in a player's instance.

Compile against the SRG Minecraft jar first, followed by the production mod/dependency jars as described above.
The common checks require an obfuscated environment and exercise the actual ZenScript method, furnace output,
page arrays, invalid inputs, final recipe replacement/removal, page/research edits and rollback. Successful dedicated
runs print `TD_FURNACE_SERVER_PASS`; client runs print `TD_FURNACE_CLIENT_PASS` and save
`screenshots/furnace-page.png` after rendering the native Thaumonomicon page. A client also checks automatic unlock
and recipe click-through lookup using its real player.

For a separate connection, start the dedicated probe with `-Dtd.furnace.network=true` to keep it running, then start
the client with `-Dtd.furnace.server=127.0.0.1:PORT`. The remote client may start with an empty scripts directory to
verify normal MineTweaker script synchronization. Stop the dedicated server after the client finishes.

For manual inspection, install **only** `manual-furnace-pages.zs`, without a probe jar. The dev copy is
`run/client/scripts/thaumicdabblery_furnace_demo.zs`. Open the Thaumonomicon's **Furnace Demo** tab and its automatically
unlocked **Baked Treats** research: one wooden bowl smelts into two cookies. This is a demonstration recipe;
remove the script and reload to restore normal recipes/research. The tracked fixture and dev copy should match.

## Scan-all debug command

Use `ScanAllChecks.java`, `ScanAllNetwork.java` and `ScanAllServerProbe.java` for a dedicated-server probe.
For a client probe, replace `ScanAllServerProbe.java` with `ScanAllClientProbe.java`. Compile against SRG
Minecraft first and the production mod/dependency jars, as above. Install `scan-all.zs` under `scripts/`
and copy `scan-all-aspects/definitions.zs` to `config/thaumicdabblery/aspects/definitions.zs`.
Use disposable instances: the tests change player knowledge and create test research.

The server probe runs `/td scanall`, checks preservation of every existing aspect balance, discovery at zero
points, native scan-triggered clues and scripted scan gates, item/entity variants, direct scan rejection,
player isolation, node exclusion, compact packet encoding, repeat calls and native NBT persistence. It also
checks a player whose research list was cleared separately, preventing Thaumcraft's lazy data load from
replacing live balances. Reusing the same instance tests the saved `scanall-saved.dat` across processes.
Success prints `TD_SCANALL_SERVER_PASS`, plus `TD_SCANALL_RESTART_PASS` on subsequent runs.

The client creates a creative test world, runs the common checks, then applies the command to its real player
through the server console. It verifies synced discoveries, unchanged points and scan flags, renders the native
Thaumonomicon aspect pages, and checks that the scripted clock appears as a source with the right amount.
A script reload must invalidate and rebuild the catalog while the page is open. Success prints
`TD_SCANALL_CLIENT_PASS` and saves `screenshots/scan-all-aspects.png`.

For a dedicated connection, start the server probe with `-Dtd.scanall.network=true` and the client with
`-Dtd.scanall.server=127.0.0.1:PORT`. The client can start with no ordinary scripts to test script sync.
Launch it again with the same username to verify reconnect persistence without rerunning the command;
the server prints `TD_SCANALL_NETWORK_REJOIN`. Stop the server when finished.

Run both CurseForge ModTweaker 0.9.6 and GTNH ModTweaker 0.14.0, with and without TC4Tweaks/Salis Arcana.
For the optional-dependency control, remove MineTweaker, ModTweaker, the script and the aspect definitions;
the probe supplies equivalent native fixtures using a compound aspect instead of a concealed primal.
For development runs, remap the probe's SRG identifiers to MCP and invert its environment assertion.
Also rerun the primal-aspect probes to verify ordinary scanning for players without the completion marker.

## Research and tab removal

Compile `ResearchRemovalChecks.java` with `ResearchRemovalServerProbe.java` for dedicated-server checks, or
with `ResearchRemovalClientProbe.java` for client checks, using the SRG Minecraft jar and production dependencies
as above. Install the resulting probe jar only in a disposable instance with MineTweaker and ModTweaker.
The probe creates research fixtures and rewrites `scripts/zz-research-removal-test.zs`; no separate script is needed.

The common checks exercise the actual `removeTab`, `removeResearch` and `orphanResearch` ZenScript operations
through repeated MineTweaker reloads. They cover duplicate ordinary/hidden parents and siblings, links within and
between tabs, multiple removed tabs, unrelated references, null/empty arrays, exact undo order and multiplicity,
empty/missing tabs, later removal/movement of a dependent research, snapshot isolation and action reuse.
Success prints `TD_REMOVAL_SERVER_PASS`. Missing-dependent restoration intentionally logs a warning.

The client also renders the surviving Thaumonomicon tab after deletion, after undo, and after deleting multiple
tabs, exercising the native browser's parent/sibling lookups. Success prints `TD_REMOVAL_CLIENT_PASS` and saves
`screenshots/research-tab-removed.png`. Run with GTNH ModTweaker 0.14.0 and CurseForge ModTweaker 0.9.6, both with
and without TC4Tweaks/Salis Arcana. The real reload lifecycle handles TC4Tweaks cache invalidation.
For development runs, remap SRG names to MCP and invert the common probe's environment assertion.
