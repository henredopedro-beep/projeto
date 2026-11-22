package com.codespace.marvelzombie;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(MarvelZombieMod.MOD_ID)
public class MarvelZombieMod {
    public static final String MOD_ID = "marvel_zombie";

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MOD_ID);

    public static final RegistryObject<Item> CAPTAIN_SHIELD = ITEMS.register(
            "captain_shield", CaptainShieldItem::new);
    public static final RegistryObject<Item> MJOLNIR = ITEMS.register(
            "mjolnir", () -> new MjolnirItem(Tiers.NETHERITE, 4, -2.4F,
                    new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> SMOKE_BOMB = ITEMS.register(
            "smoke_bomb", SmokeBombItem::new);
    public static final RegistryObject<Item> SLING_RING = ITEMS.register(
            "sling_ring", SlingRingItem::new);

    public static final RegistryObject<CreativeModeTab> APOCALYPSE_TAB = CREATIVE_TABS.register(
            "apocalypse", () -> CreativeModeTab.builder()
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> CAPTAIN_SHIELD.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(CAPTAIN_SHIELD.get());
                        output.accept(MJOLNIR.get());
                        output.accept(SMOKE_BOMB.get());
                        output.accept(SLING_RING.get());
                    })
                    .build());

    public static final RegistryObject<EntityType<CyclopsZombie>> CYCLOPS_ZOMBIE =
            ENTITY_TYPES.register("cyclops_zombie", () ->
                    EntityType.Builder.<CyclopsZombie>of(CyclopsZombie::new, MobCategory.MONSTER)
                            .sized(0.9F, 2.6F)
                            .clientTrackingRange(8)
                            .updateInterval(3)
                            .build(new ResourceLocation(MOD_ID, "cyclops_zombie").toString()));

    public static final RegistryObject<EntityType<ThorZombie>> THOR_ZOMBIE =
            ENTITY_TYPES.register("thor_zombie", () ->
                    EntityType.Builder.<ThorZombie>of(ThorZombie::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F)
                            .clientTrackingRange(8)
                            .updateInterval(3)
                            .build(new ResourceLocation(MOD_ID, "thor_zombie").toString()));

    public static final RegistryObject<EntityType<HulkZombie>> HULK_ZOMBIE =
            ENTITY_TYPES.register("hulk_zombie", () ->
                    EntityType.Builder.<HulkZombie>of(HulkZombie::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F)
                            .clientTrackingRange(8)
                            .updateInterval(3)
                            .build(new ResourceLocation(MOD_ID, "hulk_zombie").toString()));

    public static final RegistryObject<EntityType<CaptainZombie>> CAPTAIN_ZOMBIE =
            ENTITY_TYPES.register("captain_zombie", () ->
                    EntityType.Builder.<CaptainZombie>of(CaptainZombie::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F)
                            .clientTrackingRange(8)
                            .updateInterval(3)
                            .build(new ResourceLocation(MOD_ID, "captain_zombie").toString()));

    public static final RegistryObject<EntityType<ThrownCaptainShield>> THROWN_CAPTAIN_SHIELD =
            ENTITY_TYPES.register("thrown_captain_shield", () ->
                    EntityType.Builder.<ThrownCaptainShield>of(
                            ThrownCaptainShield::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(4)
                            .updateInterval(2)
                            .build(new ResourceLocation(MOD_ID, "thrown_captain_shield").toString()));

    public static final RegistryObject<EntityType<ThrownMjolnir>> THROWN_MJOLNIR =
            ENTITY_TYPES.register("thrown_mjolnir", () ->
                    EntityType.Builder.<ThrownMjolnir>of(ThrownMjolnir::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(4)
                            .updateInterval(2)
                            .build(new ResourceLocation(MOD_ID, "thrown_mjolnir").toString()));

    public static final RegistryObject<EntityType<ThrownSmokeBomb>> THROWN_SMOKE_BOMB =
            ENTITY_TYPES.register("thrown_smoke_bomb", () ->
                    EntityType.Builder.<ThrownSmokeBomb>of(ThrownSmokeBomb::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(4)
                            .updateInterval(10)
                            .build(new ResourceLocation(MOD_ID, "thrown_smoke_bomb").toString()));

    public static final RegistryObject<EntityType<SlingRingPortal>> SLING_RING_PORTAL =
            ENTITY_TYPES.register("sling_ring_portal", () ->
                    EntityType.Builder.<SlingRingPortal>of(SlingRingPortal::new, MobCategory.MISC)
                            .sized(2.4F, 2.8F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build(new ResourceLocation(MOD_ID, "sling_ring_portal").toString()));

    public MarvelZombieMod(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        ITEMS.register(modBus);
        CREATIVE_TABS.register(modBus);
        ENTITY_TYPES.register(modBus);
        modBus.addListener(this::registerEntityAttributes);
        modBus.addListener(this::registerSpawnPlacements);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener(ClientSetup::registerEntityRenderers);
        }
        ShieldNetwork.register();
        ThorNetwork.register();
        SlingRingNetwork.register();
    }

    private void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(CYCLOPS_ZOMBIE.get(), CyclopsZombie.createAttributes().build());
        event.put(THOR_ZOMBIE.get(), ThorZombie.createAttributes().build());
        event.put(HULK_ZOMBIE.get(), HulkZombie.createAttributes().build());
        event.put(CAPTAIN_ZOMBIE.get(), CaptainZombie.createAttributes().build());
    }

    private void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
        event.register(CYCLOPS_ZOMBIE.get(), SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
                SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(THOR_ZOMBIE.get(), SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
                SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(HULK_ZOMBIE.get(), SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
                SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(CAPTAIN_ZOMBIE.get(), SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
                SpawnPlacementRegisterEvent.Operation.REPLACE);
    }

    public static class ClientSetup {
        public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(CYCLOPS_ZOMBIE.get(), CyclopsZombieRenderer::new);
            event.registerEntityRenderer(THOR_ZOMBIE.get(), ThorZombieRenderer::new);
            event.registerEntityRenderer(HULK_ZOMBIE.get(), HulkZombieRenderer::new);
            event.registerEntityRenderer(CAPTAIN_ZOMBIE.get(), CaptainZombieRenderer::new);
            event.registerEntityRenderer(THROWN_CAPTAIN_SHIELD.get(), ThrownCaptainShieldRenderer::new);
            event.registerEntityRenderer(THROWN_MJOLNIR.get(),
                    ThrownMjolnirRenderer::new);
            event.registerEntityRenderer(THROWN_SMOKE_BOMB.get(),
                    context -> new ThrownItemRenderer<>(context));
            event.registerEntityRenderer(SLING_RING_PORTAL.get(), SlingRingPortalRenderer::new);
        }
    }
}
