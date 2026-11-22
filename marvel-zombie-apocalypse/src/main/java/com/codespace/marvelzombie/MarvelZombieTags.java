package com.codespace.marvelzombie;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class MarvelZombieTags {
    public static final TagKey<Block> HULK_INDESTRUCTIBLE = TagKey.create(
            Registries.BLOCK, new ResourceLocation(MarvelZombieMod.MOD_ID, "hulk_indestructible"));

    private MarvelZombieTags() {
    }
}
