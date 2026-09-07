package com.bigammobox.client;

import com.bigammobox.BigAmmoBoxMod;
import com.bigammobox.client.reload.ClientQuickChargeState;
import com.bigammobox.enchantment.AmmoCaseEnchantments;
import com.tacz.guns.api.event.common.GunReloadEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BigAmmoBoxMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientForgeEvents {
    private ClientForgeEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onReload(GunReloadEvent event) {
        if (event.getLogicalSide() != LogicalSide.CLIENT || event.isCanceled()) return;
        if (!(event.getEntity() instanceof AbstractClientPlayer player)) return;

        Minecraft mc = Minecraft.getInstance();
        boolean local = mc.player != null && player.getId() == mc.player.getId();
        float speed;
        if (local) {
            // TaCZ fires the local CLIENT GunReloadEvent before it sends the server reload packet.
            // Predict once from the current client inventory now; later getReloadTime() calls reuse
            // this snapshot instead of live-rescanning cases mid-reload.
            speed = ClientQuickChargeState.beginLocal(player,
                    (float) AmmoCaseEnchantments.quickChargeMultiplier(player, event.getGunItemStack()));
        } else {
            speed = ClientQuickChargeState.beginRemote(player);
        }

        // TaCZ does not create its third-person once animation for the local player while the
        // camera is first-person. Do not wrap whatever stale/other once animation happens to exist.
        boolean localFirstPerson = local && mc.options.getCameraType().isFirstPerson();
        if (!localFirstPerson) ClientQuickChargeState.applyThirdPersonSpeed(player, speed);
    }
    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientQuickChargeState.clearAll();
    }
}
