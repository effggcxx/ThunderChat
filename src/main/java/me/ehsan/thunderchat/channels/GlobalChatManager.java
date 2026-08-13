package me.ehsan.thunderchat.channels;

import me.clip.placeholderapi.PlaceholderAPI;
import me.ehsan.thunderchat.ThunderChat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import java.io.*;
import java.util.*;

public final class GlobalChatManager implements PluginMessageListener {
    public enum Channel { GLOBAL("global","GLOBAL CHAT"), DONATOR("donator","DONATOR CHAT"), STAFF("staff","STAFF CHAT"), ADMIN("admin","ADMIN CHAT"), HIGHRANK("highrank","HIGH RANK CHAT");
        final String id, display; Channel(String id,String display){this.id=id;this.display=display;} }
    private final ThunderChat plugin;
    private final Map<UUID,Channel> active=new HashMap<>();
    public GlobalChatManager(ThunderChat plugin){this.plugin=plugin;plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin,"BungeeCord");plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin,"BungeeCord",this);}
    public Channel get(Player p){return active.get(p.getUniqueId());}
    public void set(Player p,Channel c){if(c==null)active.remove(p.getUniqueId());else active.put(p.getUniqueId(),c);}
    public boolean canUse(Player p,Channel c){return p.hasPermission(plugin.getPluginConfig().getString("channels."+c.id+".permission","thunderchat.channel."+c.id));}
    public void send(Player p,String text){Channel c=get(p);if(c==null)return;if(plugin.getMuteManager().isMuted(p,c.id)){p.sendMessage(ChatColor.RED+"That chat is currently muted for you.");return;}String prefix=prefix(p),server=plugin.getPluginConfig().getString("network.server-name","server");String f=plugin.getPluginConfig().getString("format.global","&7[{channel}] [{server}] {prefix}{player}&7: &f{message}");String out=format(f,c,server,prefix,p.getName(),text);for(Player r:Bukkit.getOnlinePlayers())if(canUse(r,c)&&!plugin.getMuteManager().isMuted(r,c.id))r.sendMessage(out);forward(p,c,prefix,text,server);}
    private String format(String f,Channel c,String s,String pre,String player,String msg){return ChatColor.translateAlternateColorCodes('&',f.replace("{channel}",c.display).replace("{server}",s).replace("{prefix}",pre).replace("{player}",player).replace("{message}",msg));}
    private String prefix(Player p){String x=plugin.getPluginConfig().getString("format.prefix-placeholder","");return x.isEmpty()||!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")?"":PlaceholderAPI.setPlaceholders(p,x);}
    private void forward(Player p,Channel c,String pre,String msg,String server){try{ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(b);d.writeInt(3);d.writeUTF("CHAT");d.writeUTF(c.id);d.writeUTF(p.getName());d.writeUTF(server);d.writeUTF(pre);d.writeUTF(msg);d.flush();ByteArrayOutputStream o=new ByteArrayOutputStream();DataOutputStream x=new DataOutputStream(o);x.writeUTF("Forward");x.writeUTF("ALL");x.writeUTF("ThunderChat");x.writeShort(b.size());x.write(b.toByteArray());x.flush();p.sendPluginMessage(plugin,"BungeeCord",o.toByteArray());}catch(IOException e){plugin.getLogger().warning("Could not forward global chat: "+e.getMessage());}}
    @Override public void onPluginMessageReceived(String ch,Player source,byte[] data){if(!"BungeeCord".equals(ch))return;try{DataInputStream o=new DataInputStream(new ByteArrayInputStream(data));if(!"ThunderChat".equals(o.readUTF()))return;int n=o.readUnsignedShort();if(n<=0||n>o.available())return;byte[] b=new byte[n];o.readFully(b);DataInputStream d=new DataInputStream(new ByteArrayInputStream(b));if(d.readInt()!=3||!"CHAT".equals(d.readUTF()))return;Channel c=Channel.valueOf(d.readUTF().toUpperCase(Locale.ROOT));String player=d.readUTF(),server=d.readUTF(),pre=d.readUTF(),msg=d.readUTF();String f=plugin.getPluginConfig().getString("format.global","&7[{channel}] [{server}] {prefix}{player}&7: &f{message}");String out=format(f,c,server,pre,player,msg);for(Player r:Bukkit.getOnlinePlayers())if(canUse(r,c)&&!plugin.getMuteManager().isMuted(r,c.id))r.sendMessage(out);}catch(Exception e){plugin.getLogger().warning("Malformed ThunderChat global message.");}}
}
