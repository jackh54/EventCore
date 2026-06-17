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

        prefix = MessageUtil.translateColorCodes(config.getString("Messages.Prefix", ""));
        startTimerSeconds = config.getInt("Messages.StartTimer.Timer", 5);
        startTimerMessage = config.getString("Messages.StartTimer.Message", "");
        startTimerTitle = config.getString("Messages.StartTimer.Title", "");
        startTimerSubTitle = config.getString("Messages.StartTimer.SubTitle", "");
        startMessage = config.getString("Messages.Start.Message", "");
        startTitle = config.getString("Messages.Start.Title", "");
        startSubTitle = config.getString("Messages.Start.SubTitle", "");
        stopEnabled = config.getBoolean("Messages.Stop.Enabled", true);
        stopMessage = config.getString("Messages.Stop.Message", "");
        stopTitle = config.getString("Messages.Stop.Title", "");
        stopSubTitle = config.getString("Messages.Stop.SubTitle", "");
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
        playerJoinMessage = config.getString("Messages.PlayerJoin.Message", "");
        playerQuitEnabled = config.getBoolean("Messages.PlayerQuit.Enabled", true);
        playerQuitMessage = config.getString("Messages.PlayerQuit.Message", "");
        playerDeathEnabled = config.getBoolean("Messages.PlayerDeath.Enabled", true);
        playerDeathMessage1 = config.getString("Messages.PlayerDeath.Message1", "");
        playerDeathMessage2 = config.getString("Messages.PlayerDeath.Message2", "");
        updateNotifyOnJoin = config.getBoolean("Settings.Updates.NotifyOnJoin", true);
        updateOutdatedMessage = config.getString("Messages.Updates.Outdated", "");
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
        ConfigurationSection commands = config.getConfigurationSection("Messages.Commands");
        if (commands != null) {
            for (String category : commands.getKeys(false)) {
                ConfigurationSection section = commands.getConfigurationSection(category);
                if (section == null) {
                    continue;
                }
                for (String key : section.getKeys(false)) {
                    commandMessages.put(category + "." + key, section.getString(key, ""));
                }
            }
        }
    }

    public String commandMessage(String key) {
        return commandMessages.getOrDefault(key, "");
    }

    public String startTimerColor(int seconds) {
        return startTimerColors.getOrDefault(seconds, "&f");
    }

}
