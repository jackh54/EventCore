package me.david.command.impl;

import me.david.EventCore;
import me.david.command.BukkitCommand;
import me.david.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KitCommand extends BukkitCommand {

    private final EventCore plugin;

    public KitCommand(EventCore plugin) {
        super("kit", "event.command.kit", "kits", "playerkit");
        this.plugin = plugin;
    }

    @Override
    public void onCommand(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof final Player player)) return;
        if (!player.hasPermission("event.command")) return;

        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("*")) {
                Bukkit.getOnlinePlayers().forEach(plugin.getKitManager()::give);
                MessageUtil.sendPrefixed(player, "Kit.EquipAll", Map.of());
                return;
            }

            final Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                MessageUtil.sendPrefixed(player, "Kit.PlayerOffline", Map.of());
                return;
            }

            plugin.getKitManager().give(target);
            MessageUtil.sendPrefixed(player, "Kit.EquipPlayer", Map.of(
                    "%player%", Component.text(target.getName())
            ));
            return;
        }

        if (args.length == 2) {
            String kit = args[1].toLowerCase();

            if (args[0].equalsIgnoreCase("enable")) {
                if (!plugin.getKitManager().getKits().containsKey(kit)) {
                    MessageUtil.sendPrefixed(player, "Kit.KitMissing", Map.of());
                    return;
                }

                plugin.getKitManager().enable(kit);
                MessageUtil.sendPrefixed(player, "Kit.KitEnabled", Map.of(
                        "%kit%", Component.text(kit)
                ));
                return;
            }

            if (args[0].equalsIgnoreCase("save")) {
                plugin.getKitManager().save(kit, player);
                MessageUtil.sendPrefixed(player, "Kit.KitSaved", Map.of(
                        "%kit%", Component.text(kit)
                ));
                return;
            }

            if (args[0].equalsIgnoreCase("delete")) {
                if (!plugin.getKitManager().getKits().containsKey(kit)) {
                    MessageUtil.sendPrefixed(player, "Kit.KitMissing", Map.of());
                    return;
                }

                if (plugin.getKitManager().getEnabledKit().equalsIgnoreCase(kit)) {
                    MessageUtil.sendPrefixed(player, "Kit.KitDeleteEnabled", Map.of());
                    return;
                }

                plugin.getKitManager().delete(kit);
                MessageUtil.sendPrefixed(player, "Kit.KitDeleted", Map.of(
                        "%kit%", Component.text(kit)
                ));
                return;
            }
        }

        MessageUtil.sendPrefixed(player, "Kit.UsageGive", Map.of());
        MessageUtil.sendPrefixed(player, "Kit.UsageEnable", Map.of());
        MessageUtil.sendPrefixed(player, "Kit.UsageSave", Map.of());
        MessageUtil.sendPrefixed(player, "Kit.UsageDelete", Map.of());
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) throws IllegalArgumentException {
        if (!(sender instanceof final Player player)) return new ArrayList<>();
        if (!player.hasPermission("event.command.kit")) return new ArrayList<>();
        final List<String> list = new ArrayList<>();

        if (args.length == 2) {
            list.addAll(plugin.getKitManager().getKits().keySet());
        }

        if (args.length == 1) {
            list.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            list.add("*");
            list.add("enable");
            list.add("save");
            list.add("delete");
        }

        return list.stream().filter(content -> content.toLowerCase().startsWith(args[args.length - 1].toLowerCase())).sorted().toList();
    }

}
