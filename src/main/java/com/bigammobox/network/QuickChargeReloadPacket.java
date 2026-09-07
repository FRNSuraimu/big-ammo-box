package com.bigammobox.network;

import com.bigammobox.client.reload.ClientQuickChargeState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record QuickChargeReloadPacket(int entityId, float multiplier) {
    public static void encode(QuickChargeReloadPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId);
        buf.writeFloat(packet.multiplier);
    }

    public static QuickChargeReloadPacket decode(FriendlyByteBuf buf) {
        return new QuickChargeReloadPacket(buf.readVarInt(), buf.readFloat());
    }

    public static void handle(QuickChargeReloadPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientQuickChargeState.receive(packet.entityId, packet.multiplier)));
        context.setPacketHandled(true);
    }
}
