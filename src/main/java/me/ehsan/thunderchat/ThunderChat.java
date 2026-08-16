package me.ehsan.thunderchat;

import me.ehsan.thunderchat.alerts.AlertManager;
import me.ehsan.thunderchat.channels.GlobalChatManager;
import me.ehsan.thunderchat.channels.GlobalChatManager.Channel;
import me.ehsan.thunderchat.chatcolor.ChatColorCommand;
import me.ehsan.thunderchat.chatcolor.ChatColorListener;
import me.ehsan.thunderchat.chatcolor.ChatColorManager;
import me.ehsan.thunderchat.commands.ChannelCommand;
import me.ehsan.thunderchat.commands.ChannelMuteListCommand;
import me.ehsan.thunderchat.commands.ChatChannelCommand;
import me.ehsan.thunderchat.commands.ChatHideCommand;
import me.ehsan.thunderchat.commands.ClearChatCommand;
import me.ehsan.thunderchat.commands.IgnoreCommand;
import me.ehsan.thunderchat.commands.MsgCommand;
import me.ehsan.thunderchat.commands.ReplyCommand;
import me.ehsan.thunderchat.commands.SpyCommand;
import me.ehsan.thunderchat.commands.ThunderChatCommand;
import me.ehsan.thunderchat.commands.ThunderChatTabCompleter;
import me.ehsan.thunderchat.filter.CapsManager;
import me.ehsan.thunderchat.filter.FilterManager;
import me.ehsan.thunderchat.listeners.ChatListener;
import me.ehsan.thunderchat.listeners.PlayerStateListener;
import me.ehsan.thunderchat.messaging.IgnoreManager;
import me.ehsan.thunderchat.messaging.PrivateMessageManager;
import me.ehsan.thunderchat.muting.MuteManager;
import me.ehsan.thunderchat.network.NetworkMessenger;
import me.ehsan.thunderchat.spy.SpyListener;
import me.ehsan.thunderchat.spy.SpyManager;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;

