package com.ded.hywg;

public final class WorldHeightConfig {
    private WorldHeightConfig() {}

    public static final int WORLD_HEIGHT = 4096;
    public static final int CHUNK_SECTIONS = WORLD_HEIGHT / 16; 
    public static final int WORLD_HEIGHT_M1 = WORLD_HEIGHT - 1; 

    public static final int PRIMER_SIZE = 16 * 16 * WORLD_HEIGHT; 

    public static final int BITMASK_LONGS = 4; 

    public static final ThreadLocal<long[]> SECTION_MASK_TL = new ThreadLocal<>();
}
