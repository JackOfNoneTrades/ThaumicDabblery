# Wand component production tests

These are disposable Forge test mods, **not** ordinary deobfuscated JUnit tests.
Both probes assert that `fml.deobfuscatedEnvironment` is false. Do not install them in a player's instance:
the server probe alters scripts/config temporarily, and both probes stop their instance when finished.

## Server probe

`WandComponentStatsProbe.java` checks actual arcane recipe costs, capacity and charging, native and scripted
regeneration, independent aspect timing, fractional amounts, ceilings, Potency stacking, individual bracelet
variants, wildcard precedence, validation, feature toggling, and repeated MineTweaker rollback/replay.

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
javac --release 8 -proc:none -cp "$probe_classpath" -d "$probe_classes" tests/obfuscated/WandComponentStatsProbe.java
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

## Client probe

`WandComponentClientProbe.java` is client-only. Compile it similarly, also including LWJGL 2 in the compilation
classpath. Use a separate jar/instance from the server probe. It waits for client ticks, forces expanded Salis
tooltips, and checks the actual Forge tooltip event pipeline, including baseline descriptions, scripted numeric
values, removal of obsolete descriptions, disabled regeneration, and assembled-item Potency.

Run with production jars through `runObfClient`, in an isolated working directory. Require
`TD_WAND_CLIENT_ALL_PASS` and no `TD_WAND_CLIENT_FAILED`. Cover Salis Arcana 1.1.71-GTNH and v2.6.0,
plus a client startup with Concilium installed.
