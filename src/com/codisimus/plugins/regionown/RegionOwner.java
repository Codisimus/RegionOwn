package com.codisimus.plugins.regionown;

import com.codisimus.plugins.textplayer.TextPlayer;
import com.codisimus.plugins.textplayer.User;
import java.util.LinkedList;
import org.bukkit.entity.Player;

/**
 * An RegionOwner is a Player that owns Regions
 *
 * @author Codisimus
 */
public class RegionOwner {
    public String name;
    public int blockCounter = 0;
    
    public LinkedList<String> coOwners = new LinkedList<String>();
    public LinkedList<String> groups = new LinkedList<String>();
    
    /**
     * Constructs a new RegionOwner to represent the given Player
     *
     * @param player The given Player
     */
    public RegionOwner(String player) {
        name = player;
    }
    
    /**
     * Sends the given message to the RegionOwner
     * If they are offline it will attempt to Text them through TextPlayer
     * 
     * @param msg The message to be sent
     */
    public void sendMessage(String msg) {
        Player player = RegionOwn.server.getPlayer(name);
        if (player != null) {
            player.sendMessage(msg);
        } else if (RegionOwn.pm.isPluginEnabled("TextPlayer")) {
            User user = TextPlayer.findUser(name);
            user.sendText(msg);
        }
    }
    
    /**
     * Writes this RegionOwner to file
     * 
     */
    public void save() {
        RegionOwn.saveRegionOwner(this);
    }
}