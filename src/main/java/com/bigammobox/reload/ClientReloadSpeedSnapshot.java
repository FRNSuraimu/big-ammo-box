package com.bigammobox.reload;

import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Common-safe storage for client logical-side reload-start snapshots. */
public final class ClientReloadSpeedSnapshot {
    private static final ConcurrentHashMap<UUID, Double> SPEED_BY_ENTITY = new ConcurrentHashMap<>();

    private ClientReloadSpeedSnapshot() {}

    public static void begin(LivingEntity entity, double multiplier) {
        if (entity != null) SPEED_BY_ENTITY.put(entity.getUUID(), sanitize(multiplier));
    }

    public static double current(LivingEntity entity, double fallback) {
        if (entity == null) return sanitize(fallback);
        return SPEED_BY_ENTITY.getOrDefault(entity.getUUID(), sanitize(fallback));
    }

    public static void clear(LivingEntity entity) {
        if (entity != null) SPEED_BY_ENTITY.remove(entity.getUUID());
    }

    public static void clearAll() {
        SPEED_BY_ENTITY.clear();
    }

    private static double sanitize(double multiplier) {
        if (!Double.isFinite(multiplier) || multiplier < 1.0D) return 1.0D;
        return Math.min(multiplier, 1_000_000.0D);
    }
}
