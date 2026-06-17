package me.david.util;

import lombok.experimental.UtilityClass;
import me.david.EventCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class MessageUtil {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final Pattern LEGACY_COLOR = Pattern.compile("&([0-9a-fk-or])");
    private static final Pattern HEX_COLOR = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public Component get(@NotNull String key) {
        return translateColorCodes(EventCore.getInstance().getConfig().getString(key, ""));
    }

    public Component getPrefix() {
        return ConfigCache.get().getPrefix();
    }

    public @NotNull Component format(@NotNull String key, @NotNull Map<String, ? extends ComponentLike> replacements) {
        Component component = get(key);
        for (var entry : replacements.entrySet()) {
            component = component.replaceText(builder -> builder
                    .matchLiteral(entry.getKey())
                    .replacement(entry.getValue())
            );
        }
        return component;
    }

    public @NotNull Component formatCached(@NotNull String message, @NotNull Map<String, ? extends ComponentLike> replacements) {
        Component component = translateColorCodes(message);
        for (var entry : replacements.entrySet()) {
            component = component.replaceText(builder -> builder
                    .matchLiteral(entry.getKey())
                    .replacement(entry.getValue())
            );
        }
        return component;
    }

    public @NotNull Component formatCommand(@NotNull String key, @NotNull Map<String, ? extends ComponentLike> replacements) {
        return formatCached(ConfigCache.get().commandMessage(key), replacements);
    }

    public void sendPrefixed(@NotNull CommandSender sender, @NotNull String commandKey) {
        sender.sendMessage(getPrefix().append(formatCommand(commandKey, Map.of())));
    }

    public void sendPrefixed(@NotNull CommandSender sender, @NotNull String commandKey, @NotNull Map<String, ? extends ComponentLike> replacements) {
        sender.sendMessage(getPrefix().append(formatCommand(commandKey, replacements)));
    }

    public void sendPrefixed(@NotNull Player player, @NotNull Component component) {
        player.sendMessage(getPrefix().append(component));
    }

    @NotNull
    public Component translateColorCodes(@NotNull String message) {
        if (message.isEmpty()) {
            return Component.empty();
        }

        if (message.indexOf('<') >= 0 && message.indexOf('>') > message.indexOf('<')) {
            return MINI.deserialize(message);
        }

        String legacy = LEGACY_COLOR.matcher(message).replaceAll("§$1");
        Matcher matcher = HEX_COLOR.matcher(legacy);
        StringBuilder buffer = new StringBuilder(legacy.length() + 16);

        while (matcher.find()) {
            String hex = matcher.group(1);
            matcher.appendReplacement(buffer, "§x"
                    + "§" + hex.charAt(0)
                    + "§" + hex.charAt(1)
                    + "§" + hex.charAt(2)
                    + "§" + hex.charAt(3)
                    + "§" + hex.charAt(4)
                    + "§" + hex.charAt(5));
        }
        matcher.appendTail(buffer);
        return LEGACY.deserialize(buffer.toString());
    }

}
