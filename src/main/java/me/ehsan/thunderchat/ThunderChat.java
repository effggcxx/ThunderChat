package me.ehsan.thunderchat;

import me.ehsan.thunderchat.channels.GlobalChatManager;
import me.ehsan.thunderchat.channels.GlobalChatManager.Channel;
import me.ehsan.thunderchat.commands.ChannelCommand;
import me.ehsan.thunderchat.commands.ChatChannelCommand;
import me.ehsan.thunderchat.commands.ChatHideCommand;
import me.ehsan.thunderchat.commands.ClearChatCommand;
import me.ehsan.thunderchat.commands.IgnoreCommand;
import me.ehsan.thunderchat.commands.MsgCommand;
import me.ehsan.thunderchat.commands.ReplyCommand;
import me.ehsan.thunderchat.commands.ThunderChatCommand;
import me.ehsan.thunderchat.filter.CapsManager;
import me.ehsan.thunderchat.filter.FilterManager;
import me.ehsan.thunderchat.listeners.ChatListener;
import me.ehsan.thunderchat.messaging.IgnoreManager;
import me.ehsan.thunderchat.messaging.PrivateMessageManager;
import me.ehsan.thunderchat.muting.MuteManager;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class ThunderChat extends JavaPlugin {
    private static ThunderChat instance;
    private GlobalChatManager globalChatManager;
    private FilterManager filterManager;
    private CapsManager capsManager;
    private PrivateMessageManager messageManager;
    private IgnoreManager ignoreManager;
    private MuteManager muteManager;
    private FileConfiguration config;

    @Override
    public void onEnable() {
        instance = this;
        loadPluginConfig();
        this.muteManager = new MuteManager(this);
        this.globalChatManager = new GlobalChatManager(this);
        this.filterManager = new FilterManager(this);
        this.capsManager = new CapsManager(this);
        this.messageManager = new PrivateMessageManager(this);
        this.ignoreManager = new IgnoreManager(this);

        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getCommand("msg").setExecutor(new MsgCommand(this));
        getCommand("reply").setExecutor(new ReplyCommand(this));
        IgnoreCommand ignoreCmd = new IgnoreCommand(this);
        getCommand("ignore").setExecutor(ignoreCmd);
        getCommand("unignore").setExecutor(ignoreCmd);
        getCommand("channel").setExecutor(new ChannelCommand(this));
        getCommand("clearchat").setExecutor(new ClearChatCommand(this));
        getCommand("thunderchat").setExecutor(new ThunderChatCommand(this));
        getCommand("chat").setExecutor(new ChatChannelCommand(this, null));
        getCommand("chathide").setExecutor(new ChatHideCommand(this));
        getCommand("staffchat").setExecutor(new ChatChannelCommand(this, Channel.STAFF));
        getCommand("donatorchat").setExecutor(new ChatChannelCommand(this, Channel.DONATOR));
        getCommand("adminchat").setExecutor(new ChatChannelCommand(this, Channel.ADMIN));
        getCommand("highrankchat").setExecutor(new ChatChannelCommand(this, Channel.HIGHRANK));
        getCommand("gc").setExecutor(new ChatChannelCommand(this, Channel.GLOBAL));
        printEnableBanner();
    }

    @Override
    public void onDisable() {
        if (ignoreManager != null) ignoreManager.save();
        getServer().getConsoleSender().sendMessage(ChatColor.GOLD + "[ThunderChat] " + ChatColor.RED + "Disabled.");
    }

    public void loadPluginConfig() {
        saveDefaultConfig();
        reloadConfig();
        this.config = getConfig();
    }

    private void printEnableBanner() {
        String prefix = ChatColor.GOLD + "" + ChatColor.BOLD + "ThunderChat" + ChatColor.RESET;
        String version = getPluginMeta().getVersion();
        getServer().getConsoleSender().sendMessage("");
        getServer().getConsoleSender().sendMessage(ChatColor.GOLD + "  ⚡ " + prefix + ChatColor.GRAY + " v" + version);
        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "  ✔ Plugin enabled");
        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "  ✔ Config loaded " + ChatColor.GRAY + "(" + config.getKeys(true).size() + " keys)");
        getServer().getConsoleSender().sendMessage("");
    }

    public static ThunderChat getInstance() { return instance; }
    public GlobalChatManager getGlobalChatManager() { return globalChatManager; }
    public FilterManager getFilterManager() { return filterManager; }
    public CapsManager getCapsManager() { return capsManager; }
    public PrivateMessageManager getMessageManager() { return messageManager; }
    public IgnoreManager getIgnoreManager() { return ignoreManager; }
    public MuteManager getMuteManager() { return muteManager; }
    public FileConfiguration getPluginConfig() { return config; }
}
