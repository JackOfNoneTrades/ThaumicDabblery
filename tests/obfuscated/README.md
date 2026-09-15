# Obfuscated production tests

These are disposable Forge test mods, **not** ordinary deobfuscated JUnit tests.
The probes assert that `fml.deobfuscatedEnvironment` is false. Do not install them in a player's instance:
server probes alter scripts/config temporarily, and probes stop their instance when finished.

Include the production `fentlib-04136bd-snapshot.jar` and GTNHLib 0.11.37 in automated instances.
Leave FentLib's `terminalDepLoaderProgress` enabled (the default) to send dependency-loader progress to
the terminal instead of opening popups. Never install its `-dev` artifact in these obfuscated runtimes.

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
