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

## Baubles Expanded slot customization — 2026-09-13

Production artifact SHA-256:
`814e6d6f17853bd17d524d9b19abc8a68c9261ba2d1a33bb7b5a2b044338af73`.

| ModTweaker | Baubles Expanded | Witching Gadgets | TC4Tweaks | Server assertions | Client assertions |
| --- | --- | --- | --- | --- | --- |
| GTNH 0.14.0 | 2.2.23-GTNH | 1.8.49-GTNH | 1.5.47 | 94 passed | 45 passed |
| CurseForge 0.9.6 | 2.2.23-GTNH | 1.8.49-GTNH | 1.5.47 | 94 passed | 45 passed |
| GTNH 0.14.0 | 2.2.23-GTNH | 1.8.49-GTNH | absent | 94 passed | 45 passed |
| CurseForge 0.9.6 | 2.2.23-GTNH | 1.8.49-GTNH | absent | 94 passed | 45 passed |
| GTNH 0.14.0 | 2.2.21-GTNH | 1.8.49-GTNH | 1.5.47 | 94 passed | 45 passed |
| GTNH 0.14.0 | 2.2.23-GTNH | absent | 1.5.47 | 75 passed | 38 passed |

All runs assert an obfuscated environment and use the reobfuscated mod jar with production Forge 1614,
Thaumcraft 4.2.3.5, CraftTweaker 3.4.8, GTNHLib 0.11.23 and UniMixins 0.3.1. Production IC2 2.2.828 is
included for CraftTweaker's generated class registry, as in the aspect suite; it is not a new mod dependency.
Dedicated servers use Java 8. Clients run through `runObfClient` under Xvfb and join disposable integrated worlds.
Counts include repeated rollback/replay checks, not only distinct cases.

Coverage includes ordinary items and native baubles, several allowed types, wildcard/exact metadata precedence,
durability-independent targets, input validation, undo, feature disable/re-enable, tooltips and slot hover,
native equip/unequip/worn callbacks and restrictions, normal clicks, shift-click stack limits and item count
conservation, shared-helper right-click, NBT serialization, repeated script reloads, and equipped-item recovery
after script removal with both available space and a full inventory.

WG checks cover relocated double-jump charms, haste vambraces, sniper movement and zoom, cloak/kama discovery,
storage GUI selection, and focus-pouch saves from slots beyond the original four. Same-metadata belt/held
pouches and a second storage cloak are checked against accidental replacement or overwritten contents.
The client invokes the native raven-kama tick and verifies that its outgoing glide packet identifies the actual
new slot. The packet is captured, not delivered over a dedicated-server connection. Real multiplayer and manual
save/rejoin testing are not claimed by this suite; inventory NBT roundtrips are covered.

Earlier probes found Expanded's whole-stack shift-click behavior and a FakePlayer-only chat issue; both were
fixed before the final runs. Optional WG probe code was separated for the no-WG control. The client glide
fixture explicitly presses WG's unbound activate key and runs after its first-login initialization.

Final logs have no failed assertions, script compilation/execution errors, or mixin application/injection failures.
The known IC2/CraftTweaker `WeightedItemStack is already defined in that package` registry message remains;
the previous-feature baseline documented above reproduces it. Forge version-check, module-info discovery,
and missing-texture messages are unrelated to these checks.

Build (`spotlessApply build -x test`) and whitespace checks passed. No deobfuscated runtime tests were used.
Final logs and disposable instances: `/tmp/td-baubles-obf.2gcSup/`, `passed.log` in all six `server-*` and six
`client-*` directories. Earlier `first`, `matrix`, `final`, `release`, and `verified` logs are retained as history,
not final-result evidence.

Manual fixture: `tests/obfuscated/manual-bauble-slots.zs`. The prepared isolated client is
`run/bauble-slots-test/`; launch with `bash run/bauble-slots-test/launch.sh`. Its README lists assignments,
give commands, stack/reload checks and WG controls. Manual user verification is pending.

## Salis Arcana bracelet replacement exclusion — 2026-09-14

Production artifact: `thaumicdabblery-94be0ec-snapshot-master.2+fdf5fefac1-dirty.jar`

SHA-256: `a796fe1d73861300bf4288fe0a62b75f506efd3b5597fab33f9e6c9f1a149268`

| ModTweaker | Salis Arcana | Thaumic Bases | TC4Tweaks | Server checks | Client checks |
| --- | --- | --- | --- | --- | --- |
| 0.14.0 | 1.1.71-GTNH | 1.8.13 | 1.5.47 | 2476 | 1430 |
| 0.9.6 | v2.6.0 | 1.3.1710.4 | 1.5.47 | 2476 | 1430 |
| 0.14.0 | 1.1.71-GTNH | 1.8.13 | absent | 2476 | 1238 |
| 0.9.6 | v2.6.0 | 1.3.1710.4 | absent | 2476 | 1238 |

All runs also include Thaumic Concilium 1.1.1, Thaumic Tinkerer 2.12.31, Forbidden Magic 0.9.17-GTNH,
Thaumcraft 4.2.3.5, CraftTweaker 3.4.8, Baubles Expanded 2.2.21, GTNHLib 0.11.23, UniMixins 0.3.1,
and production IC2 2.2.828 for CraftTweaker's generated class registry. DummyCore is 1.20.0 for the GTNH
set and 1.13 for the original set. Clients include NEI 2.8.130 and CodeChickenCore 1.4.17.
The clients with TC4Tweaks additionally include Aspect Recipe Index 1.1.3 for the actual generated-recipe checks.
Aspect Recipe Index requires TC4Tweaks' API, so it is omitted in the no-TC4Tweaks clients, also exercising
the optional client-integration guard. Dedicated servers have neither NEI nor Aspect Recipe Index.

Every probe asserts the obfuscated-environment flag. The four dedicated servers and four clients passed
9904 server and 5336 client assertions in total, including repeated checks after script reloads.
The client probes join disposable integrated worlds; this is not a remote multiplayer or manual GUI-click test.

Coverage:

- All 13 Thaumic Bases and 8 Concilium metadata entries in every one of the nine crafting-input slots.
  This includes variants whose optional addons are absent; they are checked for rejection, not made playable.
- Actual Salis cap/core recipe matching, direct output and aspect-cost queries, research queries, and unchanged
  rejected bracelet metadata/NBT. Bracelet inputs cannot produce either replacement output.
- Ordinary wand, staff, sceptre and staff/sceptre replacement remains functional, preserves custom NBT, and
  retains Vis costs. A bracelet in the workbench power slot does not reject an otherwise valid replacement.
- Bracelet casting-item recognition, Vis storage/consumption, scripted capacity and MineTweaker reloads remain
  functional. Recipe APIs are exercised directly; no actual player crafting click is simulated.
- Both real NEI replacement handlers return no bracelet usages but still generate ordinary replacement usages,
  before and after reload. Original bracelet crafting recipes are not removed by the implementation.

Final logs have no failed assertions, script compilation/execution errors or mixin application/injection failures.
The previously documented IC2/CraftTweaker `WeightedItemStack is already defined in that package` registry
message remains, along with unrelated Forge version-check, module-info discovery and missing-texture messages.
An initial no-TC4Tweaks client fixture stopped at Forge's dependency screen because Aspect Recipe Index was
installed without its required API; removing that addon from the fixture resolved startup without a mod-code change.

Build (`spotlessApply build -x test`), `spotlessCheck`, and whitespace checks passed. The repository could not
resolve its newly added FentLib `04136bd-snapshot` development runtime dependency, so a temporary Gradle init
script supplied the cached jar of that exact version. Project dependency files were left unchanged. No
deobfuscated runtime tests were used.

Raw instances and logs: `/tmp/td-bracelet-replacement.hKXPD7/`, `final.log` in the four `server-*` directories
and `verified.log` in the four `client-*` directories. Earlier attempts are retained separately.
The manual client is `run/bracelet-replacement-test/`; launch its `launch.sh`. It contains the same tested
production artifact and GTNH mod set, without automated probes or stat-changing scripts. Manual verification
is pending; a `clankus` notification was sent when it became available.

## Legacy Witching Gadgets and FentLib snapshot — 2026-09-14

Production artifact SHA-256: `89602663fb35968596f4d5b1d2eaffbe4eaa536103986444a8c791f7b77066a3`

All 26 obfuscated runs passed: dedicated server and client for each of the following combinations,
plus one server/client control without Witching Gadgets or Traveller's Gear.

