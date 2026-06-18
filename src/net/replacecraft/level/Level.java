package net.replacecraft.level;

import java.util.ArrayList;
import java.util.List;

public class Level {
    public final int width;
    public final int height;
    public final int depth;
    private byte[] blocks;
    private List<LevelListener> listeners = new ArrayList<>();
    private boolean generated = false;

    public Level(int w, int h, int d) {
        this(w, h, d, true);
    }

    public Level(int w, int h, int d, boolean generate) {
        this.width = w;
        this.height = h;
        this.depth = d;
        this.blocks = new byte[w * h * d];
        if (generate) generateLevel();
    }

    public Level(int w, int h, int d, byte[] serverBlocks) {
        this.width = w;
        this.height = h;
        this.depth = d;
        this.blocks = new byte[w * h * d];
        System.arraycopy(serverBlocks, 0, this.blocks, 0, Math.min(serverBlocks.length, this.blocks.length));
        this.generated = true;
    }

    private void generateLevel() {
        LevelGen generator = new LevelGen(width, height, depth);
        this.blocks = generator.generateMap();
        this.generated = true;
    }

    public void generateIfNeeded() {
        if (!generated) generateLevel();
    }

    public boolean isGenerated() { return generated; }

    public byte[] getBlocks() { return blocks; }

    public void addListener(LevelListener listener) { listeners.add(listener); }

    public void tick() {
        if (!generated) return;
        java.util.Random rnd = new java.util.Random();
        for (int i = 0; i < 200; i++) {
            int x = rnd.nextInt(width);
            int y = rnd.nextInt(height - 1);
            int z = rnd.nextInt(depth);
            if (getBlock(x, y, z) == 2 && getBlock(x, y + 1, z) == 0) {
                if (getBlock(x + 1, y, z) == 3 || getBlock(x - 1, y, z) == 3 ||
                    getBlock(x, y, z + 1) == 3 || getBlock(x, y, z - 1) == 3) {
                    setBlock(x, y, z, 3);
                }
            }
        }
    }

    public int getBlock(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= width || y >= height || z >= depth) return 0;
        return blocks[(y * depth + z) * width + x] & 0xFF;
    }

    public void setBlock(int x, int y, int z, int type) {
        if (x < 0 || y < 0 || z < 0 || x >= width || y >= height || z >= depth) return;
        blocks[(y * depth + z) * width + x] = (byte) type;
        for (LevelListener listener : listeners) {
            listener.blockChanged(x, y, z);
        }
    }

    /** Массовая установка блока без вызова событий (для загрузки мира) */
    public void setBlockRaw(int x, int y, int z, int type) {
        if (x < 0 || y < 0 || z < 0 || x >= width || y >= height || z >= depth) return;
        blocks[(y * depth + z) * width + x] = (byte) type;
    }
    
    public void loadBlocks(byte[] newBlocks) {
        System.arraycopy(newBlocks, 0, this.blocks, 0, Math.min(newBlocks.length, this.blocks.length));
        this.generated = true;
        for (LevelListener listener : listeners) {
            listener.allChunksChanged();
        }
    }
}