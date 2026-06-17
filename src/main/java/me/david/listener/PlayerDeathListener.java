package me.david.listener;

import me.david.util.ConfigCache;
import me.david.util.MessageUtil;
import me.david.util.PlayerUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Map;

public class PlayerDeathListener implements Listener {

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        final Player player = event.getEntity();

        if (player.getGameMode() == GameMode.SPECTATOR) {
            event.deathMessage(Component.empty());
            return;
        }

        ConfigCache cache = ConfigCache.get();
        if (cache.isPlayerDeathEnabled()) {
            if (player.getKiller() != null) {
                event.deathMessage(MessageUtil.formatCached(cache.getPlayerDeathMessage1(), Map.of(
                        "%player%", Component.text(player.getName()),
                        "%killer%", Component.text(player.getKiller().getName())
                )));
            } else {
                event.deathMessage(MessageUtil.formatCached(cache.getPlayerDeathMessage2(), Map.of(
                        "%player%", Component.text(player.getName())
                )));
            }
        } else {
            event.deathMessage(Component.empty());
        }

        event.setKeepLevel(true);
        event.setDroppedExp(0);

        PlayerUtil.cleanPlayer(player);
        player.setGameMode(GameMode.SPECTATOR);
    }

}
