package com.codespace.marvelzombie;

import net.minecraft.resources.ResourceLocation;
import net.threetag.palladium.power.ability.AbilityReference;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.SyncType;

public final class CyclopsPalladium {
    public static final IntegerProperty BEAM_ACTIVE = createBeamActiveProperty();
    public static final AbilityReference BEAM_ABILITY = new AbilityReference(
            new ResourceLocation(MarvelZombieMod.MOD_ID, "cyclops_zombie"), "optic_blast");

    private CyclopsPalladium() {
    }

    private static IntegerProperty createBeamActiveProperty() {
        IntegerProperty property = new IntegerProperty("cyclops_beam_active");
        property.sync(SyncType.EVERYONE);
        property.disablePersistence();
        return property;
    }
}
