package com.example.examplemod.manager;

import com.example.examplemod.config.StackConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.animal.Sheep;
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

    private StackGroupManager() {
    }

    public static StackGroupManager getInstance() {
        return INSTANCE;
    }

    public void rebuild(ClientLevel level) {
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
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity livingEntity)) {
                continue;
            }
            if (!isGroupCandidate(minecraft, player, livingEntity, maxDistanceSqr)) {
                continue;
            }
            StackKey key = new StackKey(livingEntity.getType(), livingEntity.blockPosition(), getVariant(livingEntity));
            candidates.computeIfAbsent(key, ignored -> new ArrayList<>()).add(livingEntity);
        }

        for (Map.Entry<StackKey, List<LivingEntity>> entry : candidates.entrySet()) {
            List<LivingEntity> members = entry.getValue();
            if (members.size() < StackConfig.minimumStackSize) {
                continue;
            }
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

    public void clear() {
        groups.clear();
        suppressedEntities.clear();
        representativeCounts.clear();
        lastDimension = null;
    }

    private boolean isGroupCandidate(Minecraft minecraft, Player player, LivingEntity entity, double maxDistanceSqr) {
        if (entity == player) {
            return false;
        }
        if (entity instanceof ArmorStand) {
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

    private int getVariant(LivingEntity entity) {
        if (entity.getType() == EntityType.SHEEP && entity instanceof Sheep sheep && StackConfig.groupSheepByColor) {
            return sheep.getColor().getId();
        }
        return 0;
    }
}
