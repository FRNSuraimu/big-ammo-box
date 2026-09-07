package com.bigammobox.storage;

import com.bigammobox.enchantment.AmmoCaseEnchantments;
import com.bigammobox.item.AmmoBoxCaseItem;
import com.bigammobox.item.BigAmmoBoxItem;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.config.sync.SyncConfig;
import com.tacz.guns.api.item.IAmmoBox;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import java.math.BigInteger;
import java.util.concurrent.ThreadLocalRandom;

public final class AmmoBoxCaseStorage {
    public static final String INVENTORY_TAG = "AmmoBoxCaseInventory";
    private static final String SLOT_TAG = "Slot";
    private static final String AUTO_RELOAD_READY_TICK_TAG = "BigAmmoBoxAutoReloadReadyTick";

    private AmmoBoxCaseStorage() {}

    public static int capacity(ItemStack caseStack) {
        return caseStack.getItem() instanceof AmmoBoxCaseItem item ? item.tier().capacity() : 0;
    }

    public static boolean isAllowed(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getItem() instanceof IAmmoBox
                && !(stack.getItem() instanceof AmmoBoxCaseItem);
    }

    public static NonNullList<ItemStack> read(ItemStack caseStack) {
        int capacity = capacity(caseStack);
        NonNullList<ItemStack> items = NonNullList.withSize(capacity, ItemStack.EMPTY);
        if (capacity <= 0 || !caseStack.hasTag()) {
            return items;
        }
        CompoundTag root = caseStack.getTag();
        if (root == null || !root.contains(INVENTORY_TAG, Tag.TAG_LIST)) {
            return items;
        }
        ListTag list = root.getList(INVENTORY_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int slot = entry.getInt(SLOT_TAG);
            if (slot < 0 || slot >= capacity) {
                continue;
            }
            ItemStack stored = ItemStack.of(entry);
            if (isAllowed(stored)) {
                items.set(slot, stored);
            }
        }
        return items;
    }

    public static void write(ItemStack caseStack, NonNullList<ItemStack> items) {
        CompoundTag root = caseStack.getOrCreateTag();
        ListTag list = new ListTag();
        int max = Math.min(items.size(), capacity(caseStack));
        for (int i = 0; i < max; i++) {
            ItemStack stored = items.get(i);
            if (stored.isEmpty() || !isAllowed(stored)) {
                continue;
            }
            CompoundTag entry = new CompoundTag();
            stored.save(entry);
            entry.putInt(SLOT_TAG, i);
            list.add(entry);
        }
        if (list.isEmpty()) {
            root.remove(INVENTORY_TAG);
        } else {
            root.put(INVENTORY_TAG, list);
        }
    }

    public static void copyInventory(ItemStack from, ItemStack to) {
        if (!from.hasTag()) {
            return;
        }
        CompoundTag src = from.getTag();
        if (src == null || !src.contains(INVENTORY_TAG, Tag.TAG_LIST)) {
            return;
        }
        to.getOrCreateTag().put(INVENTORY_TAG, src.get(INVENTORY_TAG).copy());
    }

    public static int countUsed(ItemStack caseStack) {
        int used = 0;
        for (ItemStack stack : read(caseStack)) {
            if (!stack.isEmpty()) used++;
        }
        return used;
    }

    /**
     * Checks case contents in left-top to right-bottom storage order.
     */
    public static boolean hasCompatibleAmmo(ItemStack caseStack, ItemStack gunStack) {
        for (ItemStack inner : read(caseStack)) {
            if (!(inner.getItem() instanceof IAmmoBox box)) {
                continue;
            }
            if (!box.isAmmoBoxOfGun(gunStack, inner)) {
                continue;
            }
            if (box.isAllTypeCreative(inner) || box.isCreative(inner)) {
                return true;
            }
            if (inner.getItem() instanceof BigAmmoBoxItem bigBox) {
                if (bigBox.getExactAmmoCount(inner).signum() > 0) return true;
            } else if (box.getAmmoCount(inner) > 0) {
                return true;
            }
        }
        return false;
    }

    /** True when the case can currently supply this gun, including an Infinity-typed zero-count box. */
    public static boolean canSupplyGun(ItemStack caseStack, ItemStack gunStack) {
        return hasCompatibleAmmo(caseStack, gunStack) || providesInfiniteAmmo(caseStack, gunStack);
    }

    /** Returns true when this case provides the current gun without consuming a represented round. */
    public static boolean providesInfiniteAmmo(ItemStack caseStack, ItemStack gunStack) {
        boolean caseInfinity = AmmoCaseEnchantments.hasInfinity(caseStack);
        for (ItemStack inner : read(caseStack)) {
            if (!(inner.getItem() instanceof IAmmoBox box)) continue;
            if (!box.isAmmoBoxOfGun(gunStack, inner)) continue;
            if (box.isAllTypeCreative(inner) || box.isCreative(inner)) return true;
            if (!caseInfinity) continue;
            // Infinity does not invent an ammo type. The inner box must already represent this gun's ammo.
            ResourceLocation id = box.getAmmoId(inner);
            if (id != null && !DefaultAssets.EMPTY_AMMO_ID.equals(id)) return true;
        }
        return false;
    }

