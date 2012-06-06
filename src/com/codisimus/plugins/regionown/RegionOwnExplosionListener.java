package com.codisimus.plugins.regionown;

import java.util.Iterator;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

/**
 * Block Explosions within Owned Chunks which have protection
 * 
 * @author Codisimus
 */
public class RegionOwnExplosionListener implements Listener {
    @EventHandler (ignoreCancelled=true, priority = EventPriority.HIGHEST)
    public void onEntityExplode(EntityExplodeEvent event) {
        Iterator<Block> itr = event.blockList().iterator();
        while (itr.hasNext()) {
            
            Region region = RegionOwn.findRegion(itr.next().getLocation());
            if (region != null && region.blockExplosions)
                itr.remove();
        }
    }
}