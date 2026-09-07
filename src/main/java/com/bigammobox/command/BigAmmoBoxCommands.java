package com.bigammobox.command;

import com.bigammobox.BigAmmoBoxMod;
import com.bigammobox.item.BigAmmoBoxItem;
import com.mojang.brigadier.CommandDispatcher;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.math.BigInteger;
import java.util.Map;

@Mod.EventBusSubscriber(modid = BigAmmoBoxMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BigAmmoBoxCommands {
    private BigAmmoBoxCommands() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("bigammobox")
                .requires(source -> source.hasPermission(2));

        for (FillMode mode : FillMode.values()) {
            root.then(Commands.literal(mode.literal)
                    .executes(ctx -> setCurrent(ctx.getSource(), mode)));
        }

        var ammo = Commands.argument("ammo", ResourceLocationArgument.id())
                .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                        TimelessAPI.getAllCommonAmmoIndex().stream().map(Map.Entry::getKey), builder));
        for (FillMode mode : FillMode.values()) {
            ammo.then(Commands.literal(mode.literal)
                    .executes(ctx -> setSpecified(ctx.getSource(),
                            ResourceLocationArgument.getId(ctx, "ammo"), mode)));
        }
        root.then(ammo);
        dispatcher.register(root);
    }

    private static int setCurrent(CommandSourceStack source, FillMode mode) {
        ServerPlayer player = player(source);
        if (player == null) return 0;

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof BigAmmoBoxItem box)) {
            source.sendFailure(Component.translatable("command.big_ammo_box.hold_box"));
            return 0;
        }

        ResourceLocation ammoId = box.getAmmoId(stack);
        if (ammoId == null || DefaultAssets.EMPTY_AMMO_ID.equals(ammoId)) {
            source.sendFailure(Component.translatable("command.big_ammo_box.no_ammo_type"));
            return 0;
        }
        if (TimelessAPI.getCommonAmmoIndex(ammoId).isEmpty()) {
            source.sendFailure(Component.translatable("command.big_ammo_box.invalid_ammo", ammoId.toString()));
            return 0;
        }

        return apply(source, player, stack, box, ammoId, mode);
    }

    private static int setSpecified(CommandSourceStack source, ResourceLocation ammoId, FillMode mode) {
        ServerPlayer player = player(source);
        if (player == null) return 0;

        if (TimelessAPI.getCommonAmmoIndex(ammoId).isEmpty()) {
            source.sendFailure(Component.translatable("command.big_ammo_box.invalid_ammo", ammoId.toString()));
            return 0;
        }

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof BigAmmoBoxItem box)) {
            source.sendFailure(Component.translatable("command.big_ammo_box.hold_box"));
            return 0;
        }

        box.setAmmoId(stack, ammoId);
        return apply(source, player, stack, box, ammoId, mode);
    }

    private static int apply(CommandSourceStack source, ServerPlayer player, ItemStack stack,
                             BigAmmoBoxItem box, ResourceLocation ammoId, FillMode mode) {
        BigInteger capacity = box.capacityBigFor(ammoId);
        BigInteger target = mode.target(capacity);
        box.setExactAmmoCount(stack, target);
        syncHeldStack(player);
        source.sendSuccess(() -> Component.translatable(mode.messageKey,
                stack.getHoverName(), ammoId.toString(), compact(target)), false);
        return 1;
    }

    private static ServerPlayer player(CommandSourceStack source) {
        try {
            return source.getPlayerOrException();
        } catch (Exception ex) {
            source.sendFailure(Component.translatable("command.big_ammo_box.player_only"));
            return null;
        }
    }

    private static void syncHeldStack(ServerPlayer player) {
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
    }

    private static String compact(BigInteger value) {
        if (value.toString().length() <= 19) return value.toString();
        return BigAmmoBoxItem.scientific(value, 6);
    }

    private enum FillMode {
        ZERO("ZERO", "command.big_ammo_box.zeroed") {
            @Override BigInteger target(BigInteger capacity) { return BigInteger.ZERO; }
        },
        HALF("HALF", "command.big_ammo_box.halved") {
            @Override BigInteger target(BigInteger capacity) { return capacity.max(BigInteger.ZERO).shiftRight(1); }
        },
        MAX("MAX", "command.big_ammo_box.filled") {
            @Override BigInteger target(BigInteger capacity) { return capacity.max(BigInteger.ZERO); }
        };

        private final String literal;
        private final String messageKey;

        FillMode(String literal, String messageKey) {
            this.literal = literal;
            this.messageKey = messageKey;
        }

        abstract BigInteger target(BigInteger capacity);
    }
}
