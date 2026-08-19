package me.ehsan.thunderchat;

import me.ehsan.thunderchat.alerts.AlertManager;
import me.ehsan.thunderchat.channels.GlobalChatManager;
import me.ehsan.thunderchat.channels.GlobalChatManager.Channel;
import me.ehsan.thunderchat.chatcolor.ChatColorCommand;
import me.ehsan.thunderchat.chatcolor.ChatColorListener;
import me.ehsan.thunderchat.chatcolor.ChatColorManager;
import me.ehsan.thunderchat.commands.*;
import me.ehsan.thunderchat.filter.CapsManager;
import me.ehsan.thunderchat.filter.FilterManager;
import me.ehsan.thunderchat.interactive.InteractiveChatManager;
import me.ehsan.thunderchat.listeners.ChatListener;
import me.ehsan.thunderchat.listeners.PlayerStateListener;
import me.ehsan.thunderchat.messages.MessagesManager;
import me.ehsan.thunderchat.messaging.IgnoreManager;
import me.ehsan.thunderchat.messaging.PrivateMessageManager;
import me.ehsan.thunderchat.muting.MuteManager;
import me.ehsan.thunderchat.network.NetworkMessenger;
import me.ehsan.thunderchat.spy.SpyInputListener;
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
    private NetworkMessenger networkMessenger; private GlobalChatManager globalChatManager; private FilterManager filterManager; private AlertManager alertManager; private CapsManager capsManager; private PrivateMessageManager messageManager; private IgnoreManager ignoreManager; private MuteManager muteManager; private SpyManager spyManager; private ChatColorManager chatColorManager; private MessagesManager messagesManager; private InteractiveChatManager interactiveChatManager; private FileConfiguration config;
    @Override public void onEnable() {
        instance = this; loadPluginConfig(); this.messagesManager = new MessagesManager(this); this.networkMessenger = new NetworkMessenger(this);
        this.muteManager = new MuteManager(this); this.globalChatManager = new GlobalChatManager(this); this.alertManager = new AlertManager(this); this.filterManager = new FilterManager(this); this.capsManager = new CapsManager(this); this.messageManager = new PrivateMessageManager(this); this.ignoreManager = new IgnoreManager(this); this.spyManager = new SpyManager(this); this.chatColorManager = new ChatColorManager(this); this.interactiveChatManager = new InteractiveChatManager(this); registerChatColorPermissions(); registerInteractivePermissions();
        getServer().getPluginManager().registerEvents(new ChatListener(this), this); getServer().getPluginManager().registerEvents(new SpyListener(this), this); getServer().getPluginManager().registerEvents(new SpyInputListener(this), this); getServer().getPluginManager().registerEvents(new PlayerStateListener(this), this);
        getCommand("msg").setExecutor(new MsgCommand(this)); getCommand("reply").setExecutor(new ReplyCommand(this)); IgnoreCommand ignoreCmd = new IgnoreCommand(this); getCommand("ignore").setExecutor(ignoreCmd); getCommand("unignore").setExecutor(ignoreCmd); getCommand("channel").setExecutor(new ChannelCommand(this)); getCommand("channelmutelist").setExecutor(new ChannelMuteListCommand(this)); getCommand("clearchat").setExecutor(new ClearChatCommand(this)); getCommand("thunderchat").setExecutor(new ThunderChatCommand(this)); getCommand("chat").setExecutor(new ChatChannelCommand(this, null)); getCommand("chathide").setExecutor(new ChatHideCommand(this));
        getCommand("staffchat").setExecutor(new ChatChannelCommand(this, Channel.STAFF)); getCommand("donatorchat").setExecutor(new ChatChannelCommand(this, Channel.DONATOR)); getCommand("adminchat").setExecutor(new ChatChannelCommand(this, Channel.ADMIN)); getCommand("highrankchat").setExecutor(new ChatChannelCommand(this, Channel.HIGHRANK)); getCommand("gc").setExecutor(new ChatChannelCommand(this, Channel.GLOBAL)); getCommand("spy").setExecutor(new SpyCommand(this)); getCommand("stafflist").setExecutor(new ChannelListCommand(this, Channel.STAFF)); getCommand("highranklist").setExecutor(new ChannelListCommand(this, Channel.HIGHRANK)); getCommand("adminlist").setExecutor(new ChannelListCommand(this, Channel.ADMIN)); getCommand("donatorlist").setExecutor(new ChannelListCommand(this, Channel.DONATOR));
        ChatColorCommand chatColorCommand = new ChatColorCommand(chatColorManager); getCommand("chatcolor").setExecutor(chatColorCommand); getServer().getPluginManager().registerEvents(new ChatColorListener(this, chatColorManager, chatColorCommand), this);
        ThunderChatTabCompleter completer = new ThunderChatTabCompleter(); String[] completable = {"msg", "ignore", "unignore", "channel", "channelmutelist", "clearchat", "chathide", "chat", "spy", "thunderchat", "chatcolor", "stafflist", "highranklist", "adminlist", "donatorlist"}; for (String commandName : completable) { PluginCommand command = getCommand(commandName); if (command != null) command.setTabCompleter(completer); }
        printEnableBanner();
    }
    private void registerChatColorPermissions() { registerPermission("thunderchat.chatcolor.color.*", "Allows every Chat Color color", PermissionDefault.FALSE); registerPermission("thunderchat.chatcolor.gradient.*", "Allows every Chat Color gradient", PermissionDefault.FALSE); registerPermission("thunderchat.chatcolor.style.*", "Allows every Chat Color style", PermissionDefault.FALSE); registerPermission("thunderchat.chatcolor.custom.*", "Allows every Chat Color custom formatting option", PermissionDefault.FALSE); registerPermission("thunderchat.chatcolor.custom", "Allows custom MiniMessage chat formatting", PermissionDefault.FALSE); for (String color : ChatColorManager.COLORS) registerPermission("thunderchat.chatcolor.color." + color, "Allows the " + color + " chat color", PermissionDefault.FALSE); for (String gradient : ChatColorManager.GRADIENTS) registerPermission("thunderchat.chatcolor.gradient." + gradient, "Allows the " + gradient + " chat gradient", PermissionDefault.FALSE); for (String style : ChatColorManager.STYLES) registerPermission("thunderchat.chatcolor.style." + style, "Allows the " + style + " chat style", PermissionDefault.FALSE); }
    private void registerInteractivePermissions() { registerPermission("thunderchat.interactive.item", "Allows using the [item] and [i] chat placeholders", PermissionDefault.TRUE); registerPermission("thunderchat.interactive.inventory", "Allows using the [inv] chat placeholder", PermissionDefault.TRUE); }
    private void registerPermission(String name, String description, PermissionDefault defaultValue) { if (getServer().getPluginManager().getPermission(name) == null) getServer().getPluginManager().addPermission(new Permission(name, description, defaultValue)); }
    @Override public void onDisable() { if (muteManager != null) muteManager.save(); if (ignoreManager != null) ignoreManager.save(); if (spyManager != null) spyManager.save(); if (chatColorManager != null) chatColorManager.save(); getServer().getConsoleSender().sendMessage(ChatColor.GOLD + "[ThunderChat] " + ChatColor.RED + "Disabled."); }
    public void loadPluginConfig() { saveDefaultConfig(); reloadConfig(); this.config = getConfig(); if (messagesManager != null) messagesManager.reload(); }
    private void printEnableBanner() { getServer().getConsoleSender().sendMessage("[ThunderChat] Plugin enabled."); }
    public static ThunderChat getInstance() { return instance; }
    public NetworkMessenger getNetworkMessenger() { return networkMessenger; } public GlobalChatManager getGlobalChatManager() { return globalChatManager; } public FilterManager getFilterManager() { return filterManager; } public AlertManager getAlertManager() { return alertManager; } public CapsManager getCapsManager() { return capsManager; } public PrivateMessageManager getMessageManager() { return messageManager; } public IgnoreManager getIgnoreManager() { return ignoreManager; } public MuteManager getMuteManager() { return muteManager; } public SpyManager getSpyManager() { return spyManager; } public ChatColorManager getChatColorManager() { return chatColorManager; } public MessagesManager getMessagesManager() { return messagesManager; } public InteractiveChatManager getInteractiveChatManager() { return interactiveChatManager; } public FileConfiguration getPluginConfig() { return config; }
}
