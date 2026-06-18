package me.david.util;

import me.david.EventCore;
import org.bukkit.GameMode;
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
    private static final long BORDER_TICK_COOLDOWN_MS = 250L;

    /**
     * Distance, in blocks, kept between the player and the exact border edge when
     * clamping them back inside. The world border is square, so each axis is
     * clamped independently to guarantee the smallest possible correction.
     */
    private static final double BORDER_EDGE_INSET = 0.35;

    private static final Map<UUID, Long> LAST_BORDER_TICK = new ConcurrentHashMap<>();
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

    /**
     * Returns the player location clamped to the nearest point just inside the
     * (square) world border. Each axis is clamped independently so the player is
     * only nudged by the minimum amount required to stay inside, which prevents
     * the large diagonal jumps that radial clamping produced near corners.
     */
    public static @NotNull Location clampInsideBorder(@NotNull Location location) {
        WorldBorder worldBorder = location.getWorld().getWorldBorder();
        Location center = worldBorder.getCenter();
        double limit = Math.max(0.0, (worldBorder.getSize() / 2.0) - BORDER_EDGE_INSET);

        double minX = center.getX() - limit;
        double maxX = center.getX() + limit;
        double minZ = center.getZ() - limit;
        double maxZ = center.getZ() + limit;

        double clampedX = clamp(location.getX(), minX, maxX);
        double clampedZ = clamp(location.getZ(), minZ, maxZ);
        if (clampedX == location.getX() && clampedZ == location.getZ()) {
            return location;
        }

        return new Location(
                location.getWorld(),
                clampedX,
                location.getY(),
                clampedZ,
                location.getYaw(),
                location.getPitch()
        );
    }

    private static double clamp(double value, double min, double max) {
        if (min > max) {
            return (min + max) / 2.0;
        }
        return Math.min(max, Math.max(min, value));
    }

    /**
     * Checks whether pushing the player back toward the border center has open
     * space, so the boost does not shove the player into a wall or underground.
     */
    public static boolean hasSafeBoostSpace(@NotNull Player player) {
        return hasSafeBoostSpace(player.getWorld().getWorldBorder(), player.getLocation());
    }

    private static boolean hasSafeBoostSpace(@NotNull WorldBorder worldBorder, @NotNull Location location) {
        if (!hasStandingRoom(location)) {
            return false;
        }

        Vector direction = inwardDirection(worldBorder, location);
        if (direction == null) {
            return true;
        }

        Location probe = location.clone();
        for (int step = 1; step <= SAFE_PATH_CHECK_STEPS; step++) {
            probe.add(direction);
            if (!hasStandingRoom(probe)) {
                return false;
            }
        }

        return true;
    }

    private static @Nullable Vector inwardDirection(@NotNull WorldBorder worldBorder, @NotNull Location location) {
        Location center = worldBorder.getCenter();
        Vector direction = center.toVector().subtract(location.toVector());
        direction.setY(0);
        if (direction.lengthSquared() < 0.0001) {
            return null;
        }
        return direction.normalize();
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

    public static boolean shouldHandlePlayer(@NotNull Player player) {
        return player.isValid() && !player.isDead() && player.getGameMode() == GameMode.SURVIVAL;
    }

    /**
     * Applies the border response for a player whose attempted destination is
     * outside the border. The caller is expected to have already clamped the
     * player's movement back inside; this method only adds the velocity boost
     * (or unsafe damage) so the player feels a sensible bounce instead of a
     * teleport.
     */
    public static void handleOutsidePlayer(@NotNull Player player, @Nullable Location outsideLocation) {
        if (!isBoostEnabled() || !shouldHandlePlayer(player)) {
            return;
        }
        if (outsideLocation == null || isInsideBorder(outsideLocation)) {
            return;
        }

        long now = System.currentTimeMillis();
        UUID playerId = player.getUniqueId();
        if (now - LAST_BORDER_TICK.getOrDefault(playerId, 0L) < BORDER_TICK_COOLDOWN_MS) {
            return;
        }
        LAST_BORDER_TICK.put(playerId, now);

        WorldBorder worldBorder = outsideLocation.getWorld().getWorldBorder();
        if (hasSafeBoostSpace(worldBorder, outsideLocation)) {
            applyBoost(player, worldBorder, outsideLocation);
        } else {
            applyUnsafeDamage(player);
        }
    }

    public static @Nullable Location resolvePearlDestination(@Nullable Location from, @Nullable Location to) {
        if (to == null) {
            return null;
        }

        if (!isOutsideBorder(to)) {
            return to;
        }

        if (from != null && isInsideBorder(from)) {
            return clampInsideBorder(to);
        }

        return null;
    }

    private static void applyBoost(@NotNull Player player, @NotNull WorldBorder worldBorder, @NotNull Location outsideLocation) {
        Vector direction = inwardDirection(worldBorder, outsideLocation);
        if (direction == null) {
            return;
        }

        double boostXZ = EventCore.getInstance().getConfig().getDouble("Settings.WorldBorder.Boost.StrengthXZ", 1.3);
        player.setVelocity(direction.multiply(boostXZ));
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

    public static void clearPlayer(@NotNull UUID playerId) {
        LAST_BORDER_TICK.remove(playerId);
        LAST_UNSAFE_DAMAGE.remove(playerId);
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