    /**
     * Extracts from inner ammo boxes in GUI order (slot 0 = left-top). Creative TaCZ boxes remain
     * infinite. Case Infinity behaves the same only for ammo types already represented in the case.
     * Ammo Retention is evaluated per provided round and never exceeds 100%.
     */
    public static int extractCompatibleAmmo(ItemStack caseStack, ItemStack gunStack, int requested) {
        if (requested <= 0) return 0;
        if (providesInfiniteAmmo(caseStack, gunStack)) return requested;

        NonNullList<ItemStack> items = read(caseStack);
        int remaining = requested;
        boolean changed = false;
        double retentionChance = AmmoCaseEnchantments.ammoRetentionChance(caseStack);
        for (int i = 0; i < items.size() && remaining > 0; i++) {
            ItemStack inner = items.get(i);
            if (!(inner.getItem() instanceof IAmmoBox box)) continue;
            if (!box.isAmmoBoxOfGun(gunStack, inner)) continue;

            if (box.isAllTypeCreative(inner) || box.isCreative(inner)) {
                remaining = 0;
                break;
            }

            if (inner.getItem() instanceof BigAmmoBoxItem bigBox) {
                BigInteger current = bigBox.getExactAmmoCount(inner);
                if (current.signum() <= 0) continue;
                if (retentionChance <= 0.0D) {
                    int take = bigBox.extractAmmoExact(inner, remaining);
                    if (take > 0) { remaining -= take; changed = true; }
                    continue;
                }
                int consumed = 0;
                while (remaining > 0 && current.compareTo(BigInteger.valueOf(consumed)) > 0) {
                    remaining--;
                    if (ThreadLocalRandom.current().nextDouble() >= retentionChance) consumed++;
                }
                if (consumed > 0) {
                    BigInteger next = current.subtract(BigInteger.valueOf(consumed));
                    bigBox.setExactAmmoCount(inner, next);
                    if (next.signum() <= 0) box.setAmmoId(inner, DefaultAssets.EMPTY_AMMO_ID);
                    changed = true;
                }
                continue;
            }

            int current = Math.max(0, box.getAmmoCount(inner));
            if (current <= 0) continue;
            if (retentionChance <= 0.0D) {
                int take = Math.min(current, remaining);
                int next = current - take;
                box.setAmmoCount(inner, next);
                if (next <= 0) box.setAmmoId(inner, DefaultAssets.EMPTY_AMMO_ID);
                remaining -= take;
                changed = take > 0 || changed;
                continue;
            }
            int consumed = 0;
            while (remaining > 0 && consumed < current) {
                remaining--;
                if (ThreadLocalRandom.current().nextDouble() >= retentionChance) consumed++;
            }
            if (consumed > 0) {
                int next = current - consumed;
                box.setAmmoCount(inner, next);
                if (next <= 0) box.setAmmoId(inner, DefaultAssets.EMPTY_AMMO_ID);
                changed = true;
            }
        }
        if (changed) write(caseStack, items);
        return requested - remaining;
    }

    /**
     * Deposits a normal TaCZ ammo stack into boxes inside a case. Existing boxes of the same
     * ammo type are filled first in GUI order, then empty finite boxes are initialized and
     * filled in GUI order. Creative boxes and incompatible boxes are never mutated.
     *
     * @return number of rounds accepted; zero means the caller must consume nothing.
     */
    public static int depositAmmo(ItemStack caseStack, ResourceLocation ammoId, int available) {
        if (available <= 0 || ammoId == null || DefaultAssets.EMPTY_AMMO_ID.equals(ammoId)) return 0;
        NonNullList<ItemStack> items = read(caseStack);
        int remaining = available;

        // Pass 1: top up already-bound boxes before consuming empty boxes.
        for (int i = 0; i < items.size() && remaining > 0; i++) {
            ItemStack inner = items.get(i);
            if (!(inner.getItem() instanceof IAmmoBox box)) continue;
            if (box.isAllTypeCreative(inner) || box.isCreative(inner)) continue;
            ResourceLocation existingId = box.getAmmoId(inner);
            if (!ammoId.equals(existingId)) continue;
            remaining -= addAmmo(inner, box, ammoId, remaining);
        }

        // Pass 2: initialize empty boxes only after every matching box is full.
        for (int i = 0; i < items.size() && remaining > 0; i++) {
            ItemStack inner = items.get(i);
            if (!(inner.getItem() instanceof IAmmoBox box)) continue;
            if (box.isAllTypeCreative(inner) || box.isCreative(inner)) continue;
            ResourceLocation existingId = box.getAmmoId(inner);
            if (existingId != null && !DefaultAssets.EMPTY_AMMO_ID.equals(existingId)) continue;
            remaining -= addAmmo(inner, box, ammoId, remaining);
        }

        int accepted = available - remaining;
        if (accepted > 0) write(caseStack, items);
        return accepted;
    }

