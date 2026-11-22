package com.codespace.marvelzombie;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.Power;
import net.threetag.palladium.power.PowerManager;
import net.threetag.palladium.power.ability.AbilityInstance;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = MarvelZombieMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ThorSlamHandler {
    private static final String ABILITY_ID = "storm_slam";
    private static final String ACTIVE_TAG = "MarvelZombieThorSlamV2Active";
    private static final String LEFT_GROUND_TAG = "MarvelZombieThorSlamV2LeftGround";
    private static final String STARTED_AT_TAG = "MarvelZombieThorSlamV2StartedAt";
    private static final String COOLDOWN_TAG = "MarvelZombieThorSlamV2Cooldown";
    private static final String PENDING_UNTIL_TAG = "MarvelZombieThorSlamV2PendingUntil";
    private static final int COOLDOWN_TICKS = 100;
    private static final int ACTIVATION_GRACE_TICKS = 5;
    private static final double LAUNCH_SPEED = 1.45D;
    private static final double IMPACT_RADIUS = 25.0D;

    private ThorSlamHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        if (event.phase != TickEvent.Phase.END || player.level().isClientSide) {
            return;
        }

        acceptPalladiumPress(player);
        updateFlight(player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        var data = event.getEntity().getPersistentData();
        data.remove(PENDING_UNTIL_TAG);
        data.remove(ACTIVE_TAG);
        data.remove(LEFT_GROUND_TAG);
        data.remove(STARTED_AT_TAG);
    }

    private static void acceptPalladiumPress(Player player) {
        var data = player.getPersistentData();
        long now = player.level().getGameTime();
        boolean pressedNow = getSlamAbility(player)
                .filter(AbilityInstance::isEnabled)
                .map(ability -> ability.getEnabledTicks() == 1)
                .orElse(false);

        if (pressedNow && isHoldingMjolnir(player)) {
            data.putLong(PENDING_UNTIL_TAG, now + ACTIVATION_GRACE_TICKS);
        }

        if (!data.contains(PENDING_UNTIL_TAG)) {
            return;
        }
        if (now > data.getLong(PENDING_UNTIL_TAG)) {
            data.remove(PENDING_UNTIL_TAG);
        } else if (launch(player)) {
            data.remove(PENDING_UNTIL_TAG);
        }
    }

    private static Optional<AbilityInstance> getSlamAbility(Player player) {
        Power power = PowerManager.getInstance(player.level()).getPower(ThorEnergy.POWER_ID);
        if (power == null) {
            return Optional.empty();
        }
        return PowerManager.getPowerHandler(player)
                .map(handler -> handler.getPowerHolder(power))
                .map(IPowerHolder::getAbilities)
                .map(abilities -> abilities.get(ABILITY_ID));
    }

    private static boolean isHoldingMjolnir(Player player) {
        return player.getMainHandItem().is(MarvelZombieMod.MJOLNIR.get())
                || player.getOffhandItem().is(MarvelZombieMod.MJOLNIR.get());
    }

    private static boolean launch(Player player) {
        var data = player.getPersistentData();
        long now = player.level().getGameTime();
        if (!isHoldingMjolnir(player) || ThorEnergy.getEnergy(player) <= 0
                || data.getBoolean(ACTIVE_TAG) || data.getLong(COOLDOWN_TAG) > now) {
            return false;
        }

        data.putBoolean(ACTIVE_TAG, true);
        data.putBoolean(LEFT_GROUND_TAG, false);
        data.putLong(STARTED_AT_TAG, now);
        data.putLong(COOLDOWN_TAG, now + COOLDOWN_TICKS);

        player.jumpFromGround();
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x * 0.35D, LAUNCH_SPEED, motion.z * 0.35D);
        player.hasImpulse = true;
        player.fallDistance = 0.0F;
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(player));
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1,
                SoundSource.PLAYERS, 1.0F, 0.8F);
        return true;
    }

    private static void updateFlight(Player player) {
        var data = player.getPersistentData();
        if (!data.getBoolean(ACTIVE_TAG)) {
            return;
        }

        long elapsed = player.level().getGameTime() - data.getLong(STARTED_AT_TAG);
        if (elapsed > 100L) {
            clearFlight(player);
        } else if (!player.onGround()) {
            data.putBoolean(LEFT_GROUND_TAG, true);
        } else if (elapsed >= 5L && data.getBoolean(LEFT_GROUND_TAG)) {
            strikeGround(player);
            clearFlight(player);
        }
    }

    private static void clearFlight(Player player) {
        var data = player.getPersistentData();
        data.remove(ACTIVE_TAG);
        data.remove(LEFT_GROUND_TAG);
        data.remove(STARTED_AT_TAG);
    }

    private static void strikeGround(Player player) {
        AABB area = player.getBoundingBox().inflate(IMPACT_RADIUS);
        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, area,
                entity -> entity.isAlive() && entity.distanceToSqr(player) <= IMPACT_RADIUS * IMPACT_RADIUS)) {
            mob.hurt(player.level().damageSources().playerAttack(player), 20.0F);
            mob.knockback(2.0D, player.getX() - mob.getX(), player.getZ() - mob.getZ());

            var lightning = EntityType.LIGHTNING_BOLT.create(player.level());
            if (lightning != null) {
                lightning.moveTo(mob.getX(), mob.getY(), mob.getZ());
                if (player instanceof ServerPlayer serverPlayer) {
                    lightning.setCause(serverPlayer);
                }
                player.level().addFreshEntity(lightning);
            }
        }

        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 0.1D,
                    player.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 0.2D,
                    player.getZ(), 48, 3.0D, 0.3D, 3.0D, 0.18D);
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.PLAYERS, 1.2F, 0.75F);
    }
}
