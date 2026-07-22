package net.replacecraft;

import net.replacecraft.level.Level;
import net.replacecraft.level.LevelListener;
import net.replacecraft.level.Chunk;
import org.lwjgl.opengl.GL11;

public class Renderer implements LevelListener {
    private Level level;
    private Player player;
    private int textureID;
    
    private Chunk[] chunks;
    private int xChunks, yChunks, zChunks;

    public Renderer(Level level, Player player) {
        this.level = level;
        this.player = player;
        this.textureID = Texture.loadTexture("terrain.png"); 
        
        this.xChunks = level.width / 16;
        this.yChunks = level.height / 16;
        this.zChunks = level.depth / 16;
        
        this.chunks = new Chunk[xChunks * yChunks * zChunks];
        
        for (int x = 0; x < xChunks; x++) {
            for (int y = 0; y < yChunks; y++) {
                for (int z = 0; z < zChunks; z++) {
                    int idx = (y * zChunks + z) * xChunks + x;
                    chunks[idx] = new Chunk(level, x * 16, y * 16, z * 16, 
                        (x + 1) * 16, (y + 1) * 16, (z + 1) * 16);
                }
            }
        }
        
        level.addListener(this);  // <-- строка 34
    }

    public void setLevel(Level newLevel) {
        this.level = newLevel;
        this.level.addListener(this);
        
        // Пересоздаём чанки под новый размер
        this.xChunks = level.width / 16;
        this.yChunks = level.height / 16;
        this.zChunks = level.depth / 16;
        this.chunks = new Chunk[xChunks * yChunks * zChunks];
        
        for (int x = 0; x < xChunks; x++) {
            for (int y = 0; y < yChunks; y++) {
                for (int z = 0; z < zChunks; z++) {
                    int idx = (y * zChunks + z) * xChunks + x;
                    chunks[idx] = new Chunk(level, x * 16, y * 16, z * 16,
                        (x + 1) * 16, (y + 1) * 16, (z + 1) * 16);
                }
            }
        }
        allChunksChanged();
    }

    public void render() {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);

        for (Chunk chunk : chunks) {
            chunk.render();
        }
    }

    @Override
    public void blockChanged(int x, int y, int z) {
        int cx = x / 16;
        int cy = y / 16;
        int cz = z / 16;
        
        setChunkDirty(cx, cy, cz);

        if (x % 16 == 0) setChunkDirty(cx - 1, cy, cz);
        if (x % 16 == 15) setChunkDirty(cx + 1, cy, cz);
        if (y % 16 == 0) setChunkDirty(cx, cy - 1, cz);
        if (y % 16 == 15) setChunkDirty(cx, cy + 1, cz);
        if (z % 16 == 0) setChunkDirty(cx, cy, cz - 1);
        if (z % 16 == 15) setChunkDirty(cx, cy, cz + 1);
    }

    private void setChunkDirty(int cx, int cy, int cz) {
        if (cx >= 0 && cy >= 0 && cz >= 0 && cx < xChunks && cy < yChunks && cz < zChunks) {
            chunks[(cy * zChunks + cz) * xChunks + cx].setDirty();
        }
    }
    
    
    
    public int getTextureID() {
        return this.textureID;
    }

    @Override
    public void allChunksChanged() {
        for (Chunk chunk : chunks) {
            chunk.forceDirty();
        }
    }
    
    public void markAllChunksDirty() {
        for (Chunk chunk : chunks) {
            chunk.forceDirty();
        }
    }
    
    public void rebuildAllChunks() {
        for (Chunk chunk : chunks) {
            chunk.setDirty();
            chunk.rebuildNow();  // Перестроить немедленно
        }
    }
}