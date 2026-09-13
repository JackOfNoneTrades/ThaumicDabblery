import mods.baubles.Baubles;

// dirt can go on your fingers or around your neck
Baubles.setSlots(<minecraft:dirt>, ["ring", "amulet"]);
Baubles.addSlot(<minecraft:stick>, "belt");
Baubles.remove(<minecraft:stick>);
Baubles.addSlot(<minecraft:diamond>, "ring");
Baubles.addSlot(<minecraft:diamond>, "amulet");
Baubles.removeSlot(<minecraft:diamond>, "ring");
Baubles.setSlots(<minecraft:wool:*>, ["ring"]);
Baubles.setSlots(<minecraft:wool:14>, ["amulet"]);