    private static int addAmmo(ItemStack boxStack, IAmmoBox box, ResourceLocation ammoId, int requested) {
        if (requested <= 0) return 0;
        ResourceLocation existingId = box.getAmmoId(boxStack);
        if (existingId != null && !DefaultAssets.EMPTY_AMMO_ID.equals(existingId) && !ammoId.equals(existingId)) return 0;

        if (boxStack.getItem() instanceof BigAmmoBoxItem bigBox) {
            BigInteger capacity = bigBox.capacityBigFor(ammoId);
            if (capacity.signum() <= 0) return 0;
            BigInteger current = bigBox.getExactAmmoCount(boxStack);
            BigInteger room = capacity.subtract(current);
            if (room.signum() <= 0) return 0;
            int take = room.min(BigInteger.valueOf(requested)).intValue();
            if (take <= 0) return 0;
            if (existingId == null || DefaultAssets.EMPTY_AMMO_ID.equals(existingId)) box.setAmmoId(boxStack, ammoId);
            bigBox.setExactAmmoCount(boxStack, current.add(BigInteger.valueOf(take)));
            return take;
        }

        int capacity = standardAmmoBoxCapacity(box, boxStack, ammoId);
        if (capacity <= 0) return 0;
        int current = Math.max(0, box.getAmmoCount(boxStack));
        int room = capacity - Math.min(capacity, current);
        if (room <= 0) return 0;
        int take = Math.min(room, requested);
        if (take <= 0) return 0;
        if (existingId == null || DefaultAssets.EMPTY_AMMO_ID.equals(existingId)) box.setAmmoId(boxStack, ammoId);
        box.setAmmoCount(boxStack, current + take);
        return take;
    }

    /** Matches TaCZ AmmoBoxItem capacity: ammo stack size * server box stack size * (level + 1). */
    private static int standardAmmoBoxCapacity(IAmmoBox box, ItemStack boxStack, ResourceLocation ammoId) {
        int levelMultiplier = Math.max(1, box.getAmmoLevel(boxStack) + 1);
        int configuredStacks = Math.max(1, SyncConfig.AMMO_BOX_STACK_SIZE.get());
        return TimelessAPI.getCommonAmmoIndex(ammoId).map(index -> {
            long capacity = (long) Math.max(1, index.getStackSize()) * configuredStacks * levelMultiplier;
            return (int) Math.min(Integer.MAX_VALUE, capacity);
        }).orElse(0);
    }

    /** Adds generated rounds only to already-typed finite boxes; empty boxes never gain a type. */
    public static int regenerateExistingAmmo(ItemStack caseStack, int requested) {
        if (requested <= 0) return 0;
        NonNullList<ItemStack> items = read(caseStack);
        int remaining = requested;
        for (int i = 0; i < items.size() && remaining > 0; i++) {
            ItemStack inner = items.get(i);
            if (!(inner.getItem() instanceof IAmmoBox box)) continue;
            if (box.isAllTypeCreative(inner) || box.isCreative(inner)) continue;
            ResourceLocation ammoId = box.getAmmoId(inner);
            if (ammoId == null || DefaultAssets.EMPTY_AMMO_ID.equals(ammoId)) continue;
            remaining -= addAmmo(inner, box, ammoId, remaining);
        }
        int added = requested - remaining;
        if (added > 0) write(caseStack, items);
        return added;
    }

    public static long getAutoReloadReadyTick(ItemStack caseStack) {
        CompoundTag tag = caseStack.getTag();
        return tag == null ? 0L : Math.max(0L, tag.getLong(AUTO_RELOAD_READY_TICK_TAG));
    }

    public static void setAutoReloadReadyTick(ItemStack caseStack, long readyTick) {
        caseStack.getOrCreateTag().putLong(AUTO_RELOAD_READY_TICK_TAG, Math.max(0L, readyTick));
    }

    public static boolean handlerHasCompatibleCase(IItemHandler handler, ItemStack gunStack) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack candidate = handler.getStackInSlot(slot);
            if (candidate.getItem() instanceof AmmoBoxCaseItem && canSupplyGun(candidate, gunStack)) {
                return true;
            }
        }
        return false;
    }

    public static int extractFromCases(IItemHandler handler, ItemStack gunStack, int requested) {
        if (requested <= 0) return 0;
        // Infinite sources win before finite cases so carrying a Creative/Infinity supply never burns reserves.
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack candidate = handler.getStackInSlot(slot);
            if (candidate.getItem() instanceof AmmoBoxCaseItem && providesInfiniteAmmo(candidate, gunStack)) {
                return requested;
            }
        }
        int remaining = requested;
        for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
            ItemStack candidate = handler.getStackInSlot(slot);
            if (!(candidate.getItem() instanceof AmmoBoxCaseItem)) continue;
            int got = extractCompatibleAmmo(candidate, gunStack, remaining);
            remaining -= got;
        }
        return requested - remaining;
    }
}
