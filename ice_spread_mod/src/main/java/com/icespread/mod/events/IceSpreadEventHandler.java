package com.icespread.mod.events;

import com.icespread.mod.IceSpreadMod;
import com.icespread.mod.config.IceSpreadConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.util.*;

public class IceSpreadEventHandler {

    private final Map<BlockPos, Long> iceTimestamps = new HashMap<>();
    private long tickCounter = 0;

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        tickCounter++;
        convertIceToPacked();
        if (tickCounter % IceSpreadConfig.ICE_SPREAD_INTERVAL.get() == 0) {
            spreadIce();
        }
    }

    private void spreadIce() {
        if (!IceSpreadConfig.SPREAD_FROM_PLAYERS.get()) return;
        net.minecraft.server.MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        for (ServerWorld world : server.getAllLevels()) {
            List<? extends PlayerEntity> players = world.players();
            if (players.isEmpty()) continue;

            int radius = IceSpreadConfig.ICE_SPREAD_RADIUS.get();
            int maxBlocks = IceSpreadConfig.MAX_BLOCKS_PER_TICK.get();
            List<BlockPos> candidates = new ArrayList<>();

            for (PlayerEntity player : players) {
                BlockPos playerPos = player.blockPosition();
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        for (int dy = -radius; dy <= radius; dy++) {
                            if (dx*dx + dy*dy + dz*dz > radius*radius) continue;
                            BlockPos pos = playerPos.offset(dx, dy, dz);
                            if (shouldFreeze(world, pos)) candidates.add(pos);
                        }
                    }
                }
            }

            Collections.shuffle(candidates);
            int frozen = 0;
            for (BlockPos pos : candidates) {
                if (frozen >= maxBlocks) break;
                freezeBlock(world, pos);
                frozen++;
            }
        }
    }

    private boolean shouldFreeze(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);

        if (IceSpreadConfig.FREEZE_WATER.get() && state.is(Blocks.WATER)) {
            return hasAdjacentAir(world, pos);
        }

        if (IceSpreadConfig.FREEZE_AIR.get() && state.isAir(world, pos)) {
            BlockPos below = pos.below();
            BlockState belowState = world.getBlockState(below);
            if (belowState.isSolidRender(world, below)
                    && !belowState.is(Blocks.ICE)
                    && !belowState.is(Blocks.PACKED_ICE)
                    && !belowState.is(Blocks.BLUE_ICE)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAdjacentAir(ServerWorld world, BlockPos pos) {
        for (BlockPos nb : getSides(pos)) {
            if (world.getBlockState(nb).isAir(world, nb)) return true;
        }
        return false;
    }

    private BlockPos[] getSides(BlockPos pos) {
        return new BlockPos[]{
            pos.north(), pos.south(), pos.east(), pos.west(), pos.above(), pos.below()
        };
    }

    private void freezeBlock(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.is(Blocks.WATER) || state.isAir(world, pos)) {
            world.setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState());
            iceTimestamps.put(pos.immutable(), tickCounter);
        }
    }

    private void convertIceToPacked() {
        net.minecraft.server.MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        int packedDelay = IceSpreadConfig.PACKED_ICE_DELAY.get();
        Iterator<Map.Entry<BlockPos, Long>> iter = iceTimestamps.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry<BlockPos, Long> entry = iter.next();
            if (tickCounter - entry.getValue() >= packedDelay) {
                BlockPos pos = entry.getKey();
                for (ServerWorld world : server.getAllLevels()) {
                    if (world.getBlockState(pos).is(Blocks.ICE)) {
                        world.setBlockAndUpdate(pos, Blocks.PACKED_ICE.defaultBlockState());
                    }
                    break;
                }
                iter.remove();
            }
        }
    }
}
