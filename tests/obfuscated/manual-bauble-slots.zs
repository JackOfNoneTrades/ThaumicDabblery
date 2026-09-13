import mods.baubles.Baubles;

// dirt goes in either ring slot or the amulet slot
Baubles.setSlots(<minecraft:dirt>, ["ring", "amulet"]);

// white wool goes on your fingers and red wool goes around your neck
Baubles.setSlots(<minecraft:wool:*>, ["ring"]);
Baubles.setSlots(<minecraft:wool:14>, ["amulet"]);

// the double jump charm is a ring now
Baubles.setSlots(<WitchingGadgets:item.WG_Baubles:0>, ["ring"]);

// the haste vambrace is an amulet and the sniper ring is a belt
Baubles.setSlots(<WitchingGadgets:item.WG_Baubles:3>, ["amulet"]);
Baubles.setSlots(<WitchingGadgets:item.WG_Baubles:6>, ["belt"]);

// storage cloak on your finger and storage kama around your neck
Baubles.setSlots(<WitchingGadgets:item.WG_Cloak:2>, ["ring"]);
Baubles.setSlots(<WitchingGadgets:item.WG_Kama:2>, ["amulet"]);

// the raven kama keeps its glide toggle when moved onto your finger
Baubles.setSlots(<WitchingGadgets:item.WG_Kama:4>, ["ring"]);

// the focus pouch goes beyond the original four bauble slots
Baubles.setSlots(<Thaumcraft:FocusPouch>, ["charm"]);
