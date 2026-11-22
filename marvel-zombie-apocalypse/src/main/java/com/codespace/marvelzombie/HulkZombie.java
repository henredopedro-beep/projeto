package com.codespace.marvelzombie;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class HulkZombie extends Zombie {
    public static final String SMOKE_CLOUD_MARKER = "marvel_zombie_hulk_calming_smoke";
    private static final EntityDataAccessor<Boolean> HULK =
            SynchedEntityData.defineId(HulkZombie.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> RAGE =
            SynchedEntityData.defineId(HulkZombie.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CALM =
            SynchedEntityData.defineId(HulkZombie.class, EntityDataSerializers.INT);
    private static final int CALM_REQUIRED_TICKS = 100;
    private static final int MAX_RAGE = 100;
    private static final int SEISMIC_LEAP_COOLDOWN = 320;
    private static final int GROUND_SLAM_COOLDOWN = 110;
    private int nextLeapTick = 120;
    private int nextGroundSlamTick;
    private int leapStartTick;
    private Boolean lastDisplayedHulk;
    private boolean leapActive;
    private double leapTargetX;
    private double leapTargetZ;
    private double leapStartY;
    private double leapPeakY;

    public HulkZombie(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        xpReward = 15;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 35.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(HULK, false);
        entityData.define(RAGE, 0);
        entityData.define(CALM, 0);
    }

    public boolean isHulk() {
        return entityData.get(HULK);
    }

    public int getRagePercent() {
        return entityData.get(RAGE);
    }

    public int getCalmProgress() {
        return entityData.get(CALM);
    }

    public boolean isSlamActive() {
        return leapActive;
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        boolean damaged = super.hurt(source, amount);
        if (damaged && isAlive() && amount > 0.0F && !isHulk()) {
            addRage(Math.min(30, Math.max(8, Mth.ceil(amount * 5.0F))));
        }
        return damaged;
    }

    private void addRage(int amount) {
        if (isHulk()) {
            return;
        }
        int rage = Math.min(MAX_RAGE, getRagePercent() + amount);
        entityData.set(RAGE, rage);
        updateBannerAttributes();
        updateDisplayName();
        if (rage >= MAX_RAGE) {
            transformIntoHulk();
        }
    }

    private void transformIntoHulk() {
        entityData.set(HULK, true);
        setAttribute(Attributes.MAX_HEALTH, 260.0D);
        setAttribute(Attributes.MOVEMENT_SPEED, 0.40D);
        setAttribute(Attributes.ATTACK_DAMAGE, 18.0D);
        setAttribute(Attributes.FOLLOW_RANGE, 48.0D);
        setAttribute(Attributes.KNOCKBACK_RESISTANCE, 0.95D);
        setHealth(getMaxHealth());
        nextLeapTick = tickCount + SEISMIC_LEAP_COOLDOWN;
        refreshDimensions();
        updateDisplayName();
        level().playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR,
                SoundSource.HOSTILE, 3.0F, 0.65F);
        level().broadcastEntityEvent(this, (byte) 60);
    }

    private void revertToBanner() {
        entityData.set(HULK, false);
        entityData.set(RAGE, 0);
        entityData.set(CALM, 0);
        leapActive = false;
        setAttribute(Attributes.MAX_HEALTH, 20.0D);
        setAttribute(Attributes.MOVEMENT_SPEED, 0.23D);
        setAttribute(Attributes.ATTACK_DAMAGE, 3.0D);
        setAttribute(Attributes.FOLLOW_RANGE, 35.0D);
        setAttribute(Attributes.KNOCKBACK_RESISTANCE, 0.0D);
        setHealth(Math.min(getHealth(), getMaxHealth()));
        refreshDimensions();
        updateDisplayName();
        level().playSound(null, blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CURE,
                SoundSource.HOSTILE, 1.6F, 1.1F);
    }

    private void updateBannerAttributes() {
        int rage = getRagePercent();
        float oldMaxHealth = getMaxHealth();
        float healthRatio = oldMaxHealth > 0.0F ? getHealth() / oldMaxHealth : 1.0F;
        setAttribute(Attributes.MAX_HEALTH, 20.0D + rage * 0.8D);
        setAttribute(Attributes.MOVEMENT_SPEED, 0.23D + rage * 0.0011D);
        setAttribute(Attributes.ATTACK_DAMAGE, 3.0D + rage * 0.07D);
        setAttribute(Attributes.FOLLOW_RANGE, 35.0D + rage * 0.13D);
        setAttribute(Attributes.KNOCKBACK_RESISTANCE, rage * 0.007D);
        setHealth(Math.max(1.0F, Math.min(getMaxHealth(), healthRatio * getMaxHealth())));
    }

    private void updateDisplayName() {
        if (lastDisplayedHulk != null && isHulk() == lastDisplayedHulk) {
            return;
        }
        String translationKey = isHulk()
                ? "entity.marvel_zombie.hulk"
                : "entity.marvel_zombie.hulk_banner";
        setCustomName(Component.translatable(translationKey));
        setCustomNameVisible(true);
        lastDisplayedHulk = isHulk();
    }

    private void setAttribute(Attribute attribute, double value) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        EntityDimensions base = super.getDimensions(pose);
        return isHulk() ? base.scale(1.75F, 1.8F) : base;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }

        updateDisplayName();
        updateCalmFromSmoke();
        if (!isHulk()) {
            return;
        }

        if (leapActive) {
            tickSeismicLeap();
            return;
        }

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            Player nearby = level().getNearestPlayer(this, 24.0D);
            if (nearby != null) {
                setTarget(nearby);
                target = nearby;
            }
        }
        if (target instanceof Player player && onGround()) {
            double distance = distanceToSqr(player);
            if (!hasLineOfSight(player) && tickCount % 6 == 0) {
                breakPathBlock(player);
            }
            if (distance <= 30.25D && tickCount >= nextGroundSlamTick) {
                groundSlam();
            } else if (distance >= 144.0D && distance <= 2500.0D
                    && tickCount >= nextLeapTick) {
                beginSeismicLeap(player);
            }
        }
    }

    private void updateCalmFromSmoke() {
        AreaEffectCloud cloud = level().getEntitiesOfClass(AreaEffectCloud.class,
                        getBoundingBox().inflate(6.0D),
                        candidate -> candidate.getCustomName() != null
                                && SMOKE_CLOUD_MARKER.equals(candidate.getCustomName().getString())
                                && candidate.isAlive())
                .stream()
                .filter(candidate -> candidate.distanceToSqr(this)
                        <= candidate.getRadius() * candidate.getRadius())
                .findFirst()
                .orElse(null);

        if (cloud == null) {
            if (getCalmProgress() != 0) {
                entityData.set(CALM, 0);
            }
            return;
        }

        if (!isHulk()) {
            if (getRagePercent() != 0) {
                entityData.set(RAGE, 0);
                updateBannerAttributes();
                updateDisplayName();
            }
            entityData.set(CALM, 0);
            return;
        }

        int progress = Math.min(CALM_REQUIRED_TICKS, getCalmProgress() + 1);
        entityData.set(CALM, progress);
        if (tickCount % 4 == 0 && level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    getX(), getY() + 2.3D, getZ(), 2, 0.5D, 0.4D, 0.5D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    getX(), getY() + 1.2D, getZ(), 2, 0.5D, 0.4D, 0.5D, 0.01D);
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                    getX(), getY() + 1.1D, getZ(), 2, 0.4D, 0.35D, 0.4D, 0.01D);
        }
        if (progress >= CALM_REQUIRED_TICKS) {
            revertToBanner();
        }
    }

    private void beginSeismicLeap(Player target) {
        Vec3 direction = target.position().subtract(position()).multiply(1.0D, 0.0D, 1.0D);
        if (direction.lengthSqr() >= 1.0E-6D) {
            direction = direction.normalize();
        }

        leapActive = true;
        leapStartTick = tickCount;
        leapTargetX = target.getX();
        leapTargetZ = target.getZ();
        leapStartY = getY();
        leapPeakY = leapStartY;
        nextLeapTick = tickCount + SEISMIC_LEAP_COOLDOWN;
        double launchHeight = Mth.clamp(2.2D + distanceTo(target) * 0.07D, 2.2D, 6.5D);
        setDeltaMovement(direction.scale(0.35D).add(0.0D, launchHeight, 0.0D));
        hasImpulse = true;
        getNavigation().stop();
        playSound(SoundEvents.RAVAGER_ATTACK, 2.0F, 0.55F);
    }

    private void tickSeismicLeap() {
        leapPeakY = Math.max(leapPeakY, getY());
        LivingEntity target = getTarget();
        if (target instanceof Player player && player.isAlive()) {
            leapTargetX = player.getX();
            leapTargetZ = player.getZ();
        }

        Vec3 horizontal = new Vec3(leapTargetX - getX(), 0.0D, leapTargetZ - getZ());
        double distance = horizontal.length();
        Vec3 motion = getDeltaMovement();
        if (distance > 0.15D) {
            double speed = Math.min(0.8D, distance * 0.16D);
            Vec3 steering = horizontal.normalize().scale(speed);
            setDeltaMovement(steering.x, motion.y, steering.z);
        } else {
            setDeltaMovement(0.0D, motion.y, 0.0D);
        }

        if ((onGround() && tickCount - leapStartTick > 8) || tickCount - leapStartTick > 120) {
            landSeismicLeap();
        }
    }

    private void landSeismicLeap() {
        leapActive = false;
        int x = Mth.floor(leapTargetX);
        int z = Mth.floor(leapTargetZ);
        int y = level().getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        double jumpHeight = Math.max(0.0D, leapPeakY - leapStartY);
        int craterRadius = Mth.clamp(2 + Mth.floor(jumpHeight * 0.8D), 2, 8);
        double impactRadius = 5.5D + Math.min(4.0D, jumpHeight * 0.5D);
        setPos(x + 0.5D, y, z + 0.5D);
        setDeltaMovement(Vec3.ZERO);
        createCrater(BlockPos.containing(x, y - 1, z), craterRadius);
        damagePlayersNearImpact(x + 0.5D, y, z + 0.5D, impactRadius);
        level().playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE,
                SoundSource.HOSTILE, 2.4F, 0.65F);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                    x + 0.5D, y + 0.5D, z + 0.5D, 2, 1.2D, 0.4D, 1.2D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    x + 0.5D, y + 0.2D, z + 0.5D, 35, 1.8D, 0.3D, 1.8D, 0.08D);
        }
    }

    private void createCrater(BlockPos surface, int radius) {
        int depth = Math.max(1, radius / 3);
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double distance = Math.sqrt(x * x + z * z);
                if (distance > radius) {
                    continue;
                }
                BlockPos top = surface.offset(x, 0, z);
                for (int layer = 0; layer < depth; layer++) {
                    if (distance <= radius - layer) {
                        destroyCraterBlock(top.below(layer));
                    }
                }
            }
        }
    }

    private void destroyCraterBlock(BlockPos pos) {
        if (!level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                || !level().hasChunkAt(pos)) {
            return;
        }
        var state = level().getBlockState(pos);
        if (state.isAir() || state.hasBlockEntity() || state.is(BlockTags.WITHER_IMMUNE)
                || state.is(Blocks.BEDROCK) || state.is(Blocks.OBSIDIAN)
                || state.is(Blocks.CRYING_OBSIDIAN) || state.is(Blocks.END_PORTAL_FRAME)
                || state.is(MarvelZombieTags.HULK_INDESTRUCTIBLE)
                || state.getDestroySpeed(level(), pos) < 0.0F) {
            return;
        }
        level().destroyBlock(pos, true, this);
    }

    private void breakPathBlock(Player target) {
        Vec3 start = getEyePosition();
        Vec3 end = target.getEyePosition();
        BlockHitResult hit = level().clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() != BlockHitResult.Type.BLOCK) {
            return;
        }

        BlockPos blockPos = hit.getBlockPos();
        if (!level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                || !level().hasChunkAt(blockPos)) {
            return;
        }
        var state = level().getBlockState(blockPos);
        if (state.isAir() || state.hasBlockEntity() || state.is(BlockTags.WITHER_IMMUNE)
                || state.is(Blocks.BEDROCK) || state.is(Blocks.OBSIDIAN)
                || state.is(Blocks.CRYING_OBSIDIAN) || state.is(Blocks.END_PORTAL_FRAME)
                || state.is(MarvelZombieTags.HULK_INDESTRUCTIBLE)
                || state.getDestroySpeed(level(), blockPos) < 0.0F) {
            return;
        }

        level().destroyBlock(blockPos, true, this);
        level().playSound(null, blockPos, state.getSoundType().getBreakSound(),
                SoundSource.HOSTILE, 1.0F, 0.65F);
    }

    private void damagePlayersNearImpact(double x, double y, double z, double radius) {
        double verticalRange = radius + 3.0D;
        AABB impactArea = new AABB(x - radius, y - verticalRange, z - radius,
                x + radius, y + verticalRange, z + radius);
        for (Player player : level().getEntitiesOfClass(Player.class, impactArea)) {
            double dx = player.getX() - x;
            double dz = player.getZ() - z;
            if (dx * dx + dz * dz > radius * radius
                    || Math.abs(player.getY() - y) > verticalRange) {
                continue;
            }
            if (player.hurt(level().damageSources().mobAttack(this), 16.0F)) {
                player.knockback(1.2D, x - player.getX(), z - player.getZ());
            }
        }
    }

    private void groundSlam() {
        nextGroundSlamTick = tickCount + GROUND_SLAM_COOLDOWN;
        getNavigation().stop();
        playSound(SoundEvents.RAVAGER_ATTACK, 2.0F, 0.75F);
        AABB impactArea = getBoundingBox().inflate(4.5D, 2.0D, 4.5D);
        for (Player player : level().getEntitiesOfClass(Player.class, impactArea)) {
            if (distanceToSqr(player) > 30.25D) {
                continue;
            }
            if (player.hurt(level().damageSources().mobAttack(this), 10.0F)) {
                player.knockback(0.9D, getX() - player.getX(), getZ() - player.getZ());
            }
        }
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                    getX(), getY() + 0.1D, getZ(), 1, 0.8D, 0.1D, 0.8D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    getX(), getY() + 0.1D, getZ(), 24, 1.8D, 0.2D, 1.8D, 0.06D);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Hulk", isHulk());
        tag.putInt("Rage", getRagePercent());
        tag.putInt("CalmProgress", getCalmProgress());
        tag.putInt("NextLeapTick", nextLeapTick);
        tag.putInt("NextGroundSlamTick", nextGroundSlamTick);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(HULK, tag.getBoolean("Hulk"));
        entityData.set(RAGE, tag.getInt("Rage"));
        entityData.set(CALM, tag.getInt("CalmProgress"));
        nextLeapTick = tag.getInt("NextLeapTick");
        nextGroundSlamTick = tag.getInt("NextGroundSlamTick");
        leapActive = false;
        if (isHulk()) {
            setAttribute(Attributes.MAX_HEALTH, 260.0D);
            setAttribute(Attributes.MOVEMENT_SPEED, 0.40D);
            setAttribute(Attributes.ATTACK_DAMAGE, 18.0D);
            setAttribute(Attributes.FOLLOW_RANGE, 48.0D);
            setAttribute(Attributes.KNOCKBACK_RESISTANCE, 0.95D);
            refreshDimensions();
        } else {
            updateBannerAttributes();
        }
        updateDisplayName();
    }
}
