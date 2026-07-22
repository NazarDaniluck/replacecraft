package net.replacecraft.network;

import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class NetworkManager {
    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean connecting = new AtomicBoolean(false);
    
    private Thread listenerThread;
    private Thread sendThread;
    private PacketHandler packetHandler;
    private final ConcurrentLinkedQueue<byte[]> sendQueue = new ConcurrentLinkedQueue<>();
    
    // Максимальный размер очереди перед сбросом старых пакетов
    private static final int MAX_QUEUE_SIZE = 200;
    
    // Интервал отправки позиции (мс)
    private static final long POSITION_SEND_INTERVAL = 250;
    private long lastPositionSend = 0;
    
    // Последняя отправленная позиция (чтобы не слать одинаковые)
    private float lastSentX, lastSentY, lastSentZ;
    private float lastSentYaw, lastSentPitch;
    private static final float MIN_POSITION_DELTA = 0.05f;
    private static final float MIN_ROTATION_DELTA = 1.0f;
    
    // Время последней активности
    private volatile long lastPacketReceived = 0;
    private static final long TIMEOUT = 60000; // 60 секунд таймаут
    
    public NetworkManager() {
        lastPacketReceived = System.currentTimeMillis();
    }

    public void setPacketHandler(PacketHandler handler) {
        this.packetHandler = handler;
    }

    /**
     * Подключается к серверу
     */
    public void connect(String host, int port, String username, String mppass) throws IOException {
        if (connecting.get() || connected.get()) {
            disconnect();
        }
        
        connecting.set(true);
        
        try {
            System.out.println("[Network] Connecting to " + host + ":" + port + "...");
            
            this.socket = new Socket();
            socket.setTcpNoDelay(true);
            socket.setSoTimeout(10000); // Таймаут на подключение 10 сек
            socket.connect(new InetSocketAddress(host, port), 10000);
            socket.setSoTimeout(0); // Убираем таймаут после подключения
            
            input = new DataInputStream(new BufferedInputStream(socket.getInputStream(), 8192));
            output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream(), 8192));

            connected.set(true);
            lastPacketReceived = System.currentTimeMillis();
            
            System.out.println("[Network] Connected to " + host + ":" + port);

            // Запускаем поток отправки
            startSendThread();

            // Отправляем идентификацию
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            DataOutputStream dataOut = new DataOutputStream(byteOut);
            dataOut.writeByte(0x00); // Packet ID
            dataOut.writeByte(0x07); // Protocol version
            writeString64(dataOut, username);
            writeString64(dataOut, ClientIdentity.getFullIdentifier());
            dataOut.writeByte(0x00); // Unused
            sendRaw(byteOut.toByteArray());

            System.out.println("[Network] Sent identification: " + username);

            // Ждём первый ответ синхронно (с таймаутом)
            socket.setSoTimeout(5000);
            try {
                int firstPacketId = input.readUnsignedByte();
                System.out.println("[Network] Received first packet: 0x" + Integer.toHexString(firstPacketId));
                if (packetHandler != null) {
                    packetHandler.handlePacket(firstPacketId, input);
                }
            } catch (SocketTimeoutException e) {
                System.err.println("[Network] Timeout waiting for server response");
                disconnect();
                throw new IOException("Server did not respond");
            }
            socket.setSoTimeout(0);

            // Запускаем слушатель
            startListening();
            
        } catch (IOException e) {
            connecting.set(false);
            connected.set(false);
            throw e;
        } finally {
            connecting.set(false);
        }
    }

    /**
     * Отправляет позицию игрока (только если она изменилась)
     */
    public void sendPlayerPosition(float x, float y, float z, float yaw, float pitch) {
        if (!connected.get()) return;
        
        long now = System.currentTimeMillis();
        
        // Проверяем интервал отправки
        if (now - lastPositionSend < POSITION_SEND_INTERVAL) {
            return;
        }
        
        // Проверяем, изменилась ли позиция
        float dx = Math.abs(x - lastSentX);
        float dy = Math.abs(y - lastSentY);
        float dz = Math.abs(z - lastSentZ);
        float dyaw = Math.abs(yaw - lastSentYaw);
        float dpitch = Math.abs(pitch - lastSentPitch);
        
        if (dx < MIN_POSITION_DELTA && dy < MIN_POSITION_DELTA && dz < MIN_POSITION_DELTA 
            && dyaw < MIN_ROTATION_DELTA && dpitch < MIN_ROTATION_DELTA) {
            return; // Позиция не изменилась
        }
        
        lastPositionSend = now;
        lastSentX = x;
        lastSentY = y;
        lastSentZ = z;
        lastSentYaw = yaw;
        lastSentPitch = pitch;
        
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        DataOutputStream dataOut = new DataOutputStream(byteOut);
        
        try {
            dataOut.writeByte(0x08);
            dataOut.writeByte(0xFF); // playerId = -1 (свой)
            writeFloat16(dataOut, x);
            writeFloat16(dataOut, y);
            writeFloat16(dataOut, z);
            dataOut.writeByte((byte) Math.round(yaw));
            dataOut.writeByte((byte) Math.round(pitch));
            sendRaw(byteOut.toByteArray());
        } catch (IOException e) {
            // ByteArrayOutputStream не бросает IOException
        }
    }

    /**
     * Отправляет установку/разрушение блока
     */
    public void sendSetBlock(int x, int y, int z, byte mode, byte blockType) {
        if (!connected.get()) return;
        
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        DataOutputStream dataOut = new DataOutputStream(byteOut);
        
        try {
            dataOut.writeByte(0x05);
            dataOut.writeShort((short) x);
            dataOut.writeShort((short) y);
            dataOut.writeShort((short) z);
            dataOut.writeByte(mode);
            dataOut.writeByte(blockType);
            sendRaw(byteOut.toByteArray());
        } catch (IOException e) {
            // Не должно произойти
        }
    }

    /**
     * Отправляет сообщение в чат
     */
    public void sendMessage(String message) {
        if (!connected.get() || message == null || message.isEmpty()) return;
        
        // Обрезаем длинные сообщения
        if (message.length() > 60) {
            message = message.substring(0, 60);
        }
        
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        DataOutputStream dataOut = new DataOutputStream(byteOut);
        
        try {
            dataOut.writeByte(0x0D);
            dataOut.writeByte((byte) 0xFF);
            writeString64(dataOut, message);
            sendRaw(byteOut.toByteArray());
        } catch (IOException e) {
            // Не должно произойти
        }
    }

    /**
     * Добавляет пакет в очередь отправки
     */
    private void sendRaw(byte[] data) {
        if (!connected.get() || socket == null || socket.isClosed()) return;
        
        // Ограничиваем размер очереди
        while (sendQueue.size() > MAX_QUEUE_SIZE) {
            sendQueue.poll(); // Удаляем самый старый пакет
        }
        
        sendQueue.add(data);
    }

    /**
     * Запускает поток отправки пакетов
     */
    private void startSendThread() {
        sendThread = new Thread(() -> {
            while (connected.get()) {
                try {
                    byte[] data = sendQueue.poll();
                    if (data != null) {
                        synchronized (output) {
                            output.write(data);
                            output.flush();
                        }
                    } else {
                        // Очередь пуста — небольшая пауза
                        Thread.sleep(1);
                    }
                } catch (InterruptedException e) {
                    break;
                } catch (IOException e) {
                    System.err.println("[Network] Send error: " + e.getMessage());
                    disconnect();
                    break;
                }
            }
        }, "Network-Send");
        sendThread.setDaemon(true);
        sendThread.start();
    }

    /**
     * Запускает поток прослушивания входящих пакетов
     */
    private void startListening() {
        listenerThread = new Thread(() -> {
            try {
                while (connected.get() && socket != null && !socket.isClosed()) {
                    int packetId;
                    
                    try {
                        packetId = input.readUnsignedByte();
                        lastPacketReceived = System.currentTimeMillis();
                    } catch (EOFException e) {
                        System.out.println("[Network] Server closed connection");
                        break;
                    } catch (SocketException e) {
                        if (connected.get()) {
                            System.out.println("[Network] Connection lost: " + e.getMessage());
                        }
                        break;
                    } catch (SocketTimeoutException e) {
                        // Таймаут — проверяем, не пора ли отключиться
                        if (System.currentTimeMillis() - lastPacketReceived > TIMEOUT) {
                            System.out.println("[Network] Connection timed out");
                            break;
                        }
                        continue;
                    }
                    
                    if (packetHandler != null) {
                        try {
                            packetHandler.handlePacket(packetId, input);
                        } catch (Exception e) {
                            System.err.println("[Network] Error handling packet 0x" + Integer.toHexString(packetId) + ": " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            } catch (IOException e) {
                if (connected.get()) {
                    System.err.println("[Network] Network error: " + e.getMessage());
                }
            } finally {
                disconnect();
            }
        }, "Network-Listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    /**
     * Отключается от сервера
     */
    public void disconnect() {
        if (!connected.getAndSet(false) && !connecting.getAndSet(false)) {
            return; // Уже отключены
        }
        
        System.out.println("[Network] Disconnecting...");
        
        // Очищаем очередь отправки
        sendQueue.clear();
        
        // Закрываем сокет
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // Игнорируем
        }
        
        socket = null;
        input = null;
        output = null;
        
        System.out.println("[Network] Disconnected");
    }

    /**
     * Проверяет, подключены ли мы
     */
    public boolean isConnected() {
        return connected.get() && socket != null && !socket.isClosed();
    }
    
    /**
     * Проверяет, идёт ли подключение
     */
    public boolean isConnecting() {
        return connecting.get();
    }

    // ===== Вспомогательные методы для работы с протоколом =====

    private void writeString64(DataOutputStream out, String str) throws IOException {
        byte[] bytes = str.getBytes("UTF-8");
        byte[] data = new byte[64];
        int len = Math.min(bytes.length, 64);
        System.arraycopy(bytes, 0, data, 0, len);
        // Заполняем пробелами
        for (int i = len; i < 64; i++) {
            data[i] = 0x20;
        }
        out.write(data);
    }

    public static String readString64(DataInputStream in) throws IOException {
        byte[] data = new byte[64];
        in.readFully(data);
        // Убираем завершающие пробелы
        int end = 63;
        while (end >= 0 && data[end] == 0x20) {
            end--;
        }
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