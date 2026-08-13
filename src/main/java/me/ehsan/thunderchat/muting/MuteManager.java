package me.ehsan.thunderchat.muting;

import me.ehsan.thunderchat.ThunderChat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import java.io.*;
import java.util.*;

public final class MuteManager implements PluginMessageListener {
    private static final String SUB="ThunderChat";
    private final ThunderChat plugin;
    private final Set<String> global=new HashSet<>();
    private final Map<String,Set<UUID>> players=new HashMap<>();
    public MuteManager(ThunderChat plugin){this.plugin=plugin;for(String c:channels())players.put(c,new HashSet<>());plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin,"BungeeCord");plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin,"BungeeCord",this);}
    private List<String> channels(){return Arrays.asList("global","donator","staff","admin","highrank");}
    public boolean isMuted(Player p,String channel){return !p.hasPermission("thunderchat.bypass.mute")&&!p.hasPermission("thunderchat.bypass.mute."+channel)&&(global.contains(channel)||players.get(channel).contains(p.getUniqueId()));}
    public void setGlobalMuted(String channel,boolean muted){if(muted)global.add(channel);else global.remove(channel);broadcast(channel,null,muted);}
    public void setPlayerMuted(String channel,UUID id,boolean muted){if(muted)players.get(channel).add(id);else players.get(channel).remove(id);broadcast(channel,id,muted);}
    private void broadcast(String channel,UUID id,boolean muted){Player carrier=Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);if(carrier==null)return;try{ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(b);d.writeInt(3);d.writeUTF("MUTE");d.writeUTF(channel);d.writeBoolean(id!=null);if(id!=null)d.writeUTF(id.toString());d.writeBoolean(muted);d.flush();ByteArrayOutputStream o=new ByteArrayOutputStream();DataOutputStream x=new DataOutputStream(o);x.writeUTF("Forward");x.writeUTF("ALL");x.writeUTF(SUB);x.writeShort(b.size());x.write(b.toByteArray());x.flush();carrier.sendPluginMessage(plugin,"BungeeCord",o.toByteArray());}catch(IOException e){plugin.getLogger().warning("Could not synchronize chat mute: "+e.getMessage());}}
    @Override public void onPluginMessageReceived(String channel,Player source,byte[] data){if(!"BungeeCord".equals(channel))return;try{DataInputStream o=new DataInputStream(new ByteArrayInputStream(data));if(!SUB.equals(o.readUTF()))return;int n=o.readUnsignedShort();if(n<=0||n>o.available())return;byte[] b=new byte[n];o.readFully(b);DataInputStream d=new DataInputStream(new ByteArrayInputStream(b));if(d.readInt()!=3||!"MUTE".equals(d.readUTF()))return;String c=d.readUTF();boolean has=d.readBoolean();UUID id=has?UUID.fromString(d.readUTF()):null;boolean muted=d.readBoolean();Set<UUID> set=players.computeIfAbsent(c,k->new HashSet<>());if(id==null){if(muted)global.add(c);else global.remove(c);}else{if(muted)set.add(id);else set.remove(id);}}catch(Exception e){plugin.getLogger().warning("Malformed ThunderChat mute message.");}}
}
