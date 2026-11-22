package com.codespace.marvelzombie;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.Power;
import net.threetag.palladium.power.PowerManager;
import net.threetag.palladium.power.SuperpowerUtil;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.power.energybar.EnergyBar;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.joml.Vector3f;

@Mod.EventBusSubscriber(modid = MarvelZombieMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ThorEnergy {
    public static final ResourceLocation POWER_ID =
            new ResourceLocation(MarvelZombieMod.MOD_ID, "godofthunder");
    public static final ResourceLocation WORTHY_POWER_ID =
            new ResourceLocation(MarvelZombieMod.MOD_ID, "worthy");
    public static final String BAR_ID = "storm_energy";
    public static final String CHARGE_ABILITY_ID = "charge_storm";
    public static final int MAX_ENERGY = 1000;
    public static final int BLAST_COST = 40;
    public static final int BLAST_COOLDOWN = 60;
    private static final UUID CHARGED_ATTRIBUTE_MODIFIER_UUID =
            UUID.fromString("c8441c95-a2d8-4d7f-86d5-726084f953a1");
    private static final String STORED_ENERGY_TAG = "MarvelZombieThorStoredEnergy";
    private static final String HAS_STORED_ENERGY_TAG = "MarvelZombieThorHasStoredEnergy";
    private static final String ENERGY_BAR_ACTIVE_TAG = "MarvelZombieThorEnergyBarActive";
    private static final String STORM_COOLDOWN_UNTIL_TAG = "MarvelZombieThorStormCooldownUntil";
    private static final String STORM_IMMOBILE_UNTIL_TAG = "MarvelZombieThorStormImmobileUntil";
    private static final String COUNTER_IMMOBILE_UNTIL_TAG = "MarvelZombieThorCounterImmobileUntil";
    private static final String COUNTER_WAS_NO_AI_TAG = "MarvelZombieThorCounterWasNoAi";
    private static final String SPIN_STARTED_AT_TAG = "MarvelZombieMjolnirSpinStartedAt";
    private static final String SPIN_EXPIRED_TAG = "MarvelZombieMjolnirSpinExpired";
    private static final String SPIN_TARGET_COOLDOWN_TAG = "MarvelZombieMjolnirSpinCooldownUntil";
    private static final String MJOLNIR_WORTHY_TAG = "MarvelZombieMjolnirWorthy";
    private static final String MJOLNIR_LIFTING_TAG = "MarvelZombieMjolnirLifting";
    private static final String MJOLNIR_BOUND_ID_TAG = "MarvelZombieMjolnirBoundId";
    private static final String MJOLNIR_ITEM_ID_TAG = "MarvelZombieMjolnirId";
    public static final String MJOLNIR_OWNER_ID_TAG = "MarvelZombieMjolnirOwnerId";
    public static final String MJOLNIR_OWNER_NAME_TAG = "MarvelZombieMjolnirOwnerName";
    private static final int STORM_COOLDOWN_TICKS = 2400;
    private static final int STORM_IMMOBILE_TICKS = 20;
    private static final int MAX_SPIN_TICKS = 100;

    private ThorEnergy() {
    }

    private static void activateStormOnPress(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || !isWorthy(player)
                || !holdsMjolnir(player) || getEnergy(player) > 0) {
            return;
        }
        boolean pressed = getAbility(player, CHARGE_ABILITY_ID)
                .map(ability -> ability.isEnabled() && ability.getEnabledTicks() == 1)
                .orElse(false);
        long now = player.level().getGameTime();
        var data = player.getPersistentData();
        if (!pressed || data.getLong(STORM_COOLDOWN_UNTIL_TAG) > now) {
            return;
        }

        data.putLong(STORM_COOLDOWN_UNTIL_TAG, now + STORM_COOLDOWN_TICKS);
        data.putLong(STORM_IMMOBILE_UNTIL_TAG, now + STORM_IMMOBILE_TICKS);
        addEnergy(player, MAX_ENERGY - getEnergy(player));
        player.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN, STORM_IMMOBILE_TICKS, 10, false, false));
        player.setDeltaMovement(Vec3.ZERO);
        player.hasImpulse = true;
        player.setSprinting(false);
        player.fallDistance = 0.0F;
        if (player.level() instanceof ServerLevel level) {
            DustParticleOptions dust = new DustParticleOptions(
                    new Vector3f(0.35F, 0.72F, 1.0F), 1.5F);
            level.sendParticles(dust, player.getX(), player.getY() + 1.0D,
                    player.getZ(), 40, 0.8D, 1.0D, 0.8D, 0.02D);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(),
                    player.getY() + 1.0D, player.getZ(), 24, 0.7D, 0.9D, 0.7D, 0.08D);
        }
    }

    private static void keepStormUserImmobile(Player player) {
        if (player.getPersistentData().getLong(STORM_IMMOBILE_UNTIL_TAG)
                <= player.level().getGameTime()) {
            player.getPersistentData().remove(STORM_IMMOBILE_UNTIL_TAG);
            return;
        }
        player.setDeltaMovement(Vec3.ZERO);
        player.hasImpulse = true;
        player.setSprinting(false);
        player.fallDistance = 0.0F;
    }

    private static void spinMjolnir(Player player) {
        var data = player.getPersistentData();
        if (!holdsMjolnir(player) || getEnergy(player) > 0
                || !isAbilityEnabled(player, "mjolnir_spin")) {
            data.remove(SPIN_STARTED_AT_TAG);
            data.remove(SPIN_EXPIRED_TAG);
            return;
        }

        if (data.getBoolean(SPIN_EXPIRED_TAG)) {
            return;
        }
        long now = player.level().getGameTime();
        if (!data.contains(SPIN_STARTED_AT_TAG)) {
            data.putLong(SPIN_STARTED_AT_TAG, now);
        }
        if (now - data.getLong(SPIN_STARTED_AT_TAG) >= MAX_SPIN_TICKS) {
            data.remove(SPIN_STARTED_AT_TAG);
            data.putBoolean(SPIN_EXPIRED_TAG, true);
            return;
        }
        if (player.tickCount % 10 != 0) {
            return;
        }

        for (Mob mob : player.level().getEntitiesOfClass(Mob.class,
                player.getBoundingBox().inflate(3.0D),
                target -> target.isAlive() && target.distanceToSqr(player) <= 9.0D)) {
            var mobData = mob.getPersistentData();
            if (mobData.getLong(SPIN_TARGET_COOLDOWN_TAG) > now) {
                continue;
            }
            mobData.putLong(SPIN_TARGET_COOLDOWN_TAG, now + 10L);
            mob.hurt(player.level().damageSources().playerAttack(player), 2.0F);
            Vec3 direction = mob.position().subtract(player.position());
            if (direction.horizontalDistanceSqr() < 0.01D) {
                direction = new Vec3(player.getLookAngle().x, 0.0D, player.getLookAngle().z);
            }
            if (direction.horizontalDistanceSqr() > 0.0D) {
                direction = direction.normalize();
                mob.setDeltaMovement(direction.x * 1.8D, 0.45D, direction.z * 1.8D);
                mob.hasImpulse = true;
            }
        }
        if (player.level() instanceof ServerLevel level) {
            DustParticleOptions dust = new DustParticleOptions(
                    new Vector3f(0.35F, 0.72F, 1.0F), 1.0F);
            for (int segment = 0; segment < 12; segment++) {
                double angle = segment * Math.PI / 6.0D + now * 0.25D;
                level.sendParticles(ParticleTypes.CLOUD,
                        player.getX() + Math.cos(angle) * 2.4D,
                        player.getY() + 0.6D + (segment % 3) * 0.25D,
                        player.getZ() + Math.sin(angle) * 2.4D,
                        1, 0.05D, 0.08D, 0.05D, 0.0D);
                if (segment % 2 == 0) {
                    level.sendParticles(dust,
                            player.getX() + Math.cos(angle) * 1.7D,
                            player.getY() + 0.8D,
                            player.getZ() + Math.sin(angle) * 1.7D,
                            1, 0.0D, 0.05D, 0.0D, 0.0D);
                }
            }
        }
    }

    public static void attach(LivingEntity entity) {
        Power power = PowerManager.getInstance(entity.level()).getPower(POWER_ID);
        if (power == null) {
            throw new IllegalStateException("Missing Palladium power " + POWER_ID);
        }
        if (!SuperpowerUtil.hasSuperpower(entity, power)) {
            SuperpowerUtil.addSuperpower(entity, power);
        }
    }

    public static void attachWorthiness(LivingEntity entity) {
        Power power = PowerManager.getInstance(entity.level()).getPower(WORTHY_POWER_ID);
        if (power == null) {
            throw new IllegalStateException("Missing Palladium power " + WORTHY_POWER_ID);
        }
        if (!SuperpowerUtil.hasSuperpower(entity, power)) {
            SuperpowerUtil.addSuperpower(entity, power);
        }
    }

    public static Optional<EnergyBar> getBar(LivingEntity entity) {
        Power power = PowerManager.getInstance(entity.level()).getPower(POWER_ID);
        if (power == null) {
            return Optional.empty();
        }
        return PowerManager.getPowerHandler(entity)
                .map(handler -> handler.getPowerHolder(power))
                .map(holder -> holder.getEnergyBars().get(BAR_ID));
    }

    public static int getEnergy(LivingEntity entity) {
        return getBar(entity).map(EnergyBar::get).orElse(0);
    }

    public static int addEnergy(LivingEntity entity, int amount) {
        return getBar(entity).map(bar -> {
            int added = bar.add(amount);
            persistEnergy(entity, bar.get());
            return added;
        }).orElse(0);
    }

    public static boolean consume(LivingEntity entity, int amount) {
        Optional<EnergyBar> bar = getBar(entity);
        if (bar.isEmpty() || bar.get().get() < amount) {
            return false;
        }
        bar.get().add(-amount);
        persistEnergy(entity, bar.get().get());
        return true;
    }

    private static boolean holdsMjolnir(Player player) {
        return player.getMainHandItem().is(MarvelZombieMod.MJOLNIR.get())
                || player.getOffhandItem().is(MarvelZombieMod.MJOLNIR.get());
    }

    public static boolean isWorthy(Player player) {
        return player.getPersistentData().getBoolean(MJOLNIR_WORTHY_TAG);
    }

    public static void grantWorthiness(Player player) {
        grantWorthiness(player, findHeldMjolnir(player));
    }

    public static void grantWorthiness(Player player, ItemStack mjolnir) {
        if (!isWorthy(player)) {
            player.getPersistentData().putBoolean(MJOLNIR_WORTHY_TAG, true);
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                var advancement = serverPlayer.server.getAdvancements()
                        .getAdvancement(new ResourceLocation("x", "worthy"));
                if (advancement != null) {
                    serverPlayer.getAdvancements().award(advancement, "worthy");
                }
            }
        }
        if (!mjolnir.isEmpty()) {
            UUID itemId = ensureMjolnirIdentity(mjolnir);
            player.getPersistentData().putUUID(MJOLNIR_BOUND_ID_TAG, itemId);
            mjolnir.getOrCreateTag().putUUID(MJOLNIR_OWNER_ID_TAG, player.getUUID());
            mjolnir.getOrCreateTag().putString(MJOLNIR_OWNER_NAME_TAG, player.getName().getString());
        }
    }

    public static Optional<UUID> getBoundMjolnirId(Player player) {
        var data = player.getPersistentData();
        return data.hasUUID(MJOLNIR_BOUND_ID_TAG)
                ? Optional.of(data.getUUID(MJOLNIR_BOUND_ID_TAG))
                : Optional.empty();
    }

    public static boolean isBoundMjolnir(Player player, ItemStack mjolnir) {
        return getBoundMjolnirId(player)
                .map(itemId -> mjolnir.hasTag()
                        && mjolnir.getTag().hasUUID(MJOLNIR_ITEM_ID_TAG)
                        && mjolnir.getTag().getUUID(MJOLNIR_ITEM_ID_TAG).equals(itemId))
                .orElse(false);
    }

    private static UUID ensureMjolnirIdentity(ItemStack mjolnir) {
        var tag = mjolnir.getOrCreateTag();
        if (!tag.hasUUID(MJOLNIR_ITEM_ID_TAG)) {
            tag.putUUID(MJOLNIR_ITEM_ID_TAG, UUID.randomUUID());
        }
        return tag.getUUID(MJOLNIR_ITEM_ID_TAG);
    }

    private static ItemStack findHeldMjolnir(Player player) {
        if (player.getMainHandItem().is(MarvelZombieMod.MJOLNIR.get())) {
            return player.getMainHandItem();
        }
        if (player.getOffhandItem().is(MarvelZombieMod.MJOLNIR.get())) {
            return player.getOffhandItem();
        }
        return ItemStack.EMPTY;
    }

    private static void bindHeldMjolnir(Player player) {
        ItemStack mjolnir = findHeldMjolnir(player);
        if (!mjolnir.isEmpty() && isWorthy(player)) {
            grantWorthiness(player, mjolnir);
        }
    }

    public static void detach(LivingEntity entity) {
        Power power = PowerManager.getInstance(entity.level()).getPower(POWER_ID);
        if (power != null) {
            SuperpowerUtil.removeSuperpower(entity, power);
        }
    }

    private static void synchronizeEnergyBar(Player player) {
        var data = player.getPersistentData();
        if (!holdsMjolnir(player)) {
            data.putBoolean(ENERGY_BAR_ACTIVE_TAG, false);
            return;
        }

        Optional<EnergyBar> energyBar = getBar(player);
        if (energyBar.isEmpty()) {
            return;
        }

        EnergyBar bar = energyBar.get();
        if (!data.getBoolean(ENERGY_BAR_ACTIVE_TAG) && data.getBoolean(HAS_STORED_ENERGY_TAG)) {
            int savedEnergy = Math.max(0, Math.min(data.getInt(STORED_ENERGY_TAG), bar.getMax()));
            bar.add(savedEnergy - bar.get());
        }
        data.putBoolean(ENERGY_BAR_ACTIVE_TAG, true);
        persistEnergy(player, bar.get());
    }

    private static void persistEnergy(LivingEntity entity, int energy) {
        if (!(entity instanceof Player player)) {
            return;
        }
        var data = player.getPersistentData();
        data.putInt(STORED_ENERGY_TAG, energy);
        data.putBoolean(HAS_STORED_ENERGY_TAG, true);
    }

    private static void updateChargedAttributes(Player player) {
        boolean charged = holdsMjolnir(player) && getEnergy(player) > 0;
        var attributes = new net.minecraft.world.entity.ai.attributes.Attribute[] {
                Attributes.MAX_HEALTH,
                Attributes.MOVEMENT_SPEED,
                Attributes.ATTACK_DAMAGE,
                Attributes.ARMOR,
                Attributes.ARMOR_TOUGHNESS,
                Attributes.ATTACK_SPEED,
                Attributes.ATTACK_KNOCKBACK,
                Attributes.KNOCKBACK_RESISTANCE,
                Attributes.FLYING_SPEED
        };

        for (var attribute : attributes) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) {
                continue;
            }
            boolean hasModifier = instance.getModifier(CHARGED_ATTRIBUTE_MODIFIER_UUID) != null;
            if (charged && !hasModifier) {
                instance.addTransientModifier(new AttributeModifier(
                        CHARGED_ATTRIBUTE_MODIFIER_UUID,
                        "Marvel Zombie Charged Mjolnir",
                        1.0D,
                        AttributeModifier.Operation.MULTIPLY_TOTAL));
            } else if (!charged && hasModifier) {
                instance.removeModifier(CHARGED_ATTRIBUTE_MODIFIER_UUID);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        if (event.phase != TickEvent.Phase.END || player.level().isClientSide) {
            return;
        }
        if (holdsMjolnir(player) && isWorthy(player)) {
            attach(player);
            attachWorthiness(player);
        } else if (!holdsMjolnir(player)) {
            detach(player);
        }

        bindHeldMjolnir(player);

        if (holdsMjolnir(player) && !isWorthy(player)) {
            player.getPersistentData().putInt(MJOLNIR_LIFTING_TAG,
                    player.getPersistentData().getInt(MJOLNIR_LIFTING_TAG) + 1);
            if (player.getPersistentData().getInt(MJOLNIR_LIFTING_TAG) >= 20) {
                grantWorthiness(player, findHeldMjolnir(player));
                attach(player);
                attachWorthiness(player);
            }
        } else if (isWorthy(player)) {
            attachWorthiness(player);
        } else if (!holdsMjolnir(player)) {
            player.getPersistentData().remove(MJOLNIR_LIFTING_TAG);
        }
        recallPressedMjolnir(player);
        synchronizeEnergyBar(player);
        updateChargedAttributes(player);
        activateStormOnPress(player);
        spinMjolnir(player);
        keepStormUserImmobile(player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        event.getEntity().getPersistentData().putBoolean(ENERGY_BAR_ACTIVE_TAG, false);
    }

    @SubscribeEvent
    public static void onMjolnirInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof ThrownMjolnir mjolnir)
                || mjolnir.level().isClientSide) {
            return;
        }
        Player player = event.getEntity();
        if (mjolnir.tryPickup(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMjolnirDropped(ItemTossEvent event) {
        Player player = event.getPlayer();
        if (!event.getEntity().getItem().is(MarvelZombieMod.MJOLNIR.get())
                || player.level().isClientSide) {
            return;
        }
        var droppedItem = event.getEntity();
        ThrownMjolnir mjolnir = new ThrownMjolnir(
                player.level(), player, droppedItem.getItem().copy());
        mjolnir.setPos(droppedItem.position());
        mjolnir.setDeltaMovement(droppedItem.getDeltaMovement());
        if (!player.level().addFreshEntity(mjolnir)) {
            return;
        }
        droppedItem.discard();
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player player
                && holdsMjolnir(player)
                && getEnergy(player) >= MAX_ENERGY
                && event.getSource().getEntity() instanceof Mob attacker
                && event.getSource().getDirectEntity() == attacker
                && player.getRandom().nextFloat() < 0.3F) {
            var data = attacker.getPersistentData();
            if (!data.contains(COUNTER_WAS_NO_AI_TAG)) {
                data.putBoolean(COUNTER_WAS_NO_AI_TAG, attacker.isNoAi());
            }
            data.putLong(COUNTER_IMMOBILE_UNTIL_TAG, player.level().getGameTime() + 20L);
            attacker.setNoAi(true);
            attacker.getNavigation().stop();
            attacker.setDeltaMovement(Vec3.ZERO);
            attacker.invulnerableTime = 0;
            attacker.hurt(player.level().damageSources().lightningBolt(), 10.0F);
            if (player.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        attacker.getX(), attacker.getY() + attacker.getBbHeight() * 0.6D,
                        attacker.getZ(), 16, 0.35D, 0.5D, 0.35D, 0.08D);
            }
        }
    }

    @SubscribeEvent
    public static void onCounterMobTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide) {
            return;
        }
        var data = mob.getPersistentData();
        long immobileUntil = data.getLong(COUNTER_IMMOBILE_UNTIL_TAG);
        if (immobileUntil <= mob.level().getGameTime()) {
            if (data.contains(COUNTER_IMMOBILE_UNTIL_TAG)) {
                mob.setNoAi(data.getBoolean(COUNTER_WAS_NO_AI_TAG));
                data.remove(COUNTER_IMMOBILE_UNTIL_TAG);
                data.remove(COUNTER_WAS_NO_AI_TAG);
            }
            return;
        }
        mob.getNavigation().stop();
        mob.setDeltaMovement(Vec3.ZERO);
        mob.hasImpulse = true;
    }

    private static boolean isAbilityEnabled(Player player, String abilityId) {
        return getAbility(player, abilityId)
                .map(AbilityInstance::isEnabled)
                .orElse(false);
    }

    private static Optional<AbilityInstance> getAbility(Player player, String abilityId) {
        Power power = PowerManager.getInstance(player.level()).getPower(POWER_ID);
        if (power == null) {
            return Optional.empty();
        }
        return PowerManager.getPowerHandler(player)
                .map(handler -> handler.getPowerHolder(power))
                .map(IPowerHolder::getAbilities)
                .map(abilities -> abilities.get(abilityId));
    }

    private static Optional<AbilityInstance> getWorthyAbility(Player player, String abilityId) {
        Power power = PowerManager.getInstance(player.level()).getPower(WORTHY_POWER_ID);
        if (power == null) {
            return Optional.empty();
        }
        return PowerManager.getPowerHandler(player)
                .map(handler -> handler.getPowerHolder(power))
                .map(IPowerHolder::getAbilities)
                .map(abilities -> abilities.get(abilityId));
    }

    private static void recallPressedMjolnir(Player player) {
        if (!isWorthy(player) || holdsMjolnir(player)) {
            return;
        }
        boolean pressed = getWorthyAbility(player, "mjolnir_recall")
                .map(ability -> ability.getEnabledTicks() == 1)
                .orElse(false);
        if (!pressed) {
            return;
        }
        player.level().getEntitiesOfClass(ThrownMjolnir.class,
                        player.getBoundingBox().inflate(256.0D),
                        mjolnir -> mjolnir.getOwner() == player
                                && isBoundMjolnir(player, mjolnir.getItem()))
                .forEach(ThrownMjolnir::recall);
    }

}
