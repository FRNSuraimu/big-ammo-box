package com.bigammobox.server.reload;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-authoritative reload-session speed snapshot.
 *
 * <p>Quick Charge is resolved once when TaCZ posts the server GunReloadEvent.  Subsequent
 * getReloadTime() calls use this stored value, so moving/replacing Ammo Box Box cases during an
 * already-started reload cannot change its speed halfway through.</p>
 */
public final class ServerQuickChargeState {
    private static final ConcurrentHashMap<UUID, Double> SPEED_BY_PLAYER = new ConcurrentHashMap<>();

    private ServerQuickChargeState() {}

    public static void begin(Player player, double multiplier) {
        if (player == null) return;
        SPEED_BY_PLAYER.put(player.getUUID(), sanitize(multiplier));
    }

    public static double current(LivingEntity living, double fallback) {
        if (!(living instanceof Player player)) return sanitize(fallback);
        return SPEED_BY_PLAYER.getOrDefault(player.getUUID(), sanitize(fallback));
    }

    public static void clear(Player player) {
        if (player != null) SPEED_BY_PLAYER.remove(player.getUUID());
    }

    private static double sanitize(double multiplier) {
        if (!Double.isFinite(multiplier) || multiplier < 1.0D) return 1.0D;
        return Math.min(multiplier, 1_000_000.0D);
    }
}
