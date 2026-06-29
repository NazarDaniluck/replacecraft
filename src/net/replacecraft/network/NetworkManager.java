package net.replacecraft.network;

import java.io.*;
import java.net.*;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class NetworkManager {
    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;
    private boolean connected;
    private Thread listenerThread;
    private PacketHandler packetHandler;
    private Queue<byte[]> sendQueue = new ConcurrentLinkedQueue<>();
    private Thread sendThread;

    public NetworkManager() {
        this.connected = false;
    }

    public void setPacketHandler(PacketHandler handler) {
        this.packetHandler = handler;
    }

    public void connect(String host, int port, String username, String mppass) throws IOException {
        if (this.socket != null && !this.socket.isClosed()) {
            try { this.socket.close(); } catch (IOException e) {}
        }

        this.socket = new Socket(host, port);
        socket.setTcpNoDelay(true);

        input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

        connected = true;
        System.out.println("Connected to " + host + ":" + port);

        // Поток отправки
        sendThread = new Thread(() -> {
            while (connected) {
                try {
                    byte[] data;
                    while ((data = sendQueue.poll()) != null) {
                        output.write(data);
                    }
                    output.flush();
                    Thread.sleep(10);
                } catch (Exception e) { break; }
            }
        }, "Send-Client");
        sendThread.setDaemon(true);
        sendThread.start();

        // Отправляем идентификацию
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        DataOutputStream dataOut = new DataOutputStream(byteOut);
        dataOut.writeByte(0x00);
        dataOut.writeByte(0x07);
        writeString64(dataOut, username);
        writeString64(dataOut, mppass.isEmpty() ? "-" : mppass);
        dataOut.writeByte(0x00);
        sendRaw(byteOut.toByteArray());

        System.out.println("Sent identification: " + username);

        // Ждём первый ответ синхронно
        System.out.println("Waiting for server response...");
        int firstPacketId = input.readUnsignedByte();
        System.out.println("Received first packet: 0x" + Integer.toHexString(firstPacketId));
        if (packetHandler != null) {
            packetHandler.handlePacket(firstPacketId, input);
        }

        // Запускаем слушатель
        startListening();
    }

    public void sendPlayerPosition(float x, float y, float z, float yaw, float pitch) throws IOException {
        if (!connected) return;
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        DataOutputStream dataOut = new DataOutputStream(byteOut);
        dataOut.writeByte(0x08);
        dataOut.writeByte(0xFF);
        writeFloat16(dataOut, x);
        writeFloat16(dataOut, y);
        writeFloat16(dataOut, z);
        dataOut.writeByte((byte) yaw);
        dataOut.writeByte((byte) pitch);
        sendRaw(byteOut.toByteArray());
    }

    public void sendSetBlock(int x, int y, int z, byte mode, byte blockType) throws IOException {
        if (!connected) return;
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        DataOutputStream dataOut = new DataOutputStream(byteOut);
        dataOut.writeByte(0x05);
        dataOut.writeShort((short) x);
        dataOut.writeShort((short) y);
        dataOut.writeShort((short) z);
        dataOut.writeByte(mode);
        dataOut.writeByte(blockType);
        sendRaw(byteOut.toByteArray());
    }

    public void sendMessage(String message) throws IOException {
        if (!connected) return;
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        DataOutputStream dataOut = new DataOutputStream(byteOut);
        dataOut.writeByte(0x0D);
        dataOut.writeByte((byte) 0xFF);
        writeString64(dataOut, message);
        sendRaw(byteOut.toByteArray());
    }

    private void sendRaw(byte[] data) throws IOException {
        if (!connected || socket.isClosed()) return;
        sendQueue.add(data);
    }

    private void startListening() {
        listenerThread = new Thread(() -> {
            try {
                while (connected && !socket.isClosed()) {
                    int packetId = input.readUnsignedByte();
                    if (packetHandler != null) {
                        packetHandler.handlePacket(packetId, input);
                    }
                }
            } catch (EOFException e) {
                System.out.println("Server closed connection.");
            } catch (SocketException e) {
                System.out.println("Connection lost.");
            } catch (IOException e) {
                if (connected) System.err.println("Network error: " + e.getMessage());
            } finally {
                disconnect();
            }
        });
        listenerThread.setName("Network-Listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public void disconnect() {
        connected = false;
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {}
        System.out.println("Disconnected.");
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    private void writeString64(DataOutputStream out, String str) throws IOException {
        byte[] bytes = str.getBytes("UTF-8");
        byte[] data = new byte[64];
        int len = Math.min(bytes.length, 64);
        System.arraycopy(bytes, 0, data, 0, len);
        for (int i = len; i < 64; i++) data[i] = 0x20;
        out.write(data);
    }

    public static String readString64(DataInputStream in) throws IOException {
        byte[] data = new byte[64];
        in.readFully(data);
        int end = 63;
        while (end >= 0 && data[end] == 0x20) end--;
        return new String(data, 0, end + 1, "UTF-8").trim();
    }

    private void writeFloat16(DataOutputStream out, float value) throws IOException {
        int fixed = (int)(value * 32.0f);
        out.writeInt(fixed);
    }

    public static float readFloat16(DataInputStream in) throws IOException {
        return in.readInt() / 32.0f;
    }
}