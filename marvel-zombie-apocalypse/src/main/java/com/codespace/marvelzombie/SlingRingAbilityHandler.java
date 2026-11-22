package com.codespace.marvelzombie;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.Power;
import net.threetag.palladium.power.PowerManager;
import net.threetag.palladium.power.ability.AbilityInstance;

@Mod.EventBusSubscriber(modid = MarvelZombieMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SlingRingAbilityHandler {
    private static final ResourceLocation POWER_ID =
            new ResourceLocation(MarvelZombieMod.MOD_ID, "sling_ring");
    private static final String OPEN_ABILITY_ACTIVE_TAG = "MarvelZombieSlingRingAbilityActive";

    private SlingRingAbilityHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        if (event.phase != TickEvent.Phase.END || player.level().isClientSide) {
            return;
        }

        boolean enabled = isAbilityEnabled(player);
        var data = player.getPersistentData();
        boolean wasEnabled = data.getBoolean(OPEN_ABILITY_ACTIVE_TAG);
        data.putBoolean(OPEN_ABILITY_ACTIVE_TAG, enabled);

        if (enabled && !wasEnabled
                && (player.getMainHandItem().is(MarvelZombieMod.SLING_RING.get())
                || player.getOffhandItem().is(MarvelZombieMod.SLING_RING.get()))
                && !player.getCooldowns().isOnCooldown(MarvelZombieMod.SLING_RING.get())
                && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            SlingRingNetwork.openScreen(serverPlayer);
        }
    }

    private static boolean isAbilityEnabled(Player player) {
        Power power = PowerManager.getInstance(player.level()).getPower(POWER_ID);
        if (power == null) {
            return false;
        }
        return PowerManager.getPowerHandler(player)
                .map(handler -> handler.getPowerHolder(power))
                .map(IPowerHolder::getAbilities)
                .map(abilities -> abilities.get("open_portal"))
                .map(AbilityInstance::isEnabled)
                .orElse(false);
    }
}