| Witching Gadgets | Traveller's Gear | ModTweaker | TC4Tweaks | Checks per server/client run |
| --- | --- | --- | --- | --- |
| Original 1.1.10 | 1.16.6 | 0.14.0 and 0.9.6 | 1.5.47 and absent | 126 / 56 |
| KryptonCaptain 1.2.9 | 1.16.6 | 0.14.0 and 0.9.6 | 1.5.47 and absent | 126 / 56 |
| GTNH 1.8.51 | absent | 0.14.0 and 0.9.6 | 1.5.47 and absent | 101 / 52 |
| Absent control | absent | 0.14.0 | 1.5.47 | 75 / 38 |

Runtime dependencies: FentLib **04136bd-snapshot** (production jar), GTNHLib **0.11.37**, Baubles Expanded
2.2.23, CraftTweaker 3.4.8, Thaumcraft 4.2.3.5, UniMixins 0.3.1, and IC2 2.2.828 for CraftTweaker's generated
registry. Every run logged `FentLib redirected FalsePattern DepLoader progress to the terminal.` Its
`terminalDepLoaderProgress` option remained enabled, avoiding dependency-loader progress popups.
No development jars were installed in the runtime instances. Normal Gradle dependency resolution now works;
the client init script only selects the isolated working directory, with no dependency overrides.

Coverage includes script application and reloads, moved jump/haste/sniper effects, real registered sniper
metadata, cloak and kama discovery/storage, expanded focus pouches, native Traveller's Gear equipment,
same-metadata native/moved storage isolation, closing a bag after unequip, exact storage GUI coordinates,
expanded-slot wolf events, raven ability activation and glide, callbacks, expanded Vis amulets, and invalid
slot/disabled-feature handling. Client checks exercise Traveller's Gear's ability-list IDs and native packet
serialization; server checks exercise its receiver. This is not a remote multiplayer delivery test.

All final game and MineTweaker logs were audited separately: no failed assertions, script execution/compilation
errors, or mixin application/injection failures. The existing CraftTweaker `WeightedItemStack is already defined
in that package` registry message remains, as do unrelated Forge signature, module-info discovery and test-item
texture messages. The multi-target focus-pouch mixin also reports the expected absent alternate WG class.

An earlier Krypton fixture used metadata 6 after that fork moved the sniper ring to 5. The corrected fixture
and added full-script assertions passed the complete rerun above. Temporary runner log-path checks initially
looked outside `logs/`; the final independent audit checked all 26 actual `logs/minetweaker.log` files.
`spotlessApply assemble`, `spotlessCheck`, and whitespace checks passed. No deobfuscated runtime tests were used.

Raw instances: `/tmp/td-wg-legacy.J91YHc/`, with final game output in each instance's `verified.log`.
The manual client is `run/legacy-witching-gadgets-test/launch.sh`: original WG, Traveller's Gear, CurseForge
ModTweaker, TC4Tweaks, the same production artifact and FentLib snapshot, and no automatic probes.
Use Traveller's Gear's active-abilities wheel for moved cloaks. Manual verification remains pending;
`clankus` notifications were sent when the instance was ready and after its FentLib update.

## Champion mob configuration — 2026-09-16

Production artifact SHA-256: `563b98142a0404f41c52e5cfec461322adbcd74e5d3ea2c4b05d20df1e8bbe09`

All 12 obfuscated runs passed, with **41 dedicated-server assertions** and **46 client/integrated-server
assertions** in each matching pair (522 assertions total):

| ModTweaker | TC4Tweaks | Salis Arcana |
| --- | --- | --- |
| 0.14.0 | absent | absent |
| 0.14.0 | 1.5.47 | absent |
| 0.14.0 | 1.5.47 | 1.1.71-GTNH |
| 0.9.6 | absent | absent |
| 0.9.6 | 1.5.47 | absent |
| 0.9.6 | 1.5.47 | v2.6.0 |

All instances use production Forge 10.13.4.1614, Thaumcraft 4.2.3.5, Baubles Expanded 2.2.23, CraftTweaker
3.4.8, IC2 2.2.828 for its generated class registry, UniMixins 0.3.1, GTNHLib 0.11.37 and FentLib
04136bd-snapshot. Every log confirms FentLib's terminal DepLoader redirect. No development jars were installed
in the instances; compilation uses the SRG-first classpath, and probes assert the obfuscated runtime flag.

Checks invoke Thaumcraft's transformed `entitySpawns` handler, including its actual dangerous-room lookup,
and also exercise a real Forge world insertion. Coverage includes exact versus inherited eligibility,
whitelist/blacklist modes, guaranteed precedence, chance boundaries and fractions, vanilla difficulty/biome/
Outer Lands/maze bonuses, the native reduced-chance switch, addon weights, base-health threshold, creeper
modifier restriction, withers, a registered modded probe mob, and safe rejection of a missing attack attribute.
Invalid configuration entries/mode and chance clamping are covered. Thaumcraft applies the actual champion
buff/name; native modifier identity is retained for TC4Tweaks.

Champion NBT is written to disk, read back and rejoined without duplicate buffs or stripping; checked ordinary
mob NBT likewise retains its decision after config changes, including disabling our feature. This is an entity
serialization roundtrip, not a full server/world restart assertion. Client probes run common checks on the
integrated server thread, spawn a champion beside the logged-in player, and verify its synced modifier, health
and name over the real local connection. Fresh client-side entities are not independently converted. Remote
dedicated-server multiplayer delivery is not covered by these automated checks.

Final logs have no failed assertions, script compilation/execution errors or mixin application/injection failures.
The expected invalid-entry/mode/missing-attribute warnings are deliberate negative tests. Existing unrelated
CraftTweaker `WeightedItemStack` registry, Forge signature/version-check, module-info discovery, optional-class
and texture warnings remain. Early client fixtures could miss the spawned mob or pause on lost focus; the
final probe creates its fixture on player login and disables pause-on-lost-focus, and all six clients were rerun.

`spotlessApply assemble`, `spotlessCheck`, and whitespace checks passed. No deobfuscated runtime tests were used.
Raw instances/logs: `/tmp/td-champions-obf.mlVDlY/`, each instance's `verified.log` and `logs/minetweaker.log`.
The manual instance is `run/champion-mobs-test/launch.sh` with CurseForge ModTweaker and TC4Tweaks, the same
production artifact and FentLib snapshot, and no automated probe. `champions-manual.cfg` guarantees fresh
zombies, creepers and silverfish while excluding skeletons. A `clankus` alert was sent when ready.
The user subsequently confirmed that the manual champion test worked.

# Gadomancy Aura Pylon verification — 2026-09-29

Production artifact: `thaumicdabblery-eb6538c-snapshot-master+eb6538cfc9-dirty.jar`.
SHA-256: `9c44a8234a84f85c67faefbd11cca15f800de96cb8ddedf9bf70c021b92989fb`.
`./gradlew spotlessApply build` passed, including compilation, reobfuscation and Checkstyle.

All production runs used Forge 1.7.10-10.13.4.1614, Java 8, Thaumcraft 4.2.3.5,
CraftTweaker 3.4.8, UniMixins 0.3.1, Baubles Expanded 2.2.21-GTNH, GTNHLib 0.11.52,
and production FentLib 04136bd-snapshot. Probes assert `fml.deobfuscatedEnvironment=false`.

| ModTweaker | Gadomancy | TC4Tweaks | Dedicated server | Integrated client |
| --- | --- | --- | --- | --- |
| GTNH 0.14.0 | Original 1.0.7.3 | absent | 198 passed | 201 passed |
| CurseForge 0.9.6 | GTNH 1.5.16 | absent | 198 passed | 202 passed |
| GTNH 0.14.0 | GTNH 1.5.16 | 1.5.47 | 198 passed | 201 passed |
| CurseForge 0.9.6 | Original 1.0.7.3 | 1.5.47 | 198 passed | 201 passed |

All four dedicated servers passed `TD_PYLON_DISK_RESTART_PASS` against a world saved by
an earlier successful run. The extra client assertion in the CurseForge/GTNH case is
also a saved-world restart check. Both ModTweaker versions additionally passed startup
without Gadomancy and the explicit missing-mod diagnostic (`TD_PYLON_ABSENT_PASS`).
The two diagonal combinations with TC4Tweaks also passed an earlier no-TC4Tweaks core run.

Two separate dedicated-server/client connections passed live potion and aura-research
synchronization and client localization: GTNH ModTweaker with original Gadomancy, and
CurseForge ModTweaker with GTNH Gadomancy. Each server logged `TD_PYLON_NETWORK_READY`
and its client logged `TD_PYLON_CLIENT_PASS ... remote=true`. Both servers stopped after
client disconnect. These runs used the same production mod artifact; their common
server checks predate the final additional unfueled/overlapping-rule assertions.

