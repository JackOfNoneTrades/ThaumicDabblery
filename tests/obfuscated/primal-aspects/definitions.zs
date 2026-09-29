import mods.thaumcraft.CustomAspects;
CustomAspects.register("tdisland", 0x7654AB, "thaumcraft:textures/aspects/ordo.png", "tdhidden", "tdhidden", "Hidden island");
CustomAspects.registerPrimal("tdhidden", 0xABCDEF, "thaumcraft:textures/aspects/ordo.png", "Hidden primal", true);
CustomAspects.registerPrimal("tdvisible", 0xFEDCBA, "thaumcraft:textures/aspects/aer.png", "Visible primal", false);
CustomAspects.setComponents("vacuos", "tdhidden", "tdvisible");
CustomAspects.register("tdoldvoid", 0x123456, "thaumcraft:textures/aspects/vacuos.png", "aer", "perditio", "Old void pair");
