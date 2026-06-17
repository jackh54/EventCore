package me.david.util;

import me.david.EventCore;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class AutoBroadcast implements Runnable {

    private final List<String> messages;
    private int index;

    public AutoBroadcast() {
        messages = ConfigCache.get().getAutoBroadcastMessages();
    }

    @Override
    public void run() {
        ConfigCache cache = ConfigCache.get();
        if (!cache.isAutoBroadcastEnabled() || messages.isEmpty()) {
            return;
        }

        if (index >= messages.size()) {
            index = 0;
        }

        String message = messages.get(index++);
        Component component = MessageUtil.translateColorCodes(message);

        if (cache.isAutoBroadcastUseCommand()) {
            String command = cache.getAutoBroadcastCommand().replace("%message%", message);
            Scheduler.dispatchCommand(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
            return;
        }

        Scheduler.runSync(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(component);
            }
        });
    }

}
