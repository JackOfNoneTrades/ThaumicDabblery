package org.fentanylsolutions.thaumicdabblery.feature.warpevents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.util.FakePlayer;

import org.fentanylsolutions.thaumicdabblery.ThaumicDabblery;

public final class CustomWarpEvents {

    private static final Map<String, Event> EVENTS = new LinkedHashMap<>();
    private static final Pattern TARGET = Pattern.compile("(?<!\\S)@w(?!\\S)");
    private static final ThreadLocal<Boolean> RUNNING = new ThreadLocal<>();

    private CustomWarpEvents() {}

    public static final class Event {

        public final String name;
        public final int minWarp, maxWarp;
        public final List<String> commands;
        public final String message;

        public Event(String name, int minWarp, int maxWarp, String[] commands) {
            this(name, minWarp, maxWarp, commands, null);
        }

        public Event(String name, int minWarp, int maxWarp, String[] commands, String message) {
            if (name == null || !name.matches("[a-zA-Z0-9_.:-]+"))
                throw new IllegalArgumentException("event names require letters, digits, _, ., : or -");
            if (minWarp < 0 || maxWarp < minWarp) throw new IllegalArgumentException("require 0 <= minWarp <= maxWarp");
            if (commands == null || commands.length == 0)
                throw new IllegalArgumentException("supply at least one command");
            List<String> copy = new ArrayList<>();
            for (String command : commands) {
                if (command == null || command.trim()
                    .isEmpty() || command.indexOf('\n') >= 0 || command.indexOf('\r') >= 0)
                    throw new IllegalArgumentException("each command must be one non-empty line");
                String text = command.trim();
                if (text.startsWith("/")) text = text.substring(1)
                    .trim();
                if (text.isEmpty()) throw new IllegalArgumentException("empty command");
                copy.add(text);
            }
            this.name = name;
            this.minWarp = minWarp;
            this.maxWarp = maxWarp;
            this.commands = Collections.unmodifiableList(copy);
            this.message = message == null || message.trim()
                .isEmpty() ? null : message;
        }
    }

    public static synchronized void add(Event event) {
        if (EVENTS.containsKey(event.name)) throw new IllegalArgumentException("duplicate warp event: " + event.name);
        EVENTS.put(event.name, event);
    }

    public static synchronized void undo(Event event) {
        EVENTS.remove(event.name, event);
    }

    public static synchronized Event get(String name) {
        return EVENTS.get(name);
    }

    public static synchronized List<Event> events() {
        return new ArrayList<>(EVENTS.values());
    }

    public static Event select(int total, Random random) {
        if (!WarpEventsFeature.isEnabled() || WarpEventsFeature.chance() <= 0) return null;
        List<Event> eligible = new ArrayList<>();
        for (Event event : events()) if (total >= event.minWarp && total <= event.maxWarp) eligible.add(event);
        if (eligible.isEmpty() || random.nextDouble() >= WarpEventsFeature.chance()) return null;
        return eligible.get(random.nextInt(eligible.size()));
    }

    public static int execute(Event event, EntityPlayerMP player) {
        if (!WarpEventsFeature.isEnabled()) throw new IllegalArgumentException("Custom warp events are disabled");
        if (player == null || player instanceof FakePlayer || player.worldObj.isRemote)
            throw new IllegalArgumentException("Warp events require a real server player");
        if (Boolean.TRUE.equals(RUNNING.get()))
            throw new IllegalArgumentException("Warp events cannot trigger other warp events recursively");
        RUNNING.set(true);
        int succeeded = 0;
        try {
            if (event.message != null) player.addChatMessage(
                new ChatComponentText(event.message).setChatStyle(
                    new ChatStyle().setColor(EnumChatFormatting.DARK_PURPLE)
                        .setItalic(true)));
            for (String template : event.commands) {
                String command = TARGET.matcher(template)
                    .replaceAll(java.util.regex.Matcher.quoteReplacement(player.getCommandSenderName()));
                WarpCommandSender sender = new WarpCommandSender(player, event.name, command);
                try {
                    int result = MinecraftServer.getServer()
                        .getCommandManager()
                        .executeCommand(sender, command);
                    if (result > 0 && !sender.failed()) succeeded++;
                    else sender.logFailure();
                } catch (Exception error) {
                    ThaumicDabblery.LOG.error(
                        "Warp event {} for {} failed executing {}",
                        event.name,
                        player.getCommandSenderName(),
                        command,
                        error);
                }
            }
        } finally {
            RUNNING.remove();
        }
        return succeeded;
    }
}
