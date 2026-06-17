package me.david.command.impl;

import me.david.EventCore;
import me.david.command.BukkitCommand;
import me.david.util.BorderUtil;
import me.david.util.MessageUtil;
import me.david.util.Scheduler;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class EventCommand extends BukkitCommand {

    private final EventCore plugin;

    public EventCommand(EventCore plugin) {
        super("event", "event.command", "e");
        this.plugin = plugin;
    }

    private String getSoftware() {
        return Scheduler.isFOLIA() ? "Folia" : "PaperMC";
    }

    @Override
    public void onCommand(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof final Player player)) return;

        if (args.length == 0) {
            player.sendMessage(Component.empty());
            MessageUtil.sendPrefixed(player, "Event.InfoVersion", Map.of(
                    "%version%", Component.text(plugin.getPluginMeta().getVersion()),
                    "%platform%", Component.text(getSoftware())
            ));
            MessageUtil.sendPrefixed(player, "Event.InfoDownload", Map.of());
            player.sendMessage(Component.empty());
            return;
        }

        if (args.length == 1) {
            switch (args[0].toLowerCase()) {
                case "start" -> {
                    plugin.getGameManager().start();
                    return;
                }
                case "stop" -> {
                    MessageUtil.sendPrefixed(player, "Event.StopUsage", Map.of());
                    return;
                }
                case "drop" -> {
                    plugin.getMapManager().drop();
                    return;
                }
                case "reset" -> {
                    plugin.getMapManager().reset();
                    return;
                }
                case "reload" -> {
                    long start = System.currentTimeMillis();
                    plugin.reloadRuntimeConfig();
                    long reloadMs = System.currentTimeMillis() - start;
                    MessageUtil.sendPrefixed(player, "Event.ReloadSuccess", Map.of(
                            "%ms%", Component.text(String.valueOf(reloadMs))
                    ));
                    return;
                }
                case "setspawn" -> {
                    plugin.getMapManager().saveSpawnLocation(player);
                    MessageUtil.sendPrefixed(player, "Event.SpawnSaved", Map.of());
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 5, 5);
                    player.closeInventory();
                    return;
                }
                case "kickspec" -> {
                    for (final Player target : Bukkit.getOnlinePlayers()) {
                        if (!target.hasPermission("event.spec") && target.getGameMode() == GameMode.SPECTATOR) {
                            target.kick();
                        }
                    }
                    MessageUtil.sendPrefixed(player, "Event.KickspecSuccess", Map.of());
                    return;
                }
                case "kickall" -> {
                    for (final Player target : Bukkit.getOnlinePlayers()) {
                        if (!target.hasPermission("event.spec")) {
                            target.kick();
                        }
                    }
                    MessageUtil.sendPrefixed(player, "Event.KickallSuccess", Map.of());
                    return;
                }
                case "clearall" -> {
                    for (final Player target : Bukkit.getOnlinePlayers()) {
                        target.getInventory().setArmorContents(null);
                        target.getInventory().clear();
                    }
                    MessageUtil.sendPrefixed(player, "Event.ClearallSuccess", Map.of());
                    return;
                }
            }
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("autoBorder")) {
            if (args[1].equalsIgnoreCase("on")) {
                BorderUtil.setAutoBorder(true);
                MessageUtil.sendPrefixed(player, "Event.AutoBorderOn", Map.of());
                return;
            }

            if (args[1].equalsIgnoreCase("off")) {
                BorderUtil.setAutoBorder(false);
                BorderUtil.lastOptimal = BorderUtil.borderDefault;
                MessageUtil.sendPrefixed(player, "Event.AutoBorderOff", Map.of());
                return;
            }
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("stop")) {
            String winner = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            plugin.getGameManager().stop(winner);
            return;
        }

        sendUsage(player);
    }

    private void sendUsage(Player player) {
        player.sendMessage(Component.empty());
        MessageUtil.sendPrefixed(player, "Event.UsageStart", Map.of());
        MessageUtil.sendPrefixed(player, "Event.UsageStop", Map.of());
        MessageUtil.sendPrefixed(player, "Event.UsageDrop", Map.of());
        MessageUtil.sendPrefixed(player, "Event.UsageReset", Map.of());
        MessageUtil.sendPrefixed(player, "Event.UsageAutoBorder", Map.of());
        MessageUtil.sendPrefixed(player, "Event.UsageSetSpawn", Map.of());
        MessageUtil.sendPrefixed(player, "Event.UsageReload", Map.of());
        MessageUtil.sendPrefixed(player, "Event.UsageKickspec", Map.of());
        MessageUtil.sendPrefixed(player, "Event.UsageKickall", Map.of());
        MessageUtil.sendPrefixed(player, "Event.UsageClearall", Map.of());
        player.sendMessage(Component.empty());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof final Player player)) return new ArrayList<>();
        if (!player.hasPermission("event.command")) return new ArrayList<>();

        List<String> list = new ArrayList<>();

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("stop")) {
                list.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            }

            if (args[0].equalsIgnoreCase("autoBorder")) {
                list.addAll(Arrays.asList("on", "off"));
            }
        }

        if (args.length == 1) {
            list.addAll(Arrays.asList("start", "stop", "drop", "reset", "autoBorder", "kickspec", "kickall", "clearall", "setSpawn", "reload"));
        }

        return list.stream().filter(content -> content.toLowerCase().startsWith(args[args.length - 1].toLowerCase())).sorted().toList();
    }

}
