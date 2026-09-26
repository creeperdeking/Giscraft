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
apples, makes logs require an axe (punching is slow and drops nothing, like
stone without a pickaxe), makes fishing catch only the original vanilla fish,
and changes chest collision and rendering to use a full-cube shape with a
limited lid angle.

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