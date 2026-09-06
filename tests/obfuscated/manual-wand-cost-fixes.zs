import mods.thaumcraft.WandComponents;

// no need to change forbidden magic's creative components
WandComponents.setCapCraftingMultiplier("gold", 4);
WandComponents.setCapCraftingMultiplier("thaumium", 24);
WandComponents.setCoreCraftingCost("silverwood", 22);
WandComponents.setCoreCraftingCost("greatwood_staff", 24);
WandComponents.setCoreCraftingCost("silverwood_staff", 24);
WandComponents.setCoreCraftingCost("primal_staff", 32);

// no potency line on this core or on a finished primal staff
WandComponents.setCorePotency("primal_staff", 0);

// this one should still show its positive bonus
WandComponents.setCorePotency("greatwood_staff", 2);
