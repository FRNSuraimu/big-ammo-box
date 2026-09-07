package com.bigammobox.item;

public enum AmmoBoxCaseTier {
    IRON(4, 0x596064, 0xA7AFB3),
    GOLD(8, 0x5C584A, 0xFFD34F),
    DIAMOND(16, 0x425B60, 0x62EAF3),
    NETHERITE(32, 0x353139, 0x6F6873),
    NETHER_STAR(64, 0x41374A, 0xC5A6FF),
    COMPRESSED(128, 0x3B3149, 0xFFFFFF);

    private final int capacity;
    private final int bodyColor;
    private final int accentColor;

    AmmoBoxCaseTier(int capacity, int bodyColor, int accentColor) {
        this.capacity = capacity;
        this.bodyColor = bodyColor;
        this.accentColor = accentColor;
    }

    public int capacity() { return capacity; }
    public int bodyColor() { return bodyColor; }
    public int accentColor() { return accentColor; }
    public boolean isGamingAccent() { return this == COMPRESSED; }
}
