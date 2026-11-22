package com.codespace.marvelzombie;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class ThrownMjolnir extends ThrowableItemProjectile {
    private static final EntityDataAccessor<Boolean> RETURNING =
            SynchedEntityData.defineId(ThrownMjolnir.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> STUCK =
            SynchedEntityData.defineId(ThrownMjolnir.class, EntityDataSerializers.BOOLEAN);
    private int age;
    private boolean returnsItem = true;

    public ThrownMjolnir(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
    }

    public ThrownMjolnir(Level level, LivingEntity owner, ItemStack item) {
        this(level, owner, item, true);
    }

    public ThrownMjolnir(Level level, LivingEntity owner, ItemStack item, boolean returnsItem) {
        super(MarvelZombieMod.THROWN_MJOLNIR.get(), owner, level);
        setItem(item.copy());
        this.returnsItem = returnsItem;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(RETURNING, false);
        entityData.define(STUCK, false);
    }

    @Override
    protected Item getDefaultItem() {
        return MarvelZombieMod.MJOLNIR.get();
    }

    private boolean isReturning() {
        return entityData.get(RETURNING);
    }

    private void setReturning(boolean returning) {
        entityData.set(RETURNING, returning);
        noPhysics = returning;
    }

    private boolean isStuck() {
        return entityData.get(STUCK);
    }

    private void setStuck() {
        entityData.set(STUCK, true);
        entityData.set(RETURNING, false);
        noPhysics = true;
        setDeltaMovement(Vec3.ZERO);
    }

    public boolean tryPickup(Player player) {
        if (!isStuck() || !canBePickedUpBy(player)) {
            return false;
        }
        ThorEnergy.grantWorthiness(player, getItem());
        player.getInventory().placeItemBackInInventory(getItem().copy(), true);
        discard();
        return true;
    }

    private boolean canBePickedUpBy(Player player) {
        ItemStack stack = getItem();
        if (stack.hasTag() && stack.getTag().hasUUID(ThorEnergy.MJOLNIR_OWNER_ID_TAG)) {
            return stack.getTag().getUUID(ThorEnergy.MJOLNIR_OWNER_ID_TAG)
                    .equals(player.getUUID());
        }
        return ThorEnergy.isWorthy(player);
    }

    public void recall() {
        if (isStuck()) {
            entityData.set(STUCK, false);
            setReturning(true);
        }
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

        if (isStuck()) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }

        if (++age > 160) {
            returnToOwner();
            return;
        }

        if (isReturning()) {
            returnToOwnerIfClose();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity entity = result.getEntity();
        if (!(entity instanceof LivingEntity living) || entity == getOwner()
                || !living.isAlive() || isReturning()) {
            return;
        }

        Entity owner = getOwner();
        boolean charged = owner instanceof Player player && ThorEnergy.getEnergy(player) > 0;
        living.hurt(level().damageSources().thrown(this, owner), charged ? 8.0F : 4.0F);
        if (charged && owner instanceof Player player) {
            living.invulnerableTime = 0;
            living.hurt(level().damageSources().lightningBolt(), 20.0F);
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 10));
            LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level());
            if (lightning != null) {
                lightning.moveTo(living.getX(), living.getY(), living.getZ());
                if (player instanceof ServerPlayer serverPlayer) {
                    lightning.setCause(serverPlayer);
                }
                level().addFreshEntity(lightning);
            }
            if (level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                        living.getX(), living.getY() + living.getBbHeight() * 0.5D, living.getZ(),
                        18, 0.35D, 0.45D, 0.35D, 0.08D);
            }
        }
        living.knockback(charged ? 1.3D : 0.65D, getX() - entity.getX(), getZ() - entity.getZ());
        setReturning(true);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (!isReturning()) {
            if (!returnsItem) {
                discard();
                return;
            }
            setStuck();
        }
    }

    private void returnToOwnerIfClose() {
        if (!(getOwner() instanceof LivingEntity owner) || !owner.isAlive()) {
            returnToOwner();
            return;
        }

        Vec3 destination = owner.getEyePosition().subtract(0.0D, 0.25D, 0.0D);
        Vec3 direction = destination.subtract(position());
        if (direction.lengthSqr() < 2.25D) {
            returnToOwner();
            return;
        }

        setDeltaMovement(direction.normalize().scale(1.55D));
    }

    private void returnToOwner() {
        ItemStack stack = getItem();
        if (returnsItem && !stack.isEmpty()) {
            if (getOwner() instanceof Player player && player.isAlive()) {
                player.getInventory().placeItemBackInInventory(stack.copy(), true);
            } else {
                spawnAtLocation(stack.copy());
            }
        }
        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("Mjolnir", getItem().save(new CompoundTag()));
        tag.putBoolean("Returning", isReturning());
        tag.putBoolean("Stuck", isStuck());
        tag.putInt("Age", age);
        tag.putBoolean("ReturnsItem", returnsItem);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setItem(ItemStack.of(tag.getCompound("Mjolnir")));
        setReturning(tag.getBoolean("Returning"));
        if (tag.getBoolean("Stuck")) {
            setStuck();
        }
        age = tag.getInt("Age");
        returnsItem = !tag.contains("ReturnsItem") || tag.getBoolean("ReturnsItem");
    }
}
