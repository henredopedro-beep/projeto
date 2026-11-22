package com.codespace.marvelzombie;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

public final class ThorNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MarvelZombieMod.MOD_ID, "thor_controls"),
            () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

    private ThorNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(0, ThorActionPacket.class, ThorActionPacket::encode,
                ThorActionPacket::decode, ThorActionPacket::handle);
    }

    public static void sendAction(Action action) {
        CHANNEL.sendToServer(new ThorActionPacket(action));
    }

    private static void handle(ServerPlayer player, Action action) {
        if (action == Action.RECALL_MJOLNIR) {
            if (!ThorEnergy.isWorthy(player)) {
                return;
            }
            player.level().getEntitiesOfClass(ThrownMjolnir.class,
                    player.getBoundingBox().inflate(256.0D),
                    mjolnir -> mjolnir.getOwner() == player
                            && ThorEnergy.isBoundMjolnir(player, mjolnir.getItem()))
                    .forEach(ThrownMjolnir::recall);
            return;
        }

        if (action != Action.THROW_MJOLNIR || !holdsMjolnir(player) || !player.isCrouching()
                || !ThorEnergy.isWorthy(player)
                || player.getCooldowns().isOnCooldown(MarvelZombieMod.MJOLNIR.get())) {
            return;
        }
        InteractionHand hand = player.getMainHandItem().is(MarvelZombieMod.MJOLNIR.get())
                ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        ItemStack stack = player.getItemInHand(hand);
        ThrownMjolnir projectile = new ThrownMjolnir(player.level(), player, stack);
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), -7.0F, 1.7F, 0.04F);
        Vec3 look = player.getLookAngle();
        projectile.setPos(player.getX() + look.x * 0.8D,
                player.getEyeY() - 0.2D + look.y * 0.8D,
                player.getZ() + look.z * 0.8D);
        if (player.level().addFreshEntity(projectile)) {
            player.setItemInHand(hand, ItemStack.EMPTY);
            player.getCooldowns().addCooldown(MarvelZombieMod.MJOLNIR.get(), 40);
            player.level().playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW,
                    SoundSource.PLAYERS, 1.0F, 0.7F);
        }
    }

    private static boolean holdsMjolnir(ServerPlayer player) {
        return player.getMainHandItem().is(MarvelZombieMod.MJOLNIR.get())
                || player.getOffhandItem().is(MarvelZombieMod.MJOLNIR.get());
    }

    public enum Action {
        THROW_MJOLNIR,
        RECALL_MJOLNIR
    }

    private record ThorActionPacket(Action action) {
        private static void encode(ThorActionPacket packet, FriendlyByteBuf buffer) {
            buffer.writeEnum(packet.action);
        }

        private static ThorActionPacket decode(FriendlyByteBuf buffer) {
            return new ThorActionPacket(buffer.readEnum(Action.class));
        }

        private static void handle(ThorActionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player != null) {
                    ThorNetwork.handle(player, packet.action);
                }
            });
            context.setPacketHandled(true);
        }
    }
}
