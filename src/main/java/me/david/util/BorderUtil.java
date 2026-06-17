package me.david.util;

import me.david.EventCore;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BorderUtil implements Runnable {

    public static int borderDefault = 200;
    public static double borderDamageBuffer = 0.0;
    public static double borderDamageAmount = 0.2;
    public static int lastOptimal = borderDefault;
    public static boolean autoBorder;

    private static final int SAFE_PATH_CHECK_STEPS = 3;
    private static final Map<UUID, Long> LAST_UNSAFE_DAMAGE = new ConcurrentHashMap<>();

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

    public static double getUnsafeDamage() {
        return EventCore.getInstance().getConfig().getDouble("Settings.WorldBorder.Boost.UnsafeDamage", 4.0);
    }

    public static @Nullable WorldBorder getBorder(@Nullable Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        return location.getWorld().getWorldBorder();
    }

    public static boolean isInsideBorder(@Nullable Location location) {
        WorldBorder worldBorder = getBorder(location);
        return location != null && worldBorder != null && worldBorder.isInside(location);
    }

    public static boolean isOutsideBorder(@Nullable Location location) {
        return !isInsideBorder(location);
    }

    public static @NotNull Location clampInsideBorder(@NotNull Location location) {
        WorldBorder worldBorder = location.getWorld().getWorldBorder();
        Location center = worldBorder.getCenter();
        double halfSize = Math.max(0.1, (worldBorder.getSize() / 2.0) - 0.35);

        double dx = location.getX() - center.getX();
        double dz = location.getZ() - center.getZ();
        double horizontalDistance = Math.hypot(dx, dz);
        if (horizontalDistance <= halfSize) {
            return location;
        }

        double scale = halfSize / horizontalDistance;
        return new Location(
                location.getWorld(),
                center.getX() + (dx * scale),
                location.getY(),
                center.getZ() + (dz * scale),
                location.getYaw(),
                location.getPitch()
        );
    }

    public static boolean hasSafeBoostSpace(@NotNull Player player) {
        Location location = player.getLocation();
        if (!hasStandingRoom(location)) {
            return false;
        }

        WorldBorder worldBorder = player.getWorld().getWorldBorder();
        Location center = worldBorder.getCenter();
        Vector direction = center.toVector().subtract(location.toVector());
        direction.setY(0);
        if (direction.lengthSquared() < 0.0001) {
            return true;
        }

        direction.normalize();
        Location probe = location.clone();
        for (int step = 1; step <= SAFE_PATH_CHECK_STEPS; step++) {
            probe.add(direction);
            if (!hasStandingRoom(probe)) {
                return false;
            }
        }

        return true;
    }

    private static boolean hasStandingRoom(@NotNull Location location) {
        World world = location.getWorld();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        return isPassable(world.getBlockAt(x, y, z)) && isPassable(world.getBlockAt(x, y + 1, z));
    }

    private static boolean isPassable(@NotNull Block block) {
        return block.isPassable() || block.isLiquid();
    }

    public static void tickOutsidePlayer(@NotNull Player player) {
        if (!isBoostEnabled() || !isOutsideBorder(player.getLocation())) {
            return;
        }

        Scheduler.runForEntity(player, () -> {
            if (!isOutsideBorder(player.getLocation())) {
                return;
            }

            if (hasSafeBoostSpace(player)) {
                applyBoost(player);
            } else {
                applyUnsafeDamage(player);
            }
        });
    }

    private static void applyBoost(@NotNull Player player) {
        Location playerLocation = player.getLocation();
        WorldBorder worldBorder = player.getWorld().getWorldBorder();
        if (worldBorder.isInside(playerLocation) || !hasSafeBoostSpace(player)) {
            return;
        }

        double boostXZ = EventCore.getInstance().getConfig().getDouble("Settings.WorldBorder.Boost.StrengthXZ", 1.3);

        Location center = worldBorder.getCenter();
        Vector push = center.toVector().subtract(playerLocation.toVector());
        push.setY(0);
        if (push.lengthSquared() < 0.0001) {
            return;
        }

        push.normalize().multiply(boostXZ);
        player.setVelocity(push);
    }

    private static void applyUnsafeDamage(@NotNull Player player) {
        double damage = getUnsafeDamage();
        if (damage <= 0) {
            return;
        }

        long now = System.currentTimeMillis();
        UUID playerId = player.getUniqueId();
        if (now - LAST_UNSAFE_DAMAGE.getOrDefault(playerId, 0L) < 500L) {
            return;
        }
        LAST_UNSAFE_DAMAGE.put(playerId, now);

        DamageSource source = DamageSource.builder(DamageType.OUTSIDE_BORDER).build();
        player.damage(damage, source);
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
        border.setDamageAmount(borderDamageAmount);
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
            border.setDamageAmount(borderDamageAmount);
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
