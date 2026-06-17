package me.david.listener;

import me.david.EventCore;
import me.david.util.BorderUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class EntityDamageListener implements Listener {

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (EventCore.getInstance().getConfig().getBoolean("Settings.DisableFallDamage", true)) {
            if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                event.setCancelled(true);
                return;
            }
        }

        if (!(EventCore.getInstance().getGameManager().isRunning())) {
            event.setCancelled(true);
            return;
        }

        if (!(event.getEntity() instanceof Player)) return;

        if (BorderUtil.isBoostEnabled() && event.getCause() == EntityDamageEvent.DamageCause.WORLD_BORDER) {
            event.setCancelled(true);
        }
    }

}
