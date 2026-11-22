package com.codespace.marvelzombie;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class CaptainZombie extends Zombie {
    private int nextShieldThrowTick = 80;
    private int nextShieldBlockTick;

    public CaptainZombie(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        xpReward = 18;
        setItemSlot(EquipmentSlot.OFFHAND, MarvelZombieMod.CAPTAIN_SHIELD.get().getDefaultInstance());
        setDropChance(EquipmentSlot.OFFHAND, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 70.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.26D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D);
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        LivingEntity attacker = source.getEntity() instanceof LivingEntity living ? living : null;
        if (getOffhandItem().is(MarvelZombieMod.CAPTAIN_SHIELD.get())
                && attacker != null && tickCount >= nextShieldBlockTick) {
            Vec3 towardAttacker = attacker.position().subtract(position()).normalize();
            if (getViewVector(1.0F).dot(towardAttacker) > 0.25D) {
                nextShieldBlockTick = tickCount + 18;
                level().playSound(null, blockPosition(), SoundEvents.SHIELD_BLOCK,
                        SoundSource.HOSTILE, 0.8F, 0.8F);
                amount *= 0.25F;
            }
        }
        return super.hurt(source, amount);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            Player nearest = level().getNearestPlayer(this, 24.0D);
            if (nearest != null) {
                setTarget(nearest);
                target = nearest;
            }
        }
        if (!(target instanceof Player) || !target.isAlive()) {
            return;
        }

        lookAt(target, 30.0F, 30.0F);
        double distance = distanceToSqr(target);
        if (distance < 7.0D * 7.0D || distance > 24.0D * 24.0D) {
            return;
        }

        if (tickCount < nextShieldThrowTick) {
            return;
        }

        ThrownCaptainShield shield = new ThrownCaptainShield(level(), this);
        shield.setItem(MarvelZombieMod.CAPTAIN_SHIELD.get().getDefaultInstance());
        shield.setRicochetTarget(target);
        shield.setReturnDelay(55);

        Vec3 aimPoint = target.getEyePosition().add(0.0D, 3.0D, 0.0D);
        Vec3 direction = aimPoint.subtract(getEyePosition()).normalize();
        shield.setPos(getX() + direction.x * 0.7D,
                getEyeY() - 0.15D + direction.y * 0.7D,
                getZ() + direction.z * 0.7D);
        shield.setDeltaMovement(direction.scale(1.65D));

        if (level().addFreshEntity(shield)) {
            nextShieldThrowTick = tickCount + 75;
            level().playSound(null, blockPosition(), SoundEvents.TRIDENT_THROW,
                    SoundSource.HOSTILE, 1.0F, 0.75F);
        }
    }
}
