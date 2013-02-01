package com.codisimus.plugins.regionown;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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
    private static HashMap<String, LinkedList<Block>> selections = new HashMap<String, LinkedList<Block>>(); //Incomplete Selections
    private static HashMap<String, Region> regions = new HashMap<String, Region>(); //Selected Regions
    private static EnumSet<BlockFace> cardinal = EnumSet.of(
            BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST);

    /**
     * Starts a new selection for the given Player
     *
     * @param player The given Player
     */
    public static void startSelection(Player player) {
        String playerName = player.getName();
        //Clear any previous selections
        if (selections.containsKey(playerName)) {
            selections.remove(playerName);
            player.sendMessage("§5Your selection has been cleared");
        }
        if (regions.containsKey(playerName)) {
            regions.remove(playerName);
            player.sendMessage("§5Your previous Region selection has been deleted");
        }

        //Start a new selection
        selections.put(playerName, new LinkedList<Block>());

        player.sendMessage("§bClick Blocks to create an outline of your Region. For a video tutorial, visit §1<Youtube link here>§b."
                + " When you are finished selecting, use §2/" + RegionOwnCommand.command + " done§b for a list of commands");
    }

    /**
     * Listens for Players Selecting Blocks
     *
     * @param event The PlayerInteractEvent that occurred
     */
    @EventHandler (priority = EventPriority.LOWEST)
    public void onSelectBlock(PlayerInteractEvent event) {
        //Return if the Player is not selecting or they are in a disabled World
        Player player = event.getPlayer();
        String playerName = player.getName();
        if (!selections.containsKey(playerName) || !RegionOwn.enabledInWorld(player.getWorld())) {
            return;
        }

        //Determine which Block was selected
        Block block;
        switch (event.getAction()) {
        case LEFT_CLICK_BLOCK: block = event.getClickedBlock(); break; //The clicked Block
        case RIGHT_CLICK_BLOCK: block = event.getClickedBlock().getRelative(event.getBlockFace()); break; //The clicked BlockFace
        case LEFT_CLICK_AIR: block = player.getLocation().getBlock(); break; //The Player's Location
        case RIGHT_CLICK_AIR: block = player.getLocation().getBlock().getRelative(0, 2, 0); break; //Above the Player's head
        default: return;
        }

        event.setCancelled(true); //No destroying blocks while selecting

        //Notify if the Block is already in a Region
        Region region = RegionOwn.findRegion(block.getLocation());
        if (region != null) {
            player.sendMessage("§4That Block is already in a Region, if purchased your selection will be trimmed");
        }

        LinkedList<Block> selection = selections.get(playerName);
        if (selection.contains(block)) {
            endSelection(player);
        } else {
            //Add the new Block to the Player's selection
            selections.get(playerName).add(block);
        }
    }

    /**
     * Set the given Region as the Player's current Selection
     *
     * @param player The given Player
     * @param region The given Region
     */
    static void setSelection(Player player, Region region) {
        regions.put(player.getName(), region);
    }

    /**
     * Returns true if the Player has an incomplete selection
     *
     * @param player The Player who may be selecting
     * @return True if the Player has an incomplete selection
     */
    public static boolean isSelecting(Player player) {
        return selections.containsKey(player.getName());
    }

    /**
     * Returns true if the Player has a completed selection
     *
     * @param player The Player who may have a selection
     * @return True if the Player has a completed selection
     */
    public static boolean hasSelection(Player player) {
        return regions.containsKey(player.getName());
    }

    /**
     * Completes the selection for the given Player
     *
     * @param player the given Player
     */
    public static void endSelection(Player player) {
        String playerName = player.getName();

        //Create the Region from the List of Blocks
        LinkedList<Block> list = selections.get(playerName);
        Region region;
        switch (list.size()) {
        case 0: //Fall through
        case 1: player.sendMessage("§4A selection must contain 2 Blocks for a Cubiod or 3+ for a Polygonal Region"); return;
        case 2: region = new Region(list.getFirst(), list.getLast()); break;
        default: region = new Region(list); break;
        }

        //Set the name of the Region as the Players name so snapshots can be easily found
        region.name = playerName;

        //Remove the incompleted selection
        selections.remove(playerName);
        //Add the Completed Selection
        regions.put(playerName, region);

        //Display Selection command options
        player.sendMessage("§5Region Selected! (§6" + region.size() + "§5 blocks)");
        player.sendMessage("§2/" + RegionOwnCommand.command + " buy§b Purchase the Region for " + Econ.format(Econ.getBuyPrice(player.getName(), region)));
        if (RegionOwn.hasPermission(player, "save")) {
            player.sendMessage("§2/" + RegionOwnCommand.command + " save§b Save the Region for backing up or selection later");
        }
        if (RegionOwn.hasPermission(player, "tools")) {
            player.sendMessage("§2/" + RegionOwnCommand.command + " help tools§b View a list of Region Tools");
        }
    }

    /**
     * Returns the selection of the given Player
     *
     * @param player The given Player
     * @return The Player's selection or null if the Player does not have a selection
     */
    public static Region getSelection(Player player) {
        return regions.get(player.getName());
    }

    /**
     * Returns the selection of the given Player
     *
     * @param player The given Player
     * @return The Player's selection or null if the Player does not have a selection
     */
    public static void selectBiome(Player player) {
        String playerName = player.getName();

        //Clear any previous selections
        if (selections.containsKey(playerName)) {
            selections.remove(playerName);
            player.sendMessage("§5Your selection has been cleared");
        }
        if (regions.containsKey(playerName)) {
            regions.remove(playerName);
            player.sendMessage("§5Your previous Region selection has been deleted");
        }

        Block block = player.getLocation().getBlock();
        Biome biome = block.getBiome();
        LinkedList<Block> blockList = new LinkedList<Block>();
        //HashSet<Block> blockList = new HashSet<Block>();

        while (block.getBiome() == biome) {
            block = block.getRelative(BlockFace.NORTH);
        }
        block = block.getRelative(BlockFace.SOUTH);

        wallCrawler(blockList, block, BlockFace.NORTH, biome);
        //expandSelection(blockList, block, BlockFace.SELF, biome);

        Region region = new Region(blockList);
        region.y1 = 0;
        region.y2 = player.getWorld().getMaxHeight();
        region.height = region.y2 - region.y1;

        player.sendMessage("§6" + biome.toString() + " §5Biome Selected! (§6" + region.size() + "§5 blocks)");
        regions.put(playerName, region);
    }

    private static boolean wallCrawler(LinkedList<Block> blockList, Block block, BlockFace from, Biome biome) {
        //System.out.println(from.getOppositeFace() + " to " + block.getBiome() + ": " + block.getLocation().getBlockX() + "." + block.getLocation().getBlockZ());
        if (block.getBiome() != biome) {
            return false;
        } else if (!blockList.isEmpty() && blockList.getFirst().equals(block)) {
            return true;
        } else {
            blockList.add(block);
            BlockFace to = turnLeft(from);
            if (wallCrawler(blockList, block.getRelative(to), to.getOppositeFace(), biome)) {
                return true;
            }
            to = straight(from);
            if (wallCrawler(blockList, block.getRelative(to), to.getOppositeFace(), biome)) {
                return true;
            }
            to = turnRight(from);
            if (wallCrawler(blockList, block.getRelative(to), to.getOppositeFace(), biome)) {
                return true;
            }
            return false;
        }
    }

    private static BlockFace turnLeft(BlockFace from) {
        switch (from) {
        case NORTH: return BlockFace.EAST;
        case EAST: return BlockFace.SOUTH;
        case SOUTH: return BlockFace.WEST;
        case WEST: return BlockFace.NORTH;
        default: throw new RuntimeException("Not a valid Direction");
        }
    }

    private static BlockFace straight(BlockFace from) {
        return from.getOppositeFace();
    }

    private static BlockFace turnRight(BlockFace from) {
        switch (from) {
        case NORTH: return BlockFace.WEST;
        case EAST: return BlockFace.NORTH;
        case SOUTH: return BlockFace.EAST;
        case WEST: return BlockFace.SOUTH;
        default: throw new RuntimeException("Not a valid Direction");
        }
    }

    private static void expandSelection(HashSet<Block> blockList, Block block, BlockFace from, Biome biome) {
        if (block.getBiome() == biome && !blockList.contains(block)) {
            blockList.add(block);
            for (BlockFace to: cardinal) {
                if (to != from) {
                    expandSelection(blockList, block.getRelative(to)
                                    , to.getOppositeFace(), biome);
                }
            }
        }
    }

    /**
     * Creates smoke animations for all selected Blocks
     */
    static void animateSelections() {
        //Repeat every tick
    	RegionOwn.server.getScheduler().scheduleSyncRepeatingTask(RegionOwn.plugin, new Runnable() {
            @Override
    	    public void run() {
                for (LinkedList<Block> list: selections.values()) {
                    for (Block block: list) {
                        //Play smoke effect
                        World world = block.getWorld();
                        Location location = block.getLocation();
                        world.playEffect(location, Effect.SMOKE, 4);
                    }
                }
    	    }
    	}, 0L, 1L);
    }
}