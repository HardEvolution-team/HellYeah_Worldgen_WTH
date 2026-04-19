package com.ded.hywg;

import com.ded.hywg.world.WorldTypeHYWG;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = Reference.MOD_ID, name = Reference.MOD_NAME, version = Reference.VERSION)
public class ExampleMod {

    public static final Logger LOGGER = LogManager.getLogger(Reference.MOD_NAME);
    public static WorldTypeHYWG WORLD_TYPE_HYWG;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("Hello From {}!", Reference.MOD_NAME);
        WORLD_TYPE_HYWG = new WorldTypeHYWG();
    }

}
