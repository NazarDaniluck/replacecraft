package net.replacecraft.network;

public interface NetworkListener {
    void onLevelReceived(int width, int height, int depth, byte[] blocks);
    void onBlockChanged(int x, int y, int z, int blockType);
    void onPlayerSpawned(byte playerId, String name, float x, float y, float z, byte yaw, byte pitch);
    void onPlayerDespawned(byte playerId);
    void onOwnPositionReceived(float x, float y, float z, byte yaw, byte pitch);
    void onPlayerPositionUpdate(byte playerId, float x, float y, float z, byte yaw, byte pitch);
    void onMessageReceived(byte playerId, String message);
    void onDisconnected(String reason);
}