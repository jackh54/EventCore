package me.david.command.impl;

import me.david.EventCore;
import me.david.command.BukkitCommand;
import me.david.util.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class SpawnCommand extends BukkitCommand {

    private final EventCore plugin;

    public SpawnCommand(EventCore plugin) {
        super("spawn", "event.command.spawn");
        this.plugin = plugin;
    }

    @Override
    public void onCommand(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof final Player player)) return;

        if (plugin.getMapManager().getSpawnLocation() == null) {
            MessageUtil.sendPrefixed(player, "Spawn.MissingSpawn", Map.of());
            return;
        }

        if (plugin.getGameManager().isRunning() && !player.hasPermission("event.bypass")) {
            MessageUtil.sendPrefixed(player, "Spawn.Running", Map.of());
            return;
        }

        player.teleportAsync(plugin.getMapManager().getSpawnLocation());
    }
}
