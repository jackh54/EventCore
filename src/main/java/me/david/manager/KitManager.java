package me.david.manager;

import lombok.Getter;
import me.david.EventCore;
import me.david.api.events.kit.KitDeleteEvent;
import me.david.api.events.kit.KitEnableEvent;
import me.david.api.events.kit.KitGiveEvent;
import me.david.api.events.kit.KitSaveEvent;
import me.david.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@Getter
public class KitManager {

    private final Logger LOGGER = Logger.getLogger("KitManager");

    private volatile String enabledKit = "default";
    private final Map<String, Map<Integer, ItemStack>> kits = new ConcurrentHashMap<>();

    public KitManager() {
        loadAllKits();
    }

    public void give(@NotNull final Player player) {
        Map<Integer, ItemStack> kitItems = kits.get(enabledKit);
        if (kitItems == null || kitItems.isEmpty()) {
            return;
        }

        KitGiveEvent kitGiveEvent = new KitGiveEvent(player, enabledKit);
        Bukkit.getPluginManager().callEvent(kitGiveEvent);
        if (kitGiveEvent.isCancelled()) {
            return;
        }

        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        for (Map.Entry<Integer, ItemStack> entry : kitItems.entrySet()) {
            ItemStack item = entry.getValue();
            if (item != null) {
                player.getInventory().setItem(entry.getKey(), item.clone());
            }
        }
    }

    public void loadAllKits() {
        kits.clear();
        final FileConfiguration config = EventCore.getInstance().getConfig();
        enabledKit = config.getString("Kits.EnabledKit", "default");

        final ConfigurationSection section = config.getConfigurationSection("Kits.Kits");
        if (section == null) {
            LOGGER.warning("No kits found in config.");
            return;
        }

        for (String kitName : section.getKeys(false)) {
            Map<Integer, ItemStack> loaded = loadKit(section, kitName);
            kits.put(kitName, loaded);
            if (!loaded.isEmpty()) {
                LOGGER.info("Loaded kit '" + kitName + "' with " + loaded.size() + " items.");
            }
        }

        if (!kits.containsKey(enabledKit)) {
            kits.put(enabledKit, new ConcurrentHashMap<>());
        }

        int enabledItems = kits.getOrDefault(enabledKit, Map.of()).size();
        LOGGER.info("Loaded " + kits.size() + " kits into memory. Enabled kit: " + enabledKit + " (" + enabledItems + " items)");
        if (enabledItems == 0) {
            LOGGER.warning("Enabled kit '" + enabledKit + "' has no items. Save it with /kit save " + enabledKit
                    + " or check that the config uses a supported kit format.");
        }
    }

    private @NotNull Map<Integer, ItemStack> loadKit(@NotNull ConfigurationSection kitsSection, @NotNull String kitName) {
        ConfigurationSection kitSection = kitsSection.getConfigurationSection(kitName);
        if (kitSection != null) {
            return loadFromSection(kitSection);
        }

        String legacyValue = kitsSection.getString(kitName);
        if (legacyValue == null || legacyValue.isBlank() || legacyValue.equals("-")) {
            return new ConcurrentHashMap<>();
        }

        Map<Integer, ItemStack> legacyKit = loadFromLegacyBase64(kitName, legacyValue);
        if (!legacyKit.isEmpty()) {
            return legacyKit;
        }

        LOGGER.warning("Kit '" + kitName + "' uses an unsupported config format.");
        return new ConcurrentHashMap<>();
    }

    private @NotNull Map<Integer, ItemStack> loadFromSection(@NotNull ConfigurationSection kitSection) {
        final Map<Integer, ItemStack> map = new ConcurrentHashMap<>();
        for (String key : kitSection.getKeys(false)) {
            try {
                int slot = Integer.parseInt(key);
                ItemStack item = kitSection.getItemStack(key);
                if (item != null) {
                    map.put(slot, item.clone());
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return map;
    }

    private @NotNull Map<Integer, ItemStack> loadFromLegacyBase64(@NotNull String kitName, @NotNull String base64) {
        final Map<Integer, ItemStack> map = new ConcurrentHashMap<>();
        try {
            byte[] data = Base64.getMimeDecoder().decode(base64.replaceAll("\\s+", ""));
            try (BukkitObjectInputStream input = new BukkitObjectInputStream(new ByteArrayInputStream(data))) {
                Object first = input.readObject();
                if (first instanceof Map<?, ?> serializedMap) {
                    for (Map.Entry<?, ?> entry : serializedMap.entrySet()) {
                        try {
                            if (entry.getValue() instanceof ItemStack item) {
                                map.put(parseSlotKey(entry.getKey()), item.clone());
                            }
                        } catch (Exception ignored) {
                        }
                    }
                    return map;
                }

                if (first instanceof ItemStack firstItem) {
                    map.put(0, firstItem.clone());
                    for (int slot = 1; slot < 41; slot++) {
                        Object value = input.readObject();
                        if (value instanceof ItemStack item) {
                            map.put(slot, item.clone());
                        }
                    }
                }
            }
        } catch (Exception exception) {
            LOGGER.warning("Failed to load legacy base64 kit '" + kitName + "': " + exception.getMessage());
        }
        return map;
    }

    private int parseSlotKey(@Nullable Object key) {
        if (key instanceof Number number) {
            return number.intValue();
        }
        if (key instanceof String string) {
            return Integer.parseInt(string);
        }
        throw new IllegalArgumentException("Unsupported slot key type: " + key);
    }

    public void save(@NotNull final String kit, @NotNull final Player player) {
        final KitSaveEvent kitSaveEvent = new KitSaveEvent(kit, player);
        Bukkit.getPluginManager().callEvent(kitSaveEvent);
        if (kitSaveEvent.isCancelled()) {
            return;
        }

        try {
            final FileConfiguration config = EventCore.getInstance().getConfig();
            config.set("Kits.Kits." + kit, null);
            final ConfigurationSection kitSection = config.createSection("Kits.Kits." + kit);

            Map<Integer, ItemStack> cacheMap = new ConcurrentHashMap<>();
            for (int i = 0; i < 41; i++) {
                ItemStack item = player.getInventory().getItem(i);
                if (item != null) {
                    kitSection.set(String.valueOf(i), item);
                    cacheMap.put(i, item.clone());
                }
            }

            kits.put(kit, cacheMap);
            EventCore.getInstance().saveConfig();

            player.sendMessage(MessageUtil.getPrefix().append(MessageUtil.formatCommand("Kit.SaveSuccess", Map.of())));
        } catch (Exception e) {
            player.sendMessage(MessageUtil.getPrefix().append(MessageUtil.formatCommand("Kit.SaveFailed", Map.of())));
            LOGGER.warning("[KitManager] Failed to save kit: " + kit + " :" + e.getMessage());
        }
    }

    public void enable(@NotNull final String kit) {
        if (!kits.containsKey(kit)) {
            LOGGER.warning("Tried to enable unknown kit: " + kit);
            return;
        }

        final String previousKit = enabledKit;
        final KitEnableEvent event = new KitEnableEvent(kit, previousKit);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        enabledKit = kit;
        EventCore.getInstance().getConfig().set("Kits.EnabledKit", kit);
        EventCore.getInstance().saveConfig();

        Bukkit.getOnlinePlayers().forEach(this::give);
    }

    public void delete(@NotNull final String kit) {
        final KitDeleteEvent event = new KitDeleteEvent(kit);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        kits.remove(kit);
        EventCore.getInstance().getConfig().set("Kits.Kits." + kit, null);
        EventCore.getInstance().saveConfig();
    }
}
