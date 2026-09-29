// Ordinary scripts/*.zs; requires Gadomancy and either ModTweaker fork.
import mods.gadomancy.AuraPylon;

// A pylon supplied with Aqua now affects players only, with two potion effects.
AuraPylon.clear("aqua");
AuraPylon.add("aqua", "players", 13); // Water breathing
AuraPylon.add("aqua", "players", 16); // Night vision

// Exanimis now grants fire resistance to undead, without its native zombie spawning.
AuraPylon.clear("exanimis");
AuraPylon.add("exanimis", "undead", 12);

// Add an exact entity target while keeping Aer's native behavior.
AuraPylon.add("aer", "entity:Cow", 12, 0, 10, 1200, 4, 8);

// Remove this script and /mt reload to restore original behavior.
// Already applied potions expire normally.
