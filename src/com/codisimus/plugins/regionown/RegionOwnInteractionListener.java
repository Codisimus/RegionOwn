package com.codisimus.plugins.regionown;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Lock Chests/Doors & Disable Buttons Add-ons
 * 
 * @author Codisimus
 */
public class RegionOwnInteractionListener implements Listener {
    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        //Return if the Event was arm flailing
        Action action = event.getAction();
        switch (action) {
            case LEFT_CLICK_AIR: return;
            case RIGHT_CLICK_AIR: return;
            default: break;
        }
        
        //Return if the Block is not in a Region
        Region region = RegionOwn.findRegion(event.getClickedBlock().getLocation());
        if (region == null)
            return;
        
        //Return if the player is an Owner/Co-owner
        Player player = event.getPlayer();
        if (region.isCoOwner(player))
            return;
        
        switch (event.getMaterial()) {
            case DISPENSER: //Fall through
            case FURNACE: //Fall through
            case BURNING_FURNACE: //Fall through
            case CHEST:
                //Return if the Event was not opening the Block Inventory
                if (action != Action.RIGHT_CLICK_BLOCK)
                    return;
                
                //Cancel the Event if Blocks are locked
                if (region.lockChests)
                    event.setCancelled(true);
                
                break;
                
            case IRON_DOOR: //Fall through
            case IRON_DOOR_BLOCK:
                //Return if the Event was left clicking an Iron Door
                if (action == Action.LEFT_CLICK_BLOCK)
                    return;
                //Fall through
            case WOOD_DOOR: //Fall through
            case WOODEN_DOOR:
                //Return if the Event was not opening the Door
                switch (action) {
                    case LEFT_CLICK_BLOCK: break;
                    case RIGHT_CLICK_BLOCK: break;
                    default: return;
                }
                
                //Cancel the Event if Doors are locked
                if (region.lockDoors)
                    event.setCancelled(true);
                
                break;
                
            case STONE_BUTTON: //Fall through
            case LEVER: //Fall through
            case WOOD_PLATE: //Fall through
            case STONE_PLATE:
                //Cancel the Event if Buttons are disabled
                if (region.disableButtons)
                    event.setCancelled(true);
                
                break;
                
            default: break;
        }
    }
}