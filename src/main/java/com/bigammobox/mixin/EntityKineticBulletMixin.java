package com.bigammobox.mixin;

import com.bigammobox.enchantment.AmmoCaseEnchantments;
import com.bigammobox.access.BabBulletAccess;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EntityKineticBullet.class, remap = false)
public abstract class EntityKineticBulletMixin implements BabBulletAccess {
    @Shadow private int pierce;
    @Shadow private float armorIgnore;
    @Shadow private float shotDamageMultiplier;

    @Unique private float bigAmmoBox$damageMultiplier = 1.0F;
    @Unique private float bigAmmoBox$headshotBonus = 0.0F;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/resources/ResourceLocation;ZLcom/tacz/guns/resource/pojo/data/gun/GunData;Lcom/tacz/guns/resource/pojo/data/gun/BulletData;)V",
            at = @At("TAIL"), require = 0)
    private void bigAmmoBox$applyCaseBallistics(EntityType<? extends Projectile> type, Level level,
                                                 LivingEntity shooter, ItemStack gunStack,
                                                 ResourceLocation gunId, ResourceLocation gunDisplayId,
                                                 ResourceLocation ammoId, boolean tracer,
                                                 GunData gunData, BulletData bulletData,
                                                 CallbackInfo ci) {
        AmmoCaseEnchantments.Effects effects = AmmoCaseEnchantments.resolveShootingEffects(shooter, gunStack);
        if (effects == AmmoCaseEnchantments.Effects.NONE) return;

        long pierceValue = (long) this.pierce + Math.max(0L, effects.piercingLevel());
        this.pierce = (int) Math.min(Integer.MAX_VALUE, Math.max(0L, pierceValue));

        double armor = this.armorIgnore + effects.armorIgnoreBonus();
        if (!Double.isFinite(armor)) armor = 1.0D;
        this.armorIgnore = (float) Math.min(1.0D, Math.max(0.0D, armor));

        this.bigAmmoBox$damageMultiplier = bigAmmoBox$safeFloat(effects.finalDamageMultiplier(), 1.0F);
        this.bigAmmoBox$headshotBonus = bigAmmoBox$safeFloat(effects.headshotMultiplierBonus(), 0.0F);
        this.shotDamageMultiplier = bigAmmoBox$multiply(this.shotDamageMultiplier, this.bigAmmoBox$damageMultiplier);
    }

    @Inject(method = "setShotDamageMultiplier(F)V", at = @At("TAIL"), require = 0)
    private void bigAmmoBox$applyDamageMultiplierAfterTaCZ(float rawMultiplier, CallbackInfo ci) {
        if (this.bigAmmoBox$damageMultiplier == 1.0F) return;
        this.shotDamageMultiplier = bigAmmoBox$multiply(rawMultiplier, this.bigAmmoBox$damageMultiplier);
    }

    @Override
    public float bigAmmoBox$getHeadshotMultiplierBonus() {
        return this.bigAmmoBox$headshotBonus;
    }

    @Unique
    private static float bigAmmoBox$multiply(float left, float right) {
        double value = (double) left * (double) right;
        if (!Double.isFinite(value)) return Float.MAX_VALUE;
        if (value > Float.MAX_VALUE) return Float.MAX_VALUE;
        if (value < -Float.MAX_VALUE) return -Float.MAX_VALUE;
        return (float) value;
    }

    @Unique
    private static float bigAmmoBox$safeFloat(double value, float fallback) {
        if (!Double.isFinite(value)) return value > 0 ? Float.MAX_VALUE : fallback;
        if (value > Float.MAX_VALUE) return Float.MAX_VALUE;
        if (value < -Float.MAX_VALUE) return -Float.MAX_VALUE;
        return (float) value;
    }
}
