package me.david.util;

import me.david.EventCore;

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
