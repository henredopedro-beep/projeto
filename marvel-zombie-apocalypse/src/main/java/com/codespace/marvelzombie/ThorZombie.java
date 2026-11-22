package com.codespace.marvelzombie;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.phys.Vec3;

public class ThorZombie extends Zombie {
    private static final EntityDataAccessor<Boolean> BEAM_FIRING =
            SynchedEntityData.defineId(ThorZombie.class, EntityDataSerializers.BOOLEAN);
    private int nextChargeTick = 0;
    private int nextAttackTick = 80;
    private int nextThrowTick = 120;
    private int beamTicks;
    private boolean powerAttached;
    private boolean charging;

    public ThorZombie(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        xpReward = 20;
        setItemSlot(EquipmentSlot.MAINHAND,
                new ItemStack(MarvelZombieMod.MJOLNIR.get()));
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 90.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.95D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(BEAM_FIRING, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (beamTicks > 0 && --beamTicks == 0) {
            entityData.set(BEAM_FIRING, false);
        }

        if (!powerAttached) {
            ThorEnergy.attach(this);
            powerAttached = true;
        }

        LivingEntity target = getTarget();
        int energy = ThorEnergy.getEnergy(this);
        if (energy == 0) {
            charging = true;
        }

        if (charging) {
            setDeltaMovement(Vec3.ZERO);
            hasImpulse = true;
            getNavigation().stop();
            if (tickCount >= nextChargeTick) {
                nextChargeTick = tickCount + 4;
                ThorEnergy.addEnergy(this, 10);
                level().broadcastEntityEvent(this, (byte) 6);
                level().playSound(null, blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT,
                        getSoundSource(), 0.65F, 1.2F);
                net.minecraft.world.entity.LightningBolt lightning =
                        EntityType.LIGHTNING_BOLT.create(level());
                if (lightning != null) {
                    lightning.setVisualOnly(true);
                    lightning.moveTo(getX(), getY(), getZ());
                    level().addFreshEntity(lightning);
                }
                ((ServerLevel) level()).sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        getX(), getY() + 1.0D, getZ(), 16, 0.6D, 0.8D, 0.6D, 0.08D);
            }
            if (ThorEnergy.getEnergy(this) >= ThorEnergy.MAX_ENERGY) {
                charging = false;
            }
        }

        if (charging) {
            return;
        }

        energy = ThorEnergy.getEnergy(this);
        if (target != null && target.isAlive() && energy >= ThorEnergy.BLAST_COST
                && tickCount >= nextAttackTick && hasLineOfSight(target)) {
            lookAt(target, 180.0F, 90.0F);
            Vec3 look = target.getEyePosition().subtract(getEyePosition()).normalize();
            setYRot((float) (Math.atan2(look.z, look.x) * (180.0D / Math.PI)) - 90.0F);
            setXRot((float) (-Math.atan2(look.y,
                    Math.sqrt(look.x * look.x + look.z * look.z)) * (180.0D / Math.PI)));
            if (ThorEnergy.consume(this, ThorEnergy.BLAST_COST)) {
                beamTicks = 6;
                entityData.set(BEAM_FIRING, true);
                MjolnirItem.fireLightningBlast(level(), this);
                nextAttackTick = tickCount + ThorEnergy.BLAST_COOLDOWN;
            }
        }

        if (target != null && target.isAlive() && tickCount >= nextThrowTick
                && target.distanceToSqr(this) >= 49.0D && target.distanceToSqr(this) <= 625.0D) {
            lookAt(target, 180.0F, 90.0F);
            ThrownMjolnir mjolnir = new ThrownMjolnir(level(), this, ItemStack.EMPTY, false);
            mjolnir.shootFromRotation(this, getXRot(), getYRot(), -7.0F, 1.7F, 0.04F);
            mjolnir.setPos(getX(), getEyeY() - 0.2D, getZ());
            if (level().addFreshEntity(mjolnir)) {
                nextThrowTick = tickCount + 40;
                level().playSound(null, blockPosition(), SoundEvents.TRIDENT_THROW,
                        getSoundSource(), 1.0F, 0.7F);
            }
        }

        if (target == null && tickCount % 40 == 0) {
            Player nearest = level().getNearestPlayer(this, 24.0D);
            if (nearest != null) {
                setTarget(nearest);
            }
        }
    }

    public boolean isBeamFiring() {
        return entityData.get(BEAM_FIRING);
    }
}
