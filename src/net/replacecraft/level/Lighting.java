package net.replacecraft.level;

public class Lighting {
    private Level level;
    
    private static final float TOP = 1.0f;
    private static final float SIDE_NS = 0.8f;
    private static final float SIDE_EW = 0.6f;
    private static final float BOTTOM = 0.5f;
    
    public static boolean enabled = true;
    
    public Lighting(Level level) {
        this.level = level;
    }
    
    public float getVertexBrightness(int x, int y, int z, int face, int corner) {
        if (!enabled) return 1.0f;
    	float base;
        switch (face) {
            case 0: base = TOP; break;
            case 1: base = BOTTOM; break;
            case 2: case 3: base = SIDE_NS; break;
            case 4: case 5: base = SIDE_EW; break;
            default: return 1.0f;
        }
        
        float skyLight = getSkyLight(x, y, z);
        float brightness = base * skyLight;
        
        if (brightness < 0.1f) brightness = 0.1f;
        if (brightness > 1.0f) brightness = 1.0f;
        
        return brightness;
    }
    
    private float getSkyLight(int x, int y, int z) {
        // Считаем сколько сторон закрыто
        int blocked = 0;
        if (isOpaque(x - 1, y, z)) blocked++;
        if (isOpaque(x + 1, y, z)) blocked++;
        if (isOpaque(x, y, z - 1)) blocked++;
        if (isOpaque(x, y, z + 1)) blocked++;
        
        // Пещера только если ВСЕ 4 стороны + блок сверху (потолок)
        boolean enclosed = (blocked == 4) && isOpaque(x, y + 1, z);
        
        if (enclosed) {
            return 0.3f;
        }
        
        // Ищем блок сверху (но не вплотную)
        for (int cy = y + 2; cy < level.height; cy++) { // +2 чтобы пропустить вплотную
            if (isOpaque(x, cy, z)) {
                // Нашли висящий блок — тень
                int dist = cy - y;
                if (dist <= 2) return 0.3f;
                if (dist <= 4) return 0.4f;
                if (dist <= 6) return 0.5f;
                return 0.6f;
            }
        }
        
        // Открытое небо
        return 1.0f;
    }
    
    private boolean isOpaque(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= level.width || y >= level.height || z >= level.depth) {
            return false;
        }
        int block = level.getBlock(x, y, z);
        return block != 0 && block != 7;
    }
}