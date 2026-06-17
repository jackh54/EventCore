package me.david.util;

import lombok.experimental.UtilityClass;
import me.david.EventCore;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

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

        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(20.0);
            player.setHealth(maxHealth.getValue());
        } else {
            player.setHealth(20.0);
        }

        player.setFoodLevel(20);
        player.setFireTicks(0);
        new ArrayList<>(player.getActivePotionEffects()).forEach(effect -> player.removePotionEffect(effect.getType()));
        player.getInventory().setArmorContents(null);
        player.getInventory().clear();
        EventCore.getInstance().getKitManager().give(player);
    }

}