A deobfuscated development server also passed 197 assertions with GTNH ModTweaker 0.14.0
and original Gadomancy 1.0.7.3, using the project's dev dependencies and GradleStartServer.
Its disposable probe copy was translated from SRG to MCP names using stable_12 CSVs,
with its environment assertion inverted to require development mode. The committed
production probes retain their obfuscated-environment requirement. Development launch
and transformed sources are retained under the raw instance directory below.

Coverage includes fueled/unfueled pylon updates, custom aspects, group/exact entity
selection, exclusions, range, independent native/custom intervals, native block and
entity callbacks, complete clear/redefine, selective scripted removal, duplicate and
overlapping rules, duration caps/amplifiers/immunity, invalid input without mutation,
configuration disable/re-enable, third-party registry replacement detection, repeated
MineTweaker undo/replay, original object restoration, research ownership/cache updates,
world persistence, and actual client packet delivery.

Tests exposed and resolved TC4Tweaks retaining removed aura research in its cache. The
integration also sends Thaumcraft's completion packet for newly discovered managed aura
effects; native Gadomancy only sends a notification. Early fixture failures came from
an unconnected fake player, reused tile counters, and granting research before the
integrated client's initial player-data synchronization. Final fixtures correct these.

No final pylon script errors, assertion failures, or mixin application/injection errors
were found. The test stack emits a pre-existing MineTweaker bootstrap message,
`WeightedItemStack is already defined in that package`, also reproduced in both
no-Gadomancy controls; pylon scripts still compile/apply successfully. Other unrelated
baseline warnings include Forge's obsolete update endpoint and scanning dependency
`module-info.class` files. Remote clients deliberately exit after passing, producing a
connection-reset message during server shutdown.

Limits: probes construct the multiblock through blocks/NBT and keep its crystal heartbeat
alive; they do not automate wand assembly or pipe delivery. Common checks manually tick
real tiles, while live client fixtures use the normal world tick loop. Localization and
research visibility are asserted through their game APIs, not a visual Thaumonomicon
inspection. Original MineTweaker3 itself was not tested; both ModTweaker forks ran on
CraftTweaker 3.4.8, matching the project's supported test stack.

Raw instances and logs: `/tmp/td-pylon-test/`. Final server/client matrix logs are
`passed.log` in the four matching server directories and `client-*` directories;
no-Gadomancy controls also use `passed.log`. Network evidence is in
`network-*/network.log` and `remote-*/verified.log`; development evidence is in
`dev-gtnh-original/test.log`. Initial-attempt logs are retained separately.

# Primal aspects and component editing verification (2026-09-29)

Production artifact: `thaumicdabblery-ac0f10e-snapshot-master.1+4361800230-dirty.jar`.
SHA-256: `f697c75c0bc5b41a034e4e92192875f1ec3e56aa1decaaf812ceae95ed215d4b`.
`./gradlew spotlessCheck build` passed. All production instances used identical copies of this jar.

The production matrix used Java 8, Forge 1.7.10-10.13.4.1614, Thaumcraft 4.2.3.5, CraftTweaker 3.4.8,
UniMixins 0.3.1, Baubles Expanded 2.2.21-GTNH, GTNHLib 0.11.52 and production FentLib 04136bd-snapshot.
The probes require `fml.deobfuscatedEnvironment=false`.

| ModTweaker | TC4Tweaks | Salis Arcana | Dedicated server | Integrated client |
| --- | --- | --- | --- | --- |
| GTNH 0.14.0 | absent | absent | 56 passed | 65 passed |
| CurseForge 0.9.6 | absent | absent | 56 passed | 65 passed |
| GTNH 0.14.0 | 1.5.47 | 1.1.71-GTNH | 56 passed | 65 passed |
| CurseForge 0.9.6 | 1.5.47 | 1.1.71-GTNH | 56 passed | 65 passed |

All final matrix runs also read the zero-point discovery NBT file saved by an earlier process and logged
`TD_PRIMAL_RESTART_PASS`. This is a disk knowledge roundtrip across processes, not an automated GUI save/rejoin.
The original compound-only server probes passed 121 assertions on each ModTweaker variant, including their
existing disk restart check. A deobfuscated development server using MCP-translated disposable copies of the
new probes passed 56 checks with GTNH ModTweaker; its environment assertion requires development mode.

Separate production client/server processes also passed on both ModTweaker variants without the optional addons.
Each dedicated server passed 56 checks; each remote client passed 9 checks over a real loopback TCP connection.
The clients verified concealment before a delayed successful server item scan, then received discovery and positive
research points, rendered the newly known cost, and checked the wand and vis-amulet tooltips. Both final pairs shut
down normally. An earlier CurseForge attempt timed out during login; the final fresh-player run passed.

Coverage includes two real custom primals, forward references, a disconnected compound rooted in a hidden primal,
editing Vacuos without changing object identity, reusing its freed combination in the same startup batch, rejected
cycles through new/existing aspects, duplicate/missing components, non-mutating validation failures, API aspect
lists, native combination and reduction, centrifuge outputs, energized-node vis, native wand recipe costs, explicit
primal vis spending, blocked initial/generic knowledge grants and fragments, failed scans, successful item and
item-entity scans, recovery for already-scanned seed items, per-player discovery isolation, zero-point knowledge
restoration and repeated ordinary-script reloads.

Clients rendered both workbench pages before/after discovery, invoked the actual page-click handler, checked
vis-amulet tooltip concealment and discovered wand tooltip output, and verified knowledge after a delayed server
scan. Screenshots were inspected: unknown costs show a question mark, discovered costs show their icon, and the
page label has a readable background. Extra workbench costs use native consumption modifiers, including the
wood/iron wand surcharge visible in the fixture screenshots.

Fixture corrections during development: multi-aspect ModTweaker strings need comma separators; client checks
must capture aspect objects after startup registration; tooltip assertions must account for whether Shift is held;
and initial-concealment checks need a fresh player/world rather than a player whose discovery was already saved.
Screenshots must be taken after a rendered frame, not immediately after opening/closing a GUI in a tick callback.
The fake-player harness also needed an embedded network channel: a dummy packet handler alone caused FML
routing exceptions despite passing assertions. It now uses an embedded channel with a handler and no dispatcher,
so fake-player packets are discarded without routing errors; separate real connections verify delivery.
These fixture issues were corrected in the retained probes. Final runs have no probe failures, fake-player packet
routing exceptions or mixin application/injection errors.

Limits: original MineTweaker3 itself and original Salis Arcana releases were not tested; both ModTweaker variants
ran on CraftTweaker 3.4.8, and the addon matrix used Salis 1.1.71-GTNH. GUI checks use constructed workbench tiles
and call the real click/render handlers; they do not automate physical mouse input or crafting through the output
slot. Primal-number/depth boundaries are validated in code but were not exhaustively stress-tested in live worlds.
Vanilla knowledge API callers are gated; addons writing the public knowledge maps directly can bypass that gate.
Baseline Forge update-endpoint/module-info warnings remain unrelated to this feature; the fixture scripts apply
successfully.

Raw instances, exported mixin classes and logs: `/tmp/td-primal-test/`. Final matrix logs are `final.log` in the four
server and four `client-*` directories; compound regression runs use `regression-{gtnh,curse}/final.log`.
Development evidence is `dev-gtnh-original/test.log`. Separate multiplayer logs are `network-{gtnh,curse}/final.log`
and `remote-{gtnh,curse}/final.log`. Rendered screenshots are in each client's `screenshots/`.

# Furnace research page verification (2026-09-30)

Production artifact: `thaumicdabblery-4361800-snapshot-master.1+dbcffde423-dirty.jar`.
SHA-256: `8506cc8db720157d41186becff124498895985f69b035824a83b395aa12e4bfa`.
`./gradlew spotlessCheck build` passed. The same production jar was used throughout the matrix.

The production instances used Java 8, Forge 1.7.10-10.13.4.1614, Thaumcraft 4.2.3.5, CraftTweaker 3.4.8,
UniMixins 0.3.1, Baubles Expanded 2.2.21-GTNH, GTNHLib 0.11.52 and FentLib 04136bd-snapshot.
The retained probes assert `fml.deobfuscatedEnvironment=false`.

