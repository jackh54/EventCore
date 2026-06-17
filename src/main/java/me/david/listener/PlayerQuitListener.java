package me.david.listener;

import me.david.util.ConfigCache;
import me.david.util.HostUtil;
import me.david.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;

public class PlayerQuitListener implements Listener {

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        HostUtil.removeHost(player);
        ConfigCache cache = ConfigCache.get();

        if (cache.isPlayerQuitEnabled()) {
            Component message = MessageUtil.formatCached(cache.getPlayerQuitMessage(), Map.of(
                    "%player%", Component.text(player.getName())
            ));
            event.quitMessage(message);
        } else {
            event.quitMessage(Component.empty());
        }
    }

}
