package com.codespace.marvelzombie;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.joml.Vector3f;

public class ThrownSmokeBomb extends ThrowableItemProjectile {
    private static final DustParticleOptions GREEN_SMOKE =
            new DustParticleOptions(new Vector3f(0.08F, 0.8F, 0.2F), 2.0F);

    public ThrownSmokeBomb(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
    }

    public ThrownSmokeBomb(Level level, LivingEntity owner) {
        super(MarvelZombieMod.THROWN_SMOKE_BOMB.get(), owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return MarvelZombieMod.SMOKE_BOMB.get();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            net.minecraft.world.entity.AreaEffectCloud cloud =
                    new net.minecraft.world.entity.AreaEffectCloud(level(), getX(), getY(), getZ());
            cloud.setOwner(getOwner() instanceof LivingEntity living ? living : null);
            cloud.setRadius(2.0F);
            cloud.setRadiusOnUse(0.0F);
            cloud.setRadiusPerTick(0.01F);
            cloud.setDuration(240);
            cloud.setWaitTime(0);
            cloud.setParticle(GREEN_SMOKE);
            cloud.setCustomName(net.minecraft.network.chat.Component.literal(
                    HulkZombie.SMOKE_CLOUD_MARKER));
            cloud.setCustomNameVisible(false);
            cloud.setNoGravity(true);
            level().addFreshEntity(cloud);
            level().playSound(null, blockPosition(), SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.PLAYERS, 1.4F, 0.75F);
            discard();
        }
    }
}
