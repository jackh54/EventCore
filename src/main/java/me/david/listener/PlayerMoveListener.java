package me.david.listener;

import me.david.util.BorderUtil;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerMoveListener implements Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!BorderUtil.isBoostEnabled() || !BorderUtil.shouldHandlePlayer(event.getPlayer())) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }

        if (BorderUtil.isInsideBorder(from) && BorderUtil.isOutsideBorder(to)) {
            event.setTo(BorderUtil.clampInsideBorder(to));
            return;
        }

        if (BorderUtil.isOutsideBorder(to)) {
            BorderUtil.handleOutsidePlayer(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        BorderUtil.clearPlayer(event.getPlayer().getUniqueId());
    }

}
