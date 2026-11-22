package com.codespace.marvelzombie;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.joml.Vector3f;

import java.util.UUID;

public class SlingRingPortal extends Entity {
    private static final EntityDataAccessor<String> TARGET_DIMENSION =
            SynchedEntityData.defineId(SlingRingPortal.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<BlockPos> TARGET_POSITION =
            SynchedEntityData.defineId(SlingRingPortal.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Float> TARGET_YAW =
            SynchedEntityData.defineId(SlingRingPortal.class, EntityDataSerializers.FLOAT);
    private static final DustParticleOptions PORTAL_DUST =
            new DustParticleOptions(new Vector3f(1.0F, 0.42F, 0.08F), 1.0F);
    public static final String ACTIVE_PAIR_TAG = "MarvelZombieSlingRingActivePair";
    public static final int LIFETIME_TICKS = 1_200;
    private static final String TELEPORT_COOLDOWN_TAG = "MarvelZombieSlingRingCooldown";
    private static final String ENTITY_COOLDOWN_TAG = "MarvelZombieSlingRingEntityCooldown";
    private UUID ownerId;
    private UUID portalPairId;
    private long expiresAt;
    private int age;

    public SlingRingPortal(EntityType<? extends SlingRingPortal> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(TARGET_DIMENSION, Level.OVERWORLD.location().toString());
        entityData.define(TARGET_POSITION, BlockPos.ZERO);
        entityData.define(TARGET_YAW, 0.0F);
    }

    public void connectTo(ResourceKey<Level> dimension, Vec3 position, float yaw) {
        entityData.set(TARGET_DIMENSION, dimension.location().toString());
        entityData.set(TARGET_POSITION, BlockPos.containing(position));
        entityData.set(TARGET_YAW, yaw);
    }

    public void setOwner(UUID ownerId, UUID portalPairId, long expiresAt) {
        this.ownerId = ownerId;
        this.portalPairId = portalPairId;
        this.expiresAt = expiresAt;
    }

    public boolean isOwnedBy(UUID ownerId) {
        return ownerId.equals(this.ownerId);
    }

    public boolean belongsTo(UUID portalPairId) {
        return portalPairId.equals(this.portalPairId);
    }

    public float getPortalYaw() {
        return getYRot();
    }

    public ResourceLocation getTargetDimension() {
        ResourceLocation location = ResourceLocation.tryParse(entityData.get(TARGET_DIMENSION));
        return location == null ? Level.OVERWORLD.location() : location;
    }

    public BlockPos getTargetPosition() {
        return entityData.get(TARGET_POSITION);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !(level() instanceof ServerLevel sourceLevel)) {
            return;
        }

        if (ownerId != null) {
            ServerPlayer owner = sourceLevel.getServer().getPlayerList().getPlayer(ownerId);
            if (owner == null || portalPairId == null
                    || !owner.getPersistentData().hasUUID(ACTIVE_PAIR_TAG)
                    || !owner.getPersistentData().getUUID(ACTIVE_PAIR_TAG).equals(portalPairId)) {
                discard();
                return;
            }
        }

        if (sourceLevel.getServer().overworld().getGameTime() >= expiresAt
                || ++age >= LIFETIME_TICKS) {
            discard();
            return;
        }

        emitSparks(sourceLevel);
        for (ServerPlayer player : sourceLevel.getEntitiesOfClass(
                ServerPlayer.class, getBoundingBox().inflate(0.15D))) {
            if (player.getPersistentData().getLong(TELEPORT_COOLDOWN_TAG)
                    > sourceLevel.getGameTime() || !isWithinOpening(player)) {
                continue;
            }
            teleportThrough(player, sourceLevel);
            break;
        }

        for (Entity entity : sourceLevel.getEntitiesOfClass(
                Entity.class, getBoundingBox().inflate(0.25D),
                candidate -> candidate.isAlive()
                        && !(candidate instanceof ServerPlayer)
                        && !(candidate instanceof SlingRingPortal))) {
            if (entity.getPersistentData().getLong(ENTITY_COOLDOWN_TAG)
                    > sourceLevel.getGameTime() || !isWithinOpening(entity)) {
                continue;
            }
            teleportThrough(entity, sourceLevel);
        }
    }

    private boolean isWithinOpening(Entity entity) {
        double radians = Math.toRadians(getPortalYaw());
        double normalX = -Math.sin(radians);
        double normalZ = Math.cos(radians);
        double tangentX = Math.cos(radians);
        double tangentZ = Math.sin(radians);
        double offsetX = entity.getX() - getX();
        double offsetZ = entity.getZ() - getZ();
        double depth = Math.abs(offsetX * normalX + offsetZ * normalZ);
        double lateral = Math.abs(offsetX * tangentX + offsetZ * tangentZ);
        double vertical = Math.abs(entity.getY() + entity.getBbHeight() * 0.5D
                - (getY() + 1.38D));

        return depth <= entity.getBbWidth() * 0.5D + 0.18D
                && lateral <= 0.9D + entity.getBbWidth() * 0.5D
                && vertical <= 1.25D + entity.getBbHeight() * 0.5D;
    }

    private void emitSparks(ServerLevel level) {
        if (tickCount % 2 != 0) {
            return;
        }

        for (int i = 0; i < 3; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double localX = Math.cos(angle) * 0.92D;
            double y = getY() + 1.35D + Math.sin(angle) * 1.2D;
            double localZ = level.random.nextDouble() * 0.12D - 0.06D;
            double radians = Math.toRadians(getPortalYaw());
            double x = getX() + localX * Math.cos(radians) - localZ * Math.sin(radians);
            double z = getZ() + localX * Math.sin(radians) + localZ * Math.cos(radians);
            level.sendParticles(PORTAL_DUST, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            if (i == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        x, y, z, 2, 0.04D, 0.04D, 0.04D, 0.02D);
            }
        }
    }

    private void teleportThrough(ServerPlayer player, ServerLevel sourceLevel) {
        ResourceLocation location = ResourceLocation.tryParse(entityData.get(TARGET_DIMENSION));
        if (location == null) {
            discard();
            return;
        }

        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, location);
        ServerLevel destinationLevel = sourceLevel.getServer().getLevel(dimension);
        if (destinationLevel == null) {
            discard();
            return;
        }

        float yaw = entityData.get(TARGET_YAW);
        double radians = Math.toRadians(yaw);
        BlockPos targetPosition = entityData.get(TARGET_POSITION);
        double x = targetPosition.getX() + 0.5D - Math.sin(radians) * 1.35D;
        double y = targetPosition.getY() + 0.05D;
        double z = targetPosition.getZ() + 0.5D + Math.cos(radians) * 1.35D;
        AABB destinationBounds = player.getBoundingBox().move(
                x - player.getX(), y - player.getY(), z - player.getZ());
        if (!destinationLevel.noCollision(player, destinationBounds)) {
            return;
        }

        player.getPersistentData().putLong(TELEPORT_COOLDOWN_TAG,
                destinationLevel.getGameTime() + 40L);
        player.teleportTo(destinationLevel, x, y, z, yaw, player.getXRot());
        destinationLevel.playSound(null, x, y, z,
                net.minecraft.sounds.SoundEvents.PORTAL_TRAVEL,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.7F, 1.25F);
    }

