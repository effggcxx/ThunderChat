package me.ehsan.thunderchat;

import me.ehsan.thunderchat.channels.ChannelManager;
import me.ehsan.thunderchat.commands.MsgCommand;
import me.ehsan.thunderchat.commands.ReplyCommand;
import me.ehsan.thunderchat.commands.IgnoreCommand;
import me.ehsan.thunderchat.commands.ChannelCommand;
import me.ehsan.thunderchat.commands.ThunderChatCommand;
import me.ehsan.thunderchat.commands.ChatMuteCommand;
import me.ehsan.thunderchat.filter.FilterManager;
import me.ehsan.thunderchat.listeners.ChatListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class ThunderChat extends JavaPlugin {

    private static ThunderChat instance;

    private ChannelManager channelManager;
    private FilterManager filterManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        // Managers
        this.channelManager = new ChannelManager(this);
        this.filterManager = new FilterManager(this);

        // Listeners
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);

        // Commands
        getCommand("msg").setExecutor(new MsgCommand(this));
        getCommand("reply").setExecutor(new ReplyCommand(this));
        getCommand("ignore").setExecutor(new IgnoreCommand(this));
        getCommand("channel").setExecutor(new ChannelCommand(this));
        getCommand("thunderchat").setExecutor(new ThunderChatCommand(this));
        getCommand("chatmute").setExecutor(new ChatMuteCommand(this));

        getLogger().info("ThunderChat enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("ThunderChat disabled.");
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
}