| ModTweaker | Optional addons | Dedicated server | Integrated client |
| --- | --- | --- | --- |
| GTNH 0.14.0 | none | 56 passed | 65 passed |
| CurseForge 0.9.6 | none | 56 passed | 65 passed |
| GTNH 0.14.0 | TC4Tweaks 1.5.47, Salis Arcana 1.1.71-GTNH | 56 passed | 65 passed |
| CurseForge 0.9.6 | TC4Tweaks 1.5.47, Salis Arcana 1.1.71-GTNH | 56 passed | 65 passed |

A development server with GTNH ModTweaker passed 56 checks using disposable MCP-translated copies of the probes
and the built dev jar. Its environment assertion requires development mode. The initial translation needed two
1.7.10 mapping overrides (`smelting` and `getItemDamage`); no production change was needed.

Separate production client/server pairs also passed on both ModTweaker variants without the optional addons.
Each server passed 56 checks and each remote client passed 9 checks. Remote clients began with empty script
folders and received the demo through MineTweaker synchronization; they verified the recipe, automatic unlock,
click-through target and rendered page. These logs are `network-{gtnh,curse}/final.log` and
`remote-{gtnh,curse}/final.log` under the raw test directory.

Coverage includes ZenScript static expansion on both forks, vanilla iron smelting, the native balanced-shard recipe,
a scripted bowl-to-two-cookies recipe, actual furnace input consumption/output, multiple pages, ordering and object
identity, null page lists, metadata-specific inputs sharing an output, invalid arguments/missing recipes/research,
three repeat reloads, clearPages before/after addition, research movement/removal, detached/replaced research objects,
preservation of unrelated pages, later recipe replacement/removal, and full demo script removal/reinstallation.
Native Basic Alchemy pages remain unchanged. Deliberately invalid fixture calls log expected validation errors;
final logs contain no probe failures, ZenScript compilation/execution failures or mixin application errors.

Clients confirmed automatic research unlock for a real player and recipe click-through lookup, then rendered the
native page. Screenshots were inspected: the left page describes the demo, while the right page shows the native
smelting graphic with one bowl and two cookies. The dev client demo matches the tested tracked fixture byte for byte.

Limits: physical mouse navigation/tooltips and timed fuel burning were not automated. Furnace checks call the real
smelt operation directly. Original MineTweaker3 and original Salis Arcana releases were not tested; both ModTweaker
forks ran with CraftTweaker 3.4.8. Arbitrary recipe mutations outside the script lifecycle are not monitored. Baseline
Forge update-endpoint and dependency module-info warnings remain unrelated to the feature.

Raw instances and logs: `/tmp/td-furnace-test/`. Matrix results are `final.log` in the four server and four `client-*`
directories. Development evidence is `dev-gtnh-original/test.log`; rendered evidence is `screenshots/furnace-page.png`
in each client instance. The probes and manual fixture are retained under `tests/obfuscated/`.

A subsequent launch of the user's actual `runClient25 --username=Developer` failed before script loading.
The local `run/client/mods` folder contained old production copies of nine mods already supplied by Gradle,
including NEI and CodeChickenCore. FML registered NEI's transformer twice; the second pass failed to find the
original tooltip instructions in `GuiContainer.drawScreen` (`Needle not found in Haystack`). The isolated
feature-test instances did not contain these duplicates and therefore did not catch this local setup issue.
The duplicate jars were moved intact to `run/client/disabled-mods-2026-09-30/`; the standalone IC2 API jar stayed
in `mods`. Relaunching the actual Java 25 development task completed loading and rendering without that crash.
Evidence: `/tmp/td-furnace-test/dev-client-before-fix.log` and `dev-client-relaunch.log`.


# Corpse Effigy skins verification, 2026-10-08

Production artifact SHA-256: `c4ebcb78e13c60b04d3e534370dfeedbcd3c41d3740528d891b7e67e9efc4446`. `spotlessApply build` passed.
All runtime checks used obfuscated Forge 1.7.10-10.13.4.1614, Java 8, Thaumcraft 4.2.3.5,
Thaumic Horizons 1.8.24, CraftTweaker 3.4.8 and UniMixins 0.3.1.

| ModTweaker | TC4Tweaks 1.5.47 + Salis Arcana 1.1.71 | Dedicated server | Integrated client |
| --- | --- | --- | --- |
| GTNH 0.14.0 | absent | 28 passed | 17 passed |
| CurseForge 0.9.6 | absent | 28 passed | 17 passed |
| GTNH 0.14.0 | present | 28 passed | 17 passed |
| CurseForge 0.9.6 | present | 28 passed | 17 passed |

The same 28 server and 17 client checks passed with both scripting mods absent. With Horizons
also absent, the existing optional-dependency client probe passed startup, integrated world loading
and stock Thaumonomicon rendering.

Server checks cover native beacon activation, shared-viewer selection, last-binder appearance,
rebinding elsewhere, dimensions, binding packet round trips, beacon/vat disk and description NBT,
signed texture property preservation, malformed identities, self-infusion, consumed/destroyed bodies,
and a beacon bound before construction of the vat.

Clients receive real binding and tile updates, render the transformed native vat TESR, clear the body,
disconnect, then rejoin the saved world. Pixel assertions and inspected framebuffer images cover
legacy, modern classic/slim, clothing proportions, growing/ready/self-infusion modes and native fallback.
A loopback HTTP fixture passes a texture through the native asynchronous skin loader and renders the
result. Its hostname is temporarily allowed in the disposable probe process and restored afterward;
production code preserves the native skin-domain policy. Mojang service availability and arbitrary
third-party skin replacement mods were not tested.

The existing creature infusion/breach dedicated-server suite also passed all four combinations:
14,130 assertions per base run and 14,326 per addon run.

Evidence: `/tmp/td-effigy/`, with `result.log` under each named instance, `effigy-*.png` under each
client instance, `build.log`, and `regression.log`. Regression instances are `/tmp/td-vat-breach/`.
The fixtures are `EffigySkinChecks`, `EffigySkinServerProbe` and `EffigySkinClientProbe`.


# Vat tracking and frame controls verification, 2026-10-09

Production artifact SHA-256: `8edff9d15e77a469e5619266030afcfcd3286e343fef1087d4fe6ec1128acffb`.
`spotlessApply build` passed. Runtime checks used obfuscated Forge 1.7.10-10.13.4.1614,
Java 8, Thaumcraft 4.2.3.5, Thaumic Horizons 1.8.24, CraftTweaker 3.4.8 and UniMixins 0.3.1.

| ModTweaker | TC4Tweaks 1.5.47 + Salis Arcana 1.1.71 | Dedicated server | Separate connected client |
| --- | --- | --- | --- |
| GTNH 0.14.0 | absent | 391 passed | 12 passed |
| CurseForge 0.9.6 | absent | 391 passed | 12 passed |
| GTNH 0.14.0 | present | 391 passed | 12 passed |
| CurseForge 0.9.6 | present | 391 passed | 12 passed |

Server checks cover script compilation, reload undo, validation, four frame orientations,
frame precedence and saved selection, removal/fallback, frame override of body tracking,
head tracking around a framed direction, nearest-player selection, range, yaw/pitch bounds,
turn speed and angle wrapping. The contained entity's orientation remains unchanged.
The existing Corpse Effigy server suite also passed 28 checks with scripting mods present
and 28 with MineTweaker/ModTweaker absent.

Remote clients connect through Forge's native startup connection path. Actual entity-interaction
packets rotate the diamond frame through all four positions. Checks verify replicated and animated
poses, head limits, body tracking, effigy binding to Developer, and render-time restoration of entity
angles. These caught and corrected a client update injection that missed the native method's early
return. Framebuffer screenshots show the three assembled vats and were inspected for the base GTNH run.
Initial test connections that bypassed Forge's normal startup/status setup failed before login;
the final fixture uses the native connection path and all four runs pass.

Limits: arbitrary third-party creature renderers and live authenticated skin-service availability
were not tested. The offline Developer profile uses the normal skin fallback. This feature changes
vat rendering, not creature AI or the released entity's rotation.

Evidence: `/tmp/td-vat-facing/`, with `result.log` in `{gtnh,curse,gtnh-addons,curse-addons}`
for server checks and `remote-*` / `network-*` for separate client/server runs. Remote screenshots
are `remote-*/screenshots/vat-facing-demo.png`; effigy regression results are in `regression.log`.
The retained fixtures are `VatFacingChecks`, `VatFacingServerProbe`, `VatFacingDemo` and
`VatFacingRemoteProbe`. The user's separate play instances are under ignored `run/vat-facing-*`.


# Planar Vortex recipes verification, 2026-10-09

