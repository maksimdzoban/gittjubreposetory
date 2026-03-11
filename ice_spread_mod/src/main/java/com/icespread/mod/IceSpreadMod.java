package com.icespread.mod;

import com.icespread.mod.config.IceSpreadConfig;
import com.icespread.mod.events.IceSpreadEventHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(IceSpreadMod.MOD_ID)
public class IceSpreadMod {

    public static final String MOD_ID = "icespread";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public IceSpreadMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, IceSpreadConfig.SPEC, "icespread-common.toml");
        MinecraftForge.EVENT_BUS.register(new IceSpreadEventHandler());
        LOGGER.info("[IceSpread] Mod initialized! Everything will freeze...");
    }
}
