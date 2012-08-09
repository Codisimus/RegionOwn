package com.codisimus.plugins.regionown;

import java.util.List;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;

/**
 * Block Pistons within Owned Regions which have protection
 * 
 * @author Codisimus
 */
public class RegionOwnPistonListener implements Listener {
    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        List<Block> blocks = event.getBlocks();
        for (Block block: blocks) {
            Region region = RegionOwn.findRegion(block.getLocation());
            if (region != null && region.disablePistons) {
                event.setCancelled(true);
                return;
            }
        }
        
        int size = blocks.size();
        if (size != 0) {
            Region region = RegionOwn.findRegion(blocks.get(size - 1).getRelative(event.getDirection()).getLocation());
            if (region != null && region.disablePistons)
                event.setCancelled(true);
        }
    }
    
    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (event.isSticky()) {
            Region region = RegionOwn.findRegion(event.getBlock().getRelative(event.getDirection(), 2).getLocation());
            if (region != null && region.disablePistons)
                event.setCancelled(true);
        }
    }
}