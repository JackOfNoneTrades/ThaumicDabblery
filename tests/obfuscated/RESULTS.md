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
