package com.bigammobox.item;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.api.item.nbt.AmmoBoxItemDataAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * A finite TaCZ-compatible ammo box whose capacity is owned by BIG AMMO BOX.
 *
 * TaCZ's IAmmoBox API exposes an int count. For capacities above Integer.MAX_VALUE,
 * this item stores the exact count as a BigInteger byte array and exposes a saturated
 * int compatibility window to TaCZ. Normal reloads then subtract from the exact value.
 */
public final class BigAmmoBoxItem extends Item implements AmmoBoxItemDataAccessor {
    public static final String BIG_AMMO_COUNT_TAG = "BigAmmoCount";
    private static final BigInteger INT_MAX = BigInteger.valueOf(Integer.MAX_VALUE);
    private static final BigInteger ZERO = BigInteger.ZERO;
    private final AmmoBoxTier tier;

    public BigAmmoBoxItem(AmmoBoxTier tier) {
        super(new Item.Properties().stacksTo(1));
        this.tier = tier;
    }

    public AmmoBoxTier tier() {
        return tier;
    }

    public BigInteger capacityBigFor(ResourceLocation ammoId) {
        if (tier.isFixed()) {
            return tier.fixedCapacity();
        }
        if (ammoId == null || DefaultAssets.EMPTY_AMMO_ID.equals(ammoId)) {
            return ZERO;
        }
        return TimelessAPI.getCommonAmmoIndex(ammoId)
                .map(index -> BigInteger.valueOf(index.getStackSize())
                        .multiply(BigInteger.valueOf(tier.stackMultiplier())))
                .orElse(ZERO);
    }

    public BigInteger capacityBigFor(ItemStack boxStack) {
        return capacityBigFor(getAmmoId(boxStack));
    }

    /** Legacy-compatible long view for old call sites. */
    public long capacityFor(ResourceLocation ammoId) {
        BigInteger cap = capacityBigFor(ammoId);
        return cap.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0 ? Long.MAX_VALUE : cap.longValue();
    }

    public long capacityFor(ItemStack boxStack) {
        return capacityFor(getAmmoId(boxStack));
    }

    public BigInteger getExactAmmoCount(ItemStack stack) {
        if (tier.usesBigIntegerStorage()) {
            var tag = stack.getOrCreateTag();
            if (tag.contains(BIG_AMMO_COUNT_TAG, Tag.TAG_BYTE_ARRAY)) {
                byte[] raw = tag.getByteArray(BIG_AMMO_COUNT_TAG);
                if (raw.length > 0) {
                    BigInteger value = new BigInteger(1, raw);
                    return clampExact(stack, value);
                }
            }
        }
        return BigInteger.valueOf(Math.max(0, AmmoBoxItemDataAccessor.super.getAmmoCount(stack)));
    }

    public void setExactAmmoCount(ItemStack stack, BigInteger count) {
        BigInteger cap = capacityBigFor(stack);
        if (cap.signum() <= 0) {
            cap = tier.isFixed() ? tier.fixedCapacity() : ZERO;
        }
        BigInteger clamped = count == null ? ZERO : count.max(ZERO);
        if (cap != null && cap.signum() > 0 && clamped.compareTo(cap) > 0) {
            clamped = cap;
        }

        var tag = stack.getOrCreateTag();
        if (tier.usesBigIntegerStorage()) {
            if (clamped.signum() == 0) {
                tag.remove(BIG_AMMO_COUNT_TAG);
            } else {
                tag.putByteArray(BIG_AMMO_COUNT_TAG, unsignedBytes(clamped));
            }
            tag.putInt(AMMO_COUNT_TAG, clamped.min(INT_MAX).intValue());
        } else {
            tag.remove(BIG_AMMO_COUNT_TAG);
            tag.putInt(AMMO_COUNT_TAG, clamped.min(INT_MAX).intValue());
        }
    }

    public void fillToMaximum(ItemStack stack) {
        setExactAmmoCount(stack, capacityBigFor(stack));
    }

