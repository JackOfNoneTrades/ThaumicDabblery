package org.fentanylsolutions.thaumicdabblery.compat.modtweaker;

import minetweaker.MineTweakerAPI;
import minetweaker.MineTweakerImplementationAPI;
import minetweaker.api.player.IPlayer;
import minetweaker.api.server.ICommandFunction;
import thaumcraft.api.wands.FocusUpgradeType;

public final class FocusUpgradeCommand implements ICommandFunction {

    private static final String COMMAND = "focusUpgrades";

    private FocusUpgradeCommand() {}

    public static void register() {
        if (MineTweakerAPI.server != null) {
            MineTweakerAPI.server.addMineTweakerCommand(
                COMMAND,
                new String[] { "/minetweaker " + COMMAND,
                    "    Outputs every registered Thaumcraft focus upgrade ID to minetweaker.log" },
                new FocusUpgradeCommand());
        }
    }

    @Override
    public void execute(String[] arguments, IPlayer player) {
        MineTweakerAPI.logCommand("Registered Thaumcraft focus upgrades:");
        int count = 0;
        for (FocusUpgradeType upgrade : FocusUpgradeType.types) {
            if (upgrade != null) {
                MineTweakerAPI.logCommand(upgrade.id + ": " + upgrade.getLocalizedName() + " [" + upgrade.name + "]");
                count++;
            }
        }
        MineTweakerAPI.logCommand("Found " + count + " registered Thaumcraft focus upgrades");

        if (player != null) {
            player.sendChat(
                MineTweakerImplementationAPI.platform
                    .getMessage("Focus upgrade list generated; see minetweaker.log in your logs directory"));
        }
    }
}
