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

import java.util.*;

public class IceSpreadEventHandler {

    private final Map<BlockPos, Long> iceTimestamps = new HashMap<>();
    private long tickCounter = 0;

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        tickCounter++;

        convertIceToPacked(event);

        if (tickCounter % IceSpreadConfig.ICE_SPREAD_INTERVAL.get() == 0) {
            spreadIce(event);
        }
    }

    private void spreadIce(TickEvent.ServerTickEvent event) {
        if (!IceSpreadConfig.SPREAD_FROM_PLAYERS.get()) return;

        for (ServerWorld world : event.getServer().getAllLevels()) {
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
                            if (dx * dx + dy * dy + dz * dz > radius * radius) continue;
                            BlockPos pos = playerPos.offset(dx, dy, dz);
                            if (shouldFreeze(world, pos)) {
                                candidates.add(pos);
                            }
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

    /**
     * Блок підходить для заморозки тільки якщо він є ЗОВНІШНІМ:
     * тобто він сам твердий/вода І хоча б одна його сторона торкається повітря.
     *
     * Лід ставиться НА МІСЦІ самого блоку (вода -> лід),
     * або на поверхні (повітря над твердим блоком) — але лише якщо
     * той твердий блок є зовнішнім (має відкриту сторону крім верхньої).
     */
    private boolean shouldFreeze(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);

        // --- Заморожуємо воду, якщо вона на зовнішній поверхні ---
        if (IceSpreadConfig.FREEZE_WATER.get() && state.is(Blocks.WATER)) {
            // Вода вважається "зовнішньою" якщо поруч є повітря (не вода/лід)
            return hasAdjacentAir(world, pos);
        }

        // --- Ставимо лід зверху твердого блоку, якщо зверху повітря ---
        if (IceSpreadConfig.FREEZE_AIR.get() && state.isAir(world, pos)) {
            BlockPos below = pos.below();
            BlockState belowState = world.getBlockState(below);

            boolean belowIsSolid = belowState.isSolidRender(world, below);
            boolean belowIsNotIce = !belowState.is(Blocks.ICE)
                    && !belowState.is(Blocks.PACKED_ICE)
                    && !belowState.is(Blocks.BLUE_ICE);

            if (belowIsSolid && belowIsNotIce) {
                // Блок знизу — зовнішній, якщо хоча б одна БОКОВА сторона відкрита
                // (верхня вже відкрита бо там повітря де ми стоїмо)
                return isExposedOnSides(world, below);
            }
        }

        return false;
    }

    /**
     * Повертає true якщо хоч одна з 6 сторін блоку — повітря
     * (не лід, не вода, просто повітря).
     */
    private boolean hasAdjacentAir(ServerWorld world, BlockPos pos) {
        for (BlockPos nb : getSides(pos)) {
            BlockState s = world.getBlockState(nb);
            if (s.isAir(world, nb)) return true;
        }
        return false;
    }

    /**
     * Перевіряє чи є відкриті БОКОВІ сторони (північ/південь/схід/захід).
     * Використовується для блоків під поверхнею — верх відкритий за визначенням.
     */
    private boolean isExposedOnSides(ServerWorld world, BlockPos pos) {
        BlockPos[] sides = new BlockPos[]{
            pos.north(), pos.south(), pos.east(), pos.west()
        };
        for (BlockPos nb : sides) {
            BlockState s = world.getBlockState(nb);
            if (s.isAir(world, nb)) return true;
        }
        return true; // верх відкритий — теж вважається зовнішнім
    }

    private BlockPos[] getSides(BlockPos pos) {
        return new BlockPos[]{
            pos.north(), pos.south(), pos.east(), pos.west(), pos.above(), pos.below()
        };
    }

    private boolean hasAdjacentIce(ServerWorld world, BlockPos pos) {
        for (BlockPos nb : getSides(pos)) {
            BlockState s = world.getBlockState(nb);
            if (s.is(Blocks.ICE) || s.is(Blocks.PACKED_ICE)) return true;
        }
        return false;
    }

    private void freezeBlock(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.is(Blocks.WATER) || state.isAir(world, pos)) {
            world.setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState());
            iceTimestamps.put(pos.immutable(), tickCounter);
        }
    }

    private void convertIceToPacked(TickEvent.ServerTickEvent event) {
        int packedDelay = IceSpreadConfig.PACKED_ICE_DELAY.get();
        Iterator<Map.Entry<BlockPos, Long>> iter = iceTimestamps.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry<BlockPos, Long> entry = iter.next();
            if (tickCounter - entry.getValue() >= packedDelay) {
                BlockPos pos = entry.getKey();
                for (ServerWorld world : event.getServer().getAllLevels()) {
                    if (world.getBlockState(pos).is(Blocks.ICE)) {
                        world.setBlockAndUpdate(pos, Blocks.PACKED_ICE.defaultBlockState());
                        IceSpreadMod.LOGGER.debug("[IceSpread] ICE -> PACKED_ICE at {}", pos);
                    }
                    break;
                }
                iter.remove();
            }
        }
    }
}
