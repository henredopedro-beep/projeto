package com.codespace.marvelzombie;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MarvelZombieMod.MOD_ID, value = Dist.CLIENT)
public final class ThorInputHandler {
    private ThorInputHandler() {
    }

    @SubscribeEvent
    public static void onAttackInput(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) {
            return;
        }

        Player player = Minecraft.getInstance().player;
        if (player == null || !player.isCrouching()
                || player.getCooldowns().isOnCooldown(MarvelZombieMod.MJOLNIR.get())) {
            return;
        }

        boolean holdingMjolnir = player.getMainHandItem().is(MarvelZombieMod.MJOLNIR.get())
                || player.getOffhandItem().is(MarvelZombieMod.MJOLNIR.get());
        if (holdingMjolnir) {
            event.setCanceled(true);
            ThorNetwork.sendAction(ThorNetwork.Action.THROW_MJOLNIR);
        } else if (ThorEnergy.isWorthy(player)) {
            event.setCanceled(true);
            ThorNetwork.sendAction(ThorNetwork.Action.RECALL_MJOLNIR);
        }
    }
}
