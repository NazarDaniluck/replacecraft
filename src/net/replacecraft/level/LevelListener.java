package net.replacecraft.level;

public interface LevelListener {
    void blockChanged(int x, int y, int z);
    void allChunksChanged();
}