package net.replacecraft.level;

public class LightBlock {
    private static final int LIGHT_BLOCK_ID = 10; // ID блока света
    private static final int RADIUS = 8;
    
    /**
     * Быстрый поиск света: проверяет только небольшой радиус.
     */
    public static float getLightLevel(Level level, int x, int y, int z) {
        // Проверяем только 3×3×3 вокруг (быстро)
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dy = -RADIUS; dy <= RADIUS; dy++) {
                for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                    int bx = x + dx;
                    int by = y + dy;
                    int bz = z + dz;
                    
                    if (bx >= 0 && by >= 0 && bz >= 0 && 
                        bx < level.width && by < level.height && bz < level.depth) {
                        
                        if (level.getBlock(bx, by, bz) == LIGHT_BLOCK_ID) {
                            float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                            if (dist <= RADIUS) {
                                return 1.0f - (dist / RADIUS);
                            }
                        }
                    }
                }
            }
        }
        return 0.0f;
    }
}