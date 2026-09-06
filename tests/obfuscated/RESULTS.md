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
