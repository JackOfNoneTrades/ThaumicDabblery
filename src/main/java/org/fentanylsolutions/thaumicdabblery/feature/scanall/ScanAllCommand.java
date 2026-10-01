package org.fentanylsolutions.thaumicdabblery.feature.scanall;

import java.util.Arrays;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;

import org.fentanylsolutions.thaumicdabblery.ThaumicDabblery;

public final class ScanAllCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "thaumicdabblery";
    }

    @Override
    public List<String> getCommandAliases() {
        return Arrays.asList("td");
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/td scanall [player] | /td edit";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] arguments) {
        if (arguments.length == 1 && "edit".equalsIgnoreCase(arguments[0])) {
            try {
                boolean enabled = ThaumicDabblery.proxy.toggleResearchEditor();
                sender.addChatMessage(
                    new ChatComponentText(
                        enabled
                            ? "Thaumonomicon edit mode enabled. Open the book to edit. Changes save to scripts/thaumicdabblery_research_editor.zs."
                            : "Thaumonomicon edit mode disabled."));
            } catch (IllegalArgumentException exception) {
                throw new CommandException(exception.getMessage());
            }
            return;
        }
        if (arguments.length < 1 || arguments.length > 2 || !"scanall".equalsIgnoreCase(arguments[0])) {
            throw new WrongUsageException(getCommandUsage(sender));
        }
        EntityPlayerMP target = arguments.length == 2 ? getPlayer(sender, arguments[1])
            : getCommandSenderAsPlayer(sender);
        int discovered = ScanAll.complete(target);
        target.addChatMessage(new ChatComponentTranslation("thaumicdabblery.scanall.complete", discovered));
        if (sender != target) sender.addChatMessage(
            new ChatComponentTranslation("thaumicdabblery.scanall.target", target.getCommandSenderName()));
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] arguments) {
        if (arguments.length == 1) return getListOfStringsMatchingLastWord(arguments, "scanall", "edit");
        if (arguments.length == 2 && "scanall".equalsIgnoreCase(arguments[0])) {
            return getListOfStringsMatchingLastWord(
                arguments,
                MinecraftServer.getServer()
                    .getAllUsernames());
        }
        return null;
    }

    @Override
    public boolean isUsernameIndex(String[] arguments, int index) {
        return index == 1;
    }
}
