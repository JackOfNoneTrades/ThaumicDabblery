# Wand component verification — 2026-09-06

Production artifact tested: `thaumicdabblery-0.1.1-master.22+3122a78eec-dirty.jar`

SHA-256: `78ee68c5f3700ddfd6b794d365bedd101047069a647134d1b0f72d405f12d53c`

Build and formatting checks passed. No deobfuscated runtime tests were used.

## Dedicated production servers

All six runs used Forge 1.7.10-10.13.4.1614, Java 8, CraftTweaker 3.4.8, Thaumcraft 4.2.3.5,
UniMixins 0.3.1, the same production mod artifact, and the committed server probe/scripts.
Each probe verifies the obfuscated-environment flag before asserting behavior.

| ModTweaker | Thaumic Bases | Concilium | Salis Arcana | TC4Tweaks | Checks |
| --- | --- | --- | --- | --- | --- |
| 0.14.0 | absent | absent | absent | absent | 255 passed |
| 0.9.6 | absent | absent | absent | absent | 255 passed |
| 0.14.0 | 1.8.13 | absent | 1.1.71-GTNH | 1.5.47 | 495 passed |
| 0.9.6 | 1.3.1710.4 | absent | v2.6.0 | 1.5.47 | 495 passed |
| 0.14.0 | 1.8.13 | 1.1.1 | 1.1.71-GTNH | 1.5.47 | 595 passed |
| 0.9.6 | 1.3.1710.4 | 1.1.1 | v2.6.0 | 1.5.47 | 595 passed |

Concilium runs also used Forbidden Magic 0.9.17-GTNH and Thaumic Tinkerer 2.12.31.
All 13 Thaumic Bases and all 8 Concilium capacity metadata entries were checked. Concilium's always-available
Infernal bracelet exercised Potency, custom regeneration, and preservation of the native addon callback.
Variants whose optional integrations were absent were only tested for capacity, not made playable.

The probe exercised actual assembly recipes, charging, capacity clamping, separate wand/staff/sceptre capacities,
focus upgrade stacking, innate bonus removal, regeneration intervals/amounts/ceilings, native callback preservation
and replacement, exact/wildcard/core precedence, validation, feature disable/re-enable, three consecutive reloads,
and complete script removal/restoration. Final runs had no MineTweaker script errors or mixin application errors.

## Obfuscated clients

The committed client probe passed 14 tooltip checks in each of these production clients:

- GTNH ModTweaker / Thaumic Bases / Salis Arcana.
- CurseForge ModTweaker / Thaumic Bases / Salis Arcana.
- The GTNH set with Concilium and its required addons.

It checked the real Forge tooltip event pipeline with expanded Salis tooltips, including baseline descriptions,
updated cap/core numbers, scripted regeneration and Potency, removal of obsolete descriptions, disabled
regeneration, and the assembled-item bonus. Every client reached normal startup and exited after its assertions.

Total: 2690 server assertions and 42 client assertions. Known unrelated Forge version-check, module-info discovery,
and missing-texture messages did not prevent the tests from passing.

Local raw logs and isolated instances are retained in `/tmp/td-wand-stats-obf.bRLBQO/`.
The manual-test instance is `run/wand-component-stats-test/`; its scripts and world are separate from other clients.

## Crafting safety and zero-Potency regression — 2026-09-06

Production artifact: `thaumicdabblery-3122a78-snapshot-master.1+852a99bdd4-dirty.jar`

SHA-256: `baa9d720eaac17d2b80b02ea2b2d0954a8df38918d66a77534a72a87211aa3bb`

The previous artifact reproduced the reported script failure at Silverwood core cost 22 with Forbidden Magic
installed (`old-control/logs/minetweaker.log`). The corrected artifact passed the expanded committed probes:

| ModTweaker | Server addons | Server checks | Client checks |
| --- | --- | --- | --- |
| 0.14.0 | none | 871 | — |
| 0.9.6 | none | 871 | — |
| 0.14.0 | Thaumic Bases 1.8.13, Salis 1.1.71-GTNH, TC4Tweaks | 1117 | — |
| 0.9.6 | Thaumic Bases 1.3.1710.4, Salis v2.6.0, TC4Tweaks | 1117 | — |
| 0.14.0 | corresponding full set plus Concilium, Forbidden Magic, Thaumic Tinkerer | 1242 | 580 |
| 0.9.6 | corresponding full set plus Concilium, Forbidden Magic, Thaumic Tinkerer | 1242 | 580 |

