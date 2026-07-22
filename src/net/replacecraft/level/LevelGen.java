package net.replacecraft.level;

public class LevelGen {
    private int width;
    private int height;
    private int depth;

    public LevelGen(int width, int height, int depth) {
        this.width = width;
        this.height = height;
        this.depth = depth;
    }

    public byte[] generateMap() {
        byte[] map = new byte[width * height * depth];
        NoiseMap noise1 = new NoiseMap();
        NoiseMap noise2 = new NoiseMap();
        NoiseMap caveNoise = new NoiseMap();

        // ТВОЙ ОРИГИНАЛЬНЫЙ КОД ГЕНЕРАЦИИ — ОСТАВЛЕН ПОЛНОСТЬЮ БЕЗ ИЗМЕНЕНИЙ
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                float n1 = noise1.getNoise(x * 0.02f, z * 0.02f) * 12.0f;
                float n2 = noise2.getNoise(x * 0.05f, z * 0.05f) * 4.0f;
                int surfaceHeight = (height / 2) + (int) (n1 + n2);

                if (surfaceHeight >= height) surfaceHeight = height - 1;
                if (surfaceHeight < 1) surfaceHeight = 1;

                for (int y = 0; y < height; y++) {
                    int idx = (y * depth + z) * width + x;
                    if (y < surfaceHeight - 4) {
                        map[idx] = 1; // Камень
                    } else if (y < surfaceHeight - 1) {
                        map[idx] = 2; // Земля
                    } else if (y == surfaceHeight - 1) {
                        map[idx] = 3; // Трава
                    } else {
                        map[idx] = 0; // Воздух
                    }
                }
            }
        }

        // ПЕЩЕРЫ, КОТОРЫЕ МОГУТ ПРОБИВАТЬ ВХОДЫ НА ПОВЕРХНОСТЬ
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                // Вычисляем surfaceHeight ещё раз для этого столбца
                float n1 = noise1.getNoise(x * 0.02f, z * 0.02f) * 12.0f;
                float n2 = noise2.getNoise(x * 0.05f, z * 0.05f) * 4.0f;
                int surfaceHeight = (height / 2) + (int) (n1 + n2);
                if (surfaceHeight >= height) surfaceHeight = height - 1;
                if (surfaceHeight < 1) surfaceHeight = 1;

                // Теперь пещеры идут до самой поверхности (surfaceHeight - 1)
                for (int y = 1; y < surfaceHeight - 1; y++) {
                    int idx = (y * depth + z) * width + x;
                    
                    // Грызём камень и землю
                    if (map[idx] == 1 || map[idx] == 2) {
                        float density = caveNoise.getNoise3D(x * 0.08f, y * 0.14f, z * 0.08f);
                        
                        if (density > 0.47f) {
                            map[idx] = 0; // Воздух
                            
                            // ЕСЛИ ПЕЩЕРА ПОДОШЛА БЛИЗКО К ПОВЕРХНОСТИ — ПРОБИВАЕМ ТРАВУ
                            if (y >= surfaceHeight - 3) {
                                int surfaceIdx = ((surfaceHeight - 1) * depth + z) * width + x;
                                if (map[surfaceIdx] == 3) {
                                    map[surfaceIdx] = 0; // Дыра в траве — вход в пещеру
                                }
                            }
                        }
                    }
                }
            }
        }

        return map;
    }
}