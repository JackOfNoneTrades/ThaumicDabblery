import mods.thaumcraft.WandComponents;

WandComponents.setCapCraftingMultiplier("gold", 4);
WandComponents.setCoreCraftingCost("greatwood", 11);
WandComponents.setCoreCapacity("greatwood", 80);
WandComponents.setCoreCapacity("greatwood_staff", 160);
WandComponents.setCorePotency("greatwood", 2);
WandComponents.setCorePotency("primal_staff", 0);
WandComponents.setCoreVisRegeneration("blaze", <aspect:ignis>, 40, 0.5, 20);
WandComponents.setCoreVisRegeneration("blaze", <aspect:aer>, 60, 1.0, 50);
WandComponents.disableCoreVisRegeneration("reed");
WandComponents.resetCoreVisRegeneration("reed");
