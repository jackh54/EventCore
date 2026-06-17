package me.david.util;

import me.david.EventCore;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BorderUtil implements Runnable {

    public static int borderDefault = 200;
    public static double borderDamageBuffer = 0.0;
    public static double borderDamageAmount = 0.2;
    public static int lastOptimal = borderDefault;
    public static boolean autoBorder;

    public BorderUtil() {
        reload();
    }

    public static void reload() {
        var config = EventCore.getInstance().getConfig();
        borderDefault = config.getInt("Settings.WorldBorder.DefaultSize", borderDefault);
        borderDamageBuffer = config.getDouble("Settings.WorldBorder.Damage.Buffer", borderDamageBuffer);
        borderDamageAmount = config.getDouble("Settings.WorldBorder.Damage.Amount", borderDamageAmount);
        autoBorder = config.getBoolean("Settings.WorldBorder.AutoBorder", false);
        lastOptimal = borderDefault;
    }

    public static void setAutoBorder(boolean value) {
        autoBorder = value;
        EventCore.getInstance().getConfig().set("Settings.WorldBorder.AutoBorder", value);
        EventCore.getInstance().saveConfig();
    }

    public static boolean isBoostEnabled() {
        return EventCore.getInstance().getConfig().getBoolean("Settings.WorldBorder.Boost.Enabled", false);
    }

    public static @Nullable WorldBorder getBorder(@Nullable Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        return location.getWorld().getWorldBorder();
    }

    public static boolean isOutsideBorder(@Nullable Location location) {
        WorldBorder worldBorder = getBorder(location);
        return location != null && worldBorder != null && !worldBorder.isInside(location);
    }

    public static void applyBoost(@NotNull Player player) {
        if (!isBoostEnabled()) {
            return;
        }

        Scheduler.runForEntity(player, () -> {
            Location playerLocation = player.getLocation();
            WorldBorder worldBorder = player.getWorld().getWorldBorder();
            if (worldBorder.isInside(playerLocation)) {
                return;
            }

            double boostXZ = EventCore.getInstance().getConfig().getDouble("Settings.WorldBorder.Boost.StrengthXZ", 1.3);
            double boostY = EventCore.getInstance().getConfig().getDouble("Settings.WorldBorder.Boost.StrengthY", 0.1);

            Location center = worldBorder.getCenter();
            Vector toCenter = center.toVector().subtract(playerLocation.toVector());
            toCenter.setY(0);

            if (toCenter.lengthSquared() < 0.0001) {
                player.setVelocity(player.getVelocity().add(new Vector(0, boostY, 0)));
                return;
            }

            toCenter.normalize().multiply(boostXZ);
            toCenter.setY(boostY);
            player.setVelocity(player.getVelocity().add(toCenter));
        });
    }

    public static void syncWorldBorder(@Nullable Location spawn) {
        if (spawn == null || spawn.getWorld() == null) {
            return;
        }

        World world = spawn.getWorld();
        WorldBorder border = world.getWorldBorder();
        border.setCenter(spawn.getX(), spawn.getZ());
        border.setSize(borderDefault);
        border.setDamageBuffer(borderDamageBuffer);
        border.setDamageAmount(isBoostEnabled() ? 0 : borderDamageAmount);
    }

    public static void syncAllWorldBorders(@Nullable Location spawn) {
        syncWorldBorder(spawn);

        for (World world : EventCore.getInstance().getServer().getWorlds()) {
            if (spawn != null && spawn.getWorld() != null && world.equals(spawn.getWorld())) {
                continue;
            }

            WorldBorder border = world.getWorldBorder();
            border.setSize(borderDefault);
            border.setDamageBuffer(borderDamageBuffer);
            border.setDamageAmount(isBoostEnabled() ? 0 : borderDamageAmount);
        }
    }

    @Override
    public void run() {
        if (!EventCore.getInstance().getGameManager().isRunning() || !autoBorder) {
            return;
        }

        var spawnLocation = EventCore.getInstance().getMapManager().getSpawnLocation();
        if (spawnLocation == null || spawnLocation.getWorld() == null) {
            return;
        }

        int optimal = getOptimalSize();
        if (lastOptimal <= optimal) {
            return;
        }

        lastOptimal = optimal;
        Scheduler.runSync(() -> {
            var world = spawnLocation.getWorld();
            if (world == null) {
                return;
            }
            double current = world.getWorldBorder().getSize();
            world.getWorldBorder().setSize(optimal, (long) Math.max(1, current - optimal));
        });
    }

    private int getOptimalSize() {
        int alive = PlayerUtil.getAlive();
        int optimal = (int) (((Math.pow(alive, 2)) / 60 + 4 + 0.6 * alive) * 2);
        return Math.min(borderDefault, optimal);
    }

}
