package com.bigammobox.server.autoreload;

import com.bigammobox.BigAmmoBoxMod;
import com.tacz.guns.api.item.IGun;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Login-session-scoped logical gun identity for Auto Reload.
 *
 * <p>TaCZ mutates/replaces ItemStack instances while preserving stack NBT. BAB therefore stores a
 * tiny session token + gun UUID on guns that are actually used in the current login session, while
 * the last-use time itself remains server-memory-only and is discarded on logout.</p>
 */
public final class AutoReloadGunSessionState {
    private static final String SESSION_TAG = "BigAmmoBoxAutoReloadSession";
    private static final String GUN_ID_TAG = "BigAmmoBoxAutoReloadGunId";
    private static final ConcurrentHashMap<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private AutoReloadGunSessionState() {}

    public static void begin(ServerPlayer player) {
        if (player == null) return;
        SESSIONS.put(player.getUUID(), new Session(UUID.randomUUID()));
    }

    public static void clear(Player player) {
        if (player != null) SESSIONS.remove(player.getUUID());
    }

    public static void markUsed(ServerPlayer player, ItemStack gunStack, long gameTime) {
        if (player == null || !isGun(gunStack)) return;
        Session session = session(player);
        UUID gunId = ensureCurrentIdentity(gunStack, session);
        // If a current-session NBT copy created a duplicate ID, the stack being used wins the
        // original ID and every other duplicate gets a fresh, unused identity.
        reconcileInventory(player, gunStack, session);
        gunId = readCurrentGunId(gunStack, session);
        if (gunId != null) session.lastUseTick.put(gunId, gameTime);
    }

    public static boolean hasUsedGuns(ServerPlayer player) {
        Session session = player == null ? null : SESSIONS.get(player.getUUID());
        return session != null && !session.lastUseTick.isEmpty();
    }

    public static Long lastUse(ServerPlayer player, ItemStack gunStack) {
        Session session = player == null ? null : SESSIONS.get(player.getUUID());
        if (session == null || !isGun(gunStack)) return null;
        UUID gunId = readCurrentGunId(gunStack, session);
        return gunId == null ? null : session.lastUseTick.get(gunId);
    }

    /** Resolve same-session copied NBT identities without touching old-session or unused guns. */
    public static void reconcileInventory(ServerPlayer player) {
        if (player == null) return;
        Session session = SESSIONS.get(player.getUUID());
        if (session != null) reconcileInventory(player, null, session);
    }

    private static Session session(ServerPlayer player) {
        return SESSIONS.computeIfAbsent(player.getUUID(), ignored -> new Session(UUID.randomUUID()));
    }

    private static UUID ensureCurrentIdentity(ItemStack stack, Session session) {
        CompoundTag tag = stack.getOrCreateTag();
        UUID taggedSession = readUuid(tag, SESSION_TAG);
        UUID gunId = readUuid(tag, GUN_ID_TAG);
        if (!session.sessionId.equals(taggedSession) || gunId == null) {
            gunId = UUID.randomUUID();
            tag.putUUID(SESSION_TAG, session.sessionId);
            tag.putUUID(GUN_ID_TAG, gunId);
        }
        return gunId;
    }

    private static UUID readCurrentGunId(ItemStack stack, Session session) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return null;
        UUID taggedSession = readUuid(tag, SESSION_TAG);
        if (!session.sessionId.equals(taggedSession)) return null;
        return readUuid(tag, GUN_ID_TAG);
    }

    private static void reconcileInventory(ServerPlayer player, ItemStack preferred, Session session) {
        Map<UUID, ItemStack> ownerById = new HashMap<>();
        boolean changed = false;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!isGun(stack)) continue;
            UUID id = readCurrentGunId(stack, session);
            if (id == null) continue;
            ItemStack existing = ownerById.putIfAbsent(id, stack);
            if (existing == null || existing == stack) continue;

            if (stack == preferred && existing != preferred) {
                assignFreshIdentity(existing, session);
                ownerById.put(id, stack);
                changed = true;
                BigAmmoBoxMod.LOGGER.debug("Auto Reload duplicate gun identity resolved: preferred stack kept id={}", id);
            } else {
                UUID replacement = assignFreshIdentity(stack, session);
                ownerById.put(replacement, stack);
                changed = true;
                BigAmmoBoxMod.LOGGER.debug("Auto Reload duplicate gun identity resolved: reassigned id={} -> {}", id, replacement);
            }
        }
        if (changed) player.getInventory().setChanged();
    }

    private static UUID assignFreshIdentity(ItemStack stack, Session session) {
        UUID id = UUID.randomUUID();
        CompoundTag tag = stack.getOrCreateTag();
        tag.putUUID(SESSION_TAG, session.sessionId);
        tag.putUUID(GUN_ID_TAG, id);
        return id;
    }

    private static UUID readUuid(CompoundTag tag, String key) {
        try {
            return tag != null && tag.hasUUID(key) ? tag.getUUID(key) : null;
        } catch (RuntimeException ignored) {
            // Malformed/foreign NBT must never break a player tick; a later real use remints it.
            if (tag != null && tag.contains(key, Tag.TAG_INT_ARRAY)) tag.remove(key);
            return null;
        }
    }

    private static boolean isGun(ItemStack stack) {
        return stack != null && !stack.isEmpty() && IGun.getIGunOrNull(stack) != null;
    }

    private static final class Session {
        final UUID sessionId;
        final Map<UUID, Long> lastUseTick = new HashMap<>();
        Session(UUID sessionId) { this.sessionId = sessionId; }
    }
}
