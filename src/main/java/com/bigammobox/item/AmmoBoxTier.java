package com.bigammobox.item;

import java.math.BigInteger;

/**
 * Capacity families for BIG AMMO BOX.
 *
 * The FLOAT/DOUBLE entries deliberately reproduce the finite MAX_VALUE
 * magnitudes with BigInteger so every round remains an exact integer.
 */
public enum AmmoBoxTier {
    NETHERITE(32, 0x3B3540, 0x78707A, CapacityKind.STACK_MULTIPLIER, null),
    NETHER_STAR(128, 0x44384F, 0xBDA8FF, CapacityKind.STACK_MULTIPLIER, null),
    COMPRESSED(1024, 0x3F3450, 0xFFFFFF, CapacityKind.STACK_MULTIPLIER, null),
    BILLION_21(Integer.MAX_VALUE, 0xFFFFFF, 0xFFFFFF, CapacityKind.FIXED,
            BigInteger.valueOf(Integer.MAX_VALUE)),
    KEI_922(0, 0x28344F, 0x70E8FF, CapacityKind.FIXED,
            BigInteger.valueOf(Long.MAX_VALUE)),
    KAN_340(0, 0x24452F, 0xA4FF70, CapacityKind.FIXED,
            BigInteger.ONE.shiftLeft(128).subtract(BigInteger.ONE.shiftLeft(104))),
    DOUBLE_MAX(0, 0x4B2748, 0xFF77E8, CapacityKind.FIXED,
            BigInteger.ONE.shiftLeft(1024).subtract(BigInteger.ONE.shiftLeft(971))),
    PRACTICAL_INFINITY(0, 0xFFFFFF, 0xFFFFFF, CapacityKind.FIXED,
            BigInteger.TEN.pow(1000));

    /** Hard safety ceiling kept in code, intentionally not configurable. */
    public static final int PRACTICAL_INFINITY_HARD_EXPONENT = 10_000;
    public static final int PRACTICAL_INFINITY_CURRENT_EXPONENT = 1_000;

    private final int stackMultiplier;
    private final int bodyColor;
    private final int accentColor;
    private final CapacityKind kind;
    private final BigInteger fixedCapacity;

    AmmoBoxTier(int stackMultiplier, int bodyColor, int accentColor,
                CapacityKind kind, BigInteger fixedCapacity) {
        this.stackMultiplier = stackMultiplier;
        this.bodyColor = bodyColor;
        this.accentColor = accentColor;
        this.kind = kind;
        this.fixedCapacity = fixedCapacity;
    }

    public int stackMultiplier() { return stackMultiplier; }
    public int bodyColor() { return bodyColor; }
    public int accentColor() { return accentColor; }
    public boolean isFixed() { return kind == CapacityKind.FIXED; }
    public boolean usesBigIntegerStorage() { return fixedCapacity != null && fixedCapacity.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0; }
    public boolean isFixedIntMax() { return this == BILLION_21; }
    public boolean isGaming() { return this == BILLION_21 || this == PRACTICAL_INFINITY; }
    public boolean isFloatMaxEquivalent() { return this == KAN_340; }
    public boolean isDoubleMaxEquivalent() { return this == DOUBLE_MAX; }
    public boolean isPracticalInfinity() { return this == PRACTICAL_INFINITY; }

    public BigInteger fixedCapacity() {
        return fixedCapacity;
    }

    private enum CapacityKind {
        STACK_MULTIPLIER,
        FIXED
    }
}
