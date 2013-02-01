package com.codisimus.plugins.regionown;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listens for Players Logging
 *
 * @author Codisimus
 */
public class RegionOwnChatListener implements Listener {
    @EventHandler (priority = EventPriority.MONITOR)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Region region = RegionOwn.findRegion(player.getLocation());
        Collection<Player> recipients = new LinkedList<Player>();
        if (region == null) {
            for (Player p: player.getWorld().getPlayers()) {
                if (RegionOwn.findRegion(p.getLocation()) == null) {
                    recipients.add(p);
                }
            }
        } else {
            recipients = RegionOwnMovementListener.regionInhabitors.get(region);
        }
        event.getRecipients().retainAll(recipients);
    }
}