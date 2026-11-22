package com.codespace.marvelzombie;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieRenderer;

public class ThorZombieRenderer extends ZombieRenderer {
    public ThorZombieRenderer(EntityRendererProvider.Context context) {
        super(context);
        addLayer(new ThorBeamLayer(this));
    }
}
