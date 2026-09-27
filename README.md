# Giscraft — Forge 1.7.10

A Forge 1.7.10 coremod with ridden-pig movement and small gameplay changes.

## What it changes

Ridden pigs accelerate smoothly while controlled and stop when the rider is
not holding a suitable carrot on a stick. A vanilla carrot on a stick reaches
4 blocks per second. The mod's golden carrot on a stick reaches 8 blocks per
second and is crafted shapelessly from a fishing rod and a golden carrot. It
uses a golden recolor of the vanilla carrot-on-a-stick texture. While ridden, pigs can
automatically step onto one-block-tall obstacles.

The mod also removes experience-orb generation, stops oak leaves from dropping
apples, removes poisonous potatoes from potato harvests, makes logs require an axe (punching is slow and drops nothing, like
stone without a pickaxe), makes fishing catch only the original vanilla fish,
and changes chest collision and rendering to use a full-cube shape with a
limited lid angle.

In the overworld, IC2 copper, tin, and uranium are scarcer on land than in the deep ocean. A deep-ocean chunk averages three times as many blocks of each:

| Ore | Land | Deep ocean |
|---|---:|---:|
| Copper | 33 | 99 |
| Tin | 25 | 76 |
| Uranium | 2.2 | 6.5 |

Copper and uranium vary by a vein or two from chunk to chunk; those figures are the average. Vanilla coal, iron, gold, redstone, diamond, and lapis keep their normal rate on land and are halved in deep ocean. Other dimensions are unchanged. Chunks already saved on disk keep the ores they were generated with.

## Build

Use Java 8.

This project targets Forge `1.7.10-10.13.4.1614` and uses the maintained anatawa12 ForgeGradle 1.2 fork.

The ZIP intentionally does not include a Gradle wrapper binary. The easiest route is to use the maintained `anatawa12/ForgeGradle-example` 1.7.10 template, copy this project over it, and keep that template's wrapper. Alternatively, with a compatible Gradle installation (the template uses Gradle 5.6.4), run:

```text
gradle setupDecompWorkspace
gradle build
```

The built jar will be under `build/libs/`.

Put the jar in the normal `mods` folder on both client and server.

## Implementation note

This is a coremod because chest geometry and vanilla controlled-pig movement
require targeted bytecode changes in Minecraft 1.7.10.

## How to build

```bash
.\gradlew.bat build
```

### To run

```bash
.\gradlew.bat runClient --debug-jvm
```

Press Ctrl+Shift+D, choose Attach to Minecraft (5005), then press F5.

## Map preview

`OceanBoundaryPreview` draws a top-down picture of a seed after the ocean boundary is applied. It samples Old World Gen's beta terrain, so the picture shows beta land and water plus the boundary. Run it with Java 8 against the built jar:

```text
java -cp build/libs/Giscraft-1.3.15.jar doc.fasterminecarts.OceanBoundaryPreview --seed 8675309 --out terrain-preview.png
```

North is up. The red mark is the center, which is spawn when `centerX` and `centerZ` stay at 0. The bottom scale is X and the left scale is Z, with a tick every 1024 blocks from spawn. Higher land is lighter.

Colors:

- white: snow (tundra and taiga)
- light green: open grassland (plains, savanna, shrubland)
- dark green: forest (forest, seasonal forest, rainforest, swamp)
- yellow: desert
- blue: water, including beta oceans and the boundary ocean
- dark blue: deep ocean, about 30 blocks of water, reached across a 20-block slope
- pale blue: frozen ocean, the 200 blocks of snow before the ice wall
- white: the ice wall, about 30 blocks above the sea, with an angular jagged border and an eroded lip
- gray: bare bedrock for 256 blocks after the ice
- black: the void past the bedrock

After 1536 blocks of deep ocean the ice wall rises from the sea floor. Bedrock climbs under the ice for 256 blocks, overtakes it, then runs flat for another 256 blocks and stops. Beyond that the world is empty and players fall. There is no wrap.

A default terrain map is 320 pixels on a side and takes about a minute. `--pixels 640` is sharper and takes a few minutes. `--zone` skips the terrain and draws only the ocean zones, which is immediate.

The beta picture needs `NostalgiaGenerator` on the build machine and again when you run the tool. The build looks for `C:/Users/alexi/curseforge/minecraft/Instances/1.7.10/mods/NostalgiaGenerator-1.0.0-1.7.10.jar`, or the path you pass with `-PowgJar=...`. At runtime the tool looks in that same mods folder, or uses `--owg path\to\NostalgiaGenerator.jar`. The biome colors follow the original beta climate. Worlds that switch Old World Gen to a later biome set will not match the picture.

With no `--config`, the tool reads `Instances/1.7.10/config/giscraft.cfg` when that file exists, including `circular`. `--config path\to\giscraft.cfg` selects another file. Options after it replace those values. The same names exist in the config:

```text
--seed <long>              world seed
--out <file.png>           output image (default: ocean-preview.png)
--radius <blocks>          half-width around the center
--pixels <n>               map width and height (default: 320, or 1000 with --zone)
--owg <jar>                NostalgiaGenerator jar
--threads <n>              sampling threads (default: all processors)
--centerX <blocks>
--centerZ <blocks>
--transitionStart <blocks>
--fullOceanRadius <blocks>
--seaLevel <blocks>
--oceanFloor <blocks>
--oceanFloorVariation <blocks>
--coastlineAmplitude <blocks>
--coastlineScale <number>
--deepOceanStart <blocks>
--deepOceanFloor <blocks>
--deepOceanTransition <blocks>
--iceWallGap <blocks>
--iceSnowLead <blocks>
--iceShelfLength <blocks>
--bedrockRun <blocks>
--iceWallHeight <blocks>
--bedrockExtra <blocks>
--iceWaveAmplitude <blocks>
--iceHeightJitter <blocks>
--circular true|false
--coastlineNoise true|false
--useDeepOcean true|false
```

Example with a config file and a wider transition:

```text
java -cp build/libs/Giscraft-1.3.15.jar doc.fasterminecarts.OceanBoundaryPreview --config config/giscraft.cfg --transitionStart 6000 --fullOceanRadius 8000 --out wide-preview.png
```