package com.icespread.mod.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class IceSpreadConfig {

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.IntValue ICE_SPREAD_INTERVAL;
    public static final ForgeConfigSpec.IntValue ICE_SPREAD_RADIUS;
    public static final ForgeConfigSpec.IntValue PACKED_ICE_DELAY;
    public static final ForgeConfigSpec.IntValue MAX_BLOCKS_PER_TICK;
    public static final ForgeConfigSpec.BooleanValue FREEZE_AIR;
    public static final ForgeConfigSpec.BooleanValue FREEZE_WATER;
    public static final ForgeConfigSpec.BooleanValue SPREAD_FROM_PLAYERS;

    // Новий параметр: поріг skylight для визначення "надворі"
    public static final ForgeConfigSpec.IntValue OUTDOOR_SKY_LIGHT_THRESHOLD;

    static {
        BUILDER.push("Ice Spread Settings");

        ICE_SPREAD_INTERVAL = BUILDER
                .comment("Як часто (в тіках) поширюється лід. 20 тіків = 1 секунда. За замовч.: 10")
                .defineInRange("iceSpreadInterval", 10, 1, 200);

        ICE_SPREAD_RADIUS = BUILDER
                .comment("Радіус навколо гравця де заморожуються блоки. За замовч.: 5")
                .defineInRange("iceSpreadRadius", 5, 1, 20);

        PACKED_ICE_DELAY = BUILDER
                .comment("Тіків до перетворення Ice -> Packed Ice. 400 = 20 секунд.")
                .defineInRange("packedIceDelay", 400, 20, 72000);

        MAX_BLOCKS_PER_TICK = BUILDER
                .comment("Макс. кількість блоків що заморожуються за один тік. За замовч.: 5")
                .defineInRange("maxBlocksPerTick", 5, 1, 50);

        FREEZE_AIR = BUILDER
                .comment("Чи ставити лід на поверхню твердих блоків. За замовч.: true")
                .define("freezeAir", true);

        FREEZE_WATER = BUILDER
                .comment("Чи перетворювати воду на лід. За замовч.: true")
                .define("freezeWater", true);

        SPREAD_FROM_PLAYERS = BUILDER
                .comment("Чи поширювати лід навколо гравців. За замовч.: true")
                .define("spreadFromPlayers", true);

        OUTDOOR_SKY_LIGHT_THRESHOLD = BUILDER
                .comment(
                    "Мінімальний рівень денного світла (skyLight) щоб вважати місце НАДВОРІ.\n" +
                    "15 = тільки пряме небо, 6 = напіввідкрито (крізь листя/скло), 0 = вимкнено.\n" +
                    "За замовч.: 6\n" +
                    "НАДВОРІ: лід на поверхні + вода.\n" +
                    "В БУДИНКУ: лід тільки на підлозі/стінах де немає неба."
                )
                .defineInRange("outdoorSkyLightThreshold", 6, 0, 15);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
