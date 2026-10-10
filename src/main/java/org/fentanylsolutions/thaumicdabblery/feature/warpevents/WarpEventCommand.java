package org.fentanylsolutions.thaumicdabblery.feature.warpevents;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;

public final class WarpEventCommand {

    private WarpEventCommand() {}

    public static void process(ICommandSender sender, String[] args) {
        if (!WarpEventsFeature.isEnabled()) throw new CommandException("Custom warp events are disabled");
        if (args.length == 2 && "list".equalsIgnoreCase(args[1])) {
            List<CustomWarpEvents.Event> events = CustomWarpEvents.events();
            sender.addChatMessage(new ChatComponentText("Custom warp events: " + events.size()));
            for (CustomWarpEvents.Event event : events) sender.addChatMessage(
                new ChatComponentText(
                    event.name + " (warp "
                        + event.minWarp
                        + " to "
                        + event.maxWarp
                        + ", "
                        + event.commands.size()
                        + " commands)"));
            return;
        }
        if ((args.length != 3 && args.length != 4) || !"trigger".equalsIgnoreCase(args[1]))
            throw new WrongUsageException("/td warp list | /td warp trigger <event> [player]");
        CustomWarpEvents.Event event = CustomWarpEvents.get(args[2]);
        if (event == null) throw new CommandException("Unknown warp event: " + args[2]);
        EntityPlayerMP player = args.length == 4 ? CommandBase.getPlayer(sender, args[3])
            : CommandBase.getCommandSenderAsPlayer(sender);
        try {
            int succeeded = CustomWarpEvents.execute(event, player);
            sender.addChatMessage(
                new ChatComponentText(
                    "Triggered " + event.name
                        + " for "
                        + player.getCommandSenderName()
                        + ": "
                        + succeeded
                        + "/"
                        + event.commands.size()
                        + " commands succeeded."
                        + (succeeded < event.commands.size() ? " See server log for failures." : "")));
        } catch (IllegalArgumentException error) {
            throw new CommandException(error.getMessage());
        }
    }

    public static List<String> complete(ICommandSender sender, String[] args) {
        if (args.length == 2) return CommandBase.getListOfStringsMatchingLastWord(args, "list", "trigger");
        if (args.length >= 3 && "trigger".equalsIgnoreCase(args[1])) {
            if (args.length == 4) return CommandBase.getListOfStringsMatchingLastWord(
                args,
                MinecraftServer.getServer()
                    .getAllUsernames());
            if (args.length == 3) {
                List<String> names = new ArrayList<>();
                for (CustomWarpEvents.Event event : CustomWarpEvents.events()) names.add(event.name);
                return CommandBase.getListOfStringsMatchingLastWord(args, names.toArray(new String[0]));
            }
        }
        return null;
    }
}
