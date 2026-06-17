package me.david.listener;

import me.david.util.BorderUtil;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerMoveListener implements Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!BorderUtil.isBoostEnabled()) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || !BorderUtil.isOutsideBorder(to)) {
            return;
        }

        event.setTo(from);
        BorderUtil.handleOutsideBorder(event.getPlayer());
    }

}
