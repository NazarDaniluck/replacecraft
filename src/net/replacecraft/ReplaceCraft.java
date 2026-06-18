package net.replacecraft;

import net.replacecraft.network.*;
import net.replacecraft.level.Level;
import net.replacecraft.debug.WorldIO;
import net.replacecraft.particle.ParticleEngine;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ReplaceCraft implements Runnable, NetworkListener {
    private boolean running = false;
    private int width = 854;
    private int height = 480;
    private boolean fullscreen = false;
    private int guiScale = 2; // Масштаб GUI (не зависит от разрешения)
    private float guiScaleFactor = 2.0f;
    
    private boolean needsRenderUpdate = false;
    private Level pendingLevel = null;
    
    private Level level;
    private Player player;
    private Renderer renderer;
    private ParticleEngine particleEngine;

    private Font minecraftFont;
    private int currentFps = 0;
    private long lastFpsTime = 0;
    private String fpsDisplayString = "0 fps, 0 chunk updates";

    private NetworkManager network;
    private PacketHandler packetHandler;
    private boolean isMultiplayer = false;
    private String serverIp = "";
    private int serverPort = 25565;

    private int[] inventory = {3, 2, 1, 4, 5, 6, 7};
    private int selectedSlot = 0;
    private float blockRotation = 0.0f;

    private boolean showDebugMenu = false;
    private ServerConnectGUI serverGui;
    private boolean guiOpen = false;
    private Map<Byte, PlayerEntity> remotePlayers = new HashMap<>();

    private String disconnectReason = null;
    private long disconnectMessageTime = 0;
    private long levelLoadTime = 0;       // <-- НОВОЕ
    private long lastPositionSend = 0;

    public void start() {
        this.running = true;
        new Thread(this, "Game Loop").start();
    }

    public void init() throws LWJGLException {
        GameLogger.init();
        GameLogger.log("Client starting...");

        // Пробуем загрузить сохранённые настройки
        loadSettings();

        Display.setDisplayMode(new DisplayMode(width, height));
        Display.setFullscreen(fullscreen);
        Display.setResizable(true); // <-- РАЗРЕШАЕМ ИЗМЕНЕНИЕ РАЗМЕРА
        Display.create();
        Keyboard.create();
        Mouse.create();
        Mouse.setGrabbed(true);

        network = new NetworkManager();
        packetHandler = new PacketHandler(network);
        packetHandler.setNetworkListener(this);
        network.setPacketHandler(packetHandler);
        
        this.level = new Level(256, 64, 256, true);
        this.player = new Player(level);
        this.renderer = new Renderer(level, player);
        this.particleEngine = new ParticleEngine(level);
        this.minecraftFont = new Font("default.png");

        serverGui = new ServerConnectGUI(minecraftFont);
        serverGui.setCallback((ip, port, name) -> connectToClassicServer(ip, port, name));

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glClearColor(0.5f, 0.8f, 1.0f, 1.0f);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glEnable(GL11.GL_CULL_FACE);

        lastFpsTime = System.currentTimeMillis();
        GameLogger.log("Client ready");
    }

    private void loadSettings() {
        java.io.File file = new java.io.File("client_settings.ini");
        if (file.exists()) {
            try {
                java.util.Properties props = new java.util.Properties();
                props.load(new java.io.FileInputStream(file));
                width = Integer.parseInt(props.getProperty("width", "854"));
                height = Integer.parseInt(props.getProperty("height", "480"));
                fullscreen = Boolean.parseBoolean(props.getProperty("fullscreen", "false"));
                guiScale = Integer.parseInt(props.getProperty("guiscale", "2"));
                guiScaleFactor = (float) guiScale;
            } catch (Exception e) {
                // defaults
            }
        }
    }

    private void saveSettings() {
        try {
            java.util.Properties props = new java.util.Properties();
            props.setProperty("width", String.valueOf(width));
            props.setProperty("height", String.valueOf(height));
            props.setProperty("fullscreen", String.valueOf(fullscreen));
            props.setProperty("guiscale", String.valueOf(guiScale));
            props.store(new java.io.FileOutputStream("client_settings.ini"), "ReplaceCraft Settings");
        } catch (Exception e) {}
    }
    
    public void connectToClassicServer(String ip, int port, String username) {
        this.serverIp = ip;
        this.serverPort = port;
        new Thread(() -> {
            try {
                GameLogger.log("Connecting to " + ip + ":" + port + " ...");
                network.connect(ip, port, username, "");
                isMultiplayer = true;
            } catch (IOException e) {
                GameLogger.log("Failed to connect: " + e.getMessage());
                isMultiplayer = false;
                showDisconnectMessage("Failed to connect: " + e.getMessage());
            }
        }, "ServerConnect").start();
    }

    private void showDisconnectMessage(String reason) {
        this.disconnectReason = reason;
        this.disconnectMessageTime = System.currentTimeMillis();
    }

    @Override
    public void run() {
        try { init(); } catch (LWJGLException e) { e.printStackTrace(); System.exit(0); }

        long lastTime = System.nanoTime();
        double nsPerTick = 1000000000.0 / 60.0;
        double delta = 0;

        while (running) {
            if (Display.isCloseRequested()) running = false;
            
            // Проверка изменения размера окна
            if (Display.wasResized()) {
                GL11.glViewport(0, 0, Display.getWidth(), Display.getHeight());
                if (!fullscreen) {
                    width = Display.getWidth();
                    height = Display.getHeight();
                }
            }
            
            long now = System.nanoTime();
            delta += (now - lastTime) / nsPerTick;
            lastTime = now;
            while (delta >= 1) { tick(); delta--; }
            render((float) delta);
            updateFps();
            Display.update();
        }
        destroy();
    }

    public void tick() {
        if (level == null) return;
    	if (serverGui != null && serverGui.isVisible()) {
            serverGui.handleInput();
            if (!serverGui.isVisible()) {
                Mouse.setGrabbed(true);
                guiOpen = false;
            }
            return;
        }

        while (Keyboard.next()) {
            if (Keyboard.getEventKeyState()) {
                int key = Keyboard.getEventKey();
                if (key == Keyboard.KEY_ESCAPE) Mouse.setGrabbed(!Mouse.isGrabbed());
                if (key == Keyboard.KEY_GRAVE) showDebugMenu = !showDebugMenu;
                if (key == Keyboard.KEY_T && !showDebugMenu) {
                    // Открываем чат (упрощённо — отправляем "Hello")
                    try { network.sendMessage("Hello from Player"); }
                    catch (IOException e) {}
                }
                
                if (showDebugMenu) {
                    if (key == Keyboard.KEY_F1) player.flyMode = !player.flyMode;
                    if (key == Keyboard.KEY_F2) player.noClipMode = !player.noClipMode;
                    if (key == Keyboard.KEY_F3) toggleFullscreen();
                    if (key == Keyboard.KEY_F5) WorldIO.saveWorld(level);
                    if (key == Keyboard.KEY_F9) { if (WorldIO.loadWorld(level)) renderer.allChunksChanged(); }
                    if (key == Keyboard.KEY_F10) {
                        if (serverGui.isVisible()) { serverGui.hide(); Mouse.setGrabbed(true); guiOpen = false; }
                        else { serverGui.show(); Mouse.setGrabbed(false); guiOpen = true; }
                    }
                    if (key == Keyboard.KEY_F11) {
                        network.disconnect();
                        isMultiplayer = false;
                        remotePlayers.clear();
                        showDisconnectMessage("Disconnected from server");
                    }
                }

                if (key == Keyboard.KEY_1) selectedSlot = 0;
                if (key == Keyboard.KEY_2) selectedSlot = 1;
                if (key == Keyboard.KEY_3) selectedSlot = 2;
                if (key == Keyboard.KEY_4) selectedSlot = 3;
                if (key == Keyboard.KEY_5) selectedSlot = 4;
                if (key == Keyboard.KEY_6) selectedSlot = 5;
                if (key == Keyboard.KEY_7) selectedSlot = 6;
            }
        }

        while (Mouse.next()) {
            if (Mouse.isGrabbed() && !showDebugMenu) {
                if (Mouse.getEventButton() == 0 && Mouse.getEventButtonState()) {
                    Player.HitResult hit = player.rayTrace(5.0F);
                    if (hit != null) {
                        particleEngine.spawnBlockParticles(hit.x, hit.y, hit.z, level.getBlock(hit.x, hit.y, hit.z));
                        level.setBlock(hit.x, hit.y, hit.z, 0);
                        if (isMultiplayer && network.isConnected()) {
                            try { network.sendSetBlock(hit.x, hit.y, hit.z, (byte) 0x01, (byte) 0); }
                            catch (IOException e) {}
                        }
                    }
                }
                if (Mouse.getEventButton() == 1 && Mouse.getEventButtonState()) {
                    Player.HitResult hit2 = player.rayTrace(5.0F);
                    if (hit2 != null) {
                        int blockToPlace = inventory[selectedSlot];
                        level.setBlock(hit2.faceX, hit2.faceY, hit2.faceZ, blockToPlace);
                        if (isMultiplayer && network.isConnected()) {
                            try { network.sendSetBlock(hit2.faceX, hit2.faceY, hit2.faceZ, (byte) 0x00, (byte) blockToPlace); }
                            catch (IOException e) {}
                        }
                    }
                }
            }
        }

        player.tick();
        level.tick();
        if (particleEngine != null) {
            particleEngine.tick();
        }
        blockRotation += 1.5f;
        
        if (isMultiplayer && network.isConnected()) {
            long now = System.currentTimeMillis();
            if (now - levelLoadTime > 2000 && now - lastPositionSend > 100) {
                lastPositionSend = now;
                try { 
                    network.sendPlayerPosition(player.x, player.y, player.z, player.yRot, player.xRot);
                } catch (IOException e) {
                    GameLogger.log("ERROR sending position: " + e.getMessage());
                    // НЕ отключаемся при ошибке!
                }
            }
        }
    }
    private void toggleFullscreen() {
        fullscreen = !fullscreen;
        try {
            if (fullscreen) {
                // Сохраняем текущий размер окна
                width = Display.getWidth();
                height = Display.getHeight();
                Display.setDisplayModeAndFullscreen(Display.getDesktopDisplayMode());
            } else {
                Display.setFullscreen(false);
                Display.setDisplayMode(new DisplayMode(width, height));
            }
            // Обновляем вьюпорт
            GL11.glViewport(0, 0, Display.getWidth(), Display.getHeight());
            saveSettings();
            GameLogger.log("Fullscreen: " + fullscreen);
        } catch (LWJGLException e) {
            GameLogger.log("Failed to toggle fullscreen: " + e.getMessage());
            fullscreen = !fullscreen;
        }
    }
    
    // ===== NetworkListener =====

    @Override
    public void onLevelReceived(int width, int height, int depth, byte[] blocks) {
        GameLogger.log("=== LEVEL RECEIVED ===");
        
        // Создаём новый уровень (без вызовов OpenGL!)
        Level newLevel = new Level(width, height, depth, false);
        System.arraycopy(blocks, 0, newLevel.getBlocks(), 0, Math.min(blocks.length, newLevel.getBlocks().length));
        
        // Сохраняем для обработки в главном потоке
        this.pendingLevel = newLevel;
        this.needsRenderUpdate = true;
        
        GameLogger.log("=== LEVEL PENDING ===");
    }

    @Override
    public void onBlockChanged(int x, int y, int z, int blockType) {
        if (level != null) level.setBlock(x, y, z, blockType);
    }

    @Override
    public void onPlayerSpawned(byte playerId, String name, float x, float y, float z, byte yaw, byte pitch) {
        GameLogger.log("Player joined: " + name);
        remotePlayers.put(playerId, new PlayerEntity(playerId, name, x, y, z,
            (yaw & 0xFF) * 360.0f / 256.0f, (pitch & 0xFF) * 360.0f / 256.0f));
    }

    @Override
    public void onPlayerDespawned(byte playerId) {
        remotePlayers.remove(playerId);
    }

    @Override
    public void onOwnPositionReceived(float x, float y, float z, byte yaw, byte pitch) {
        if (player != null) {
            player.setPosition(x, y, z);
            player.yRot = (yaw & 0xFF) * 360.0f / 256.0f;
            player.xRot = (pitch & 0xFF) * 360.0f / 256.0f;
        }
    }

    @Override
    public void onPlayerPositionUpdate(byte playerId, float x, float y, float z, byte yaw, byte pitch) {
        PlayerEntity entity = remotePlayers.get(playerId);
        if (entity != null) {
            entity.x = x; entity.y = y; entity.z = z;
            entity.yaw = (yaw & 0xFF) * 360.0f / 256.0f;
            entity.pitch = (pitch & 0xFF) * 360.0f / 256.0f;
        }
    }

    @Override
    public void onMessageReceived(byte playerId, String message) {
        GameLogger.log("[Chat] " + message);
    }

    @Override
    public void onDisconnected(String reason) {
        GameLogger.log("Disconnected: " + reason);
        isMultiplayer = false;
        remotePlayers.clear();
        showDisconnectMessage(reason);
        
        // Генерируем новый локальный мир
        this.level = new Level(256, 64, 256, true);
        this.player.setLevel(this.level);
        this.renderer.setLevel(this.level);
        this.particleEngine.setLevel(this.level);
        this.player.setPosition(level.width / 2f, level.height / 2f + 2, level.depth / 2f);
    }

    // ===== Render =====

    private void updateFps() {
        currentFps++;
        if (System.currentTimeMillis() - lastFpsTime >= 1000) {
            fpsDisplayString = currentFps + " fps, 0 chunk updates";
            currentFps = 0;
            lastFpsTime += 1000;
        }
    }

    public void render(float a) {
        if (Mouse.isGrabbed() && !guiOpen) {
            player.turn((float) Mouse.getDX(), (float) Mouse.getDY());
        }

        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glColor3f(1.0f, 1.0f, 1.0f);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GLU.gluPerspective(70.0f, (float) Display.getWidth() / Display.getHeight(), 0.05f, 1000.0f);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        setupCamera(a);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glEnable(GL11.GL_CULL_FACE);

        // === ВОТ СЮДА ВСТАВЬ ===
        if (needsRenderUpdate && pendingLevel != null) {
            this.level = pendingLevel;
            this.renderer = new Renderer(pendingLevel, player);  // теперь безопасно, главный поток
            this.player.setLevel(pendingLevel);
            pendingLevel = null;
            needsRenderUpdate = false;
            GameLogger.log("=== LEVEL APPLIED ===");
        }
        renderer.render();
        
        if (isMultiplayer && !remotePlayers.isEmpty()) {
            for (PlayerEntity entity : remotePlayers.values()) {
                entity.render(minecraftFont, player.yRot);
            }
        }

        if (particleEngine != null) {
            particleEngine.render(renderer.getTextureID(), player.xRot, player.yRot);
        }
        drawSelectionBox();
        renderGUI();
    }

    private void renderGUI() {
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0, Display.getWidth(), Display.getHeight(), 0, 100, -300);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);

        GL11.glPushMatrix();
        GL11.glScalef(guiScaleFactor, guiScaleFactor, 1.0f);

        if (minecraftFont != null) {
            minecraftFont.drawShadow("0.0.11a", 4, 4, 0xFFFFFF);
            minecraftFont.drawShadow(fpsDisplayString, 4, 14, 0xFFFFFF);
            if (isMultiplayer && network.isConnected()) {
                minecraftFont.drawShadow("Server: " + serverIp + ":" + serverPort, 4, 24, 0x55FF55);
            }
            if (showDebugMenu) {
                int y = 44;
                minecraftFont.drawShadow("=== DEBUG ===", 4, y, 0xFFFF00); y += 12;
                minecraftFont.drawShadow("[F1] Fly: " + (player.flyMode ? "ON" : "OFF"), 4, y, 0x00FF00); y += 12;
                minecraftFont.drawShadow("[F2] NoClip: " + (player.noClipMode ? "ON" : "OFF"), 4, y, 0x00FF00); y += 12;
                minecraftFont.drawShadow("[F3] Fullscreen: " + (fullscreen ? "ON" : "OFF"), 4, y, 0x00FF00); y += 12;
                minecraftFont.drawShadow("[F5] Save", 4, y, 0x00FFFF); y += 12;
                minecraftFont.drawShadow("[F9] Load", 4, y, 0x00FFFF); y += 12;
                minecraftFont.drawShadow("[F10] Server", 4, y, 0xFFFF00); y += 12;
                minecraftFont.drawShadow("[F11] Disconnect", 4, y, 0xFF5555);
            }

            // Сообщение об отключении
            if (disconnectReason != null) {
                long elapsed = System.currentTimeMillis() - disconnectMessageTime;
                if (elapsed < 5000) {
                    float alpha = 1.0f;
                    if (elapsed > 4000) alpha = 1.0f - (elapsed - 4000) / 1000.0f;

                    int textWidth = disconnectReason.length() * 16;
                    int tx = (Display.getWidth() / 2 - textWidth) / 2;
                    int ty = (Display.getHeight() / 2 + 40) / 2;

                    GL11.glDisable(GL11.GL_TEXTURE_2D);
                    GL11.glEnable(GL11.GL_BLEND);
                    GL11.glColor4f(0.0f, 0.0f, 0.0f, 0.7f * alpha);
                    GL11.glBegin(GL11.GL_QUADS);
                    GL11.glVertex2f(tx - 10, ty - 2);
                    GL11.glVertex2f(tx + textWidth + 10, ty - 2);
                    GL11.glVertex2f(tx + textWidth + 10, ty + 20);
                    GL11.glVertex2f(tx - 10, ty + 20);
                    GL11.glEnd();
                    GL11.glDisable(GL11.GL_BLEND);
                    GL11.glEnable(GL11.GL_TEXTURE_2D);

                    minecraftFont.drawShadow(disconnectReason, tx, ty, 0xFF5555);
                } else {
                    disconnectReason = null;
                }
            }
        }
        GL11.glPopMatrix();

        // Перекрестие
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor3f(1.0f, 1.0f, 1.0f);
        GL11.glLineWidth(2.0f);
        int cx = Display.getWidth() / 2, cy = Display.getHeight() / 2;
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(cx - 6, cy); GL11.glVertex2f(cx + 6, cy);
        GL11.glVertex2f(cx, cy - 6); GL11.glVertex2f(cx, cy + 6);
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        // Хотбар
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glPushMatrix();
        GL11.glTranslatef(Display.getWidth() - 60, 60, 0);
        GL11.glScalef(40.0f, 40.0f, 40.0f);
        BlockGuiRenderer.renderBlockInGui(inventory[selectedSlot], blockRotation, renderer.getTextureID());
        GL11.glPopMatrix();
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        if (serverGui != null) serverGui.render();
    }

    private void drawSelectionBox() {
        Player.HitResult hit = player.rayTrace(5.0F);
        if (hit != null) {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glColor3f(1.0F, 1.0F, 1.0F);
            GL11.glLineWidth(2.0F);
            float o = 0.002f;
            float x0 = hit.x - o, x1 = hit.x + 1.0F + o;
            float y0 = hit.y - o, y1 = hit.y + 1.0F + o;
            float z0 = hit.z - o, z1 = hit.z + 1.0F + o;
            GL11.glBegin(GL11.GL_LINES);
            GL11.glVertex3f(x0,y0,z0); GL11.glVertex3f(x1,y0,z0);
            GL11.glVertex3f(x1,y0,z0); GL11.glVertex3f(x1,y0,z1);
            GL11.glVertex3f(x1,y0,z1); GL11.glVertex3f(x0,y0,z1);
            GL11.glVertex3f(x0,y0,z1); GL11.glVertex3f(x0,y0,z0);
            GL11.glVertex3f(x0,y1,z0); GL11.glVertex3f(x1,y1,z0);
            GL11.glVertex3f(x1,y1,z0); GL11.glVertex3f(x1,y1,z1);
            GL11.glVertex3f(x1,y1,z1); GL11.glVertex3f(x0,y1,z1);
            GL11.glVertex3f(x0,y1,z1); GL11.glVertex3f(x0,y1,z0);
            GL11.glVertex3f(x0,y0,z0); GL11.glVertex3f(x0,y1,z0);
            GL11.glVertex3f(x1,y0,z0); GL11.glVertex3f(x1,y1,z0);
            GL11.glVertex3f(x1,y0,z1); GL11.glVertex3f(x1,y1,z1);
            GL11.glVertex3f(x0,y0,z1); GL11.glVertex3f(x0,y1,z1);
            GL11.glEnd();
            GL11.glEnable(GL11.GL_TEXTURE_2D);
        }
    }

    private void setupCamera(float a) {
        GL11.glLoadIdentity();
        GL11.glTranslatef(0.0F, 0.0F, -0.3F);
        GL11.glRotatef(player.xRot, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(player.yRot, 0.0F, 1.0F, 0.0F);
        float x = player.xo + (player.x - player.xo) * a;
        float y = player.yo + (player.y - player.yo) * a;
        float z = player.zo + (player.z - player.zo) * a;
        GL11.glTranslatef(-x, -y, -z);
    }

    public void destroy() {
        saveSettings();
        if (network != null) network.disconnect();
        GameLogger.close();
        Keyboard.destroy();
        Mouse.destroy();
        Display.destroy();
    }

    public static void main(String[] args) {
        new ReplaceCraft().start();
    }
}