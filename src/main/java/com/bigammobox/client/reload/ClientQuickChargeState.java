package com.bigammobox.client.reload;

import com.bigammobox.BigAmmoBoxMod;
import com.bigammobox.reload.ClientReloadSpeedSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/** Client-only ordering bridge for Quick Charge reload snapshots and third-person animation speed. */
public final class ClientQuickChargeState {
    private static final long REMOTE_WINDOW_NANOS = 5_000_000_000L;
    private static final Map<Integer, PendingSpeed> PENDING_REMOTE_SPEED = new HashMap<>();
    private static final Map<Integer, Long> REMOTE_EVENT_AWAITING_PACKET = new HashMap<>();
    private static float localReloadSpeed = 1.0F;

    private ClientQuickChargeState() {}

    /** Local player predicts once from its client inventory at the CLIENT GunReloadEvent. */
    public static float beginLocal(AbstractClientPlayer player, float multiplier) {
        float safe = sanitize(multiplier);
        localReloadSpeed = safe;
        ClientReloadSpeedSnapshot.begin(player, safe);
        return safe;
    }

    /** First-person animation runner has no shooter reference, so expose the same local snapshot. */
    public static float localReloadSpeed() {
        return localReloadSpeed;
    }

    /**
     * Remote TaCZ reload event. If BAB's packet already arrived consume it; otherwise mark this
     * active reload as waiting so a late packet can accelerate the animation in place.
     */
    public static float beginRemote(AbstractClientPlayer player) {
        prune();
        int entityId = player.getId();
        PendingSpeed pending = PENDING_REMOTE_SPEED.remove(entityId);
        float speed;
        if (pending != null && !pending.expired()) {
            speed = pending.speed;
            REMOTE_EVENT_AWAITING_PACKET.remove(entityId);
        } else {
            speed = 1.0F;
            REMOTE_EVENT_AWAITING_PACKET.put(entityId, deadline());
        }
        ClientReloadSpeedSnapshot.begin(player, speed);
        return speed;
    }

    /** Handles either packet-before-event or event-before-packet ordering without assuming channels. */
    public static void receive(int entityId, float multiplier) {
        prune();
        float safe = sanitize(multiplier);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getId() == entityId) {
            // Local timing is snapshotted by the earlier CLIENT GunReloadEvent. Do not change that
            // snapshot halfway through the same reload when the authoritative server packet arrives.
            return;
        }

        Long waitingUntil = REMOTE_EVENT_AWAITING_PACKET.remove(entityId);
        if (waitingUntil != null && waitingUntil >= System.nanoTime()) {
            if (mc.level != null && mc.level.getEntity(entityId) instanceof AbstractClientPlayer player) {
                ClientReloadSpeedSnapshot.begin(player, safe);
                applyThirdPersonSpeed(player, safe);
                return;
            }
        }
        PENDING_REMOTE_SPEED.put(entityId, new PendingSpeed(safe, deadline()));
    }

    public static void clearAll() {
        PENDING_REMOTE_SPEED.clear();
        REMOTE_EVENT_AWAITING_PACKET.clear();
        localReloadSpeed = 1.0F;
        ClientReloadSpeedSnapshot.clearAll();
    }

    private static void prune() {
        long now = System.nanoTime();
        PENDING_REMOTE_SPEED.entrySet().removeIf(e -> e.getValue().expiresAt < now);
        REMOTE_EVENT_AWAITING_PACKET.entrySet().removeIf(e -> e.getValue() < now);
    }

    private static long deadline() {
        long now = System.nanoTime();
        long next = now + REMOTE_WINDOW_NANOS;
        return next < now ? Long.MAX_VALUE : next;
    }

    public static void applyThirdPersonSpeed(AbstractClientPlayer player, float multiplier) {
        float safe = sanitize(multiplier);
        if (safe <= 1.00001F) return;
        try {
            // Player Animator is an optional TaCZ compatibility dependency, so reflect instead of
            // turning it into a hard compile/runtime dependency for BAB.
            Class<?> access = Class.forName("dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess");
            Method getData = access.getMethod("getPlayerAssociatedData", AbstractClientPlayer.class);
            Object data = getData.invoke(null, player);

            Class<?> compat = Class.forName("com.tacz.guns.compat.playeranimator.PlayerAnimatorCompat");
            Object key = compat.getField("ONCE_UPPER_ANIMATION").get(null);
            Method getAssociated = data.getClass().getMethod("get", ResourceLocation.class);
            Object outer = getAssociated.invoke(data, key);
            if (outer == null) return;

            Method getAnimation = outer.getClass().getMethod("getAnimation");
            Object current = getAnimation.invoke(outer);
            if (current == null || !isActualReloadAnimation(current)) return;

            Class<?> animationInterface = Class.forName("dev.kosmx.playerAnim.api.layered.IAnimation");
            Class<?> modifierBase = Class.forName("dev.kosmx.playerAnim.api.layered.modifier.AbstractModifier");
            Class<?> speedClass = Class.forName("dev.kosmx.playerAnim.api.layered.modifier.SpeedModifier");
            Object speed = speedClass.getConstructor(float.class).newInstance(safe);

            Class<?> layerClass = Class.forName("dev.kosmx.playerAnim.api.layered.ModifierLayer");
            Constructor<?> ctor = layerClass.getConstructor(animationInterface,
                    java.lang.reflect.Array.newInstance(modifierBase, 0).getClass());
            Object modifiers = java.lang.reflect.Array.newInstance(modifierBase, 1);
            java.lang.reflect.Array.set(modifiers, 0, speed);
            Object wrapped = ctor.newInstance(current, modifiers);

            Method setAnimation = outer.getClass().getMethod("setAnimation", animationInterface);
            setAnimation.invoke(outer, wrapped);
        } catch (ClassNotFoundException ignored) {
            // Player Animator is absent: TaCZ itself also has no third-person Player Animator path.
        } catch (Throwable t) {
            BigAmmoBoxMod.LOGGER.debug("Quick Charge third-person animation speed bridge unavailable", t);
        }
    }

    private static boolean isActualReloadAnimation(Object animation) {
        try {
            Method getData = animation.getClass().getMethod("getData");
            Object keyframe = getData.invoke(animation);
            if (keyframe == null) return false;
            Field extraData = keyframe.getClass().getField("extraData");
            Object extra = extraData.get(keyframe);
            if (!(extra instanceof Map<?, ?> map)) return false;
            Object name = map.get("name");
            return name instanceof String text && isReloadAnimationName(text);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isReloadAnimationName(String name) {
        return name != null && name.toLowerCase(java.util.Locale.ROOT).contains("reload");
    }

    private static float sanitize(float value) {
        if (!Float.isFinite(value) || value < 1.0F) return 1.0F;
        return Math.min(value, 1_000_000.0F);
    }

    private record PendingSpeed(float speed, long expiresAt) {
        boolean expired() { return expiresAt < System.nanoTime(); }
    }
}
