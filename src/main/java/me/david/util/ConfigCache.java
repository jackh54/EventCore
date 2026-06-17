package me.david.util;

import lombok.Getter;
import me.david.EventCore;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public final class ConfigCache {

    private static final Map<String, String> DEFAULT_COMMAND_MESSAGES = Map.ofEntries(
            Map.entry("Event.InfoVersion", "&7Running &aEventCore &7v%version% &7on &a%platform%"),
            Map.entry("Event.InfoDownload", "&7Download at &ahttps://github.com/DavidArchive/EventCore"),
            Map.entry("Event.StopUsage", "&cUsage: /event stop <winner>"),
            Map.entry("Event.ReloadSuccess", "&aYou successfully reloaded the config within %ms%ms!"),
            Map.entry("Event.SpawnSaved", "&aSpawn location saved!"),
            Map.entry("Event.KickspecSuccess", "&aAll spectators have been kicked!"),
            Map.entry("Event.KickallSuccess", "&aAll players have been kicked!"),
            Map.entry("Event.ClearallSuccess", "&aAll player inventories have been cleared!"),
            Map.entry("Event.AutoBorderOn", "&aAutoBorder has been activated!"),
            Map.entry("Event.AutoBorderOff", "&cAutoBorder has been deactivated!"),
            Map.entry("Event.UsageStart", "&cUsage: /event start"),
            Map.entry("Event.UsageStop", "&cUsage: /event stop <winner>"),
            Map.entry("Event.UsageDrop", "&cUsage: /event drop"),
            Map.entry("Event.UsageReset", "&cUsage: /event reset"),
            Map.entry("Event.UsageAutoBorder", "&cUsage: /event autoBorder <on / off>"),
            Map.entry("Event.UsageSetSpawn", "&cUsage: /event setSpawn"),
            Map.entry("Event.UsageReload", "&cUsage: /event reload"),
            Map.entry("Event.UsageKickspec", "&cUsage: /event kickspec"),
            Map.entry("Event.UsageKickall", "&cUsage: /event kickall"),
            Map.entry("Event.UsageClearall", "&cUsage: /event clearall"),
            Map.entry("Kit.EquipAll", "&aEveryone &7has been equipped!"),
            Map.entry("Kit.PlayerOffline", "&cThis player is not online!"),
            Map.entry("Kit.EquipPlayer", "&a%player% &7has been equipped!"),
            Map.entry("Kit.KitMissing", "&cThis kit does not exist!"),
            Map.entry("Kit.KitEnabled", "&a%kit% &7has been enabled!"),
            Map.entry("Kit.KitSaved", "&a%kit% &7has been saved!"),
            Map.entry("Kit.KitDeleteEnabled", "&cYou can't delete the enabled kit!"),
            Map.entry("Kit.KitDeleted", "&a%kit% &7has been deleted!"),
            Map.entry("Kit.SaveSuccess", "&aKit saved successfully!"),
            Map.entry("Kit.SaveFailed", "&cFailed to save kit!"),
            Map.entry("Kit.UsageGive", "&cUsage: /kit <player>"),
            Map.entry("Kit.UsageEnable", "&cUsage: /kit enable <kit>"),
            Map.entry("Kit.UsageSave", "&cUsage: /kit save <kit>"),
            Map.entry("Kit.UsageDelete", "&cUsage: /kit delete <kit>"),
            Map.entry("Revive.ReviveAll", "&aEveryone &7has been revived!"),
            Map.entry("Revive.PlayerOffline", "&cThis player is not online!"),
            Map.entry("Revive.RevivePlayer", "&a%player% &7has been revived!"),
            Map.entry("Revive.Usage", "&cUsage: /revive <player>"),
            Map.entry("Spawn.MissingSpawn", "&cThere isn't a spawn location yet. Set one using /event setSpawn"),
            Map.entry("Spawn.Running", "&cYou cannot teleport to the spawn while the event is running"),
            Map.entry("Announcement.Usage", "&cUsage: /announce <message>")
    );

    private static ConfigCache instance;

    private Component prefix;
    private Map<Integer, String> startTimerColors = Map.of();
    private String startTimerMessage;
    private String startTimerTitle;
    private String startTimerSubTitle;
    private String startMessage;
    private String startTitle;
    private String startSubTitle;
    private String stopMessage;
    private String stopTitle;
    private String stopSubTitle;
    private boolean stopEnabled;
    private String ingameTimerFormat;
    private boolean ingameTimerEnabled;
    private boolean actionbarEnabled;
    private String actionbarMessage;
    private boolean autoBroadcastEnabled;
    private boolean autoBroadcastUseCommand;
    private String autoBroadcastCommand;
    private List<String> autoBroadcastMessages = List.of();
    private long autoBroadcastInterval;
    private int startTimerSeconds;
    private boolean playerJoinEnabled;
    private String playerJoinMessage;
    private boolean playerQuitEnabled;
    private String playerQuitMessage;
    private boolean playerDeathEnabled;
    private String playerDeathMessage1;
    private String playerDeathMessage2;
    private boolean updateNotifyOnJoin;
    private String updateOutdatedMessage;
    private String noPermissionMessage;
    private final Map<String, String> commandMessages = new HashMap<>();

    public static ConfigCache get() {
        if (instance == null) {
            instance = new ConfigCache();
        }
        return instance;
    }

    public void reload() {
        FileConfiguration config = EventCore.getInstance().getConfig();

        prefix = MessageUtil.translateColorCodes(config.getString("Messages.Prefix", "&8 | &#559effEvent &8» &7"));
        startTimerSeconds = config.getInt("Messages.StartTimer.Timer", 5);
        startTimerMessage = config.getString("Messages.StartTimer.Message", "The game starts in &c%timer% seconds!");
        startTimerTitle = config.getString("Messages.StartTimer.Title", "%timer%");
        startTimerSubTitle = config.getString("Messages.StartTimer.SubTitle", "");
        startMessage = config.getString("Messages.Start.Message", "&aThe game starts now! Good luck!");
        startTitle = config.getString("Messages.Start.Title", "&aStart!");
        startSubTitle = config.getString("Messages.Start.SubTitle", "");
        stopEnabled = config.getBoolean("Messages.Stop.Enabled", true);
        stopMessage = config.getString("Messages.Stop.Message", "&cThe game has ended! The winner is %winner%");
        stopTitle = config.getString("Messages.Stop.Title", "&c%winner%");
        stopSubTitle = config.getString("Messages.Stop.SubTitle", "&7is the winner");
        ingameTimerFormat = config.getString("Settings.IngameTimer.Format", "&8» &chh:mm:ss &8«");
        ingameTimerEnabled = config.getBoolean("Settings.IngameTimer.Enabled", true);
        actionbarEnabled = config.getBoolean("Messages.Actionbar.Enabled", false);
        actionbarMessage = config.getString("Messages.Actionbar.Message", "&aYou are playing the best Event!");
        autoBroadcastEnabled = config.getBoolean("AutoBroadcast.Enabled", true);
        autoBroadcastUseCommand = config.getBoolean("AutoBroadcast.UseBroadcastCommand", false);
        autoBroadcastCommand = config.getString("AutoBroadcast.BroadcastCommand", "/bc %message%");
        autoBroadcastMessages = List.copyOf(config.getStringList("AutoBroadcast.Messages"));
        autoBroadcastInterval = config.getLong("AutoBroadcast.Interval", 60);
        playerJoinEnabled = config.getBoolean("Messages.PlayerJoin.Enabled", true);
        playerJoinMessage = config.getString("Messages.PlayerJoin.Message", "&a%player% &7joined the Event!");
        playerQuitEnabled = config.getBoolean("Messages.PlayerQuit.Enabled", true);
        playerQuitMessage = config.getString("Messages.PlayerQuit.Message", "&c%player% &7left the Event!");
        playerDeathEnabled = config.getBoolean("Messages.PlayerDeath.Enabled", true);
        playerDeathMessage1 = config.getString("Messages.PlayerDeath.Message1", "&8 | &c☠ &8» &c%player% &7was killed by &c%killer%&7!");
        playerDeathMessage2 = config.getString("Messages.PlayerDeath.Message2", "&8 | &c☠ &8» &c%player% &7died!");
        updateNotifyOnJoin = config.getBoolean("Settings.Updates.NotifyOnJoin", true);
        updateOutdatedMessage = config.getString("Messages.Updates.Outdated", "&7You're running an outdated version of EventCore. Please update to the latest version:");
        noPermissionMessage = config.getString("Messages.NoPermission", "&cYou don't have permission to use this command!");

        Map<Integer, String> colors = new HashMap<>();
        for (int seconds = 1; seconds <= 30; seconds++) {
            String color = config.getString("Messages.StartTimer.Colors." + seconds + "sec");
            if (color != null) {
                colors.put(seconds, color);
            }
        }
        startTimerColors = Collections.unmodifiableMap(colors);

        commandMessages.clear();
        commandMessages.putAll(DEFAULT_COMMAND_MESSAGES);
        ConfigurationSection commands = config.getConfigurationSection("Messages.Commands");
        if (commands != null) {
            for (String category : commands.getKeys(false)) {
                ConfigurationSection section = commands.getConfigurationSection(category);
                if (section == null) {
                    continue;
                }
                for (String key : section.getKeys(false)) {
                    String value = section.getString(key, "");
                    if (!value.isEmpty()) {
                        commandMessages.put(category + "." + key, value);
                    }
                }
            }
        }
    }

    public String commandMessage(String key) {
        return commandMessages.getOrDefault(key, DEFAULT_COMMAND_MESSAGES.getOrDefault(key, ""));
    }

    public String startTimerColor(int seconds) {
        return startTimerColors.getOrDefault(seconds, "&f");
    }

}
