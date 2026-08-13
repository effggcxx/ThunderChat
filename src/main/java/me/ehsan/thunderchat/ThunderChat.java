package me.ehsan.thunderchat;

import me.ehsan.thunderchat.channels.ChannelManager;
import me.ehsan.thunderchat.commands.MsgCommand;
import me.ehsan.thunderchat.commands.ReplyCommand;
import me.ehsan.thunderchat.commands.IgnoreCommand;
import me.ehsan.thunderchat.commands.ChannelCommand;
import me.ehsan.thunderchat.commands.ThunderChatCommand;
import me.ehsan.thunderchat.commands.ChatMuteCommand;
import me.ehsan.thunderchat.filter.FilterManager;
import me.ehsan.thunderchat.messaging.PrivateMessageManager;
import me.ehsan.thunderchat.listeners.ChatListener;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class ThunderChat extends JavaPlugin {

    private static ThunderChat instance;

    private ChannelManager channelManager;
    private FilterManager filterManager;
    private PrivateMessageManager messageManager;
    private FileConfiguration config;

    @Override
    public void onEnable() {
        instance = this;

        loadPluginConfig();

        // Managers
        this.channelManager = new ChannelManager(this);
        this.filterManager = new FilterManager(this);
        this.messageManager = new PrivateMessageManager(this);

        // Listeners
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);

        // Commands
        getCommand("msg").setExecutor(new MsgCommand(this));
        getCommand("reply").setExecutor(new ReplyCommand(this));
        getCommand("ignore").setExecutor(new IgnoreCommand(this));
        getCommand("channel").setExecutor(new ChannelCommand(this));
        getCommand("thunderchat").setExecutor(new ThunderChatCommand(this));
        getCommand("chatmute").setExecutor(new ChatMuteCommand(this));

        printEnableBanner();
    }

    @Override
    public void onDisable() {
        getServer().getConsoleSender().sendMessage(
                ChatColor.GOLD + "[ThunderChat] " + ChatColor.RED + "Disabled."
        );
    }

    /**
     * Saves the bundled default config.yml if none exists yet, then
     * (re)loads it into memory. Called on enable and reused by /thunderchat reload.
     */
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
        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "  ✔ Config loaded " + ChatColor.GRAY
                + "(" + config.getKeys(true).size() + " keys)");
        getServer().getConsoleSender().sendMessage("");
    }

    public static ThunderChat getInstance() {
        return instance;
    }

    public ChannelManager getChannelManager() {
        return channelManager;
    }

    public FilterManager getFilterManager() {
        return filterManager;
    }

    public PrivateMessageManager getMessageManager() {
        return messageManager;
    }

    public FileConfiguration getPluginConfig() {
        return config;
    }
}