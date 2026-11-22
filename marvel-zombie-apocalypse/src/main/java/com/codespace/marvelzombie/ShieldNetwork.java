package com.codespace.marvelzombie;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

public final class ShieldNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MarvelZombieMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private ShieldNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(0, ThrowShieldPacket.class,
                ThrowShieldPacket::encode, ThrowShieldPacket::decode, ThrowShieldPacket::handle);
    }

    public static void throwShield() {
        CHANNEL.sendToServer(new ThrowShieldPacket());
    }

    private static void throwShield(ServerPlayer player) {
        if (!player.isCrouching()
                || player.getCooldowns().isOnCooldown(MarvelZombieMod.CAPTAIN_SHIELD.get())) {
            return;
        }

        InteractionHand hand;
        if (player.getMainHandItem().is(MarvelZombieMod.CAPTAIN_SHIELD.get())) {
            hand = InteractionHand.MAIN_HAND;
        } else if (player.getOffhandItem().is(MarvelZombieMod.CAPTAIN_SHIELD.get())) {
            hand = InteractionHand.OFF_HAND;
        } else {
            return;
        }

        ItemStack shieldStack = player.getItemInHand(hand);
        ThrownCaptainShield shield = new ThrownCaptainShield(player.level(), player);
        shield.setItem(shieldStack);
        shield.shootFromRotation(player, player.getXRot(), player.getYRot(), -7.0F, 1.8F, 0.05F);
        Vec3 look = player.getLookAngle();
        shield.setPos(player.getX() + look.x * 0.8D,
                player.getEyeY() - 0.2D + look.y * 0.8D,
                player.getZ() + look.z * 0.8D);

        if (player.level().addFreshEntity(shield)) {
            player.setItemInHand(hand, ItemStack.EMPTY);
            player.getCooldowns().addCooldown(MarvelZombieMod.CAPTAIN_SHIELD.get(), 40);
        }
    }

    private static class ThrowShieldPacket {
        private static void encode(ThrowShieldPacket packet, FriendlyByteBuf buffer) {
        }

        private static ThrowShieldPacket decode(FriendlyByteBuf buffer) {
            return new ThrowShieldPacket();
        }

        private static void handle(ThrowShieldPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player != null) {
                    ShieldNetwork.throwShield(player);
                }
            });
            context.setPacketHandled(true);
        }
    }
}