Other production dependency versions match the matrix above. All runtimes asserted the obfuscated-environment
flag. Total: 6460 server assertions and 1160 client assertions, including the existing component suite.

New coverage includes:

- Core costs 22, 24 and 32 on Greatwood, Silverwood and their staff cores, plus Primal staff; cap multipliers
  22, 24 and 32; actual ZenScript loading, repeated reloads and rollback of the reported values.
- Forbidden Magic's orichalcum, neutronium and neutronium staff remain at 1000 without any workaround script.
- Actual recipe matching, output and aspect costs. Safe ordinary wand/staff recipes remain usable when the same
  product would exceed the sceptre limit. Unsafe recipes have no match/output; their costs cannot overflow.
- Signed-short boundaries and NBT save/load, zero costs, external `65536 × 65536` and
  `Integer.MAX_VALUE × Integer.MAX_VALUE` values, negative external values, and edits while disabled.
- Both recipe mixin targets on production clients and servers; both Salis tooltip event pipelines. A zero innate
  Potency hides the custom `+0` and obsolete native `+1` lines on loose cores and assembled items; undo restores
  the positive custom tooltip. Existing server checks also verify that focus upgrade Potency remains effective.

An initial production run caught a multi-target Mixin shadow annotation issue; it was corrected before the final
matrix. Final runs have no mixin application/injection failures, failed probe assertions, or MineTweaker script
errors. Build, formatting and whitespace checks passed. No deobfuscated runtime tests were used.

Raw logs and isolated instances: `/tmp/td-wand-cost-fix-obf.Biglnx/`. Initial-attempt logs are retained separately,
and `old-control/` intentionally contains the failure reproduced with the previous artifact.
Manual client: `run/wand-component-fixes-test/`, using the committed `manual-wand-cost-fixes.zs` example.

## Disabled-regeneration tooltip regression — 2026-09-06

Production artifact: `thaumicdabblery-852a99b-snapshot-master.1+23039bfc59-dirty.jar`

SHA-256: `6a7ac5f6bf3d109f0aa16c466603285bc6e7492f2554f4668651e511f02317e5`

The expanded client probe reproduced the unwanted disabled-regeneration line with the previous artifact.
The fixed artifact passed in four isolated `runObfClient` instances, all asserting the obfuscated-environment flag:

| ModTweaker | Salis Arcana | TC4Tweaks | Client checks |
| --- | --- | --- | --- |
| 0.14.0 | 1.1.71-GTNH | 1.5.47 | 607 passed |
| 0.9.6 | v2.6.0 | 1.5.47 | 607 passed |
| 0.14.0 | 1.1.71-GTNH | absent | 607 passed |
| 0.9.6 | v2.6.0 | absent | 607 passed |

These use the corresponding full production addon sets above, including Thaumic Bases, Concilium and Forbidden
Magic. Total: 2428 assertions, including existing crafting-cost and Potency regression checks.
The tooltip checks exercise disabled native and custom regeneration, loose Icy/Blaze cores and assembled wands,
casting-only overrides, repeated undo, and feature disable/re-enable. Other tooltip lines must stay unchanged;
active custom regeneration and restored native descriptions must remain visible. Disabled rules remain disabled.

All four final logs have no failed assertions, MineTweaker script errors or mixin application/injection errors.
The last client briefly stalled in LWJGL's frame-swap call, then recovered and completed without changes.
Build, formatting and whitespace checks passed; no deobfuscated runtime tests were used.
Logs and instances: `/tmp/td-regen-tooltip-obf.vk1YND/`; `old-control/` intentionally records the previous jar's failure.

## Custom aspects — 2026-09-07

Production artifact: `thaumicdabblery-852a99b-snapshot-master.2+047ab6aa43-dirty.jar`

SHA-256: `270ccf40fce36425c3a740ad7fb79b56897241a7da12bbde54b24614d3ceb9ba`

The committed custom-aspect probes passed with the final artifact in all four production combinations:

