package me.david.listener;

import me.david.EventCore;
import me.david.util.*;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Map;

public class PlayerJoinListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        ConfigCache cache = ConfigCache.get();

        HostUtil.giveHost(player);

        if (cache.isPlayerJoinEnabled()) {
            Component message = MessageUtil.getPrefix().append(
                    MessageUtil.formatCached(cache.getPlayerJoinMessage(), Map.of("%player%", Component.text(player.getName())))
            );
            event.joinMessage(message);
        } else {
            event.joinMessage(Component.empty());
        }

        player.teleportAsync(EventCore.getInstance().getMapManager().getSpawnLocation());
        PlayerUtil.cleanPlayer(player);
        if (EventCore.getInstance().getGameManager().isRunning()) {
            player.getInventory().setArmorContents(null);
            player.getInventory().clear();
            player.setGameMode(GameMode.SPECTATOR);
        }

        Scheduler.wait(() -> {
            player.teleportAsync(EventCore.getInstance().getMapManager().getSpawnLocation());
            if (EventCore.getInstance().getGameManager().isRunning()) {
                player.setGameMode(GameMode.SPECTATOR);
            }
        }, 2);

        if (player.hasPermission("event.notify") && cache.isUpdateNotifyOnJoin()) {
            UpdateChecker updateChecker = new UpdateChecker(EventCore.getInstance(), "DavidArchive", "EventCore");
            updateChecker.check();

            Scheduler.wait(() -> {
                if (updateChecker.isHasUpdate()) {
                    player.sendMessage(Component.empty());
                    player.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes(cache.getUpdateOutdatedMessage())));
                    player.sendMessage(updateChecker.getUpdateComponent());
                    player.sendMessage(Component.empty());
                }
            }, 20);
        }
    }

}
