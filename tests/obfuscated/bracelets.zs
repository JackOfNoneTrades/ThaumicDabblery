import mods.thaumcraft.WandComponents;

WandComponents.setCastingCapacity("thaumicbases:castingBracelet", 20);
WandComponents.setCastingCapacity("thaumicbases:castingBracelet", 0, 18);
WandComponents.setCastingCapacity("thaumicbases:castingBracelet", 1, 22);
WandComponents.setCastingPotency("thaumicbases:castingBracelet", 1);
WandComponents.setCastingPotency("thaumicbases:castingBracelet", 0, 3);
WandComponents.setCastingPotency("thaumicbases:castingBracelet", 1, 2);
WandComponents.setCastingVisRegeneration("thaumicbases:castingBracelet", <aspect:aer>, 20, 0.25, 100);
WandComponents.setCastingVisRegeneration("thaumicbases:castingBracelet", 8, <aspect:ignis>, 40, 0.5, 20);
WandComponents.setCastingVisRegeneration("thaumicbases:castingBracelet", 9, <aspect:aqua>, 40, 0.5, 20);
WandComponents.disableCastingVisRegeneration("thaumicbases:castingBracelet", 12);
WandComponents.resetCastingVisRegeneration("thaumicbases:castingBracelet", 12);
