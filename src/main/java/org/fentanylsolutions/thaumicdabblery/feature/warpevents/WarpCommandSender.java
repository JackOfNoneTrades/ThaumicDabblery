package org.fentanylsolutions.thaumicdabblery.feature.warpevents;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

import org.fentanylsolutions.thaumicdabblery.ThaumicDabblery;

/** Command-block permissions with the affected player's location, without operator chat spam. */
public final class WarpCommandSender implements ICommandSender {

    private final EntityPlayerMP player;
    private final String event, command;
    private final List<String> feedback = new ArrayList<>();
    private boolean failed;

    public WarpCommandSender(EntityPlayerMP player, String event, String command) {
        this.player = player;
        this.event = event;
        this.command = command;
    }

    public String getCommandSenderName() {
        return "Warp:" + event;
    }

    public IChatComponent func_145748_c_() {
        return new ChatComponentText(getCommandSenderName());
    }

    public boolean canCommandSenderUseCommand(int level, String name) {
        return level <= 2;
    }

    public ChunkCoordinates getPlayerCoordinates() {
        return player.getPlayerCoordinates();
    }

    public World getEntityWorld() {
        return player.worldObj;
    }

    public void addChatMessage(IChatComponent message) {
        feedback.add(message.getUnformattedText());
        if (message.getChatStyle()
            .getColor() == EnumChatFormatting.RED || message.getUnformattedText()
                .contains("\u00a7c"))
            failed = true;
        if (message instanceof ChatComponentTranslation) {
            String key = ((ChatComponentTranslation) message).getKey()
                .toLowerCase(java.util.Locale.ROOT);
            if (key.contains("fail") || key.contains("error") || key.contains("notfound") || key.contains("outofworld"))
                failed = true;
        }
    }

    public boolean failed() {
        return failed;
    }

    public void logFailure() {
        ThaumicDabblery.LOG.warn(
            "Warp event {} for {} failed executing '{}': {}",
            event,
            player.getCommandSenderName(),
            command,
            feedback);
    }
}