Production artifact SHA-256: `2e243074ac6408c8713241bda0d8654ec15e57119c023a30403d29f3dbdb4e51`.
`spotlessApply build` passed. Runtime checks used obfuscated Forge 1.7.10-10.13.4.1614,
Java 8, Thaumcraft 4.2.3.5, Thaumic Horizons 1.8.24, CraftTweaker 3.4.8 and UniMixins 0.3.1.

| ModTweaker | TC4Tweaks 1.5.47 + Salis Arcana 1.1.71 | Dedicated server | Separate connected client |
| --- | --- | --- | --- |
| GTNH 0.14.0 | absent | 70 passed | 9 passed |
| CurseForge 0.9.6 | absent | 70 passed | 9 passed |
| GTNH 0.14.0 | present | 70 passed | 9 passed |
| CurseForge 0.9.6 | present | 70 passed | 9 passed |

Checks use real ZenScript loading and transformed native vortex methods. Coverage includes input
remainders and complete batches through actual tile ticks, tagged item and entity outputs, distinct
entity UUIDs, cancelled entity spawns preserving inputs, input NBT matching, metadata/wildcards,
overlap priority, non-stackable output splitting, tile NBT persistence and native wand extraction.
All four native recipes are removed and restored; checks confirm original putty quantities, all six
primal charges in the disposable wand, one to four wisps, and the void golem's thrower ownership.
Three reloads, custom removal undo, reserved pearl inputs, malformed quantities/IDs/NBT, and
portal/stabilization restrictions are covered.

Separate clients receive server scripts, create dropped input entities through server commands,
send the native wand-use packet, and observe the remaining input, tagged retrieved item, and one
named zombie. The nine checks run through actual client/server connections on all four combinations.
No manual screenshot or UI-navigation claim is made; these tests verify crafting and networking.

Limits: arbitrary modded boss spawn callbacks and the full pocket-plane creation sequence were not
exercised. Primordial pearls are rejected as custom inputs, leaving the native portal branch intact.
The fixture's creative vortex bypasses attenuators for remote tests; the server fixture separately
checks that an ordinary vortex with five beams cannot craft. Integrated-server play and original
MineTweaker3 releases were not tested for this feature.

Evidence: `/tmp/td-planar/`, including `servers-final.log`, `remote.log`, each instance's `result.log`,
and the production artifact. Build output is `/tmp/td-planar-build.log`. Fixtures are retained as
`PlanarVortexChecks`, `PlanarVortexServerProbe`, `PlanarVortexDemo` and `PlanarVortexRemoteProbe`.

# Vat appearance controls verification, 2026-10-09

An isolated build based on `f330bce` plus the vat appearance changes passed `spotlessApply build`.
This deliberately excluded the concurrent Planar Vortex work. Production jar SHA-256:
`e7db41cce13833f09c91e82e4960920fb76e65d1f48f8fd581a5dade55fe833d`.
All runtime checks used obfuscated Forge 1.7.10-10.13.4.1614, Java 8, Horizons 1.8.24,
Thaumcraft 4.2.3.5, CraftTweaker 3.4.8 and UniMixins 0.3.1.

| ModTweaker | TC4Tweaks 1.5.47 + Salis Arcana 1.1.71 | Dedicated server | Separate connected client |
| --- | --- | --- | --- |
| GTNH 0.14.0 | absent | 39 passed | 32 passed |
| CurseForge 0.9.6 | absent | 39 passed | 32 passed |
| GTNH 0.14.0 | present | 39 passed | 32 passed |
| CurseForge 0.9.6 | present | 39 passed | 32 passed |

Server coverage includes global/per-entity script overloads, precedence independent of definition order,
zero amplitude, species changes, empty vats, wave extrema/partial ticks/long world clocks, NBT round trips,
legacy defaults, validation and repeated reload/removal. Clients verify actual server synchronization,
replacement of the native wave, scale matrices and pivot/Y offset calculations, matrix restoration after
normal and exceptional rendering, and unchanged physical position/hitbox. The existing remote frame and
head/body tracking assertions also pass. The GTNH base screenshot was inspected: the enlarged raised pig,
smaller raised effigy, and unchanged zombie remain in their respective vats.

Evidence: `/tmp/td-vat-appearance/`, with `build.log`, `servers.log`, `remote.log`, per-instance `result.log`,
and `remote-*/screenshots/vat-appearance-demo.png`. Tests use `VatAppearanceChecks`,
`VatAppearanceServerProbe`, and `VatAppearanceRemoteProbe` with the existing vat demo.
Arbitrary third-party entity renderers were not tested. The native dissolution bobbing behavior is unchanged.


## Planar Vortex completion modes and vis costs, 2026-10-09

Updated production artifact SHA-256: `1db39d517a4626dad3ff94e016a8197455439df79ed194beaa554ea224ad63b8`.
`spotlessApply build` passed. On the same obfuscated runtime and four ModTweaker/addon combinations
listed above, all **101 dedicated-server checks and 15 separate-client checks passed per combination**.
`VortexOptionalServerProbe` also passed startup and ordinary dropped-item merging with Horizons,
MineTweaker and ModTweaker absent.

Completion tests add instant item output and feedback protection, protected/unprotected stack merging
in both directions, output-marker save/load, collection/rethrow semantics, free and paid wand modes,
one batch per click, multi-stack output with one vis charge, all-aspect affordability, native crafting
discounts, cancelled item/creature spawn refunds, queue persistence, recipe/cost snapshots surviving
script removal, and validation. These checks caught TC4's empty AspectList.copy() behavior, which
creates a null-aspect entry, and a chat notification problem with disconnected automation players.
Both are handled in the final build.

Actual client/server packets verify instant diamonds, delayed pigs, an empty wand being refused,
and both creature creation and the charged wand's vis debit reaching the client. The remote fixture
uses survival mode with damage immunity: Salis Arcana bypasses vis payment in creative mode, so a
creative-mode empty-wand assertion is invalid with that setting. Production retains native crafting
behavior, including other mods' creative bypasses. Native recipes and unconfigured custom recipes
retain their previous free behavior.

Evidence: `/tmp/td-planar/completion-servers.log`, `completion-remote-final.log`, `optional-final.log`,
per-instance `result.log`, and `/tmp/td-planar-completion-build.log`. The remote fixture now covers
completion modes; `PlanarVortexCompletionChecks` and `VortexOptionalServerProbe` are retained too.
The user's ignored `run/planar-vortex-*` instances were redeployed with instant dirt-to-diamond and
cookie-to-pig requiring a wand with a base cost of 5 Aer plus 2 Terra. The previous play world remains
in `world`; this demo uses `completion-world`.

## Planar Vortex cost HUD and quiet failures, 2026-10-09

Production jar SHA-256: `cd571cedf08c4fa037927e8d8b1ca400d64deceef41d97f23f6d09f3f1ce0a4f`.
`spotlessApply build` passed. On obfuscated Forge 1614 / Thaumcraft 4.2.3.5 / Horizons 1.8.24,
GTNH ModTweaker 0.14.0 and CurseForge ModTweaker 0.9.6 each passed **107 dedicated-server checks
and 27 separate-client checks**, both alone and with TC4Tweaks 1.5.47 / Salis Arcana 1.1.71.
The no-Horizons/no-scripting-mod server probe also passed.

New checks exercise server-generated previews, exact hundredths after wand-cap modifiers,
per-aspect affordability without draining vis, native free-item queue priority, client packet delivery,
wand switching, look-away and held-item guards, missing-only pulsing, and clearing completed previews.
The remote client observes the native wand-failure sound and verifies that a refused payment adds no
routine failure chat. Its sound volume is nonzero so Minecraft dispatches sound events. The fixture's
zombie has zero movement speed so it cannot walk into the HUD test's line of sight.

In-game screenshots were inspected for both unaffordable and affordable costs. This caught
Thaumcraft's icon helper restoring lighting before the cost labels; the renderer now disables it again
for bright, readable text. Missing aspect icons pulse while the numerical costs remain steady.
Technical output/queue failures remain logged. These tests cover the native font/rendering path;
arbitrary third-party HUD replacements were not tested.

Evidence: `/tmp/td-planar/feedback-servers-verified.log`, `feedback-remote-verified.log`,
`feedback-optional-verified.log`, each instance's `result.log`, and
`remote-*/screenshots/vortex-cost-missing.png` / `vortex-cost-ready.png`.
Build log: `/tmp/td-planar-feedback-build.log`. The existing ignored play server and client jars were
updated; the server is available on `localhost:25569`. The already-closed play client was left closed.


## Planar Vortex crafting animation, 2026-10-09

