package com.codisimus.plugins.regionown;

import java.util.HashMap;
import java.util.LinkedList;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Listens for griefing events
 * 
 * @author Codisimus
 */
public class RegionSelector implements Listener {
    private static HashMap<Player, LinkedList<Block>> selections = new HashMap<Player, LinkedList<Block>>(); //Incomplete Selections
    private static HashMap<Player, Region> regions = new HashMap<Player, Region>(); //Selected Regions
    
    /**
     * Starts a new selection for the given Player
     * 
     * @param player The given Player
     */
    public static void startSelection(Player player) {
        //Clear any previous selections
        if (selections.containsKey(player)) {
            selections.remove(player);
            player.sendMessage("§5Your selection has been cleared");
        }
        if (regions.containsKey(player)) {
            regions.remove(player);
            player.sendMessage("§5Your previous Region selection has been deleted");
        }
        
        //Start a new selection
        selections.put(player, new LinkedList<Block>());
        
        player.sendMessage("§bClick Blocks to create an outline of your Region. For a video tutorial, visit §1<Youtube link here>§b."
                + " When you are finished selecting, use §2/"+RegionOwnCommand.command+" done§b for a list of commands");
    }
    
    /**
     * Listens for Players Selecting Blocks
     * 
     * @param event The PlayerInteractEvent that occurred
     */
    @EventHandler (priority = EventPriority.HIGHEST)
    public void onSelectBlock(PlayerInteractEvent event) {
        //Return if the Player is not selecting or they are in a disabled World
        Player player = event.getPlayer();
        if (!selections.containsKey(player) || !RegionOwn.enabledInWorld(player.getWorld()))
            return;
        
        //Determine which Block was selected
        Block block;
        switch (event.getAction()) {
            case LEFT_CLICK_BLOCK: block = event.getClickedBlock(); break; //The clicked Block
            case RIGHT_CLICK_BLOCK: block = event.getClickedBlock().getRelative(event.getBlockFace()); break; //The clicked BlockFace
            case LEFT_CLICK_AIR: block = player.getLocation().getBlock(); break; //The Player's Location
            case RIGHT_CLICK_AIR: block = player.getLocation().getBlock().getRelative(0, 2, 0); break; //Above the Player's head
            default: return; 
        }
        
        //Cancel the event (no destroying blocks in creative mode)
        event.setCancelled(true);
        
        //Notify if the Block is already in a Region
        Region region = RegionOwn.findRegion(block.getLocation());
        if (region != null)
            player.sendMessage("§4That Block is already in a Region, if purchased your selection will be trimmed");
        
        LinkedList<Block> selection = selections.get(player);
        if (selection.contains(block))
            endSelection(player);
        else
            //Add the new Block to the Player's selection
            selections.get(player).add(block);
    }
    
    /**
     * Set the given Region as the Player's current Selection
     * 
     * @param player The given Player
     * @param region The given Region
     */
    static void setSelection(Player player, Region region) {
        regions.put(player, region);
    }
    
    /**
     * Returns true if the Player has an incomplete selection
     * 
     * @param player The Player who may be selecting
     * @return True if the Player has an incomplete selection
     */
    public static boolean isSelecting(Player player) {
        return selections.containsKey(player);
    }
    
    /**
     * Returns true if the Player has a completed selection
     * 
     * @param player The Player who may have a selection
     * @return True if the Player has a completed selection
     */
    public static boolean hasSelection(Player player) {
        return regions.containsKey(player);
    }
    
    /**
     * Completes the selection for the given Player
     * 
     * @param player the given Player
     */
    public static void endSelection(Player player) {
        //Create the Region from the List of Blocks
        LinkedList<Block> list = selections.get(player);
        Region region;
        switch (list.size()) {
            case 0: //fall through
            case 1: player.sendMessage("§4A selection must contain 2 Blocks for a Cubiod or 3+ for a Polygonal Region"); return;
            case 2: region = new Region(list.getFirst(), list.getLast()); break;
            default: region = new Region(list); break;
        }
        
        //Set the name of the Region as the Players name so snapshots can be easily found
        region.name = player.getName();
        
        //Remove the incompleted selection
        selections.remove(player);
        //Add the Completed Selection
        regions.put(player, region);
        
        //Display Selection command options
        player.sendMessage("§5Region Selected! (§6"+region.size()+"§5 blocks)");
        player.sendMessage("§2/"+RegionOwnCommand.command+" buy§b Purchase the Region for "+Econ.format(Econ.getBuyPrice(player.getName(), region)));
        if (RegionOwn.hasPermission(player, "save"))
            player.sendMessage("§2/"+RegionOwnCommand.command+" save§b Save the Region for backing up or selection later");
        if (RegionOwn.hasPermission(player, "tools"))
            player.sendMessage("§2/"+RegionOwnCommand.command+" help tools§b View a list of Region Tools");
    }
    
    /**
     * Returns the selection of the given Player
     * 
     * @param player The given Player
     * @return The Player's selection or null if the Player does not have a selection
     */
    public static Region getSelection(Player player) {
        Region region = regions.get(player);
        return region;
    }
    
    /**
     * Creates smoke animations for all selected Blocks
     */
    static void animateSelections() {
        //Repeat every tick
    	RegionOwn.server.getScheduler().scheduleSyncRepeatingTask(RegionOwn.plugin, new Runnable() {
            @Override
    	    public void run() {
                for (LinkedList<Block> list: selections.values())
                    for (Block block: list) {
                        //Play smoke effect
                        World world = block.getWorld();
                        Location location = block.getLocation();
                        world.playEffect(location, Effect.SMOKE, 4);
                    }
    	    }
    	}, 0L, 1L);
    }
}