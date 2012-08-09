package com.codisimus.plugins.regionown;

import java.util.LinkedList;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.painting.PaintingBreakByEntityEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;

/**
 * Listens for griefing events
 * 
 * @author Codisimus
 */
public class RegionOwnListener implements Listener {
    /* Anti-Drop while reverting */
    static LinkedList<Region> reverting = new LinkedList<Region>();
    
    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onItemSpawn(ItemSpawnEvent event) {
        Location location = event.getLocation();
        for (Region region: reverting)
            if (region.contains(location))
                event.setCancelled(true);
    }
    
    
    /* Building/Griefing Events */
    
    /**
     * Blocks can only be placed within a Region by the Owner, a Co-owner, or an Admin
     * 
     * @param event The BlockPlaceEvent that occurred
     */
    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!RegionOwn.canBuild(event.getPlayer(), event.getBlock()))
            event.setCancelled(true);
    }
    
    /**
     * Blocks within a Region can only be broken by the Owner, a Co-owner, or an Admin
     * 
     * @param event The BlockBreakEvent that occurred
     */
    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!RegionOwn.canBuild(event.getPlayer(), event.getBlock()))
            event.setCancelled(true);
    }

    /**
     * Signs within a Region can only be changed by the Owner, a Co-owner, or an Admin
     * 
     * @param event The SignChangeEvent that occurred
     */
    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onSignChange(SignChangeEvent event) {
        if (!RegionOwn.canBuild(event.getPlayer(), event.getBlock()))
            event.setCancelled(true);
    }

    /**
     * Blocks within a Region can only be ignited by the Owner, a Co-owner, or an Admin
     * 
     * @param event The BlockIgniteEvent that occurred
     */
    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (!RegionOwn.canBuild(event.getPlayer(), event.getBlock()))
            event.setCancelled(true);
    }
    
    /**
     * Fire cannot spread within an OwnedChunk
     * 
     * @param event The BlockSpreadEvent that occurred
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockSpread(BlockSpreadEvent event) {
        if (event.getSource().getType() == Material.FIRE && !RegionOwn.canBuild(null, event.getBlock()))
            event.setCancelled(true);
    }

    /**
     * Blocks within an OwnedChunk cannot burn
     * 
     * @param event The BlockBurnEvent that occurred
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockBurn(BlockBurnEvent event) {
        if (!RegionOwn.canBuild(null, event.getBlock()))
            event.setCancelled(true);
    }

    /**
     * Eggs within a Region can only be hatched by the Owner, a Co-owner, or an Admin
     * 
     * @param event The PlayerEggThrowEvent that occurred
     */
    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEggThrow(PlayerEggThrowEvent event) {
        Player player = event.getPlayer();
        if (!RegionOwn.canBuild(player, player.getTargetBlock(null, 10)))
            event.setHatching(false);
    }
    
    /**
     * Buckets can only be emptied within a Region by the Owner, a Co-owner, or an Admin
     * 
     * @param event The PlayerBucketEmptyEvent that occurred
     */
    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        if (!RegionOwn.canBuild(event.getPlayer(), event.getBlockClicked().getRelative(event.getBlockFace())))
            event.setCancelled(true);
    }

    /**
     * Buckets can only be filled within a Region by the Owner, a Co-owner, or an Admin
     * 
     * @param event The PlayerBucketFillEvent that occurred
     */
    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerBucketFill(PlayerBucketFillEvent event) {
        if (!RegionOwn.canBuild(event.getPlayer(), event.getBlockClicked()))
            event.setCancelled(true);
    }
    
    /**
     * Paintings can only be broken within a Region by the Owner, a Co-owner, or an Admin
     * 
     * @param event The PaintingBreakByEntityEvent that occurred
     */
    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPaintingBreak(PaintingBreakByEntityEvent event) {
        Player player = null;
        Entity entity = event.getRemover();
        if (entity instanceof Player)
            player = (Player)entity;
        
        if (!RegionOwn.canBuild(player, event.getPainting().getLocation().getBlock()))
            event.setCancelled(true);
    }
    
    /**
     * Vehicles within a Region can only be damaged by the Owner, a Co-owner, or an Admin
     * 
     * @param event The VehicleDamageEvent that occurred
     */
    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onVehicleDamage(VehicleDamageEvent event) {
        Player player = null;
        Entity entity = event.getAttacker();
        if (entity instanceof Player)
            player = (Player)entity;
        
        if (!RegionOwn.canBuild(player, event.getVehicle().getLocation().getBlock()))
            event.setCancelled(true);
    }

    /**
     * Vehicles within a Region can only be destroyed by the Owner, a Co-owner, or an Admin
     * 
     * @param event The VehicleDestroyEvent that occurred
     */
    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onVehicleDestroy(VehicleDestroyEvent event) {
        Player player = null;
        Entity entity = event.getAttacker();
        if (entity instanceof Player)
            player = (Player)entity;
        
        if (!RegionOwn.canBuild(player, event.getVehicle().getLocation().getBlock()))
            event.setCancelled(true);
    }
}