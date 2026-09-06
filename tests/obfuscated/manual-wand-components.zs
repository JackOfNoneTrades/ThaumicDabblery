import mods.thaumcraft.WandComponents;

// Gold + Greatwood assembly: 44 Vis of each primal aspect before discounts.
// The corresponding sceptre costs 66 before discounts.
WandComponents.setCapCraftingMultiplier("gold", 4);
WandComponents.setCoreCraftingCost("greatwood", 11);

// Existing or newly made Greatwood wands: 80 Vis/aspect; sceptres: 120.
// Greatwood staves: 160. The focus keeps its own upgrades and gets +2 innate Potency on Greatwood wands.
WandComponents.setCoreCapacity("greatwood", 80);
WandComponents.setCoreCapacity("greatwood_staff", 160);
WandComponents.setCorePotency("greatwood", 2);
WandComponents.setCorePotency("primal_staff", 0);

// Blaze cores: 0.5 Ignis per 2 seconds, stopping at 15 Vis (20% of 75).
// Also 1 Aer per 5 seconds, stopping at 37.5 Vis (50% of 75).
WandComponents.setCoreVisRegeneration("blaze", <aspect:ignis>, 40, 0.5, 20);
WandComponents.setCoreVisRegeneration("blaze", <aspect:aer>, 100, 1.0, 50);

// Thaumic Bases: iron bracelet 18 Vis, gold bracelet 22; other capacities unchanged.
// Iron bracelet gets +1 innate Potency and 0.25 Aer per second, up to its full 18 Vis.
WandComponents.setCastingCapacity("thaumicbases:castingBracelet", 0, 18);
WandComponents.setCastingCapacity("thaumicbases:castingBracelet", 1, 22);
WandComponents.setCastingPotency("thaumicbases:castingBracelet", 0, 1);
WandComponents.setCastingVisRegeneration("thaumicbases:castingBracelet", 0, <aspect:aer>, 20, 0.25, 100);

// Concilium: infernal bracelet 18 Vis, tainted bracelet 21; other capacities unchanged.
// Infernal bracelet gets +2 innate Potency; its native addon abilities remain intact.
WandComponents.setCastingCapacity("ThaumicConcilium:CastingBracelet", 4, 18);
WandComponents.setCastingCapacity("ThaumicConcilium:CastingBracelet", 5, 21);
WandComponents.setCastingPotency("ThaumicConcilium:CastingBracelet", 4, 2);

// /mt reload should preserve these values without doubling regeneration or Potency.
// Removing this script and reloading restores native values, but not any Vis discarded after lowering capacity.
