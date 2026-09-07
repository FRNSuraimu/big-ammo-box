package com.bigammobox.event;

import com.bigammobox.BigAmmoBoxMod;
import com.bigammobox.config.BigAmmoBoxConfig;
import com.bigammobox.enchantment.AmmoCaseEnchantments;
import com.bigammobox.item.AmmoBoxCaseItem;
import com.bigammobox.item.AmmoBoxCaseTier;
import com.bigammobox.access.BabBulletAccess;
import com.bigammobox.network.ModNetwork;
import com.bigammobox.network.QuickChargeReloadPacket;
import com.bigammobox.registry.ModEnchantments;
import com.bigammobox.registry.ModItems;
import com.bigammobox.server.reload.ServerQuickChargeState;
import com.bigammobox.server.autoreload.AutoReloadGunSessionState;
import com.bigammobox.storage.AmmoBoxCaseStorage;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.api.event.common.GunReloadEvent;
import com.tacz.guns.api.event.common.GunShootEvent;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.util.AttachmentDataUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Map;

@Mod.EventBusSubscriber(modid = BigAmmoBoxMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GameplayEvents {
    private static final int MENDING_INTERVAL_TICKS = 400;
    private static final int MAGNET_INTERVAL_TICKS = 5;

    private GameplayEvents() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AutoReloadGunSessionState.begin(player);
            ServerQuickChargeState.clear(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        AutoReloadGunSessionState.clear(event.getEntity());
        ServerQuickChargeState.clear(event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onGunShoot(GunShootEvent event) {
        if (event.getLogicalSide() != LogicalSide.SERVER || event.isCanceled()) return;
        if (!(event.getShooter() instanceof ServerPlayer player)) return;
        AutoReloadGunSessionState.markUsed(player, event.getGunItemStack(), player.serverLevel().getGameTime());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onGunReload(GunReloadEvent event) {
        if (event.getLogicalSide() != LogicalSide.SERVER || event.isCanceled()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // A manual reload is also a real use of the gun for this login session.
        AutoReloadGunSessionState.markUsed(player, event.getGunItemStack(), player.serverLevel().getGameTime());
        float multiplier = (float) AmmoCaseEnchantments.quickChargeMultiplier(player, event.getGunItemStack());
        // Snapshot once at reload start. Both server timing and client animations use this exact
        // value for the entire reload, even if cases are moved while the reload is in progress.
        ServerQuickChargeState.begin(player, multiplier);
        // Send even 1.0 so a reload without Quick Charge explicitly clears any previous client speed.
        ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new QuickChargeReloadPacket(player.getId(), multiplier));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onGunHurt(EntityHurtByGunEvent.Pre event) {
        if (event.getLogicalSide() != LogicalSide.SERVER || !event.isHeadShot()) return;
        if (!(event.getBullet() instanceof BabBulletAccess access)) return;
        float bonus = access.bigAmmoBox$getHeadshotMultiplierBonus();
        if (!(bonus > 0.0F) || !Float.isFinite(bonus)) return;
        double next = (double) event.getHeadshotMultiplier() + bonus;
        event.setHeadshotMultiplier((float) Math.min(Float.MAX_VALUE, Math.max(0.0D, next)));
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        long now = player.serverLevel().getGameTime();

        if (now % MENDING_INTERVAL_TICKS == 0L) tickMending(player);
        if ((now + player.getId()) % MAGNET_INTERVAL_TICKS == 0L) tickAmmoMagnet(player);
        tickAutoReload(player, now);
    }

    private static void tickMending(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack caseStack = player.getInventory().getItem(i);
            if (!(caseStack.getItem() instanceof AmmoBoxCaseItem)) continue;
            int rounds = AmmoCaseEnchantments.mendingRounds(caseStack);
            if (rounds > 0) AmmoBoxCaseStorage.regenerateExistingAmmo(caseStack, rounds);
        }
    }

    private static void tickAmmoMagnet(ServerPlayer player) {
        ItemStack magnetCase = ItemStack.EMPTY;
        int bestLevel = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack candidate = player.getInventory().getItem(i);
            if (!(candidate.getItem() instanceof AmmoBoxCaseItem)) continue;
            int level = AmmoCaseEnchantments.ammoMagnetLevel(candidate);
            if (level > bestLevel) {
                bestLevel = level;
                magnetCase = candidate;
            }
        }
        if (bestLevel <= 0 || magnetCase.isEmpty()) return;
        double range = AmmoCaseEnchantments.ammoMagnetRange(bestLevel);
        if (!(range > 0.0D)) return;

        ServerLevel level = player.serverLevel();
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class,
                player.getBoundingBox().inflate(range), e -> e.isAlive() && !e.getItem().isEmpty())) {
            if (!entity.isAlive()) continue;
            ItemStack stack = entity.getItem();
            if (!(stack.getItem() instanceof IAmmo ammo)) continue;
            ResourceLocation ammoId = ammo.getAmmoId(stack);
            if (ammoId == null || DefaultAssets.EMPTY_AMMO_ID.equals(ammoId)) continue;
            int before = stack.getCount();
            int accepted = AmmoBoxCaseStorage.depositAmmo(magnetCase, ammoId, before);
            if (accepted <= 0 || !entity.isAlive()) continue;
            ItemStack live = entity.getItem();
            if (live.isEmpty()) continue;
            int shrink = Math.min(accepted, live.getCount());
            live.shrink(shrink);
            if (live.isEmpty()) entity.discard();
        }
    }

    private static void tickAutoReload(ServerPlayer player, long now) {
        if (!AutoReloadGunSessionState.hasUsedGuns(player)) return;

        IGunOperator operator = IGunOperator.fromLivingEntity(player);
        if (operator != null && operator.getSynReloadState() != null
                && operator.getSynReloadState().getStateType().isReloading()) {
            return;
        }

        // Keep logical identity stable across ItemStack replacement/moves and split any copied NBT
        // duplicate before consulting login-session last-use history.
        AutoReloadGunSessionState.reconcileInventory(player);
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack gunStack = player.getInventory().getItem(i);
            if (gunStack.isEmpty()) continue;
            Long lastUse = AutoReloadGunSessionState.lastUse(player, gunStack);
            if (lastUse == null) continue; // never touch a gun that has not been used this login.
            tryAutoReloadGun(player, gunStack, lastUse, now);
        }
    }

    private static void tryAutoReloadGun(ServerPlayer player, ItemStack gunStack, long lastUse, long now) {
        IGun gun = IGun.getIGunOrNull(gunStack);
        if (gun == null || gun.useInventoryAmmo(gunStack)) return;

        int current = Math.max(0, gun.getCurrentAmmoCount(gunStack));
        int max = TimelessAPI.getCommonGunIndex(gun.getGunId(gunStack))
                .map(index -> AttachmentDataUtils.getAmmoCountWithAttachment(gunStack, index.getGunData()))
                .orElse(0);
        if (max <= 0 || current >= max) return;

        long idleDelay = Math.max(0L, BigAmmoBoxConfig.AUTO_RELOAD_IDLE_DELAY_TICKS.get());
        if (current > 0 && now - lastUse < idleDelay) return;

        ItemStack selectedCase = ItemStack.EMPTY;
        int selectedLevel = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack candidate = player.getInventory().getItem(i);
            if (!(candidate.getItem() instanceof AmmoBoxCaseItem)) continue;
            int level = AmmoCaseEnchantments.autoReloadLevel(candidate);
            if (level <= 0) continue;
            if (AmmoBoxCaseStorage.getAutoReloadReadyTick(candidate) > now) continue;
            if (!AmmoBoxCaseStorage.canSupplyGun(candidate, gunStack)) continue;
            selectedCase = candidate;
            selectedLevel = level;
            break;
        }
        if (selectedCase.isEmpty()) return;

        int needed = max - current;
        int supplied = AmmoBoxCaseStorage.extractCompatibleAmmo(selectedCase, gunStack, needed);
        if (supplied <= 0) return;
        int loaded = Math.min(needed, supplied);
        gun.setCurrentAmmoCount(gunStack, current + loaded);
        AmmoBoxCaseStorage.setAutoReloadReadyTick(selectedCase,
                now + AmmoCaseEnchantments.autoReloadCooldownTicks(selectedLevel));
        player.displayClientMessage(Component.translatable("message.big_ammo_box.auto_reload",
                gunStack.getHoverName(), loaded), true);
        player.getInventory().setChanged();
    }

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        if (!(left.getItem() instanceof AmmoBoxCaseItem caseItem)) return;

        if (right.is(ModItems.COMPRESSED_NETHER_STAR.get())
                && caseItem.tier().ordinal() >= AmmoBoxCaseTier.NETHERITE.ordinal()
                && AmmoCaseEnchantments.rawLevel(left, ModEnchantments.STELLAR_OVERDRIVE.get()) <= 0) {
            ItemStack output = left.copy();
            output.enchant(ModEnchantments.STELLAR_OVERDRIVE.get(), 1);
            event.setOutput(output);
            event.setCost(30);
            event.setMaterialCost(1);
            return;
        }

        // Vanilla Mending and Infinity are mutually exclusive. BAB intentionally permits the pair on
        // cases, but only when Infinity case support is explicitly enabled and Ammo Retention is absent.
        if (!right.is(Items.ENCHANTED_BOOK)) return;
        Map<Enchantment, Integer> book = EnchantmentHelper.getEnchantments(right);
        if (book.size() != 1) return;
        Map.Entry<Enchantment, Integer> entry = book.entrySet().iterator().next();
        Enchantment incoming = entry.getKey();
        int incomingLevel = Math.max(1, entry.getValue());
        boolean addingInfinity = incoming == Enchantments.INFINITY_ARROWS
                && AmmoCaseEnchantments.rawLevel(left, Enchantments.MENDING) > 0;
        boolean addingMending = incoming == Enchantments.MENDING
                && AmmoCaseEnchantments.rawLevel(left, Enchantments.INFINITY_ARROWS) > 0;
        if (!addingInfinity && !addingMending) return;
        if (!BigAmmoBoxConfig.ALLOW_INFINITY_ON_AMMO_CASES.get()) return;
        if (AmmoCaseEnchantments.rawLevel(left, ModEnchantments.AMMO_RETENTION.get()) > 0) return;

        ItemStack output = left.copy();
        output.enchant(incoming, BigAmmoBoxConfig.RESPECT_ENCHANTMENT_MAX_LEVEL.get()
                ? Math.min(incomingLevel, incoming.getMaxLevel()) : incomingLevel);
        event.setOutput(output);
        event.setCost(4);
        event.setMaterialCost(1);
    }
}
