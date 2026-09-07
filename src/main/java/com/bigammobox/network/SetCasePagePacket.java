package com.bigammobox.network;

import com.bigammobox.menu.AmmoBoxCaseMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SetCasePagePacket(int containerId, int page) {
    public static void encode(SetCasePagePacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.containerId);
        buf.writeVarInt(msg.page);
    }

    public static SetCasePagePacket decode(FriendlyByteBuf buf) {
        return new SetCasePagePacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(SetCasePagePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        if (ctx.getSender() != null
                && ctx.getSender().containerMenu.containerId == msg.containerId
                && ctx.getSender().containerMenu instanceof AmmoBoxCaseMenu menu) {
            menu.setPage(msg.page);
        }
        ctx.setPacketHandled(true);
    }
}
