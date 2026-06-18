package net.replacecraft.network;

import java.io.*;

public class PacketHandler {
    private NetworkManager networkManager;
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
        System.out.println("[PacketHandler] Received: 0x" + Integer.toHexString(packetId));
    	switch (packetId) {
            case 0x00: handleServerIdentification(in); break;
            case 0x02: handleLevelInit(in); break;
            case 0x03: handleLevelChunk(in); break;
            case 0x04: handleLevelFinalize(in); break;
            case 0x06: handleSetBlock(in); break;
            case 0x07: handleSpawnPlayer(in); break;
            case 0x08: handlePlayerTeleport(in); break;
            case 0x0B: handleDespawnPlayer(in); break;
            case 0x0C: handleMessage(in); break;
            case 0x0D: handleDisconnect(in); break;
        }
    }

    private void handleServerIdentification(DataInputStream in) throws IOException {
        int protocolVersion = in.readUnsignedByte();
        String serverName = NetworkManager.readString64(in);
        String serverMotd = NetworkManager.readString64(in);
        int userType = in.readUnsignedByte();
        System.out.println("=== Server ===");
        System.out.println("Name: " + serverName);
        System.out.println("MOTD: " + serverMotd);
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
        byte percent = in.readByte();

        if (levelData == null) {
            levelData = chunkData;
        } else {
            byte[] newData = new byte[levelData.length + chunkData.length];
            System.arraycopy(levelData, 0, newData, 0, levelData.length);
            System.arraycopy(chunkData, 0, newData, levelData.length, chunkData.length);
            levelData = newData;
        }
        // НЕ вызываем listener.onLevelReceived() здесь!
    }

    private void handleLevelFinalize(DataInputStream in) throws IOException {
        levelWidth = in.readShort();
        levelHeight = in.readShort();
        levelDepth = in.readShort();
        System.out.println("Level finalized: " + levelWidth + "x" + levelHeight + "x" + levelDepth);
        System.out.println("Total level data: " + (levelData != null ? levelData.length : 0) + " bytes");
        
        if (listener != null && levelData != null) {
            listener.onLevelReceived(levelWidth, levelHeight, levelDepth, levelData);
            levelData = null;
        } else {
            System.out.println("WARNING: listener=" + listener + " levelData=" + levelData);
        }
    }

    private void handleSetBlock(DataInputStream in) throws IOException {
        int x = in.readShort();
        int y = in.readShort();
        int z = in.readShort();
        byte mode = in.readByte();
        byte blockType = 0;
        if (mode == 0x00) blockType = in.readByte();
        if (listener != null) {
            listener.onBlockChanged(x, y, z, mode == 0x00 ? blockType : 0);
        }
    }

    private void handleSpawnPlayer(DataInputStream in) throws IOException {
        byte playerId = in.readByte();
        String playerName = NetworkManager.readString64(in);
        float x = NetworkManager.readFloat16(in);
        float y = NetworkManager.readFloat16(in);
        float z = NetworkManager.readFloat16(in);
        byte yaw = in.readByte();
        byte pitch = in.readByte();
        System.out.println("Player joined: " + playerName);
        if (listener != null) {
            listener.onPlayerSpawned(playerId, playerName, x, y, z, yaw, pitch);
        }
    }

    private void handlePlayerTeleport(DataInputStream in) throws IOException {
        byte playerId = in.readByte();
        float x = NetworkManager.readFloat16(in);
        float y = NetworkManager.readFloat16(in);
        float z = NetworkManager.readFloat16(in);
        byte yaw = in.readByte();
        byte pitch = in.readByte();

        if (playerId == -1) {
            if (listener != null) listener.onOwnPositionReceived(x, y, z, yaw, pitch);
        } else {
            if (listener != null) listener.onPlayerPositionUpdate(playerId, x, y, z, yaw, pitch);
        }
    }

    private void handleDespawnPlayer(DataInputStream in) throws IOException {
        byte playerId = in.readByte();
        System.out.println("Player left: ID=" + playerId);
        if (listener != null) listener.onPlayerDespawned(playerId);
    }

    private void handleMessage(DataInputStream in) throws IOException {
        byte playerId = in.readByte();
        String message = NetworkManager.readString64(in);
        System.out.println("[Chat] " + message);
        if (listener != null) listener.onMessageReceived(playerId, message);
    }

    private void handleDisconnect(DataInputStream in) throws IOException {
        String reason = NetworkManager.readString64(in);
        System.out.println("Disconnected: " + reason);
        if (listener != null) listener.onDisconnected(reason);
        networkManager.disconnect();
    }
}