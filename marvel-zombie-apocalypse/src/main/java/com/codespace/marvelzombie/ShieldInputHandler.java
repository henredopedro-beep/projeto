package com.codespace.marvelzombie;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MarvelZombieMod.MOD_ID, value = Dist.CLIENT)
public final class ShieldInputHandler {
    private ShieldInputHandler() {
    }

    @SubscribeEvent
    public static void onAttackInput(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) {
            return;
        }

        Player player = Minecraft.getInstance().player;
        if (player == null || !player.isCrouching()
                || player.getCooldowns().isOnCooldown(MarvelZombieMod.CAPTAIN_SHIELD.get())) {
            return;
        }

        boolean holdingShield = player.getMainHandItem().is(MarvelZombieMod.CAPTAIN_SHIELD.get())
                || player.getOffhandItem().is(MarvelZombieMod.CAPTAIN_SHIELD.get());
        if (holdingShield) {
            event.setCanceled(true);
            ShieldNetwork.throwShield();
        }
    }
}
