package com.bigammobox.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class BigAmmoBoxConfig {
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ForgeConfigSpec.BooleanValue SHOW_HUGE_HUD_COUNTS;

    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec.BooleanValue RESPECT_ENCHANTMENT_MAX_LEVEL;
    public static final ForgeConfigSpec.BooleanValue ALLOW_INFINITY_ON_AMMO_CASES;
    public static final ForgeConfigSpec.DoubleValue STELLAR_OVERDRIVE_DAMAGE_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue AUTO_RELOAD_IDLE_DELAY_TICKS;

    static {
        ForgeConfigSpec.Builder client = new ForgeConfigSpec.Builder();
        client.comment(
                        "TaCZ HUDに表示する予備弾数の表示設定。",
                        "TaCZ gun HUD reserve-ammo display settings.")
                .translation("config.big_ammo_box.category.hud")
                .push("hud");
        SHOW_HUGE_HUD_COUNTS = client
                .comment(
                        "OFF: 有限弾薬が9999を超える場合は9999+、Creative/Infinity供給は9999と表示します。",
                        "ON: BIG AMMO BOXの巨大な実数値を表示し、Creative/Infinity供給は∞と表示します。",
                        "OFF: finite reserve ammo above 9999 is shown as 9999+, and Creative/Infinity supply as 9999.",
                        "ON: finite BIG AMMO BOX counts use their real magnitude, and Creative/Infinity supply is shown as ∞.")
                .translation("config.big_ammo_box.showHugeAmmoCounts")
                .define("showHugeAmmoCounts", false);
        client.pop();
        CLIENT_SPEC = client.build();

        ForgeConfigSpec.Builder common = new ForgeConfigSpec.Builder();
        common.comment(
                        "弾薬箱箱に付与するエンチャント効果の設定。",
                        "Ammo Box Case enchantment-effect settings.")
                .translation("config.big_ammo_box.category.enchantments")
                .push("enchantments");
        RESPECT_ENCHANTMENT_MAX_LEVEL = common
                .comment(
                        "ON: BABのエンチャント効果を通常の最大レベルまでに制限します。",
                        "OFF: Full Int Enchantやコマンド等で保存された実際のエンチャントLvを使用します。",
                        "OFFでも無効な演算や過剰なワールド走査を防ぐ技術的Safety Guardは維持されます。",
                        "ON: BAB effects clamp enchantments to their normal max level.",
                        "OFF: actual stored enchantment levels (including Full Int Enchant / commands) are used.",
                        "Technical safety guards remain active in either mode.")
                .translation("config.big_ammo_box.respectEnchantmentMaxLevel")
                .define("respectEnchantmentMaxLevel", true);
        ALLOW_INFINITY_ON_AMMO_CASES = common
                .comment(
                        "バニラの無限（Infinity）を弾薬箱箱へ付与できるようにします。",
                        "無限はCase内部に既に登録されている弾種だけを非消費化し、空Caseから弾種を生成しません。",
                        "Allow vanilla Infinity to be applied to Ammo Box Cases.",
                        "Infinity makes only ammo types already represented inside the case non-consuming.")
                .translation("config.big_ammo_box.allowInfinityOnAmmoCases")
                .define("allowInfinityOnAmmoCases", false);
        STELLAR_OVERDRIVE_DAMAGE_MULTIPLIER = common
                .comment(
                        "星核過給 I による最終射撃ダメージ倍率。",
                        "Final shooting damage multiplier from Stellar Overdrive I.")
                .translation("config.big_ammo_box.stellarOverdriveDamageMultiplier")
                .defineInRange("stellarOverdriveDamageMultiplier", 10.0D, 0.0D, 1.0E12D);
        common.pop();

        common.comment(
                        "自動リロードの待機時間設定。",
                        "Auto Reload timing settings.")
                .translation("config.big_ammo_box.category.autoReload")
                .push("autoReload");
        AUTO_RELOAD_IDLE_DELAY_TICKS = common
                .comment(
                        "実際に使用済みで、マガジンが一部残っている銃を自動リロードするまでの最終使用後tick数。",
                        "20 tick = 1秒。既定値200 = 10秒。空マガジンの即時リロード条件は変更しません。",
                        "Ticks after the last use/shot before a previously used, partially loaded gun is auto-reloaded.",
                        "20 ticks = 1 second. Default 200 = 10 seconds. Empty-magazine immediate reload behavior is unchanged.")
                .translation("config.big_ammo_box.idleDelayTicks")
                .defineInRange("idleDelayTicks", 200, 0, 72_000);
        common.pop();
        COMMON_SPEC = common.build();
    }

    private BigAmmoBoxConfig() {}
}
