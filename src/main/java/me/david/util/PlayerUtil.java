package me.david.util;

import lombok.experimental.UtilityClass;
import me.david.EventCore;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class PlayerUtil {

    public int getAlive() {
        int alive = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == GameMode.SURVIVAL) {
                alive++;
            }
        }
        return alive;
    }

    public int getTotal() {
        return Bukkit.getOnlinePlayers().size();
    }

    public void cleanPlayer(@NotNull Player player) {
        player.setGameMode(GameMode.SURVIVAL);
        player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(20.0);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setFireTicks(0);
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        player.getInventory().setArmorContents(null);
        player.getInventory().clear();
        EventCore.getInstance().getKitManager().give(player);
    }

}