| ModTweaker | Thaumic Bases | Salis Arcana | TC4Tweaks | Dedicated server | Client |
| --- | --- | --- | --- | --- | --- |
| 0.14.0 | 1.8.13 | 1.1.71-GTNH | 1.5.47 | 121 passed | passed |
| 0.9.6 | 1.3.1710.4 | v2.6.0 | 1.5.47 | 121 passed | passed |
| 0.14.0 | 1.8.13 | 1.1.71-GTNH | absent | 121 passed | passed |
| 0.9.6 | 1.3.1710.4 | v2.6.0 | absent | 121 passed | passed |

All probes asserted an obfuscated runtime, using production Forge 10.13.4.1614, Thaumcraft 4.2.3.5,
CraftTweaker 3.4.8, Baubles Expanded 2.2.21, GTNHLib 0.11.23 and UniMixins 0.3.1 on Java 8.
These fixtures also include production IC2 2.2.828 to satisfy CraftTweaker's generated class registry;
it is not a new mod dependency. Client counters include repeated polling assertions, so their raw totals
are not used as distinct coverage counts.

Coverage includes:

- Startup ZenScript compilation before normal aspect/item brackets; variables and imports; forward references
  across files; mixed-case IDs, hex and black colors, and repeated-component aspects.
- Native research-table combinations and primal decomposition, actual item values, research requirements,
  crucible matching and insufficient-essentia rejection.
- Jar transport, labels, aspect-list and jar NBT, centrifuge processing, player knowledge and Thaumcraft packet
  serialization. Every dedicated-server combination also loaded its saved custom-aspect jar after a full restart.
- Repeated MineTweaker reloads, normal research rollback, and temporarily removed startup/ordinary scripts:
  registered aspects retain object identity while ordinary operations undo and replay.
- Client icon resource loading, fallback descriptions, resource-pack and script translations, and resource
  reloads. CraftTweaker's script translation is cleared by a resource reload and restored by `/mt reload`;
  the supplied fallback description remains usable throughout.
- Non-mutating rejection of invalid IDs, colors, resource locations, empty descriptions, missing components,
  duplicate IDs/pairs, dependency cycles, late registrations and repeated registration.

A separate real dedicated-server/client connection passed: the client received 23 custom-aspect research points
and a jar containing 17 custom essentia with its matching label. Both network pass markers and ordinary probe
pass markers are present. This supplements, rather than substitutes for, the local packet serialization checks.

ContentTweaker 1.0.5 with FluxedCore 1.0.9 also passed the client probe without IC2 or TC4Tweaks. Its own early
`contentScripts/` item definition registered successfully before the isolated custom-aspect compiler pass.

Five separate negative startup controls (syntax, unknown component, duplicate ID, existing/reversed pair and
cycle) each logged the expected startup failure and stopped before creating/loading a world. The syntax error
identifies its exact file and line. Forge's exit code alone was not used to judge these controls.

Final positive runs have no failed probe assertions, script compilation/execution errors or mixin application/
injection failures. The IC2 fixtures emit `WeightedItemStack is already defined in that package`; a separate
production baseline with the previous mod jar and no custom-aspect scripts reproduces that registry message.
The ContentTweaker fixture without IC2 does not emit it. Other baseline Forge version-check, module-info
discovery and missing-texture messages remain unrelated to the assertions.

Build (`spotlessApply build -x test`) and whitespace checks passed; no deobfuscated runtime tests were used.
Two final client repeats ran under Xvfb after a desktop LWJGL buffer-swap stall; fresh disposable instances were
used after interrupting the stalled run. Existing player worlds were not modified.

Logs and isolated instances: `/tmp/td-custom-aspects-obf.Dzemqo/`. Use `final.log` in each `server-*` directory,
`client-gtnh-tweaks`, `client-cf-plain`, `final-client-cf-tweaks`, `final-client-gtnh-plain`, `client-content`,
`network-server`, `network-client`, and each `negative-*` directory. `baseline-registry/baseline.log` records the
previous-jar registry control. Earlier attempts are retained separately and are not final-result evidence.

Manual client: `run/custom-aspects-test/`, launched with `bash run/custom-aspects-test/launch.sh`. The committed
`manual-custom-aspects-definition.zs` and `manual-custom-aspects-usage.zs` fixtures register Tempus from
Ordo + Vacuos and give clocks 4 Tempus; the icon is borrowed from Ordo, so no resource pack is required.
The user also confirmed the manual in-game aspect check on 2026-09-07.
