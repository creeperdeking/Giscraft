# Faster Vanilla Minecarts — Forge 1.7.10

A Forge 1.7.10 coremod that improves vanilla minecart and ridden-pig movement.

## What it changes

Forge 1.7.10's `BlockRailBase#getRailMaxSpeed` returns `0.4` blocks/tick by default (8 m/s). This coremod replaces that one constant with a configurable value.

It does **not** replace minecarts, add rails, change powered-rail acceleration, or change recipes.

Custom rail blocks that override `getRailMaxSpeed` keep their own speed. Custom rails that simply inherit Forge's default rail speed will inherit this configured value too.

Ridden pigs now accelerate smoothly for one second until they reach a
consistent speed of 8 blocks per second. Vanilla pig steering and vertical
movement are left intact, and the acceleration resets whenever the rider
dismounts.

## Config

After first launch:

`config/FasterVanillaMinecarts.cfg`

The only setting is:

```properties
maxSpeedMetersPerSecond=16.0
```

- Vanilla: `8.0`
- Suggested: `12.0` to `16.0`
- Hard maximum in this mod: `24.0` m/s, matching Forge 1.7.10's built-in minecart-on-rail cap of `1.2` blocks/tick.

At higher speeds, Minecraft 1.7.10 minecarts can behave badly on corners/slopes and may outrun chunk loading. Straight track is safest.

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

This is a coremod because normal Forge configuration/events can raise the minecart's own speed cap, but vanilla rail blocks still impose their separate 0.4 blocks/tick limit. The ASM transformer changes only that default rail limit.

## How to build

```bash
.\gradlew.bat build
```

### To run

```bash
.\gradlew.bat runClient --debug-jvm
```

Press Ctrl+Shift+D, choose Attach to Minecraft (5005), then press F5.