    public int extractAmmoExact(ItemStack stack, int requested) {
        if (requested <= 0) return 0;
        BigInteger current = getExactAmmoCount(stack);
        if (current.signum() <= 0) return 0;
        int take = current.min(BigInteger.valueOf(requested)).intValue();
        if (take <= 0) return 0;
        BigInteger next = current.subtract(BigInteger.valueOf(take));
        setExactAmmoCount(stack, next);
        if (next.signum() <= 0) {
            setAmmoId(stack, DefaultAssets.EMPTY_AMMO_ID);
        }
        return take;
    }

    @Override
    public int getAmmoCount(ItemStack stack) {
        return getExactAmmoCount(stack).min(INT_MAX).intValue();
    }

    /**
     * Compatibility bridge for TaCZ's int-only extractor. When the exact count is
     * above Integer.MAX_VALUE, TaCZ sees Integer.MAX_VALUE and writes back a slightly
     * smaller int after a reload. Interpret that delta as consumed rounds.
     */
    @Override
    public void setAmmoCount(ItemStack stack, int count) {
        int safe = Math.max(0, count);
        BigInteger current = getExactAmmoCount(stack);
        if (tier.usesBigIntegerStorage() && current.compareTo(INT_MAX) > 0) {
            long consumed = (long) Integer.MAX_VALUE - (long) safe;
            if (consumed >= 0L) {
                setExactAmmoCount(stack, current.subtract(BigInteger.valueOf(consumed)).max(ZERO));
                return;
            }
        }
        setExactAmmoCount(stack, BigInteger.valueOf(safe));
        if (safe <= 0 && !tier.isFixed()) {
            setAmmoId(stack, DefaultAssets.EMPTY_AMMO_ID);
        }
    }

