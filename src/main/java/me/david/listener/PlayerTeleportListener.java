package me.david.listener;

import me.david.EventCore;
import me.david.util.BorderUtil;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PlayerTeleportListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!BorderUtil.shouldHandlePlayer(event.getPlayer())) {
            return;
        }

        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            return;
        }

        if (!EventCore.getInstance().getConfig().getBoolean("Settings.WorldBorder.DisableEnderPeals")) {
            return;
        }

        Location to = event.getTo();
        Location resolved = BorderUtil.resolvePearlDestination(event.getFrom(), to);
        if (resolved == null) {
            if (to != null && BorderUtil.isOutsideBorder(to)) {
                event.setCancelled(true);
            }
            return;
        }

        event.setTo(resolved);
    }

}
