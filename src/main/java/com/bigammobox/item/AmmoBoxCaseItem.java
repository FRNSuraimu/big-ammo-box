package com.bigammobox.item;

import com.bigammobox.config.BigAmmoBoxConfig;
import com.bigammobox.enchantment.AmmoCaseEnchantments;
import com.bigammobox.menu.AmmoBoxCaseMenu;
import com.bigammobox.registry.ModEnchantments;
import com.bigammobox.storage.AmmoBoxCaseStorage;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.item.IAmmo;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.List;

public final class AmmoBoxCaseItem extends Item {
    private final AmmoBoxCaseTier tier;

    public AmmoBoxCaseItem(AmmoBoxCaseTier tier) {
        super(new Item.Properties().stacksTo(1));
        this.tier = tier;
    }

    public AmmoBoxCaseTier tier() {
        return tier;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 15;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        if (enchantment == Enchantments.POWER_ARROWS
                || enchantment == Enchantments.QUICK_CHARGE
                || enchantment == Enchantments.PIERCING
                || enchantment == Enchantments.MENDING) {
            return true;
        }
        if (enchantment == Enchantments.INFINITY_ARROWS) {
            return BigAmmoBoxConfig.ALLOW_INFINITY_ON_AMMO_CASES.get()
                    && AmmoCaseEnchantments.rawLevel(stack, ModEnchantments.AMMO_RETENTION.get()) <= 0;
        }
        if (enchantment == ModEnchantments.STELLAR_OVERDRIVE.get()) {
            return false;
        }
        if (enchantment == ModEnchantments.AMMO_RETENTION.get()) {
            return AmmoCaseEnchantments.rawLevel(stack, Enchantments.INFINITY_ARROWS) <= 0;
        }
        return enchantment == ModEnchantments.BULK_AMMO_STORAGE.get()
                || enchantment == ModEnchantments.AUTO_RELOAD.get()
                || enchantment == ModEnchantments.AMMO_MAGNET.get();
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack caseStack, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) {
            return false;
        }
        ItemStack ammoStack = slot.getItem();
        if (!(ammoStack.getItem() instanceof IAmmo ammo)) {
            return false;
        }
        ResourceLocation incomingId = ammo.getAmmoId(ammoStack);
        if (incomingId == null || DefaultAssets.EMPTY_AMMO_ID.equals(incomingId)) {
            return false;
        }
        int deposited = AmmoBoxCaseStorage.depositAmmo(caseStack, incomingId, ammoStack.getCount());
        if (deposited <= 0) {
            return false;
        }
        ammoStack.shrink(deposited);
        slot.setChanged();

        // Bulk Ammo Storage is an explicit manual trigger: after the clicked stack, sweep only
        // the player's inventory and reuse the exact same same-ID -> empty-box allocation logic.
        if (!player.level().isClientSide
                && AmmoCaseEnchantments.bulkStorageLevel(caseStack) > 0) {
            bulkStorePlayerAmmo(caseStack, player);
        }
        return true;
    }

    private static void bulkStorePlayerAmmo(ItemStack caseStack, Player player) {
        boolean changed = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack candidate = player.getInventory().getItem(i);
            if (candidate.isEmpty() || !(candidate.getItem() instanceof IAmmo ammo)) continue;
            ResourceLocation ammoId = ammo.getAmmoId(candidate);
            if (ammoId == null || DefaultAssets.EMPTY_AMMO_ID.equals(ammoId)) continue;
            int accepted = AmmoBoxCaseStorage.depositAmmo(caseStack, ammoId, candidate.getCount());
            if (accepted <= 0) continue;
            candidate.shrink(accepted);
            changed = true;
        }
        if (changed) player.getInventory().setChanged();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            Component title = stack.getHoverName();
            NetworkHooks.openScreen(serverPlayer,
                    new SimpleMenuProvider((id, inventory, p) -> new AmmoBoxCaseMenu(id, inventory, stack), title),
                    buf -> buf.writeEnum(hand));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int used = AmmoBoxCaseStorage.countUsed(stack);
        tooltip.add(Component.translatable("tooltip.big_ammo_box.case_usage", used, tier.capacity())
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.big_ammo_box.case_hint")
                .withStyle(ChatFormatting.DARK_GRAY));

        int autoReload = AmmoCaseEnchantments.autoReloadLevel(stack);
        if (autoReload > 0) {
            long ready = AmmoBoxCaseStorage.getAutoReloadReadyTick(stack);
            long now = level == null ? 0L : level.getGameTime();
            if (ready <= now) {
                tooltip.add(Component.translatable("tooltip.big_ammo_box.auto_reload_ready")
                        .withStyle(ChatFormatting.GREEN));
            } else {
                double seconds = (ready - now) / 20.0D;
                tooltip.add(Component.translatable("tooltip.big_ammo_box.auto_reload_remaining",
                                String.format(java.util.Locale.ROOT, "%.1f", seconds))
                        .withStyle(ChatFormatting.GOLD));
            }
        }
    }
}
