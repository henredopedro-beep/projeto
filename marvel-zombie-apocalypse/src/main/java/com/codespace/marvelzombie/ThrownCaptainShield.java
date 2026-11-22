package com.codespace.marvelzombie;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class ThrownCaptainShield extends ThrowableProjectile {
    private static final EntityDataAccessor<ItemStack> ITEM =
            SynchedEntityData.defineId(ThrownCaptainShield.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Boolean> RETURNING =
            SynchedEntityData.defineId(ThrownCaptainShield.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> RICOCHETED =
            SynchedEntityData.defineId(ThrownCaptainShield.class, EntityDataSerializers.BOOLEAN);
    private int returnDelay = 8;
    private int age;
    private UUID ricochetTargetId;

    public ThrownCaptainShield(EntityType<? extends ThrowableProjectile> type, Level level) {
        super(type, level);
    }

    public ThrownCaptainShield(Level level, LivingEntity owner) {
        super(MarvelZombieMod.THROWN_CAPTAIN_SHIELD.get(), owner, level);
        this.setOwner(owner);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(ITEM, ItemStack.EMPTY);
        this.entityData.define(RETURNING, false);
        this.entityData.define(RICOCHETED, false);
    }

    public ItemStack getItem() {
        return this.entityData.get(ITEM);
    }

    public void setItem(ItemStack stack) {
        this.entityData.set(ITEM, stack.copy());
    }

    public void setReturnDelay(int ticks) {
        returnDelay = Math.max(1, ticks);
    }

    public void setRicochetTarget(LivingEntity target) {
        ricochetTargetId = target.getUUID();
    }

    private boolean isReturning() {
        return this.entityData.get(RETURNING);
    }

    private boolean hasRicocheted() {
        return this.entityData.get(RICOCHETED);
    }

    private void setReturning(boolean returning) {
        this.entityData.set(RETURNING, returning);
        this.noPhysics = returning;
    }

    @Override
    protected float getGravity() {
        return isReturning() ? 0.0F : 0.025F;
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            return;
        }

        age++;
        if (age > 100) {
            returnToOwner();
            return;
        }

        if (returnDelay > 0 && !isReturning()) {
            returnDelay--;
        } else if (getOwner() instanceof LivingEntity owner && owner.isAlive()) {
            setReturning(true);
            Vec3 ownerPosition = owner.position().add(0.0D, owner.getEyeHeight() * 0.5D, 0.0D);
            Vec3 direction = ownerPosition.subtract(position()).normalize();
            setDeltaMovement(direction.scale(1.35D));

            if (distanceToSqr(owner) < 2.25D) {
                returnToOwner();
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        Entity owner = getOwner();
        if (target instanceof LivingEntity living && target != owner && !isReturning()) {
            living.hurt(level().damageSources().thrown(this, owner), 10.0F);
            living.knockback(0.5D,
                    getX() - target.getX(),
                    getZ() - target.getZ());
            setReturning(true);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (!isReturning()) {
            level().playSound(null, result.getBlockPos(), SoundEvents.METAL_HIT,
                    SoundSource.PLAYERS, 0.9F, 1.25F);
            if (ricochetTargetId != null && !hasRicocheted()) {
                if (level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.CRIT, result.getLocation().x,
                            result.getLocation().y, result.getLocation().z,
                            12, 0.2D, 0.2D, 0.2D, 0.12D);
                }
                ricochetTowardTarget(result);
            } else {
                setReturning(true);
            }
        }
    }

    private void ricochetTowardTarget(BlockHitResult hit) {
        Entity target = level() instanceof ServerLevel serverLevel
                ? serverLevel.getEntity(ricochetTargetId) : null;
        if (!(target instanceof LivingEntity living) || !living.isAlive()) {
            setReturning(true);
            return;
        }

        entityData.set(RICOCHETED, true);
        if (hit != null) {
            Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
            setPos(getX() + normal.x * 0.08D,
                    getY() + normal.y * 0.08D,
                    getZ() + normal.z * 0.08D);
        }
        Vec3 direction = living.getEyePosition().subtract(position()).normalize();
        setDeltaMovement(direction.scale(1.7D));
        hasImpulse = true;
    }

    private void returnToOwner() {
        if (!getItem().isEmpty()) {
            if (getOwner() instanceof Player player && player.isAlive()) {
                player.getInventory().placeItemBackInInventory(getItem(), true);
            } else if (getOwner() instanceof CaptainZombie captain && captain.isAlive()) {
                // Captain zombies keep their shield equipped while its thrown copy is in flight.
            } else {
                spawnAtLocation(getItem());
            }
        }
        discard();
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("Shield", getItem().save(new net.minecraft.nbt.CompoundTag()));
        tag.putBoolean("Returning", isReturning());
        tag.putBoolean("Ricocheted", hasRicocheted());
        tag.putInt("ReturnDelay", returnDelay);
        tag.putInt("Age", age);
        if (ricochetTargetId != null) {
            tag.putUUID("RicochetTarget", ricochetTargetId);
        }
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setItem(ItemStack.of(tag.getCompound("Shield")));
        setReturning(tag.getBoolean("Returning"));
        entityData.set(RICOCHETED, tag.getBoolean("Ricocheted"));
        returnDelay = tag.getInt("ReturnDelay");
        age = tag.getInt("Age");
        ricochetTargetId = tag.hasUUID("RicochetTarget") ? tag.getUUID("RicochetTarget") : null;
    }
}
