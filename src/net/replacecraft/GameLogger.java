package net.replacecraft;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GameLogger {
    private static PrintWriter writer;
    private static SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");

    public static void init() {
        try {
            writer = new PrintWriter(new FileWriter("client.log", true), true);
        } catch (IOException e) {
            System.err.println("Log init failed");
        }
    }

    public static void log(String msg) {
        String line = "[" + fmt.format(new Date()) + "] " + msg;
        System.out.println(msg);
        if (writer != null) {
            writer.println(line);
            writer.flush();
        }
    }

    public static void close() {
        log("Client shutdown");
        if (writer != null) writer.close();
    }
}