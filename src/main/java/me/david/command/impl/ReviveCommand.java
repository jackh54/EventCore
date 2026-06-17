package me.david.command.impl;

import me.david.command.BukkitCommand;
import me.david.util.MessageUtil;
import me.david.util.PlayerUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReviveCommand extends BukkitCommand {

    public ReviveCommand() {
        super("revive", "event.command.revive", "respawn");
    }

    @Override
    public void onCommand(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof final Player player)) return;

        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("*")) {
                Bukkit.getOnlinePlayers().forEach(PlayerUtil::cleanPlayer);
                MessageUtil.sendPrefixed(player, "Revive.ReviveAll", Map.of());
                return;
            }

            final Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                MessageUtil.sendPrefixed(player, "Revive.PlayerOffline", Map.of());
                return;
            }

            PlayerUtil.cleanPlayer(target);
            MessageUtil.sendPrefixed(player, "Revive.RevivePlayer", Map.of(
                    "%player%", Component.text(target.getName())
            ));
            return;
        }

        MessageUtil.sendPrefixed(player, "Revive.Usage", Map.of());
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) throws IllegalArgumentException {
        if (!(sender instanceof final Player player)) return new ArrayList<>();
        if (!player.hasPermission("event.command.revive")) return new ArrayList<>();

        List<String> list = new ArrayList<>();

        if (args.length == 1) {
            list.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            list.add("*");
        }

        return list.stream().filter(content -> content.toLowerCase().startsWith(args[args.length - 1].toLowerCase())).sorted().toList();
    }

}
