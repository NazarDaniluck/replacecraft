package net.replacecraft.network;

import java.io.*;

public class PacketHandler {
    private final NetworkManager networkManager;
    private NetworkListener listener;
    private byte[] levelData;
    private int levelWidth, levelHeight, levelDepth;

    public PacketHandler(NetworkManager manager) {
        this.networkManager = manager;
    }

    public void setNetworkListener(NetworkListener listener) {
        this.listener = listener;
    }

    public void handlePacket(int packetId, DataInputStream in) throws IOException {
        switch (packetId) {
            case 0x00: handleServerIdentification(in); break;
            case 0x02: handleLevelInit(in); break;
            case 0x03: handleLevelChunk(in); break;
            case 0x04: handleLevelFinalize(in); break;
            case 0x06: handleSetBlock(in); break;
            case 0x07: handleSpawnPlayer(in); break;
            case 0x08: handlePlayerTeleport(in); break;
            case 0x09: handlePlayerMove(in); break;
            case 0x0B: handleDespawnPlayer(in); break;
            case 0x0C: handleMessage(in); break;
            case 0x0D: handleDisconnect(in); break;
            case 0x0E: handleUpdateUserType(in); break;
        }
    }

    private void handleServerIdentification(DataInputStream in) throws IOException {
        int protocolVersion = in.readUnsignedByte();
        String serverName = readString(in);
        String serverMotd = readString(in);
        int userType = in.readUnsignedByte();
        
        System.out.println("=== Server Info ===");
        System.out.println("  Name: " + serverName);
        System.out.println("  MOTD: " + serverMotd);
        System.out.println("  UserType: " + userType + " (OP=" + (userType == 0x64) + ")");
        
        if (listener != null) {
            listener.onServerIdentified(serverName, serverMotd);
            listener.onOpStatusReceived(userType == 0x64);
        }
        
        levelData = null;
    }

    private void handleLevelInit(DataInputStream in) throws IOException {
        levelData = null;
    }

    private void handleLevelChunk(DataInputStream in) throws IOException {
        short chunkLength = in.readShort();
        if (chunkLength <= 0) return;
        byte[] chunkData = new byte[chunkLength];
        in.readFully(chunkData);
        in.readByte();
        
        if (levelData == null) {
            levelData = chunkData;
        } else {
            byte[] newData = new byte[levelData.length + chunkData.length];
            System.arraycopy(levelData, 0, newData, 0, levelData.length);
            System.arraycopy(chunkData, 0, newData, levelData.length, chunkData.length);
            levelData = newData;
        }
    }

    private void handleLevelFinalize(DataInputStream in) throws IOException {
        levelWidth = in.readShort();
        levelHeight = in.readShort();
        levelDepth = in.readShort();
        
        if (listener != null && levelData != null) {
            int expectedSize = levelWidth * levelHeight * levelDepth;
            if (levelData.length < expectedSize) {
                byte[] padded = new byte[expectedSize];
                System.arraycopy(levelData, 0, padded, 0, levelData.length);
                listener.onLevelReceived(levelWidth, levelHeight, levelDepth, padded);
            } else {
                listener.onLevelReceived(levelWidth, levelHeight, levelDepth, levelData);
            }
        }
        levelData = null;
    }

    private void handleSetBlock(DataInputStream in) throws IOException {
        int x = in.readShort();
        int y = in.readShort();
        int z = in.readShort();
        byte mode = in.readByte();
        byte blockType = (mode == 0x00) ? in.readByte() : 0;
        if (listener != null) listener.onBlockChanged(x, y, z, blockType);
    }

    private void handleSpawnPlayer(DataInputStream in) throws IOException {
        byte playerId = in.readByte();
        String playerName = readString(in);
        float x = readFloat16(in);
        float y = readFloat16(in);
        float z = readFloat16(in);
        byte yaw = in.readByte();
        byte pitch = in.readByte();
        if (listener != null) listener.onPlayerSpawned(playerId, playerName, x, y, z, yaw, pitch);
    }

    private void handlePlayerTeleport(DataInputStream in) throws IOException {
        byte playerId = in.readByte();
        float x = readFloat16(in);
        float y = readFloat16(in);
        float z = readFloat16(in);
        byte yaw = in.readByte();
        byte pitch = in.readByte();
        if (playerId == -1 && listener != null) listener.onOwnPositionReceived(x, y, z, yaw, pitch);
    }

    private void handlePlayerMove(DataInputStream in) throws IOException {
        byte playerId = in.readByte();
        float x = readFloat16(in);
        float y = readFloat16(in);
        float z = readFloat16(in);
        byte yaw = in.readByte();
        byte pitch = in.readByte();
        if (playerId != -1 && listener != null) listener.onPlayerPositionUpdate(playerId, x, y, z, yaw, pitch);
    }

    private void handleDespawnPlayer(DataInputStream in) throws IOException {
        byte playerId = in.readByte();
        if (listener != null) listener.onPlayerDespawned(playerId);
    }

    private void handleMessage(DataInputStream in) throws IOException {
        byte playerId = in.readByte();
        String message = readString(in).replace('&', '\u00a7');
        if (listener != null) listener.onMessageReceived(playerId, message);
    }

    private void handleDisconnect(DataInputStream in) throws IOException {
        String reason = readString(in);
        System.out.println("[Network] Disconnected: " + reason);
        networkManager.disconnect();
        if (listener != null) listener.onDisconnected(reason);
    }

    private void handleUpdateUserType(DataInputStream in) throws IOException {
        byte userType = in.readByte();
        System.out.println("[PacketHandler] UserType: " + userType);
        
        if (listener != null) {
            // 0x64 = OP status
            // 1 = fly toggle
            // 2 = noclip toggle
            if (userType == 0x64 || userType == 0x00) {
                listener.onOpStatusReceived(userType == 0x64);
            } else if (userType == 1) {
                listener.onFlyToggle();
            } else if (userType == 2) {
                listener.onNoClipToggle();
            }
        }
    }

    private String readString(DataInputStream in) throws IOException {
        byte[] data = new byte[64];
        in.readFully(data);
        int end = 63;
        while (end >= 0 && (data[end] == 0x20 || data[end] == 0x00)) end--;
        if (end < 0) return "";
        return new String(data, 0, end + 1, "UTF-8").trim();
    }

    private float readFloat16(DataInputStream in) throws IOException {
        return in.readInt() / 32.0f;
    }
}