    private void teleportThrough(Entity entity, ServerLevel sourceLevel) {
        ResourceLocation location = ResourceLocation.tryParse(entityData.get(TARGET_DIMENSION));
        if (location == null) {
            return;
        }

        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, location);
        ServerLevel destinationLevel = sourceLevel.getServer().getLevel(dimension);
        if (destinationLevel == null) {
            return;
        }

        float yaw = entityData.get(TARGET_YAW);
        double radians = Math.toRadians(yaw);
        BlockPos targetPosition = entityData.get(TARGET_POSITION);
        double x = targetPosition.getX() + 0.5D - Math.sin(radians) * 1.35D;
        double y = targetPosition.getY() + 0.05D;
        double z = targetPosition.getZ() + 0.5D + Math.cos(radians) * 1.35D;
        AABB destinationBounds = entity.getBoundingBox().move(
                x - entity.getX(), y - entity.getY(), z - entity.getZ());
        if (!destinationLevel.noCollision(entity, destinationBounds)) {
            return;
        }

        Entity transported = entity;
        if (entity.level().dimension() != dimension) {
            Entity changed = entity.changeDimension(destinationLevel);
            if (changed == null) {
                return;
            }
            transported = changed;
        }

        transported.getPersistentData().putLong(ENTITY_COOLDOWN_TAG,
                destinationLevel.getGameTime() + 40L);
        transported.teleportTo(x, y, z);
        transported.setYRot(yaw);
        transported.setYHeadRot(yaw);
        destinationLevel.playSound(null, x, y, z,
                net.minecraft.sounds.SoundEvents.PORTAL_TRAVEL,
                net.minecraft.sounds.SoundSource.BLOCKS, 0.45F, 1.45F);
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        entityData.set(TARGET_DIMENSION, tag.getString("TargetDimension"));
        entityData.set(TARGET_POSITION, BlockPos.of(tag.getLong("TargetPosition")));
        entityData.set(TARGET_YAW, tag.getFloat("TargetYaw"));
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        portalPairId = tag.hasUUID("PortalPair") ? tag.getUUID("PortalPair") : null;
        expiresAt = tag.getLong("ExpiresAt");
        age = tag.getInt("Age");
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        tag.putString("TargetDimension", entityData.get(TARGET_DIMENSION));
        tag.putLong("TargetPosition", entityData.get(TARGET_POSITION).asLong());
        tag.putFloat("TargetYaw", entityData.get(TARGET_YAW));
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        if (portalPairId != null) {
            tag.putUUID("PortalPair", portalPairId);
        }
        tag.putLong("ExpiresAt", expiresAt);
        tag.putInt("Age", age);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
