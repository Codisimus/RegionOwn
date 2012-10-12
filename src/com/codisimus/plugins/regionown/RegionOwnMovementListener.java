package com.codisimus.plugins.regionown;

import java.util.HashMap;
import java.util.LinkedList;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Regenerate Health/Hunger & Alarm System & Notify When in Regions
 * 
 * @author Codisimus
 */
public class RegionOwnMovementListener implements Listener {
    static int rate;
    static int amount;
    private static LinkedList<Player> healing = new LinkedList<Player>();
    private static LinkedList<Player> feeding = new LinkedList<Player>();
    private static HashMap<Player, Region> inRegion = new HashMap<Player, Region>();
    
    @EventHandler (ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        //Return if the Player did not move between Blocks
        Block to = event.getTo().getBlock();
        Location location = event.getFrom();
        Block from = location.getBlock();
        if (to.equals(from)) {
            return;
        }
        
        Player player = event.getPlayer();
        
        Region regionEntered = RegionOwn.findRegion(to.getLocation());
        Region regionLeft = inRegion.get(player);
        if (regionEntered == regionLeft) {          //Region1 -> Region1
            return;
        }
        
        if (regionEntered == null) {                //? -> Wilderness
            if (regionLeft != null) {               //Region1 -> Wilderness
                playerLeftRegion(player, regionLeft);
            }                                       //Wilderness -> Wilderness
        } else {                                    //? -> Region2
            if (regionEntered.denyAccess) {
                event.setTo(to.getY() < from.getY()
                            ? location.add(0, 1, 0)
                            : location);
                player.sendMessage(RegionOwnMessages.accessDenied
                                    .replace("<name>", regionEntered.name)
                                    .replace("<owner>", regionEntered.owner.name));
            } else {
                if (regionLeft != null) {           //Wilderness -> Region2
                    playerLeftRegion(player, regionLeft);
                }                                   //Region1 -> Region2
                playerEnteredRegion(player, regionEntered);
            }
        }
    }
    
    protected static void playerLeftRegion(Player player, Region region) {
        inRegion.remove(player);
        if (!region.leavingMessage.isEmpty()) {
            player.sendMessage(region.leavingMessage
                                .replace("<name>", region.name)
                                .replace("<owner>", region.owner.name));
        }
        
        String name = player.getName();
        if (region.alarm && !region.isOwner(name)) {
            region.owner.sendMessage(name+" left your owned property");
        }
        
        healing.remove(player);
        feeding.remove(player);
    }
    
    protected static void playerEnteredRegion(Player player, Region region) {
        inRegion.put(player, region);
        if (!region.welcomeMessage.isEmpty()) {
            player.sendMessage(region.welcomeMessage
                                .replace("<name>", region.name)
                                .replace("<owner>", region.owner.name));
        }
        
        String name = player.getName();
        if (region.alarm && !region.isOwner(name)) {
            region.owner.sendMessage(name+" entered your owned property: "+region.name);
        }
        
        if (region.heal) {
            healing.add(player);
        }
        if (region.feed) {
            feeding.add(player);
        }
    }
    
    /**
     * Heals Players who are in healing Chunks
     */
    public static void scheduleHealer() {
        if (Econ.heal == -2) {
            return;
        }
        
        RegionOwn.server.getScheduler().scheduleSyncRepeatingTask(RegionOwn.plugin, new Runnable() {
            @Override
    	    public void run() {
                for (Player player: healing) {
                    player.setHealth(Math.min(player.getHealth() + amount, player.getMaxHealth()));
                }
    	    }
    	}, 0L, 20L * rate);
    }
    
    /**
     * Feeds Players who are in feeding Chunks
     */
    public static void scheduleFeeder() {
        if (Econ.feed == -2) {
            return;
        }
        
        RegionOwn.server.getScheduler().scheduleSyncRepeatingTask(RegionOwn.plugin, new Runnable() {
            @Override
    	    public void run() {
                for (Player player: feeding) {
                    player.setFoodLevel(Math.min(player.getFoodLevel() + amount, 20));
                }
    	    }
    	}, 0L, 20L * rate);
    }
}