Production jar SHA-256: `687c2f2e70b06bcd57015eb383b591e854f49aed349f72cdbb62c68b3f3a1af3`.
`spotlessApply build` passed. On obfuscated Forge 1614 / Thaumcraft 4.2.3.5 / Horizons 1.8.24,
GTNH ModTweaker 0.14.0 and CurseForge ModTweaker 0.9.6 each passed **128 dedicated-server checks
and 29 separate-client checks**, both alone and with TC4Tweaks 1.5.47 / Salis Arcana 1.1.71.
The no-Horizons/no-scripting-mod server probe also passed. A separate client with the rays disabled
passed all 29 checks.

New server checks cover delayed release, exactly-once output/payment, repeated clicks, missing inputs,
removed wands, disconnected owners, stabilization loss, chunk unload and saved-queue reload safety.
The rays reach zero at tick 20; expansion starts at tick 22. Client checks cover animation packets,
configuration and phase captures alongside the existing cost HUD and payment synchronization checks.

Additional GTNH visual runs with rays enabled and disabled passed all 29 checks, including native
renderer callback assertions for captured phases. Screenshots confirm contraction, rays disappearing
before expansion, and the contracted core remaining visible with rays disabled. The automated camera
now pans gently off-axis: its original fixed-axis view could hide the native vortex. This changes only
the test fixture. An intermediate disabled-rays run failed a HUD raycast assertion because its adult
zombie blocked the view; the stationary baby zombie fixture fixes that obstruction.

Evidence: `/tmp/td-planar/animation-servers-verified.log`, `animation-remote-verified.log`,
`animation-optional-verified.log`, `animation-visual-final.log`, and
`animation-rays-disabled-final.log`; phase screenshots are under `remote-*/screenshots/`.
Build log: `/tmp/td-planar-animation-build.log`. The ignored play server and matching standalone
client were updated with the same production jar for the dirt-to-diamond and cookie-to-pig demo.


## Planar Vortex input suction and deeper contraction, 2026-10-09

Production jar SHA-256: `221af7125097f7d86b879819e03afb04aab1c5b97e8d7e688704ef9b87c529ba`.
`spotlessApply build` passed. On obfuscated Forge 1614 / Thaumcraft 4.2.3.5 / Horizons 1.8.24,
GTNH ModTweaker 0.14.0 and CurseForge ModTweaker 0.9.6 each passed **137 dedicated-server checks**,
both alone and with TC4Tweaks 1.5.47 / Salis Arcana 1.1.71. The no-Horizons/no-scripting-mod
server probe passed with the expanded early EntityItem mixin.

The contracted scale is now 0.04 instead of 0.28; the ray envelope and release timing are unchanged.
New checks run actual item updates to verify lifting a falling input into the center, absorption
without early consumption, pickup/merge protection, exclusive ownership against a neighboring vortex,
and cancellation restoring visibility and gravity. Suction state is transient, and client prediction
uses the server's start time and input coordinates. Client absorption persists through the release
boundary until removal arrives, avoiding an input flash caused by network delay.

Both ModTweaker variants passed **30 separate-client checks**, each alone and with the addons.
A separate rays-disabled client also passed all 30 checks.
The remote fixture summons an instant input below and in front of the vortex and observes it rising
and becoming absorbed before output. In-game screenshots were inspected for the smaller core and
visible input flight. The normal demo server and client jars match the tested artifact.

Evidence: `/tmp/td-planar/suction-servers-final.log`, `suction-remote-final.log`,
`suction-optional-final.log`, and `remote-*/screenshots/vortex-input-suction.png` plus the existing
animation phase screenshots. Build log: `/tmp/td-planar-suction-build.log`.


## Osmotic Enchanter scripting, 2026-10-09

Production jar SHA-256: `7af13b93ad12b87e727053af261b526fea503a0963a454807219e2883d68d231`.
`spotlessApply build` passed. On obfuscated Forge 1614 / Thaumcraft 4.2.3.5,
each combination of original Thaumic Tinkerer 2.5-164 or GTNH Thaumic Tinkerer 2.12.33
with CurseForge ModTweaker 0.9.6 or GTNH ModTweaker 0.14.0 passed **36 dedicated-server
checks and 16 separate-client checks**. The server probe without Thaumic Tinkerer,
Thaumic Horizons or scripting mods also passed.

Checks cover real scripts, native cost scaling, exact level overrides, icons, research gates,
removal and reload undo, invalid definitions and packets, native enchantment incompatibilities,
late-registered enchantments, free recipes, idle selection refresh, and accepted running costs
surviving recipe removal and tile NBT save/load. Native pillars and an actual wand complete jobs
and pay vis. Remote checks exercise the stock GUI and packet handlers, live reload with the GUI
open, pagination beyond 16 entries, and keeping page two selected after server item synchronization.

In-game screenshots were inspected for icon rendering and pager placement beneath the wand slot.
These tests cover the stock interface; arbitrary third-party GUI replacements were not tested.
All test servers and clients were temporary fixtures, leaving the existing Planar Vortex play
instance unchanged.

Evidence: `/tmp/td-osmotic/servers-verified.log`, `remote-verified.log`,
`optional-verified.log`, individual instance `result.log` files, and
`remote-*/screenshots/osmotic-page-1.png`, `osmotic-page-2.png`, and `osmotic-selected.png`.
Build log: `/tmp/td-osmotic/build-pages.log`.

Pager appearance follow-up: SHA-256
`99e3a803f5538fc0030e3a95d4bf052c4214807e52b257c036035494c36ebd48`.
Centered pixel chevrons and an unboxed page count now align with the wand slot.
`spotlessApply build` passed, followed by 16 remote checks each with GTNH Tinkerer / GTNH
ModTweaker and original Tinkerer / CurseForge ModTweaker. The resulting in-game screenshot
was inspected for alignment. Evidence: `/tmp/td-osmotic/build-pager-style.log` and
`/tmp/td-osmotic/remote-pager-style.log`. The manual demo client was updated and restarted.

The manual demo server subsequently crashed when first loading `WandComponentVisDiscountRegistry`:
its installed jar had been overwritten while its JVM was still running during the pager deployment.
The class is present in the artifact. A clean server restart using the same artifact successfully
completed Sharpness III and verified payment of 4 Ordo plus 2 Aer, then reset the demo machine.
Evidence: `run/osmotic-server/play-restarted.log`, marker `TD_OSMOTIC_PLAY_CRAFT_PASS`.
Deployment instructions now require stopping the JVM before replacing its jars.

Start-button layout follow-up: SHA-256
`cfe24549b7d14f9346e7d478e85e9566eeb8a6d9c18725d1347143daed3b48f9`.
The native start/progress button now sits beside the vis bars, above the enchantment grid.
`spotlessApply build` passed. GTNH Tinkerer / GTNH ModTweaker and original Tinkerer /
CurseForge ModTweaker each passed 20 remote checks, including a full 16-entry grid,
nonoverlapping start-button hitboxes and actual completion/payment after activating the GUI button.
Evidence: `/tmp/td-osmotic/build-start-layout.log` and `remote-start-layout.log`.
A further GTNH run passed all 20 checks with a frame delay before the selected-page screenshot;
`remote-start-visual.log` and `remote-gtnh-gtnh/screenshots/osmotic-selected.png` confirm that
the start button is visible above two full rows. Both demo JVMs were stopped before jar replacement.

Final start-control behavior: SHA-256
`d4859367685961fc5ffbd82532856b31b2abdc233be1edd15b391e584ffe9060`.
The original unframed symbol remains visible when disabled, explains selection on hover, and follows
the vis bars down by 24 pixels on a one-row page. A temporary framed version was discarded.
`spotlessApply build` and 23 remote checks passed with both GTNH Tinkerer / GTNH ModTweaker and
original Tinkerer / CurseForge ModTweaker. Checks include the disabled tooltip, switching between
two rows and one row, unobstructed start-button hitboxes, and actual crafting/payment.
The remote fixture exposes 18 dummy enchantments so its second page fits one row.
Evidence: `/tmp/td-osmotic/build-start-adaptive.log`, `remote-start-adaptive.log`, and the page screenshots.
Both demo processes were stopped before updating their jars and the client reconnected successfully.

Tooltip lighting and overlay follow-up: SHA-256
`cc895144a23d8f7a73db0a44cdaee6cdee331ee6e1c49e4d97f86b5d85a7ad9d`.
Thaumic Tinkerer's native tooltip helper disables lighting/depth testing and changes blending/color
without restoring them. The enchanter now captures its tooltip for Forge's post-screen event,
draws it after NEI's container overlays, and restores all OpenGL attributes in a finally block.
Pending text is copied before the native GUI clears it and discarded before the next frame.

