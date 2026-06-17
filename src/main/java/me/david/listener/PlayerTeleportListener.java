package me.david.listener;

import me.david.EventCore;
import me.david.util.BorderUtil;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PlayerTeleportListener implements Listener {

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!EventCore.getInstance().getConfig().getBoolean("Settings.WorldBorder.DisableEnderPeals")) {
            return;
        }

        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            return;
        }

        Location to = event.getTo();
        if (to == null || to.getWorld() == null) {
            return;
        }

        if (BorderUtil.isOutsideBorder(to)) {
            event.setCancelled(true);
        }
    }

}
