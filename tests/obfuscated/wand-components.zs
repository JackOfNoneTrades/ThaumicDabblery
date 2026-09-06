import mods.thaumcraft.WandComponents;

WandComponents.setCapCraftingMultiplier("gold", 4);
WandComponents.setCoreCraftingCost("greatwood", 11);
// creative components must not block normal core or cap edits
WandComponents.setCoreCraftingCost("silverwood", 22);
WandComponents.setCoreCraftingCost("greatwood_staff", 24);
WandComponents.setCoreCraftingCost("silverwood_staff", 24);
WandComponents.setCoreCraftingCost("primal_staff", 32);
WandComponents.setCapCraftingMultiplier("thaumium", 24);
WandComponents.setCoreCapacity("greatwood", 80);
WandComponents.setCoreCapacity("greatwood_staff", 160);
WandComponents.setCorePotency("greatwood", 2);
WandComponents.setCorePotency("primal_staff", 0);
WandComponents.setCoreVisRegeneration("blaze", <aspect:ignis>, 40, 0.5, 20);
WandComponents.setCoreVisRegeneration("blaze", <aspect:aer>, 60, 1.0, 50);
WandComponents.disableCoreVisRegeneration("reed");
WandComponents.resetCoreVisRegeneration("reed");
