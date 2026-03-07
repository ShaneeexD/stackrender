package com.shaneeexd.stackrender.manager;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;

public record StackKey(EntityType<?> type, BlockPos pos, int variant) {
}