public final class ThunderChat extends JavaPlugin {
    private static ThunderChat instance;
    private NetworkMessenger networkMessenger; private GlobalChatManager globalChatManager; private FilterManager filterManager; private AlertManager alertManager; private CapsManager capsManager; private PrivateMessageManager messageManager; private IgnoreManager ignoreManager; private MuteManager muteManager; private SpyManager spyManager; private ChatColorManager chatColorManager; private FileConfiguration config;
    @Override public void onEnable() {
        instance = this; loadPluginConfig(); this.networkMessenger = new NetworkMessenger(this);
        this.muteManager = new MuteManager(this); this.globalChatManager = new GlobalChatManager(this); this.alertManager = new AlertManager(this); this.filterManager = new FilterManager(this); this.capsManager = new CapsManager(this); this.messageManager = new PrivateMessageManager(this); this.ignoreManager = new IgnoreManager(this); this.spyManager = new SpyManager(this); this.chatColorManager = new ChatColorManager(this); registerChatColorPermissions();
        getServer().getPluginManager().registerEvents(new ChatListener(this), this); getServer().getPluginManager().registerEvents(new SpyListener(this), this); getServer().getPluginManager().registerEvents(new PlayerStateListener(this), this);
        getCommand("msg").setExecutor(new MsgCommand(this)); getCommand("reply").setExecutor(new ReplyCommand(this)); IgnoreCommand ignoreCmd = new IgnoreCommand(this); getCommand("ignore").setExecutor(ignoreCmd); getCommand("unignore").setExecutor(ignoreCmd);
        getCommand("channel").setExecutor(new ChannelCommand(this)); getCommand("channelmutelist").setExecutor(new ChannelMuteListCommand(this)); getCommand("clearchat").setExecutor(new ClearChatCommand(this)); getCommand("thunderchat").setExecutor(new ThunderChatCommand(this)); getCommand("chat").setExecutor(new ChatChannelCommand(this, null)); getCommand("chathide").setExecutor(new ChatHideCommand(this));
        getCommand("staffchat").setExecutor(new ChatChannelCommand(this, Channel.STAFF)); getCommand("donatorchat").setExecutor(new ChatChannelCommand(this, Channel.DONATOR)); getCommand("adminchat").setExecutor(new ChatChannelCommand(this, Channel.ADMIN)); getCommand("highrankchat").setExecutor(new ChatChannelCommand(this, Channel.HIGHRANK)); getCommand("gc").setExecutor(new ChatChannelCommand(this, Channel.GLOBAL)); getCommand("spy").setExecutor(new SpyCommand(this));
        ChatColorCommand chatColorCommand = new ChatColorCommand(chatColorManager); getCommand("chatcolor").setExecutor(chatColorCommand); getServer().getPluginManager().registerEvents(new ChatColorListener(this, chatColorManager, chatColorCommand), this);
        ThunderChatTabCompleter completer = new ThunderChatTabCompleter(); String[] completable = {"msg", "ignore", "unignore", "channel", "channelmutelist", "clearchat", "chathide", "chat", "spy", "thunderchat", "chatcolor"}; for (String commandName : completable) { PluginCommand command = getCommand(commandName); if (command != null) command.setTabCompleter(completer); }
        printEnableBanner();
    }
    private void registerChatColorPermissions() {
        registerPermission("thunderchat.chatcolor.color.*", "Allows every Chat Color color", PermissionDefault.FALSE); registerPermission("thunderchat.chatcolor.gradient.*", "Allows every Chat Color gradient", PermissionDefault.FALSE); registerPermission("thunderchat.chatcolor.style.*", "Allows every Chat Color style", PermissionDefault.FALSE); registerPermission("thunderchat.chatcolor.custom.*", "Allows every Chat Color custom formatting option", PermissionDefault.FALSE); registerPermission("thunderchat.chatcolor.custom", "Allows custom MiniMessage chat formatting", PermissionDefault.FALSE);
        for (String color : ChatColorManager.COLORS) registerPermission("thunderchat.chatcolor.color." + color, "Allows the " + color + " chat color", PermissionDefault.FALSE);
        for (String gradient : ChatColorManager.GRADIENTS) registerPermission("thunderchat.chatcolor.gradient." + gradient, "Allows the " + gradient + " chat gradient", PermissionDefault.FALSE);
        for (String style : ChatColorManager.STYLES) registerPermission("thunderchat.chatcolor.style." + style, "Allows the " + style + " chat style", PermissionDefault.FALSE);
    }
    private void registerPermission(String name, String description, PermissionDefault defaultValue) { if (getServer().getPluginManager().getPermission(name) == null) getServer().getPluginManager().addPermission(new Permission(name, description, defaultValue)); }
    @Override public void onDisable() { if (muteManager != null) muteManager.save(); if (ignoreManager != null) ignoreManager.save(); if (spyManager != null) spyManager.save(); getServer().getConsoleSender().sendMessage(ChatColor.GOLD + "[ThunderChat] " + ChatColor.RED + "Disabled."); }
    public void loadPluginConfig() { saveDefaultConfig(); reloadConfig(); this.config = getConfig(); }
    private void printEnableBanner() { String prefix = ChatColor.GOLD + "" + ChatColor.BOLD + "ThunderChat" + ChatColor.RESET; String version = getPluginMeta().getVersion(); getServer().getConsoleSender().sendMessage(""); getServer().getConsoleSender().sendMessage(ChatColor.GOLD + "  ⚡ " + prefix + ChatColor.GRAY + " v" + version); getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "  ✔ Plugin enabled"); getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "  ✔ Config loaded " + ChatColor.GRAY + "(" + config.getKeys(true).size() + " keys)"); getServer().getConsoleSender().sendMessage(""); }
    public static ThunderChat getInstance() { return instance; }
    public NetworkMessenger getNetworkMessenger() { return networkMessenger; } public GlobalChatManager getGlobalChatManager() { return globalChatManager; } public FilterManager getFilterManager() { return filterManager; } public AlertManager getAlertManager() { return alertManager; } public CapsManager getCapsManager() { return capsManager; } public PrivateMessageManager getMessageManager() { return messageManager; } public IgnoreManager getIgnoreManager() { return ignoreManager; } public MuteManager getMuteManager() { return muteManager; } public SpyManager getSpyManager() { return spyManager; } public ChatColorManager getChatColorManager() { return chatColorManager; } public FileConfiguration getPluginConfig() { return config; }
}
