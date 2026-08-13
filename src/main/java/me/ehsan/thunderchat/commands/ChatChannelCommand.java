package me.ehsan.thunderchat.commands;

import me.ehsan.thunderchat.ThunderChat;
import me.ehsan.thunderchat.channels.GlobalChatManager.Channel;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.UUID;

public final class ChatChannelCommand implements CommandExecutor {
    private final ThunderChat plugin;
    private final Channel channel;
    public ChatChannelCommand(ThunderChat plugin, Channel channel){this.plugin=plugin;this.channel=channel;}
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
        if(args.length>0&&(args[0].equalsIgnoreCase("mute")||args[0].equalsIgnoreCase("unmute"))){
            if(!sender.hasPermission("thunderchat.admin")){sender.sendMessage(ChatColor.RED+"You don't have permission to manage chat mutes.");return true;}
            boolean muted=args[0].equalsIgnoreCase("mute");
            if(args.length==1){plugin.getMuteManager().setGlobalMuted(channel.id,muted);sender.sendMessage(ChatColor.GREEN+channel.display+" "+(muted?"muted":"unmuted")+" globally.");return true;}
            if(args.length!=2){sender.sendMessage(ChatColor.RED+"Usage: /"+label+" "+args[0]+" [player]");return true;}
            OfflinePlayer target=Bukkit.getOfflinePlayerIfCached(args[1]);
            if(target==null){Player online=Bukkit.getPlayerExact(args[1]);if(online!=null)target=online;}
            if(target==null){sender.sendMessage(ChatColor.RED+"That player is not cached on this server.");return true;}
            UUID id=target.getUniqueId();plugin.getMuteManager().setPlayerMuted(channel.id,id,muted);
            sender.sendMessage(ChatColor.GREEN+target.getName()+" has been "+(muted?"muted in ":"unmuted in ")+channel.display+".");return true;
        }
        if(!(sender instanceof Player player)){sender.sendMessage(ChatColor.RED+"Only players can toggle chat channels.");return true;}
        if(!plugin.getGlobalChatManager().canUse(player,channel)){player.sendMessage(ChatColor.RED+"You don't have permission to use that channel.");return true;}
        if(plugin.getGlobalChatManager().get(player)==channel){plugin.getGlobalChatManager().set(player,null);player.sendMessage(ChatColor.GREEN+"Chat channel disabled. You are back in local chat.");}
        else{plugin.getGlobalChatManager().set(player,channel);player.sendMessage(ChatColor.GREEN+"Chat channel set to "+ChatColor.YELLOW+channel.display+ChatColor.GREEN+".");}
        return true;
    }
}
