package com.bigammobox.network;

import com.bigammobox.BigAmmoBoxMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(BigAmmoBoxMod.MOD_ID, "main"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();

    private ModNetwork() {}

    public static void init() {
        int id = 0;
        CHANNEL.messageBuilder(SetCasePagePacket.class, id++)
                .encoder(SetCasePagePacket::encode)
                .decoder(SetCasePagePacket::decode)
                .consumerMainThread(SetCasePagePacket::handle)
                .add();
        CHANNEL.messageBuilder(QuickChargeReloadPacket.class, id++)
                .encoder(QuickChargeReloadPacket::encode)
                .decoder(QuickChargeReloadPacket::decode)
                .consumerMainThread(QuickChargeReloadPacket::handle)
                .add();
    }
}
