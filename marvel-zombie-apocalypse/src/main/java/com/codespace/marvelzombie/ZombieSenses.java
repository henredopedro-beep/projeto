package com.codespace.marvelzombie;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = MarvelZombieMod.MOD_ID)
public final class ZombieSenses {
    private static final Map<Level, List<SoundStimulus>> RECENT_SOUNDS = new WeakHashMap<>();
    private static final float LOUD_SOUND_THRESHOLD = 1.5F;
    private static final long SOUND_MEMORY_TICKS = 100L;

    private ZombieSenses() {
    }

    @SubscribeEvent
    public static void onSoundAtPosition(PlayLevelSoundEvent.AtPosition event) {
        rememberSound(event.getLevel(), event.getPosition(), event.getOriginalVolume());
    }

    @SubscribeEvent
    public static void onSoundAtEntity(PlayLevelSoundEvent.AtEntity event) {
        rememberSound(event.getLevel(), event.getEntity().position(), event.getOriginalVolume());
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof Zombie zombie && !zombie.level().isClientSide) {
            investigate(zombie);
        }
    }

    private static void rememberSound(Level level, Vec3 position, float volume) {
        if (level.isClientSide || volume < LOUD_SOUND_THRESHOLD) {
            return;
        }
        double radius = Math.min(40.0D, 16.0D * volume);
        List<SoundStimulus> sounds = RECENT_SOUNDS.computeIfAbsent(level, ignored -> new ArrayList<>());
        long gameTime = level.getGameTime();
        sounds.removeIf(sound -> sound.expiresAt < gameTime);
        if (sounds.size() >= 32) {
            sounds.remove(0);
        }
        sounds.add(new SoundStimulus(position, gameTime + SOUND_MEMORY_TICKS, radius));
    }

    private static void investigate(Zombie zombie) {
        LivingEntity target = zombie.getTarget();
        if ((target != null && target.isAlive() && zombie.hasLineOfSight(target))
                || zombie instanceof HulkZombie hulk && hulk.isSlamActive()) {
            return;
        }

        if (zombie.tickCount % 10 != 0) {
            return;
        }

        List<SoundStimulus> sounds = RECENT_SOUNDS.get(zombie.level());
        SoundStimulus sound = null;
        if (sounds != null) {
            long gameTime = zombie.level().getGameTime();
            sounds.removeIf(stimulus -> stimulus.expiresAt < gameTime);
            sound = sounds.stream()
                    .filter(stimulus -> zombie.distanceToSqr(stimulus.position)
                            <= stimulus.radius * stimulus.radius)
                    .min(java.util.Comparator.comparingDouble(stimulus ->
                            zombie.distanceToSqr(stimulus.position)))
                    .orElse(null);
        }
        if (sound != null) {
            if (zombie.distanceToSqr(sound.position) > 2.25D) {
                zombie.getNavigation().moveTo(sound.position.x, sound.position.y, sound.position.z, 1.0D);
            }
            return;
        }

        if (zombie.tickCount % 40 == 0) {
            BlockPos light = findNearbyLight(zombie);
            if (light != null) {
                zombie.getNavigation().moveTo(light.getX() + 0.5D, light.getY(), light.getZ() + 0.5D, 1.0D);
            }
        }
    }

    private static BlockPos findNearbyLight(Zombie zombie) {
        BlockPos origin = zombie.blockPosition();
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-8, -4, -8), origin.offset(8, 4, 8))) {
            if (!zombie.level().hasChunkAt(pos)
                    || zombie.level().getBrightness(LightLayer.BLOCK, pos) < 13) {
                continue;
            }
            double distance = pos.distSqr(origin);
            if (distance < nearestDistance) {
                nearest = pos.immutable();
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private record SoundStimulus(Vec3 position, long expiresAt, double radius) {
    }
}
