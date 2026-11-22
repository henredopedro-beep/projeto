package com.codespace.marvelzombie;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.fml.DistExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = MarvelZombieMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SlingRingNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static final int CREATION_COOLDOWN_TICKS = 600;
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MarvelZombieMod.MOD_ID, "sling_ring"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private SlingRingNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(0, OpenScreenPacket.class,
                OpenScreenPacket::encode, OpenScreenPacket::decode, OpenScreenPacket::handle);
        CHANNEL.registerMessage(1, CreatePortalsPacket.class,
                CreatePortalsPacket::encode, CreatePortalsPacket::decode, CreatePortalsPacket::handle);
    }

    public static void openScreen(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenScreenPacket());
    }

    public static void requestPortals(int x, int y, int z, ResourceLocation dimension) {
        CHANNEL.sendToServer(new CreatePortalsPacket(x, y, z, dimension));
    }

    private static void createPortals(ServerPlayer player, CreatePortalsPacket packet) {
        if (!holdsRing(player)) {
            return;
        }
        if (player.getCooldowns().isOnCooldown(MarvelZombieMod.SLING_RING.get())) {
            player.sendSystemMessage(Component.translatable(
                    "message.marvel_zombie.sling_ring.cooldown"));
            return;
        }

        ResourceLocation dimensionLocation = packet.dimension();
        if (!isSupportedDimension(dimensionLocation)) {
            player.sendSystemMessage(Component.translatable(
                    "message.marvel_zombie.sling_ring.dimension_unavailable"));
            return;
        }

        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionLocation);
        ServerLevel destinationLevel = player.serverLevel().getServer().getLevel(dimension);
        if (destinationLevel == null) {
            player.sendSystemMessage(Component.translatable(
                    "message.marvel_zombie.sling_ring.dimension_unavailable"));
            return;
        }

        BlockPos requested = new BlockPos(packet.x(), packet.y(), packet.z());
        if (packet.y() <= destinationLevel.getMinBuildHeight()
                || packet.y() >= destinationLevel.getMaxBuildHeight() - 2
                || !destinationLevel.getWorldBorder().isWithinBounds(requested)) {
            player.sendSystemMessage(Component.translatable(
                    "message.marvel_zombie.sling_ring.invalid_coordinates"));
            return;
        }

        float portalYaw = player.getYRot() + 180.0F;
        Integer safeY = findSafeY(
                player, destinationLevel, packet.x(), packet.y(), packet.z(), portalYaw);
        if (safeY == null) {
            player.sendSystemMessage(Component.translatable(
                    "message.marvel_zombie.sling_ring.unsafe_destination"));
            return;
        }

        Vec3 horizontalLook = new Vec3(player.getLookAngle().x, 0.0D, player.getLookAngle().z);
        if (horizontalLook.lengthSqr() < 0.01D) {
            horizontalLook = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            horizontalLook = horizontalLook.normalize();
        }
        Vec3 entrance = player.position().add(horizontalLook.scale(1.7D));
        Vec3 exit = new Vec3(packet.x() + 0.5D, safeY, packet.z() + 0.5D);
        if (dimension.equals(player.level().dimension())
                && exit.distanceTo(player.position()) < 4.0D) {
            player.sendSystemMessage(Component.translatable(
                    "message.marvel_zombie.sling_ring.unsafe_destination"));
            return;
        }

        UUID portalPairId = UUID.randomUUID();
        long expiresAt = player.serverLevel().getServer().overworld().getGameTime()
                + SlingRingPortal.LIFETIME_TICKS;
        SlingRingPortal entrancePortal = createPortal(
                player.level(), entrance, portalYaw, destinationLevel.dimension(), exit, portalYaw,
                player.getUUID(), portalPairId, expiresAt);
        SlingRingPortal exitPortal = createPortal(
                destinationLevel, exit, portalYaw, player.level().dimension(), entrance, portalYaw,
                player.getUUID(), portalPairId, expiresAt);

        if (!destinationLevel.addFreshEntity(exitPortal)) {
            player.sendSystemMessage(Component.translatable(
                    "message.marvel_zombie.sling_ring.dimension_unavailable"));
            return;
        }
        if (!player.level().addFreshEntity(entrancePortal)) {
            exitPortal.discard();
            player.sendSystemMessage(Component.translatable(
                    "message.marvel_zombie.sling_ring.dimension_unavailable"));
            return;
        }

        player.getPersistentData().putUUID(SlingRingPortal.ACTIVE_PAIR_TAG, portalPairId);
        closeOwnedPortals(player.serverLevel().getServer(), player.getUUID(), portalPairId);
        player.level().playSound(null, entrance.x, entrance.y + 1.3D, entrance.z,
                net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 0.7F);
        destinationLevel.playSound(null, exit.x, exit.y + 1.3D, exit.z,
                net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 0.7F);
        player.sendSystemMessage(Component.translatable(
                "message.marvel_zombie.sling_ring.opened"));
        player.getCooldowns().addCooldown(
                MarvelZombieMod.SLING_RING.get(), CREATION_COOLDOWN_TICKS);
    }

    private static SlingRingPortal createPortal(Level level, Vec3 position, float yaw,
                                                 ResourceKey<Level> targetDimension,
                                                 Vec3 targetPosition, float targetYaw,
                                                 UUID ownerId, UUID portalPairId, long expiresAt) {
        SlingRingPortal portal = new SlingRingPortal(MarvelZombieMod.SLING_RING_PORTAL.get(), level);
        portal.setPos(position.x, position.y, position.z);
        portal.setYRot(yaw);
        portal.setYBodyRot(yaw);
        portal.setYHeadRot(yaw);
        portal.connectTo(targetDimension, targetPosition, targetYaw);
        portal.setOwner(ownerId, portalPairId, expiresAt);
        return portal;
    }

    private static void closeOwnedPortals(MinecraftServer server, UUID ownerId, UUID keepPairId) {
        List<SlingRingPortal> portalsToClose = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof SlingRingPortal portal && portal.isOwnedBy(ownerId)
                        && (keepPairId == null || !portal.belongsTo(keepPairId))) {
                    portalsToClose.add(portal);
                }
            }
        }
        portalsToClose.forEach(Entity::discard);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            closeOwnedPortals(player.serverLevel().getServer(), player.getUUID(), null);
            player.getPersistentData().remove(SlingRingPortal.ACTIVE_PAIR_TAG);
        }
    }

    private static Integer findSafeY(ServerPlayer player, ServerLevel level,
                                     int x, int y, int z, float portalYaw) {
        for (int offset = 0; offset <= 12; offset++) {
            int candidateY = y + offset;
            BlockPos feet = new BlockPos(x, candidateY, z);
            BlockPos floor = feet.below();
            if (candidateY + 3 >= level.getMaxBuildHeight()
                    || level.getBlockState(floor).getCollisionShape(level, floor).isEmpty()) {
                continue;
            }

            double radians = Math.toRadians(portalYaw);
            double arrivalX = x + 0.5D - Math.sin(radians) * 1.35D;
            double arrivalZ = z + 0.5D + Math.cos(radians) * 1.35D;
            if (!level.getWorldBorder().isWithinBounds(
                    BlockPos.containing(arrivalX, candidateY, arrivalZ))) {
                continue;
            }
            AABB targetBounds = player.getBoundingBox().move(
                    arrivalX - player.getX(), candidateY - player.getY(),
                    arrivalZ - player.getZ());
            if (level.noCollision(player, targetBounds)) {
                return candidateY;
            }
        }
        return null;
    }

    private static boolean holdsRing(ServerPlayer player) {
        return player.getMainHandItem().is(MarvelZombieMod.SLING_RING.get())
                || player.getOffhandItem().is(MarvelZombieMod.SLING_RING.get());
    }

    private static boolean isSupportedDimension(ResourceLocation dimension) {
        return dimension != null
                && "minecraft".equals(dimension.getNamespace())
                && ("overworld".equals(dimension.getPath())
                || "the_nether".equals(dimension.getPath())
                || "the_end".equals(dimension.getPath()));
    }

    private record OpenScreenPacket() {
        private static void encode(OpenScreenPacket packet, FriendlyByteBuf buffer) {
        }

        private static OpenScreenPacket decode(FriendlyByteBuf buffer) {
            return new OpenScreenPacket();
        }

        private static void handle(OpenScreenPacket packet,
                                   Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                    net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> SlingRingClient::openScreen));
            context.setPacketHandled(true);
        }
    }

    private record CreatePortalsPacket(int x, int y, int z, ResourceLocation dimension) {
        private static void encode(CreatePortalsPacket packet, FriendlyByteBuf buffer) {
            buffer.writeInt(packet.x);
            buffer.writeInt(packet.y);
            buffer.writeInt(packet.z);
            buffer.writeResourceLocation(packet.dimension);
        }

        private static CreatePortalsPacket decode(FriendlyByteBuf buffer) {
            return new CreatePortalsPacket(
                    buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readResourceLocation());
        }

        private static void handle(CreatePortalsPacket packet,
                                   Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player != null) {
                    createPortals(player, packet);
                }
            });
            context.setPacketHandled(true);
        }
    }
}
