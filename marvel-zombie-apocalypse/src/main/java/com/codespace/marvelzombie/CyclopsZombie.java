package com.codespace.marvelzombie;

import com.mojang.logging.LogUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.phys.Vec3;
import net.threetag.palladium.power.SuperpowerUtil;
import net.threetag.palladium.util.property.EntityPropertyHandler;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.SyncType;
import org.joml.Vector3f;
import org.slf4j.Logger;

public class CyclopsZombie extends Zombie {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final DustParticleOptions BEAM_PARTICLE =
            new DustParticleOptions(new Vector3f(1.0F, 0.12F, 0.18F), 0.75F);
    private static final EntityDataAccessor<Boolean> BEAM_FIRING =
            SynchedEntityData.defineId(CyclopsZombie.class, EntityDataSerializers.BOOLEAN);
    private static final ResourceLocation PALLADIUM_POWER =
            new ResourceLocation(MarvelZombieMod.MOD_ID, "cyclops_zombie");
    private static final int CHARGE_WINDUP_DURATION = 14;
    private static final int BEAM_WINDUP_DURATION = 18;
    private int windupTicks;
    private int chargeTicks;
    private int nextChargeTick = 60;
    private int beamWindupTicks;
    private int beamTicks;
    private int nextBeamTick = 70;
    private boolean loggedMissingPower;

    public CyclopsZombie(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        this.xpReward = 18;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(BEAM_FIRING, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 58.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.55D);
    }

    @Override
    public void tick() {
        ensurePalladiumPower();
        super.tick();
        if (level().isClientSide) {
            if (isBeamFiring() && tickCount % 2 == 0) {
                Vec3 eye = getEyePosition();
                level().addParticle(BEAM_PARTICLE,
                        eye.x + (random.nextDouble() - 0.5D) * 0.12D,
                        eye.y + (random.nextDouble() - 0.5D) * 0.12D,
                        eye.z + (random.nextDouble() - 0.5D) * 0.12D,
                        0.0D, 0.0D, 0.0D);
            }
            return;
        }

        if (!isAlive() || isNoAi()) {
            windupTicks = 0;
            chargeTicks = 0;
            beamWindupTicks = 0;
            beamTicks = 0;
            setBeamFiring(false);
            return;
        }

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            windupTicks = 0;
            chargeTicks = 0;
            beamWindupTicks = 0;
            beamTicks = 0;
            setBeamFiring(false);
            return;
        }

        double distance = target.distanceToSqr(this);

        if (beamTicks > 0) {
            if (!canFireBeamAt(target, distance)) {
                beamTicks = 0;
                nextBeamTick = tickCount + 30;
                setBeamFiring(false);
                return;
            }
            aimAt(target);
            getNavigation().stop();
            setDeltaMovement(Vec3.ZERO);
            setBeamFiring(true);
            if (--beamTicks == 0) {
                setBeamFiring(false);
            }
            return;
        }

        if (beamWindupTicks > 0) {
            if (!canFireBeamAt(target, distance)) {
                beamWindupTicks = 0;
                setBeamFiring(false);
                return;
            }
            aimAt(target);
            getNavigation().stop();
            setDeltaMovement(Vec3.ZERO);
            if (--beamWindupTicks == 0) {
                beamTicks = isEnraged() ? 36 : 24;
                playSound(SoundEvents.GUARDIAN_ATTACK, 1.5F, isEnraged() ? 0.7F : 0.9F);
            }
            return;
        }

        if (windupTicks > 0) {
            getNavigation().stop();
            setDeltaMovement(Vec3.ZERO);
            if (--windupTicks == 0) {
                beginCharge(target);
            }
            return;
        }

        if (chargeTicks > 0) {
            chargeTicks--;
            if (target.distanceToSqr(this) <= 7.0D) {
                performChargeHit(target);
                chargeTicks = 0;
            }
            return;
        }

