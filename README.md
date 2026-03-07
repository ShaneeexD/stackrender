# Entity Stack Renderer

Entity Stack Renderer is a client-side Minecraft Forge mod for 1.20.1 that improves performance in mob-heavy areas by visually merging duplicate mobs into a single rendered entity with a floating stack count label.

Instead of rendering every mob in a dense farm or grinder individually, the mod groups matching mobs that occupy the same space and only renders one visible representative. The rest are hidden visually on your client while the real entities still exist normally in the world.

## What it does

- Visually stacks mobs of the same type into one rendered mob
- Shows a billboard label like `x12` above the visible representative
- Skips rendering duplicate mobs to reduce client-side rendering load
- Supports per-mob enable and disable toggles
- Supports optional variant-aware grouping for sheep, cats, horses, and tropical fish
- Includes an in-game config screen in the Mods menu
- Includes optional debug overlay and debug logging
- Rebuilds stack groups on a short throttle for better performance

## Performance

Performance gains depend on your hardware, settings, shaders, and how many mobs are on screen.

In one test case, FPS increased from about **35 FPS** to **60+ FPS**, which is an **up to 71% FPS increase**.

This is not guaranteed in every situation, but mob farms, breeders, grinders, and other crowded scenes can benefit significantly.

## How it works

The mod groups nearby mobs using:

- entity type
- grouping cell position
- selected visual variant rules

One mob becomes the visible representative for the group, and the others are skipped during rendering unless they should remain visible for gameplay clarity, such as when they are:

- under the crosshair
- dying, if death animations are kept visible
- leashed
- riding or being ridden
- custom named

## Configuration

You can configure the mod in-game through the Forge Mods menu.

Available options include:

- enable or disable the mod
- minimum stack size
- label scale
- max scan distance
- grouping cell size
- passive and hostile mob toggles
- per-mob stacking enable/disable
- sheep color grouping
- cat, horse, and tropical fish variant grouping
- debug overlay
- debug logging

## Requirements

- Minecraft 1.20.1
- Minecraft Forge 47.x
- Java 17

## Notes

- This is a **client-side visual optimization** mod.
- It does not merge server entities or change mob behavior.
- Other players and the server still see and handle the real entities normally.

## Building

To build the mod:

```bash
./gradlew build
```

The built jar will be generated in:

```text
build/libs/
```

## Author

ShaneeexD