    /** Safe add path retained for int callers. */
    public int addAmmoSafely(ItemStack stack, int delta) {
        BigInteger current = getExactAmmoCount(stack);
        BigInteger next = current.add(BigInteger.valueOf(delta));
        BigInteger cap = capacityBigFor(stack);
        next = next.max(ZERO);
        if (cap.signum() > 0) next = next.min(cap);
        setExactAmmoCount(stack, next);
        BigInteger diff = next.subtract(current);
        if (diff.compareTo(INT_MAX) > 0) return Integer.MAX_VALUE;
        if (diff.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) < 0) return Integer.MIN_VALUE;
        return diff.intValue();
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack boxStack, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) {
            return false;
        }

        ItemStack slotStack = slot.getItem();
        ResourceLocation boxAmmoId = getAmmoId(boxStack);

        // Right-clicking an empty slot with the box removes one normal ammo stack.
        if (slotStack.isEmpty()) {
            if (DefaultAssets.EMPTY_AMMO_ID.equals(boxAmmoId)) {
                return false;
            }
            BigInteger current = getExactAmmoCount(boxStack);
            if (current.signum() <= 0) {
                return false;
            }
            int normalStackSize = TimelessAPI.getCommonAmmoIndex(boxAmmoId)
                    .map(index -> index.getStackSize())
                    .orElse(1);
            int amount = current.min(BigInteger.valueOf(Math.max(1, normalStackSize))).intValue();
            ItemStack out = AmmoItemBuilder.create().setId(boxAmmoId).setCount(amount).build();
            if (!slot.mayPlace(out)) {
                return false;
            }
            slot.set(out);
            BigInteger next = current.subtract(BigInteger.valueOf(amount));
            setExactAmmoCount(boxStack, next);
            if (next.signum() <= 0) {
                setAmmoId(boxStack, DefaultAssets.EMPTY_AMMO_ID);
            }
            return true;
        }

        // Right-clicking a TaCZ ammo stack with the box deposits that ammo.
        if (!(slotStack.getItem() instanceof IAmmo ammo)) {
            return false;
        }
        ResourceLocation incomingId = ammo.getAmmoId(slotStack);
        if (incomingId == null || DefaultAssets.EMPTY_AMMO_ID.equals(incomingId)) {
            return false;
        }
        if (!DefaultAssets.EMPTY_AMMO_ID.equals(boxAmmoId) && !boxAmmoId.equals(incomingId)) {
            return false;
        }
        if (DefaultAssets.EMPTY_AMMO_ID.equals(boxAmmoId)) {
            setAmmoId(boxStack, incomingId);
            boxAmmoId = incomingId;
        }

        BigInteger capacity = capacityBigFor(boxAmmoId);
        BigInteger current = getExactAmmoCount(boxStack);
        BigInteger room = capacity.subtract(current);
        if (room.signum() <= 0) {
            return false;
        }
        int take = room.min(BigInteger.valueOf(slotStack.getCount())).intValue();
        if (take <= 0) {
            return false;
        }
        slotStack.shrink(take);
        slot.setChanged();
        setExactAmmoCount(boxStack, current.add(BigInteger.valueOf(take)));
        return true;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getExactAmmoCount(stack).signum() > 0 && !DefaultAssets.EMPTY_AMMO_ID.equals(getAmmoId(stack));
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        BigInteger cap = capacityBigFor(stack);
        if (cap.signum() <= 0) return 0;
        BigInteger current = getExactAmmoCount(stack);
        if (current.signum() <= 0) return 0;
        if (current.compareTo(cap) >= 0) return 13;
        BigDecimal ratio = new BigDecimal(current).divide(new BigDecimal(cap), new MathContext(8, RoundingMode.HALF_UP));
        return Math.max(0, Math.min(13, ratio.multiply(BigDecimal.valueOf(13)).setScale(0, RoundingMode.HALF_UP).intValue()));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return tier.accentColor();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        ResourceLocation ammoId = getAmmoId(stack);
        NumberFormat nf = NumberFormat.getIntegerInstance(Locale.JAPAN);
        if (ammoId != null && !DefaultAssets.EMPTY_AMMO_ID.equals(ammoId)) {
            String key = TimelessAPI.getCommonAmmoIndex(ammoId)
                    .map(index -> index.getPojo().getName())
                    .orElse(ammoId.toString());
            tooltip.add(Component.translatable("tooltip.big_ammo_box.ammo", Component.translatable(key))
                    .withStyle(ChatFormatting.GRAY));
            BigInteger exact = getExactAmmoCount(stack);
            BigInteger max = capacityBigFor(ammoId);
            tooltip.add(Component.translatable("tooltip.big_ammo_box.count",
                            formatTooltipNumber(exact, max), formatTooltipNumber(max, max))
                    .withStyle(tier.isFixed() ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA));
        } else if (tier.isFixed()) {
            BigInteger max = tier.fixedCapacity();
            tooltip.add(Component.translatable("tooltip.big_ammo_box.count_empty_fixed",
                            formatTooltipNumber(max, max))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            tooltip.add(Component.translatable("tooltip.big_ammo_box.stack_capacity", tier.stackMultiplier())
                    .withStyle(ChatFormatting.AQUA));
        }

        appendTierExplanation(tooltip);
        tooltip.add(Component.translatable("tooltip.big_ammo_box.usage").withStyle(ChatFormatting.DARK_GRAY));
    }

    private void appendTierExplanation(List<Component> tooltip) {
        switch (tier) {
            case BILLION_21 -> {
                tooltip.add(Component.translatable("tooltip.big_ammo_box.type_int").withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.translatable("tooltip.big_ammo_box.int_max").withStyle(ChatFormatting.DARK_GRAY));
            }
            case KEI_922 -> {
                tooltip.add(Component.translatable("tooltip.big_ammo_box.type_long").withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.translatable("tooltip.big_ammo_box.long_max").withStyle(ChatFormatting.DARK_GRAY));
            }
            case KAN_340 -> {
                tooltip.add(Component.translatable("tooltip.big_ammo_box.type_float_equiv").withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.translatable("tooltip.big_ammo_box.float_max").withStyle(ChatFormatting.DARK_GRAY));
                tooltip.add(Component.translatable("tooltip.big_ammo_box.bigint_exact").withStyle(ChatFormatting.DARK_GRAY));
            }
            case DOUBLE_MAX -> {
                tooltip.add(Component.translatable("tooltip.big_ammo_box.type_double_equiv").withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.translatable("tooltip.big_ammo_box.double_max").withStyle(ChatFormatting.DARK_GRAY));
                tooltip.add(Component.translatable("tooltip.big_ammo_box.double_huayan_1").withStyle(ChatFormatting.DARK_GRAY));
                tooltip.add(Component.translatable("tooltip.big_ammo_box.double_huayan_2").withStyle(ChatFormatting.DARK_GRAY));
                tooltip.add(Component.translatable("tooltip.big_ammo_box.bigint_exact").withStyle(ChatFormatting.DARK_GRAY));
            }
            case PRACTICAL_INFINITY -> {
                tooltip.add(Component.translatable("tooltip.big_ammo_box.type_bigint").withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.translatable("tooltip.big_ammo_box.bigint_java_limit").withStyle(ChatFormatting.DARK_GRAY));
                tooltip.add(Component.translatable("tooltip.big_ammo_box.bigint_huayan").withStyle(ChatFormatting.DARK_GRAY));
                tooltip.add(Component.translatable("tooltip.big_ammo_box.bigint_safe_limit",
                        AmmoBoxTier.PRACTICAL_INFINITY_HARD_EXPONENT).withStyle(ChatFormatting.DARK_GRAY));
                tooltip.add(Component.translatable("tooltip.big_ammo_box.bigint_current",
                        AmmoBoxTier.PRACTICAL_INFINITY_CURRENT_EXPONENT).withStyle(ChatFormatting.GOLD));
            }
            default -> { }
        }
    }

    private String formatTooltipNumber(BigInteger value, BigInteger max) {
        if (value == null) return "0";
        if (tier.isPracticalInfinity() && max != null && max.equals(tier.fixedCapacity())) {
            BigInteger diff = max.subtract(value);
            if (diff.signum() == 0) return "10^" + AmmoBoxTier.PRACTICAL_INFINITY_CURRENT_EXPONENT;
            if (diff.signum() > 0 && diff.bitLength() < 63) {
                return "10^" + AmmoBoxTier.PRACTICAL_INFINITY_CURRENT_EXPONENT + " - " + NumberFormat.getIntegerInstance(Locale.JAPAN).format(diff.longValue());
            }
        }
        if (value.toString().length() <= 19) {
            return NumberFormat.getIntegerInstance(Locale.JAPAN).format(value);
        }
        return scientific(value, 8);
    }

    public static String scientific(BigInteger value, int significantDigits) {
        if (value == null || value.signum() == 0) return "0";
        String digits = value.abs().toString();
        int sig = Math.max(2, significantDigits);
        int exponent = digits.length() - 1;
        int take = Math.min(sig, digits.length());
        String head = digits.substring(0, take);
        boolean roundUp = digits.length() > take && digits.charAt(take) >= '5';
        BigInteger mantissaDigits = new BigInteger(head);
        if (roundUp) mantissaDigits = mantissaDigits.add(BigInteger.ONE);
        String rounded = mantissaDigits.toString();
        if (rounded.length() > head.length()) {
            exponent++;
            rounded = rounded.substring(0, head.length());
        }
        StringBuilder out = new StringBuilder();
        if (value.signum() < 0) out.append('-');
        out.append(rounded.charAt(0));
        if (rounded.length() > 1) {
            out.append('.').append(rounded.substring(1));
        }
        out.append("e").append(exponent);
        return out.toString();
    }

    private BigInteger clampExact(ItemStack stack, BigInteger value) {
        BigInteger cap = capacityBigFor(stack);
        BigInteger safe = value.max(ZERO);
        if (cap.signum() > 0 && safe.compareTo(cap) > 0) return cap;
        return safe;
    }

    private static byte[] unsignedBytes(BigInteger value) {
        byte[] raw = value.toByteArray();
        if (raw.length > 1 && raw[0] == 0) {
            byte[] trimmed = new byte[raw.length - 1];
            System.arraycopy(raw, 1, trimmed, 0, trimmed.length);
            return trimmed;
        }
        return raw;
    }

    // These custom boxes are always finite and never reinterpret TaCZ's Creative flags.
    @Override public boolean isCreative(ItemStack stack) { return false; }
    @Override public boolean isAllTypeCreative(ItemStack stack) { return false; }
    @Override public ItemStack setCreative(ItemStack stack, boolean allType) { return stack; }
    @Override public int getAmmoLevel(ItemStack stack) { return 0; }
    @Override public ItemStack setAmmoLevel(ItemStack stack, int level) { return stack; }
}
