EntityEvents.hurt("marvel_zombie:cyclops_zombie", event => {
  const cyclops = event.entity;
  const enragedTag = "marvel_zombie_apocalypse.enraged";

  if (cyclops.health - event.damage <= cyclops.maxHealth * 0.5
      && !cyclops.persistentData.getBoolean(enragedTag)) {
    cyclops.persistentData.putBoolean(enragedTag, true);
    cyclops.potionEffects.add("minecraft:speed", 160, 1, false, true);
    cyclops.potionEffects.add("minecraft:strength", 160, 0, false, true);
  }
});

ServerEvents.recipes(event => {
  event.shaped("marvel_zombie:captain_shield", [
    "IRI",
    "RBR",
    "IRI"
  ], {
    I: "minecraft:iron_ingot",
    R: "minecraft:red_dye",
    B: "minecraft:blue_dye"
  });

  event.shaped("marvel_zombie:mjolnir", [
    "INI",
    "III",
    " S "
  ], {
    I: "minecraft:netherite_ingot",
    N: "minecraft:nether_star",
    S: "minecraft:stick"
  });

  event.shaped("marvel_zombie:smoke_bomb", [
    " G ",
    "GBG",
    " G "
  ], {
    G: "minecraft:gunpowder",
    B: "minecraft:glass_bottle"
  });
});