`spotlessApply build` and 25 remote checks passed with GTNH Tinkerer / GTNH ModTweaker and
original Tinkerer / CurseForge ModTweaker. The regression probe compares lighting, light sources,
color material, rescale normals, depth testing, blending, alpha testing, texture state, shade model,
current color and attribute-stack depth before and after rendering. Hovered/unhovered captures
show the tooltip covering NEI entries; static NEI pixels outside the tooltip are identical.
Evidence: `/tmp/td-osmotic/build-tooltip-overlay.log`, `remote-tooltip-overlay.log`, and
`remote-*/screenshots/osmotic-nei-hovered.png` / `osmotic-nei-unhovered.png`.

Full-payment and interruption follow-up: SHA-256
`77cab931530f3813f8b30a603be51113ae1fd3e5f3b542ddcccd70b37cd6afbb`.
`spotlessApply build` passed. All four original/GTNH Tinkerer and CurseForge/GTNH ModTweaker
combinations passed 69 server checks. GTNH Tinkerer / GTNH ModTweaker and original Tinkerer /
CurseForge ModTweaker each passed 28 separate-client checks.

New checks cover the full discounted cost before starting, rejection without consumption,
exact balances, and completion when the final discounted payment is less than one whole vis.
Native inventory operations cover pickup and immediate reinsertion, shift-click, hotbar swap,
dropping, direct extraction, clearing and replacement. Each cancellation clears progress without
refunding payment, preserves valid retry selections for wand removal, and emits exactly one
`thaumcraft:craftfail` sound, observed through the world's sound listener. Target removal/replacement
also cancels with one sound and clears selections. Zero extraction, setting the same stack and
closing the container leave the job running. Cancelled selections survive save/load and reinsertion;
the existing in-progress save/load test still passes. Free jobs now require a compatible wand.

Remote checks empty and refill the wand while the GUI remains open, verify Start is disabled with
the exact missing-vis tooltip, send an unaffordable start packet to verify server rejection, then
successfully craft after refilling. Evidence: `/tmp/td-osmotic/build-wand-guard.log`,
`servers-wand-guard.log`, and `remote-wand-guard.log`.
The stopped demo processes were updated; Sharpness V's demo cost is 40 of each standard primal,
allowing about 12 seconds to test cancellation before completion.

## Planar Vortex crafting sounds (2026-10-09)

Production artifact: `thaumicdabblery-dbcffde-snapshot-master.22+efb6722bff-dirty.jar`.
SHA-256: `38307d310d28700f8b59faccb8084318dad70d39d3f1c308213e04a97ce52c23`.
Build and formatting passed. All four obfuscated server configurations (CurseForge/GTNH
ModTweaker, with and without TC4Tweaks/Salis Arcana) passed 146 checks each. Separate
CurseForge and GTNH clients each passed 33 checks against dedicated servers.

The sound listener verifies opening immediately, closing exactly once at expansion (tick 22),
no closing after an earlier cancellation, and no previous crafting/completion sound during
custom recipes, native item retrieval or native golem conversion. Remote clients verify resource
registration, paired opening/closing playback and absence of the old sounds, alongside existing
animation, suction, vis and failure-feedback checks. Neither client logged missing sounds or
codec errors. The golem fixture includes a thrower, required by the native recipe.

Both bundled assets are mono 32 kHz Vorbis. The opening is trimmed to 1.1 seconds with a
0.7-to-1.1-second fade; decoded audio confirms the decreasing gain. The closing retains the
supplied 1.74125-second duration. Both assets and sounds.json are present in the production jar.
Evidence: `/tmp/td-planar/build-craft-sounds.log`, `sound-format.log`,
`sound-server-matrix.log`, and `sound-remote-matrix.log`.

## Planar Vortex research pages (2026-10-10)

Production artifact: `thaumicdabblery-dbcffde-snapshot-master.23+46547b4ebd-dirty.jar`.
SHA-256: `19b260bb93a0a0afafb184639adb504c91bbaef1da03ab048b9231d6c0c3a323`.
`spotlessApply build` passed. All four production server configurations (CurseForge/GTNH
ModTweaker, each with and without TC4Tweaks/Salis Arcana) passed 183 checks. The corresponding
four separate obfuscated clients each passed 100 checks against dedicated servers. All eight
installed jars were checked against the tested artifact hash.

Page checks cover both scripting overloads, quantities and NBT, optional display icons, actual
item-output references, live completion/cost changes, all four native recipes, multiple research
owners, removal/redefinition, repeated reloads, `clearPages` ordering, invalid arguments, null
initial page arrays and exact original-array restoration after undo.

Remote clients open the stock book, view eight pages through its native next-page mouse handler,
and check input/output tooltips, wildcard metadata preservation, lighting/depth/blend state and
GL stack restoration. Screenshots of all four spreads were captured; the first heading offset,
paid vis icons and native creature labels were visually reviewed. Existing crafting, suction,
vis, sound and animation checks also passed in each run.

Evidence: `/tmp/td-planar/build-pages.log`, `pages-servers.log`, `pages-remote.log`, and
`/tmp/td-planar/remote-*/screenshots/vortex-pages-{0,2,4,6}.png`.

Page visual follow-up: SHA-256
`cd5d5404d6f1caa06c044f6cc6d1c3703eba1823034022a9c19c1e8e820a7d76`.
Build passed; GTNH and CurseForge remote clients each passed 104 checks. The page now uses
Thaumcraft's book-forward arrow and the stabilized vortex's animated row from `nodes.png`.
Wand activation is shown by an actual wand above the vortex, with a right-click tooltip;
automatic recipes omit that indicator. Free recipes have no cost section. Updated captures
were visually reviewed, and the probe checks indicator hover only for wand recipes alongside
existing tooltip, wildcard and GL state checks. Evidence: `build-page-visuals.log`,
`page-visuals-remote.log`, and the refreshed `vortex-pages-*.png` captures under `/tmp/td-planar`.

Creature-preview follow-up: SHA-256
`7cb603bf79b5b4aa108585865276edfff536a827708fbecce3878518ecad8b5f`.
`spotlessCheck build` passed. The page renders real creature models by default, with an explicit
item icon still available as an override. The development demo now uses the pig model.
GTNH and CurseForge obfuscated remote clients each passed 126 checks against dedicated servers.
The probes verify pig, baby zombie, wisp and void golem models, applied baby/custom-name NBT,
cache reuse, absence from the world, item-icon overrides, GL culling/state/stacks and restoration
of lightmap/camera/billboard values. Screenshot review caught and corrected reversed billboard
culling for the wisp. Updated captures show the pig, baby zombie, wisp and void golem; neither
client logged preview-rendering errors. Evidence: `/tmp/td-planar/build-page-entities.log`,
`page-entities-final.log`, and refreshed `remote-*/screenshots/vortex-pages-*.png`.

Block-icon lighting fix: SHA-256
`074bd1b31cfc8500db341e0f70d3141c32e3e91b37f29eaf15355107203858cf`.
`spotlessCheck build` passed. The new framebuffer regression failed on the preceding production
jar at the dirt-versus-vanilla pixel comparison. Enabling vanilla's normal rescaling for item
renders and isolating their GL attributes fixes dark block faces. GTNH and CurseForge remote
clients each passed 132 checks with the corrected jar, including exact pixel comparisons against
vanilla inventory lighting before and after pig, zombie, wisp and golem previews. The dirt page
was visually reviewed. Evidence: `/tmp/td-planar/page-lighting-before.log`,
`page-lighting-after.log`, `build-page-lighting.log`, `dirt-before.png`, and the refreshed
`remote-*/screenshots/vortex-pages-2.png` captures.

Vertical vortex diagram (2026-10-10): production artifact
`thaumicdabblery-dbcffde-snapshot-master.24+d8e160a330-dirty.jar`, SHA-256
`9e1b77a77aa251c4b10118780325e530ba5e92ec2736fac9bff4adf3daf56d0d`.
`spotlessApply build` passed. The supplied 76x91 RGBA diagram is packaged byte-for-byte unchanged.
The smelting-style layout places the offering inside its opening, the item or creature below
its arrow, and costs underneath. The wand marker sits beside the opening. Names, entity NBT and
native golem ownership are available on hover. GTNH and CurseForge obfuscated remote clients each
passed 132 checks with updated input/output/wand hitboxes, including the block-lighting pixel
regression. Screenshots of the new paid and automatic layouts were visually reviewed.
Evidence: `/tmp/td-planar/build-page-diagram.log`, `page-diagram-remote.log`, and refreshed
`remote-*/screenshots/vortex-pages-*.png` captures.

