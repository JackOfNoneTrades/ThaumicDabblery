# Obfuscated production tests

These are disposable Forge test mods, **not** ordinary deobfuscated JUnit tests.
The probes assert that `fml.deobfuscatedEnvironment` is false. Do not install them in a player's instance:
server probes alter scripts/config temporarily, and probes stop their instance when finished.

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
