package me.ehsan.thunderchat.spy;

import me.ehsan.thunderchat.ThunderChat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.*;
import java.util.*;

/** Network-wide, per-player chat/PM/command spy state. */
public final class SpyManager implements PluginMessageListener {
    private final ThunderChat plugin;
    private final Map<UUID, EnumSet<Section>> enabled = new HashMap<>();
    private final Set<UUID> initialized = new HashSet<>();

    public enum Section { CHAT, COMMANDS, PRIVATE_MESSAGES }

    public SpyManager(ThunderChat plugin) {
        this.plugin = plugin;
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "BungeeCord", this);
    }

    public boolean canSpy(Player player) { return player.hasPermission("thunderchat.command.spy") && !player.hasPermission("thunderchat.bypass.spy"); }
    public boolean isEnabled(Player player, Section section) { return enabled.getOrDefault(player.getUniqueId(), EnumSet.noneOf(Section.class)).contains(section); }

    public void enableAll(Player player) { if (!canSpy(player)) return; enabled.put(player.getUniqueId(), EnumSet.allOf(Section.class)); initialized.add(player.getUniqueId()); }
    public void disableAll(Player player) { enabled.remove(player.getUniqueId()); initialized.add(player.getUniqueId()); }
    public void toggle(Player player, Section section) {
        if (!canSpy(player)) return;
        EnumSet<Section> set = enabled.computeIfAbsent(player.getUniqueId(), k -> EnumSet.noneOf(Section.class));
        if (!set.remove(section)) set.add(section);
        initialized.add(player.getUniqueId());
    }
    public boolean isInitialized(Player player) { return initialized.contains(player.getUniqueId()); }
    public void autoEnable(Player player) {
        if (canSpy(player) && player.hasPermission("thunderchat.spy.autoenable") && !isInitialized(player)) enableAll(player);
    }

    public String status(Player player) {
        return "chat=" + isEnabled(player, Section.CHAT) + ", commands=" + isEnabled(player, Section.COMMANDS) + ", private-messages=" + isEnabled(player, Section.PRIVATE_MESSAGES);
    }

    public void spyChat(Player source, String channel, String message) {
        if (source.hasPermission("thunderchat.bypass.spy")) return;
        String output = format("CHAT", channel, source.getName(), message);
        sendLocal(Section.CHAT, output);
        broadcast(Section.CHAT, output);
    }

    public void spyPrivateMessage(Player source, Player target, String message) {
        if (source.hasPermission("thunderchat.bypass.spy") || target.hasPermission("thunderchat.bypass.spy")) return;
        String output = format("PM", source.getName() + " -> " + target.getName(), source.getName(), message);
        sendLocal(Section.PRIVATE_MESSAGES, output);
        broadcast(Section.PRIVATE_MESSAGES, output);
    }

    public void spyCommand(Player source, String command) {
        if (source.hasPermission("thunderchat.bypass.spy")) return;
        String output = format("COMMAND", "", source.getName(), "/" + command);
        sendLocal(Section.COMMANDS, output);
        broadcast(Section.COMMANDS, output);
    }

    private String format(String type, String channel, String player, String message) {
        String format = plugin.getPluginConfig().getString("spy.format", "&8[&cSPY&r&8] &7{type}&r &8| &7{channel}&r &8| &f{player}&r &8| &f{message}");
        return ChatColor.translateAlternateColorCodes('&', format.replace("{type}", type).replace("{channel}", channel).replace("{player}", player).replace("{message}", message));
    }

    private void sendLocal(Section section, String message) {
        for (Player player : Bukkit.getOnlinePlayers()) if (isEnabled(player, section) && canSpy(player)) player.sendMessage(message);
    }

    private void broadcast(Section section, String message) {
        if (!plugin.getPluginConfig().getBoolean("spy.network", true)) return;
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream data = new DataOutputStream(bytes);
            data.writeInt(7); data.writeUTF("SPY"); data.writeUTF(section.name()); data.writeUTF(message); data.flush();
            ByteArrayOutputStream outerBytes = new ByteArrayOutputStream();
            DataOutputStream outer = new DataOutputStream(outerBytes);
            outer.writeUTF("Forward"); outer.writeUTF("ALL"); outer.writeUTF("ThunderChat");
            outer.writeShort(bytes.size()); outer.write(bytes.toByteArray()); outer.flush();
            Player carrier = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
            if (carrier != null) carrier.sendPluginMessage(plugin, "BungeeCord", outerBytes.toByteArray());
        } catch (IOException ignored) { }
    }

    @Override public void onPluginMessageReceived(String channel, Player source, byte[] data) {
        if (!"BungeeCord".equals(channel)) return;
        try {
            DataInputStream outer = new DataInputStream(new ByteArrayInputStream(data));
            if (!"ThunderChat".equals(outer.readUTF())) return;
            int length = outer.readUnsignedShort();
            byte[] payload = new byte[length]; outer.readFully(payload);
            DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload));
            if (input.readInt() != 7 || !"SPY".equals(input.readUTF())) return;
            Section section = Section.valueOf(input.readUTF());
            String message = input.readUTF();
            sendLocal(section, message);
        } catch (Exception ignored) { }
    }
}
