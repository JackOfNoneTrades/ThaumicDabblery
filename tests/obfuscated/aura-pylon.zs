import mods.gadomancy.AuraPylon;
AuraPylon.clear("aqua");
AuraPylon.add("aqua", "players", 13, 0, 10, 60, 4, 8);
AuraPylon.add("aqua", "players", 16);
AuraPylon.add("aqua", "players", 16); // Exact duplicates must not double the charge rate.
AuraPylon.clear("exanimis");
AuraPylon.add("exanimis", "undead", 12);
AuraPylon.add("tdpylon", "entity:Cow", 1, 1, 10, 40, 4, 8);
AuraPylon.add("tdpylon", "players", 13);
AuraPylon.add("tdpylon", "players", 16);
AuraPylon.removeCustom("tdpylon", "players", 16);
