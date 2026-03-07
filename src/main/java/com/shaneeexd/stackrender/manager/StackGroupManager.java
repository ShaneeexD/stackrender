package com.shaneeexd.stackrender.manager;

import com.shaneeexd.stackrender.StackRender;
import com.shaneeexd.stackrender.config.StackConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class StackGroupManager {
    private static final StackGroupManager INSTANCE = new StackGroupManager();
    private static final Comparator<LivingEntity> UUID_COMPARATOR = Comparator.comparing(Entity::getUUID);

    private final Map<StackKey, List<LivingEntity>> groups = new HashMap<>();
    private final Set<UUID> suppressedEntities = new HashSet<>();
    private final Map<UUID, Integer> representativeCounts = new HashMap<>();
    private ResourceKey<Level> lastDimension;
    private int lastCandidateCount;
    private int lastGroupCount;
    private int lastSuppressedCount;
    private long lastRebuildNanos;

    private StackGroupManager() {
    }

    public static StackGroupManager getInstance() {
        return INSTANCE;
    }

    public void rebuild(ClientLevel level) {
        long rebuildStart = System.nanoTime();
        if (!StackConfig.enabled) {
            clear();
            lastDimension = level.dimension();
            return;
        }

        if (lastDimension != null && lastDimension != level.dimension()) {
            clear();
        }
        lastDimension = level.dimension();

        groups.clear();
        suppressedEntities.clear();
        representativeCounts.clear();

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            return;
        }

        double maxDistanceSqr = (double) StackConfig.maxScanDistance * (double) StackConfig.maxScanDistance;
        Map<StackKey, List<LivingEntity>> candidates = new HashMap<>();
        int candidateCount = 0;
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity livingEntity)) {
                continue;
            }
            if (!isGroupCandidate(minecraft, player, livingEntity, maxDistanceSqr)) {
                continue;
            }
            candidateCount++;
            StackKey key = new StackKey(livingEntity.getType(), getGroupingPos(livingEntity), getVariant(livingEntity));
            candidates.computeIfAbsent(key, ignored -> new ArrayList<>()).add(livingEntity);
        }

        int groupCount = 0;
        for (Map.Entry<StackKey, List<LivingEntity>> entry : candidates.entrySet()) {
            List<LivingEntity> members = entry.getValue();
            if (members.size() < StackConfig.minimumStackSize) {
                continue;
            }
            groupCount++;
            members.sort(UUID_COMPARATOR);
            LivingEntity representative = members.get(0);
            groups.put(entry.getKey(), List.copyOf(members));
            representativeCounts.put(representative.getUUID(), members.size());
            for (int i = 1; i < members.size(); i++) {
                LivingEntity entity = members.get(i);
                if (shouldSuppress(minecraft, entity)) {
                    suppressedEntities.add(entity.getUUID());
                }
            }
        }

        lastCandidateCount = candidateCount;
        lastGroupCount = groupCount;
        lastSuppressedCount = suppressedEntities.size();
        lastRebuildNanos = System.nanoTime() - rebuildStart;
        if (StackConfig.debugLogging) {
            StackRender.LOGGER.info("Stack rebuild: candidates={}, groups={}, suppressed={}, ms={}", lastCandidateCount, lastGroupCount, lastSuppressedCount, String.format("%.3f", getLastRebuildMillis()));
        }
    }

    public boolean isSuppressed(LivingEntity entity) {
        return suppressedEntities.contains(entity.getUUID());
    }

    public int getStackCount(LivingEntity entity) {
        return representativeCounts.getOrDefault(entity.getUUID(), 0);
    }

    public boolean isRepresentative(LivingEntity entity) {
        return representativeCounts.containsKey(entity.getUUID());
    }

    public int getLastCandidateCount() {
        return lastCandidateCount;
    }

    public int getLastGroupCount() {
        return lastGroupCount;
    }

    public int getLastSuppressedCount() {
        return lastSuppressedCount;
    }

    public double getLastRebuildMillis() {
        return lastRebuildNanos / 1_000_000.0D;
    }

    public void clear() {
        groups.clear();
        suppressedEntities.clear();
        representativeCounts.clear();
        lastDimension = null;
        lastCandidateCount = 0;
        lastGroupCount = 0;
        lastSuppressedCount = 0;
        lastRebuildNanos = 0L;
    }

    private boolean isGroupCandidate(Minecraft minecraft, Player player, LivingEntity entity, double maxDistanceSqr) {
        if (entity == player) {
            return false;
        }
        if (entity instanceof ArmorStand) {
            return false;
        }
        if (!StackConfig.isEntityTypeEnabled(entity.getType())) {
            return false;
        }
        if (!entity.isAlive()) {
            return false;
        }
        if (entity.distanceToSqr(player) > maxDistanceSqr) {
            return false;
        }
        if (entity.hasCustomName()) {
            return false;
        }
        if (entity.isPassenger() || entity.isVehicle() || entity instanceof Mob mob && mob.isLeashed()) {
            return false;
        }
        MobCategory category = entity.getType().getCategory();
        if (category == MobCategory.MONSTER) {
            return StackConfig.stackHostileMobs;
        }
        return category != MobCategory.MISC && StackConfig.stackPassiveMobs;
    }

    private boolean shouldSuppress(Minecraft minecraft, LivingEntity entity) {
        if (entity == minecraft.crosshairPickEntity) {
            return false;
        }
        if (entity.hasCustomName()) {
            return false;
        }
        if (entity.isPassenger() || entity.isVehicle() || entity instanceof Mob mob && mob.isLeashed()) {
            return false;
        }
        if (StackConfig.suppressDeathAnimations && entity.deathTime > 0) {
            return false;
        }
        return entity.isAlive();
    }

    private BlockPos getGroupingPos(LivingEntity entity) {
        int cellSize = Math.max(1, StackConfig.groupingCellSize);
        BlockPos pos = entity.blockPosition();
        if (cellSize == 1) {
            return pos;
        }
        return new BlockPos(
                Math.floorDiv(pos.getX(), cellSize) * cellSize,
                Math.floorDiv(pos.getY(), cellSize) * cellSize,
                Math.floorDiv(pos.getZ(), cellSize) * cellSize
        );
    }

    private int getVariant(LivingEntity entity) {
        if (entity.getType() == EntityType.SHEEP && entity instanceof Sheep sheep && StackConfig.groupSheepByColor) {
            return sheep.getColor().getId();
        }
        if (entity instanceof Cat cat && StackConfig.groupCatByVariant) {
            return readVariantHash(cat, "variant", "CatType");
        }
        if (entity instanceof Horse horse && StackConfig.groupHorseByVariant) {
            return readVariantHash(horse, "Variant");
        }
        if (entity instanceof TropicalFish tropicalFish && StackConfig.groupTropicalFishByVariant) {
            return readVariantHash(tropicalFish, "Variant");
        }
        return 0;
    }

    private int readVariantHash(LivingEntity entity, String... keys) {
        CompoundTag tag = entity.saveWithoutId(new CompoundTag());
        int hash = 1;
        for (String key : keys) {
            if (tag.contains(key)) {
                hash = 31 * hash + tag.get(key).toString().hashCode();
            }
        }
        return hash == 1 ? 0 : hash;
    }
}


