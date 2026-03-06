# Entity Stack Renderer — AI IDE Prompt

## Project Overview
Build a Minecraft Forge 1.20.1 client-side performance mod that visually merges mobs of the same type occupying the same block into a single rendered entity, displaying a billboard label (e.g. "x7") above it. This targets entity-cramming style farms where dozens of mobs stack in one spot, causing significant client-side rendering overhead. No server-side logic is touched — this is purely a rendering optimisation.

---

## Environment & Dependencies
- **Minecraft:** 1.20.1
- **Mod Loader:** Forge (latest stable for 1.20.1)
- **Dependencies:** None (vanilla Forge only)
- **Language:** Java 17
- **Build system:** Gradle with ForgeGradle
- **Side:** Client-only

---

## Mod Metadata
- **Mod ID:** `stackrender`
- **Mod Name:** Entity Stack Renderer
- **Description:** Visually stacks duplicate mobs on the same block into one render call with a count label, improving FPS on mob farms
- **Side:** Client-only (no server-side logic whatsoever)

---

## Core Features to Implement

### 1. Per-Tick Entity Grouping
Each client tick, scan all loaded entities and build a grouping map of mobs sharing the same block position and entity type.

- Source entities from `Minecraft.getInstance().level.entitiesForRendering()`
- Filter to `LivingEntity` instances only (exclude players, armor stands, item entities, XP orbs)
- Group by: **entity type + block position** (use `BlockPos.containing(entity.position())`)
- For sheep specifically, also group by **wool color** (`SheepEntity#getColor()`) — different colors are visually distinct and must not be merged
- Only form a stack group if 3 or more entities share the same key (configurable threshold)
- Config allows enabling/disabling certain mob types
- Store the grouping map in a singleton `StackGroupManager` that is rebuilt every tick

```java
// Grouping key
record StackKey(EntityType<?> type, BlockPos pos, int variant) {}

// variant = 0 for most mobs, DyeColor.getId() for sheep
```

### 2. Render Suppression via Mixin
Inject into `LivingEntityRenderer#render()` to cancel rendering for entities marked as hidden duplicates.

```java
@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void stackrender$suppressDuplicate(LivingEntity entity, float entityYaw,
                                                float partialTick, PoseStack poseStack,
                                                MultiBufferSource bufferSource, int packedLight,
                                                CallbackInfo ci) {
        if (StackGroupManager.getInstance().isSuppressed(entity)) {
            ci.cancel();
        }
    }
}
```

**Suppression rules — never suppress if:**
- The entity is the designated "representative" of its stack group (the one that renders)
- The entity is the player's current `Minecraft.getInstance().crosshairPickEntity` (so interactions/hits work correctly)
- The entity is in a death animation (`entity.deathTime > 0`)
- The entity is currently leashed
- The entity is a passenger or has a passenger (`entity.isPassenger()` / `entity.isVehicle()`)
- The entity has a custom nametag set by a player (`entity.hasCustomName()`)

### 3. Stack Label Rendering
For each stack group's representative entity, render a billboard "x{count}" label above its head after its normal render.

Hook into rendering via:
```java
@SubscribeEvent
public void onRenderNameTag(RenderNameTagEvent event) {
    // suppress default name tag for suppressed entities
}
```

And draw the stack label via a Mixin on `LivingEntityRenderer#render()` at `TAIL`, or via `RenderLevelStageEvent` after entity rendering.

