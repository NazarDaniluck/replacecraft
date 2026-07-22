package net.replacecraft;

import java.io.*;

public class DiscordRPC {
    private static final String CLIENT_ID = "1521473639365738547";
    private static RandomAccessFile pipe;
    private static volatile boolean running = false;
    private static Thread rpcThread;

    public static void start() {
        if (running) return;
        
        rpcThread = new Thread(() -> {
            try {
                String[] pipePaths = {
                    "\\\\?\\pipe\\discord-ipc-0",
                    "\\\\.\\pipe\\discord-ipc-0"
                };
                
                for (String path : pipePaths) {
                    try {
                        pipe = new RandomAccessFile(path, "rw");
                        break;
                    } catch (Exception e) {
                        pipe = null;
                    }
                }
                
                if (pipe == null) {
                    System.out.println("Discord RPC: Discord not running");
                    return;
                }
                
                running = true;
                int nonce = 0;

                // Handshake
                sendFrame(0, "{\"v\":1,\"client_id\":\"" + CLIENT_ID + "\"}");
                safeReadFrame();

                // SET_ACTIVITY
                long startTime = System.currentTimeMillis() / 1000;
                String activity = "{\"cmd\":\"SET_ACTIVITY\"," +
                    "\"args\":{" +
                        "\"pid\":" + getPID() + "," +
                        "\"activity\":{" +
                            "\"details\":\"ReplaceCraft\"," +
                            "\"state\":\"rc-0.1c\"," +
                            "\"timestamps\":{\"start\":" + startTime + "}" +
                        "}" +
                    "}," +
                    "\"nonce\":\"" + (nonce++) + "\"}";
                sendFrame(1, activity);
                safeReadFrame();
                
                System.out.println("Discord RPC: Active!");

                while (running) {
                    Thread.sleep(15000);
                    if (!running) break;
                    sendFrame(1, "{\"cmd\":\"PING\",\"nonce\":\"" + (nonce++) + "\"}");
                    safeReadFrame();
                }
            } catch (InterruptedException e) {
                // Остановка
            } catch (Exception e) {
                System.out.println("Discord RPC: " + e.getMessage());
            } finally {
                running = false;
                try { if (pipe != null) pipe.close(); } catch (Exception e) {}
            }
        }, "DiscordRPC");
        rpcThread.setDaemon(true);
        rpcThread.start();
    }

    public static void stop() {
        running = false;
        if (rpcThread != null) {
            rpcThread.interrupt();
        }
    }

    private static void safeReadFrame() {
        try {
            readFrame();
        } catch (Exception e) {
            // Игнорируем ошибки чтения
        }
    }

    private static void sendFrame(int opcode, String data) throws IOException {
        if (pipe == null || !running) return;
        byte[] payload = data.getBytes("UTF-8");
        ByteArrayOutputStream frame = new ByteArrayOutputStream();
        writeIntLE(frame, opcode);
        writeIntLE(frame, payload.length);
        frame.write(payload);
        pipe.write(frame.toByteArray());
    }

    private static void writeIntLE(ByteArrayOutputStream out, int value) {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
        out.write((value >> 16) & 0xFF);
        out.write((value >> 24) & 0xFF);
    }

    private static String readFrame() throws IOException {
        if (pipe == null) return "";
        byte[] header = new byte[8];
        int read = pipe.read(header);
        if (read < 8) return "";
        
        int length = (header[4] & 0xFF) | ((header[5] & 0xFF) << 8) | 
                     ((header[6] & 0xFF) << 16) | ((header[7] & 0xFF) << 24);
        
        if (length > 0 && length < 65536) {
            byte[] data = new byte[length];
            pipe.readFully(data);
            return new String(data, "UTF-8");
        }
        return "";
    }

    private static long getPID() {
        try {
            String name = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
            return Long.parseLong(name.split("@")[0]);
        } catch (Exception e) {
            return 0;
        }
    }
}