        if (tickCount >= nextBeamTick && canFireBeamAt(target, distance) && onGround()) {
            beamWindupTicks = BEAM_WINDUP_DURATION;
            nextBeamTick = tickCount + (isEnraged() ? 105 : 155);
            aimAt(target);
            getNavigation().stop();
            setDeltaMovement(Vec3.ZERO);
            playSound(SoundEvents.WARDEN_ROAR, 1.4F, isEnraged() ? 0.8F : 1.0F);
            return;
        }

        if (tickCount >= nextChargeTick && distance >= 20.25D && distance <= 144.0D
                && hasLineOfSight(target) && onGround()) {
            windupTicks = CHARGE_WINDUP_DURATION;
            nextChargeTick = tickCount + (isEnraged() ? 65 : 105);
            getNavigation().stop();
            setDeltaMovement(Vec3.ZERO);
            playSound(SoundEvents.WARDEN_ROAR, 1.8F, isEnraged() ? 0.75F : 0.55F);
        }
    }

    private void ensurePalladiumPower() {
        EntityPropertyHandler handler = EntityPropertyHandler.getHandler(this)
                .orElseThrow(() -> new IllegalStateException(
                        "Palladium did not provide an entity property handler for Cyclops Zombie"));
        if (!handler.isRegistered(CyclopsPalladium.BEAM_ACTIVE)) {
            handler.register(CyclopsPalladium.BEAM_ACTIVE, 0);
        }

        if (!level().isClientSide && !SuperpowerUtil.hasSuperpower(this, PALLADIUM_POWER)
                && !SuperpowerUtil.addSuperpower(this, PALLADIUM_POWER) && !loggedMissingPower) {
            LOGGER.error("Unable to attach Palladium power {} to Cyclops Zombie", PALLADIUM_POWER);
            loggedMissingPower = true;
        }
    }

    private boolean canFireBeamAt(LivingEntity target, double distance) {
        return distance >= 81.0D && distance <= 900.0D && hasLineOfSight(target);
    }

    private void aimAt(LivingEntity target) {
        Vec3 direction = target.getEyePosition().subtract(getEyePosition());
        double horizontalDistance = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        float targetYaw = (float) (Mth.atan2(direction.z, direction.x) * (180.0D / Math.PI)) - 90.0F;
        float targetPitch = (float) (-Mth.atan2(direction.y, horizontalDistance) * (180.0D / Math.PI));

        setYRot(Mth.rotLerp(0.2F, getYRot(), targetYaw));
        setYHeadRot(getYRot());
        setYBodyRot(getYRot());
        setXRot(Mth.rotLerp(0.2F, getXRot(), targetPitch));
    }

    private void setBeamFiring(boolean firing) {
        if (entityData.get(BEAM_FIRING) != firing) {
            entityData.set(BEAM_FIRING, firing);
        }
        EntityPropertyHandler.getHandler(this).ifPresent(handler -> {
            int value = firing ? 1 : 0;
            if (handler.isRegistered(CyclopsPalladium.BEAM_ACTIVE)
                    && handler.get(CyclopsPalladium.BEAM_ACTIVE) != value) {
                handler.set(CyclopsPalladium.BEAM_ACTIVE, value);
            }
        });
    }

    public boolean isBeamFiring() {
        return entityData.get(BEAM_FIRING);
    }

    private void beginCharge(LivingEntity target) {
        if (!target.isAlive()) {
            return;
        }

        Vec3 direction = target.position().subtract(position()).multiply(1.0D, 0.0D, 1.0D);
        if (direction.lengthSqr() < 1.0E-6D) {
            return;
        }

        direction = direction.normalize();
        double speed = isEnraged() ? 1.35D : 1.05D;
        setDeltaMovement(direction.x * speed, 0.38D, direction.z * speed);
        hasImpulse = true;
        chargeTicks = 16;
        swing(InteractionHand.MAIN_HAND);
    }

    private void performChargeHit(LivingEntity target) {
        float damage = isEnraged() ? 16.0F : 12.0F;
        if (target.hurt(level().damageSources().mobAttack(this), damage)) {
            target.knockback(1.1D, getX() - target.getX(), getZ() - target.getZ());
            playSound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0F, 0.7F);
        }
    }

    private boolean isEnraged() {
        return getHealth() <= getMaxHealth() * 0.5F;
    }
}