Wand placement follow-up: SHA-256 `33b361b49c9c3f3c4e13701d2b1d664930b0ad797c6bcc5a1837075b43be504e`.
Removed the vis-cost heading and moved the wand and its hover target to the vertical midpoint
on the left of the diagram. `spotlessCheck build` passed; the obfuscated GTNH remote client
passed 132 checks. The final paid/free spread was visually reviewed. Evidence:
`/tmp/td-planar/build-page-wand-layout.log`, `page-wand-layout-remote.log`, and the refreshed
`remote-gtnh/screenshots/vortex-pages-0.png`.

## Mystical Construct pages (2026-10-10)

Artifact: `thaumicdabblery-dbcffde-snapshot-master.25+944bab394d-dirty.jar`.
SHA-256: `1fa6de38c62e0011de1e6f4cbba5cb8861df32d94ef2156cf0ec913c392be82d`. `spotlessCheck build` passed. All four production server
configurations (GTNH/CurseForge ModTweaker, each with and without TC4Tweaks/Salis Arcana)
passed 51 checks. Each corresponding remote client passed 86 checks. All installed production
jars match the tested hash. The GTNH remote pair ran without Thaumic Horizons installed.

The actual portal demo compiled and synchronized on both forks, with three automatically
unlocked pages: obsidian/flint and steel, glowstone/water bucket, and grass/water/flowers/diamond.
Checks cover all overloads, bottom-first ordering, null cells, NBT/wildcards, optional and empty
costs, merged amounts, invalid dimensions/stacks/aspects/overflow, subsequent valid calls after
rejections, repeated reloads, exact undo (including null page arrays), clear/remove/replace
interactions, and retaining unrelated pages. No craftable output is advertised by diagrams.

Clients exercised the stock book and next-page handler, activation/cost hovers, wildcard
resolution without mutating the definition, GL states and stacks, a 16-layer structure, and
liquid rendering. Screenshot review caught culled liquid faces; the new framebuffer assertion
failed on the preceding jar and passed after the correction, for water and lava with model
culling enabled. Screenshots confirm the portal layouts, liquids, activation items and costs.
The empty-cost check also catches Thaumcraft's empty AspectList.copy() null-entry quirk.

The tested fixture matches `run/client/scripts/thaumicdabblery_construct_demo.zs` byte-for-byte.
Evidence: `/tmp/td-construct/build.log`, `servers-final.log`, `remote-final.log`,
`liquid-before.log`, and `remote-*/screenshots/construct-pages-{0,1,2}.png`.

Layer-spacing correction: SHA-256
`bad69b81b6c0d61d5daa1c302ce43557281ac6da16722b9924e0e3eccf190176`.
The fixed 32-pixel layer pitch let the front flowers overlap the grass and water behind them
on the next layer. Spacing now includes the complete projected layer footprint plus a gap,
and page scaling accounts for that spacing. `spotlessApply build` passed. All four obfuscated
remote clients passed 86 checks each, with their dedicated servers passing 51 checks each.
GTNH and CurseForge screenshots were visually reviewed: all twelve flowers sit in a separate
ring above the blocks. Evidence: `/tmp/td-construct/build-spacing.log`, `remote-spacing.log`,
and refreshed `remote-*/screenshots/construct-pages-{0,1,2}.png`.

## Custom warp events (2026-10-10)

Artifact: `thaumicdabblery-dbcffde-snapshot-master.26+c0192a0e5d-dirty.jar`.
SHA-256: `20138bfd22dd299f83d47195ecb3a45eea0fd85327c9de931c8a5557a9f032c9`.
`spotlessApply spotlessCheck build` passed. All four standalone production servers passed
18 registry/script checks. Connected GTNH and CurseForge clients passed with 55 server checks
each; both combinations with TC4Tweaks/Salis Arcana passed with 56 checks each. All twelve
installed jars match the tested hash. The GTNH pair also runs without Thaumic Horizons.

Tests cover script validation, duplicate rejection, repeated reload/removal, inclusive warp
ranges, a statistical 25% selection check, uniformly choosing one eligible event, and no RNG
consumption when ineligible or disabled. Real non-operator clients exercise potion duration and
amplifier, sounds, relative teleport/summon, scripted research unlocks, explicit tellraw,
quiet vanilla/Thaumcraft command feedback, operator bystander silence, permission boundaries,
failed command continuation and recursive-trigger rejection.

The transformed native hook is exercised with deterministic severity rolls: it replaces only
the selected effect, preserves warp decay and Bath Salts/Eldritch progression, uses uncapped
total warp plus gear, and respects inactive counters, failed native rolls, tick cadence,
Warp Ward and disabled warp. Addon checks verify creative suppression separately from normal
survival triggering. The force command bypasses natural restrictions, preserves warp values,
checks permissions and feature enablement, and completes event names.

The fixture registers its research through MineTweaker before TC4Tweaks populates its cache.
Connected gameplay probes use survival mode, then explicitly enable creative for the Salis
Arcana suppression check. Expected warnings exercise invalid scripts/commands and permissions.
Evidence: `/tmp/td-warp/build-final.log`, `servers-final.log`, `remote-final.log`,
`network-*/result.log`, and `remote-*/result.log`.

## Vortex wand position and optional warp messages (2026-10-10)

Artifact: `thaumicdabblery-dbcffde-snapshot-master.27+a6c88cb9e2-dirty.jar`.
SHA-256: `bc90bd06369f568c20c6b201c37c71a4b2ab5131b63d3e655d5debc34f795307`.
`spotlessApply spotlessCheck build` passed. GTNH and CurseForge obfuscated vortex clients each
passed 132 checks with the wand hover target on the right; the updated page screenshot was
visually reviewed. Both the icon and tooltip moved together.

All four warp server configurations passed 24 script/registry checks. Connected GTNH/CurseForge
runs passed 64 server checks each; their TC4Tweaks/Salis Arcana combinations passed 65 each.
Every client verified exactly one private, dark-purple italic message for a multi-command event.
Old four-argument calls, empty/null messages, reloads, normal command-feedback suppression and
bystander silence remain covered. The dev-client example now includes a narrative message.
Evidence: `/tmp/td-warp/build-message.log`, `servers-message.log`, `remote-message.log`, and
`/tmp/td-planar/page-wand-right.log` plus refreshed `remote-*/screenshots/vortex-pages-*.png`.

## Witchery rite requirements (2026-10-10)

Artifact: `thaumicdabblery-dbcffde-snapshot-master.28+ee624d5012-dirty.jar`.
SHA-256: `54c991bd1ddce9b7b4b09191978422c5971ce8503318bc388c8b4480c88f4e49`.
`spotlessApply spotlessCheck build` passed. Tested with Witchery 0.24.1, CraftTweaker 3.4.8,
Forge 1614 and Java 8. Both GTNH ModTweaker 0.14.0 and CurseForge ModTweaker 0.9.6 passed
2,409 checks on standalone obfuscated servers, then 2,435 checks each with connected clients.
Both clients passed their script synchronization and native Circle Magic book assertions.
The GTNH remote pair also runs without Thaumic Horizons. Separate servers without Witchery
passed startup and 24 existing warp-event regression checks each on both ModTweaker forks.

Checks cover all native rite identities and registry slots, original requirement restoration,
reloads/removal, validation, disabled integration, nested and unknown sacrifice preservation,
shared condition-set isolation, exact native power timing, native item/mob consumption and
failure/refund paths, unchanged charged attuned stone output, zero-cost operation without an
altar, and actual heart-glyph activation with a placed chalk ring. A controlled IPowerSource
supplies altar power; tile steps are ticked directly, so this does not test altar power generation
or the full real-time animation duration. Optional items and native living sacrifices remain
separate from mandatory offerings. Existing upkeep effects are untouched.

Clients start with no local scripts, receive the server's edits, and open the stock Circle Magic
book. Screenshots show the diamond, optional cookie, pig, power cost and edited small-circle
layout. Both screenshots were visually reviewed. WitcheryExtras was not installed in these runs.

Evidence: `/tmp/td-witchery-build.log`, `/tmp/td-rites/servers.log`, `remote.log`,
`{gtnh,curse,network-gtnh,network-curse,remote-gtnh,remote-curse}/result.log`,
`remote-*/screenshots/witchery-rites.png`, and `/tmp/td-rites-absent/servers.log`.
All eight installed production jars matched the artifact hash. All test JVMs exited.
