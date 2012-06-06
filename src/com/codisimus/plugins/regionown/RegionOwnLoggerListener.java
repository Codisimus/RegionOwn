package com.codisimus.plugins.regionown;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listens for Players Logging
 * 
 * @author Codisimus
 */
public class RegionOwnLoggerListener implements Listener {
    /**
     * Updates the last time that Players that own Regions were seen
     * 
     * @param event The PlayerJoinEvent that occurred
     */
    @EventHandler (priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        String player = event.getPlayer().getName();
        
        //Log the Player as being online if they have a RegionOwner Object that represents them
        if (RegionOwn.findOwner(player) != null)
            logAsSeen(player);
    }
    
    /**
     * Updates the last time that Players that own Regions were seen
     * 
     * @param event The PlayerQuitEvent that occurred
     */
    @EventHandler (priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        String player = event.getPlayer().getName();
        
        //Log the Player as being online if they have a RegionOwner Object that represents them
        if (RegionOwn.findOwner(player) != null)
            logAsSeen(player);
    }
    
    /**
     * Updates the last time that Players that own Regions were seen
     * 
     * @param player The Name of the Player who was seen
     */
    private void logAsSeen(String player) {
        RegionOwn.lastDaySeen.setProperty(player, String.valueOf(RegionOwn.getDayAD()));
        RegionOwn.saveLastSeen();
    }
}