**Label appearance:**
- Text format: `x{count}` (e.g. `x3`, `x12`, `x47`)
- Position: 0.5 blocks above the entity's head (above where a nametag would appear)
- Always face the camera (billboard — use the camera's rotation from `Minecraft.getInstance().gameRenderer.getMainCamera()`)
- Color coding by count:
  - 3–9: White (`0xFFFFFF`)
  - 10–29: Yellow (`0xFFFF00`)
  - 30–99: Orange (`0xFF8800`)
  - 100+: Red (`0xFF3333`)
- Background: semi-transparent dark panel behind the text (same style as vanilla nametags)
- Scale: fixed screen-space size so it doesn't grow huge up close — scale down with distance using `poseStack.scale()`

### 4. Stack Group Manager
```java
public class StackGroupManager {
    private static final StackGroupManager INSTANCE = new StackGroupManager();
    
    // rebuilt every tick
    private Map<StackKey, List<LivingEntity>> groups;
    private Set<UUID> suppressedEntities;
    private Map<UUID, Integer> representativeCount; // representative UUID -> stack size
    
    public static StackGroupManager getInstance();
    
    public void rebuild(ClientLevel level);           // called each client tick
    public boolean isSuppressed(LivingEntity entity);
    public int getStackCount(LivingEntity entity);   // returns count if representative, 0 otherwise
    public boolean isRepresentative(LivingEntity entity);
}
```

**Choosing the representative:** Within each group, pick the entity with the lowest UUID (deterministic, stable across ticks) as the representative. All others are suppressed.

---

## Configuration
Use Forge's `ForgeConfigSpec` for a client-side config at `config/stackrender-client.toml`:

| Key | Type | Default | Description |
|---|---|---|---|
| `enabled` | boolean | true | Master toggle |
| `minimumStackSize` | int | 3 | Min entities before stacking activates |
| `showLabel` | boolean | true | Show the x{count} label |
| `labelScale` | float | 1.0 | Scale multiplier for the label |
| `groupSheepByColor` | boolean | true | Treat different wool colors as distinct |
| `suppressDeathAnimations` | boolean | true | Never suppress dying entities |
| `maxScanDistance` | int | 64 | Only consider entities within N blocks of player |
| `stackPassiveMobs` | boolean | true | Stack cows, sheep, chickens, pigs etc. |
| `stackHostileMobs` | boolean | true | Stack zombies, skeletons, creepers etc. |

---

## Client Tick Hook
Register a client tick listener to rebuild the grouping map each tick:

```java
@SubscribeEvent
public void onClientTick(TickEvent.ClientTickEvent event) {
    if (event.phase != TickEvent.Phase.END) return;
    Minecraft mc = Minecraft.getInstance();
    if (mc.level == null || mc.player == null) return;
    StackGroupManager.getInstance().rebuild(mc.level);
}
```

---

## File Structure
```
src/main/java/com/yourname/stackrender/
├── StackRender.java                  # Main mod class
├── config/
│   └── StackConfig.java              # ForgeConfigSpec
├── manager/
│   └── StackGroupManager.java        # Core grouping logic
├── mixin/
│   ├── LivingEntityRendererMixin.java # Render suppression
│   └── LivingEntityRendererLabelMixin.java # Label injection
├── render/
│   └── StackLabelRenderer.java       # Billboard label drawing
└── event/
    └── ClientTickHandler.java         # Tick listener

src/main/resources/
├── META-INF/mods.toml
├── stackrender.mixins.json
└── pack.mcmeta
```

---

## What NOT to Do
- Do not touch server-side entity logic, AI, drops, or NBT
- Do not suppress the entity the player's crosshair is targeting
- Do not suppress entities with player-set custom names
- Do not suppress entities that are leashed, mounted, or have riders
- Do not suppress entities mid-death animation
- Do not attempt to merge entity health bars or damage — each entity remains independent server-side
- Do not run the grouping scan on the server thread — client tick only

---

## Edge Cases to Handle
- Player switches dimension — clear the grouping map immediately
- Entity leaves the stack (walks away) — it must reappear instantly next tick with no delay
- Representative entity dies — a new representative is chosen next tick, the label moves
- Very large stacks (100+) — label must still render clearly, don't overflow the string
- Mob variants beyond sheep: Tropical fish (pattern + color), Cats (skin type), Horses (color + markings) — use the same variant field in `StackKey`, derive variant from the entity's data tracker/NBT

---

## Success Criteria
- FPS measurably improves in a test world with 50+ cows in a 1x1 pen
- No visual flickering when entities move in and out of stack groups
- Crosshair targeting always hits the correct entity regardless of suppression
- Death animations play correctly for all entities in a stack
- Mod loads cleanly alongside: Rubidium, Embeddium, JEI, and Journeymap
- Config file generates on first launch and hot-reloads correctly
