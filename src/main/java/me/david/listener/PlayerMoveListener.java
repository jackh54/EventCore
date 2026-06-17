package me.david.listener;

import me.david.util.BorderUtil;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerMoveListener implements Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!BorderUtil.isBoostEnabled()) {
            return;
        }

        Location to = event.getTo();
        if (to == null || !BorderUtil.isOutsideBorder(to)) {
            return;
        }

        Location clamped = BorderUtil.clampInsideBorder(to);
        event.setTo(clamped);
        BorderUtil.applyBoost(event.getPlayer());
    }

}
