Marvel Zombie Apocalypse
========================

Minecraft 1.20.1 / Forge 47.x addon for Palladium, with optional KubeJS
recipes.

Features
--------
- Captain's Shield: right-click blocks; sneak + left-click throws it and it
  returns with its durability.
- Mjölnir: grants melee strength, lightning resistance, faster flight, and
  Palladium abilities. Assign keys to them in Palladium's power menu:
  hold Charge Storm to build the storm energy bar (lightning strikes while
  charging; movement is locked, and damage or releasing the key interrupts
  charging), then hold Mjölnir's Lightning to fire while energy remains.
  Bind Storm Slam there too; activate it on the ground to leap and slam the
  landing area, damaging nearby mobs and calling lightning on them. All three
  abilities use Palladium's keybinding system. Powers are available while
  Mjölnir is held in either hand.
  A continuous lightning hit shocks a mob with nausea after 2.5 seconds.
  Sprinting boosts flight.
  Sneak + left-click throws Mjölnir; it returns to the player.
- Thor Zombie: absorbs lightning, fires energy blasts, and throws a
  ricocheting Mjölnir.
- Hulk Zombie: begins as Bruce Banner and gains rage only when damaged. At full
  rage he transforms; Hulk uses regular melee attacks, a close-range ground
  slam, and occasional long-range seismic leaps. Leap craters grow with jump
  height. Calming smoke restores Banner after 100 ticks. Bedrock, obsidian,
  portal frames, wither-immune blocks, and blocks tagged
  `marvel_zombie:hulk_indestructible` are protected.
- Cyclops Zombie: uses a Palladium optic blast and a telegraphed charge.
- Marvel and vanilla zombies investigate loud sounds and bright light when no
  player is in sight.
- Smoke Bomb: right-click to spread harmless calming smoke.

Dependencies
------------
- Minecraft 1.20.1
- Forge 47.x
- Palladium 4.5.0 or newer (required)
- KubeJS 2001.6.5 or newer (optional; required for recipes)

Development
-----------
Run `./gradlew build` to compile the mod and produce the reobfuscated JAR in
`build/libs`. The development workspace needs
`../palladium-4.5.10-1.20.1-forge.jar` as a compile-time dependency.

Test mobs:
`/summon marvel_zombie:cyclops_zombie`
`/summon marvel_zombie:thor_zombie`
`/summon marvel_zombie:hulk_zombie`

Give smoke for testing:
`/give @s marvel_zombie:smoke_bomb`

Palladium resources
-------------------
Power definitions are under `src/main/resources/data/marvel_zombie/palladium/`.
Mjölnir's ability references the lightning renderer in
`energy_beams/mjolnir_lightning.json`; keep the beam ID and resource filename
in sync. The Thor beam was configured with a positive Z offset, placing its
origin behind the arm model; Cyclops' visible beam uses a negative Z offset and
an explicit render layer. Mjölnir now uses the same visible laser renderer and
a front-facing hand offset. Thor Zombie also has a dedicated render layer;
previously its attack only emitted particles, unlike Cyclops' rendered beam.
Item powers attach the same power while Mjölnir is held in either hand.

Captain's Shield uses a built-in 3D item model for the thrown projectile. Its
ricochet now occurs only after an actual block collision, with a shield impact
sound and visible sparks at the collision point.

The KubeJS recipe and Cyclops script are in
`kubejs/server_scripts/marvel_zombie/cyclops.js`.
