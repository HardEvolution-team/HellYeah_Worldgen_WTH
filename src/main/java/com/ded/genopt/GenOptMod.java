package com.ded.genopt;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = GenOptMod.MOD_ID, name = GenOptMod.MOD_NAME, version = GenOptMod.VERSION)
public class GenOptMod {

    public static final String MOD_ID = "genopt";
    public static final String MOD_NAME = "Generation Optimization";
    public static final String VERSION = "1.0.0";

    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("Hello from Generation Optimization!");
    }

}
