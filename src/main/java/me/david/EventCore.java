package me.david;

import lombok.Getter;
import me.clip.placeholderapi.PlaceholderAPI;
import me.david.command.impl.*;
import me.david.listener.*;
import me.david.manager.GameManager;
import me.david.manager.KitManager;
import me.david.manager.MapManager;
import me.david.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
public class EventCore extends JavaPlugin {

    public static final Logger LOGGER = LoggerFactory.getLogger("EventCore");

    @Getter
    private static EventCore instance;
    private MapManager mapManager;
    private GameManager gameManager;
    private KitManager kitManager;
    private Scheduler.TaskWrapper actionbarTask;
    private Scheduler.TaskWrapper autoBroadcastTask;
    private Scheduler.TaskWrapper borderBoostTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        instance = this;

        ConfigCache.get().reload();
        BorderUtil.reload();

        new UpdateChecker(this, "DavidArchive", "EventCore").check();

        mapManager = new MapManager();
        gameManager = new GameManager();
        kitManager = new KitManager();

        new AnnouncementCommand(this);
        new EventCommand(this);
        new KitCommand(this);
        new ReviveCommand();
        new SpawnCommand(this);

        Bukkit.getPluginManager().registerEvents(new BlockBreakListener(), this);
        Bukkit.getPluginManager().registerEvents(new BlockExplodeListener(), this);
        Bukkit.getPluginManager().registerEvents(new BlockPlaceListener(), this);
        Bukkit.getPluginManager().registerEvents(new CreatureSpawnListener(), this);
        Bukkit.getPluginManager().registerEvents(new EntityDamageByEntityListener(), this);
        Bukkit.getPluginManager().registerEvents(new EntityDamageListener(), this);
        Bukkit.getPluginManager().registerEvents(new EntityExplodeListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerDeathListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerDropItemListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerInteractListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerMoveListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerPickupItemListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerQuitListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerRespawnListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerTeleportListener(), this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderHook().register();
        }

        Scheduler.timerAsync(new BorderUtil(), 20, 10);
        startAutoBroadcast();
        startActionbarTask();
        startBorderBoostTask();

        Scheduler.wait(() -> {
            World world = mapManager.getSpawnLocation().getWorld();
            if (world == null) {
                return;
            }
            world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
            world.setDifficulty(Difficulty.PEACEFUL);
            applyWorldBorderSettings();
        }, 2);

        new Metrics(this, 28277);
    }

    @Override
    public void onDisable() {
        stopActionbarTask();
        stopAutoBroadcast();
        stopBorderBoostTask();
        if (gameManager != null && gameManager.isRunning()) {
            gameManager.stop(null);
        }
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        reloadRuntimeConfig();
    }

    public void reloadRuntimeConfig() {
        ConfigCache.get().reload();
        BorderUtil.reload();
        if (mapManager != null) {
            mapManager.reloadFromConfig();
        }
        if (kitManager != null) {
            kitManager.loadAllKits();
        }
        applyWorldBorderSettings();
        restartAutoBroadcast();
        restartActionbarTask();
        restartBorderBoostTask();
    }

    private void applyWorldBorderSettings() {
        BorderUtil.syncAllWorldBorders(mapManager != null ? mapManager.getSpawnLocation() : null);
    }

    private void startAutoBroadcast() {
        stopAutoBroadcast();
        long interval = ConfigCache.get().getAutoBroadcastInterval();
        autoBroadcastTask = Scheduler.timerAsync(new AutoBroadcast(), 20, 20 * interval);
    }

    private void stopAutoBroadcast() {
        if (autoBroadcastTask != null) {
            autoBroadcastTask.cancel();
            autoBroadcastTask = null;
        }
    }

    private void restartAutoBroadcast() {
        startAutoBroadcast();
    }

    private void startActionbarTask() {
        stopActionbarTask();
        if (!ConfigCache.get().isActionbarEnabled()) {
            return;
        }

        actionbarTask = Scheduler.timer(() -> {
            String raw = ConfigCache.get().getActionbarMessage();
            for (Player player : Bukkit.getOnlinePlayers()) {
                String parsed = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null
                        ? PlaceholderAPI.setPlaceholders(player, raw)
                        : raw;
                player.sendActionBar(MessageUtil.translateColorCodes(parsed));
            }
        }, 0, 20);
    }

    private void stopActionbarTask() {
        if (actionbarTask != null) {
            actionbarTask.cancel();
            actionbarTask = null;
        }
    }

    private void restartActionbarTask() {
        startActionbarTask();
    }

    private void startBorderBoostTask() {
        stopBorderBoostTask();
        if (!BorderUtil.isBoostEnabled()) {
            return;
        }

        borderBoostTask = Scheduler.timer(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (BorderUtil.isOutsideBorder(player.getLocation())) {
                    BorderUtil.applyBoost(player);
                }
            }
        }, 1, 10);
    }

    private void stopBorderBoostTask() {
        if (borderBoostTask != null) {
            borderBoostTask.cancel();
            borderBoostTask = null;
        }
    }

    private void restartBorderBoostTask() {
        startBorderBoostTask();
    }

}
