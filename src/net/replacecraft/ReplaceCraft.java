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
    
    private BlockInventory blockInventory;
    private SettingsMenu settingsMenu;
    private Chat chat;
    private boolean needsRenderUpdate = false;
    private Level pendingLevel = null;

    private Level level;
    private Player player;
    private Renderer renderer;
    private ParticleEngine particleEngine;
    private java.util.List<Mob> mobs = new java.util.ArrayList<>();
    private int charTextureID = -1;
    
    private Font minecraftFont;
    private int currentFps = 0;
    private long lastFpsTime = 0;
    private String fpsDisplayString = "0 fps, 256 chunk updates";

    private TabMenu tabMenu;
    private long lastPingTime = 0;
    private long ping = 0;
    private String serverMotd = "";
    private NetworkManager network;
    private PacketHandler packetHandler;
    private boolean isOp = false;
    private boolean isMultiplayer = false;
    private String serverIp = "";
    private int serverPort = 25565;
    private String playerName = "Player";
    private String serverName = "";
    
    private float blockRotation = 0.0f;

    private boolean showDebugMenu = false;
    private ServerConnectGUI serverGui;
    private boolean guiOpen = false;
    private Map<Byte, PlayerEntity> remotePlayers = new HashMap<>();
    private boolean mobsSpawned = false;
    
    private String disconnectReason = null;
    private long disconnectMessageTime = 0;
    private long levelLoadTime = 0;
    private long lastPositionSend = 0;

    public void start() {
        this.running = true;
        new Thread(this, "Game Loop").start();
    }

    public void init() throws LWJGLException {
        GameLogger.init();
        GameLogger.log("Client starting...");

        Display.setDisplayMode(new DisplayMode(width, height));
        Display.setFullscreen(fullscreen);
        Display.setResizable(true);
        Display.create();
        Keyboard.create();
        Mouse.create();
        Mouse.setGrabbed(true);
        
        this.playerName = UsernameDialog.askUsername();
        
        network = new NetworkManager();
        packetHandler = new PacketHandler(network);
        packetHandler.setNetworkListener(this);
        network.setPacketHandler(packetHandler);

        this.level = new Level(256, 64, 256, true);
        this.player = new Player(level);
        this.renderer = new Renderer(level, player);
        this.particleEngine = new ParticleEngine(level);
        this.minecraftFont = new Font("default.png");
        chat = new Chat(minecraftFont);
        tabMenu = new TabMenu(minecraftFont);
        
        isMultiplayer = false;
        isOp = true;
        
        blockInventory = new BlockInventory(minecraftFont);
        settingsMenu = new SettingsMenu(minecraftFont, this);

     // В ReplaceCraft.init():
        serverGui = new ServerConnectGUI(minecraftFont, playerName);
        serverGui.setCallback((ip, port, name) -> connectToClassicServer(ip, port, name));
        charTextureID = Texture.loadTexture("char.png");
        GameLogger.log("Char texture ID: " + charTextureID);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glEnable(GL11.GL_CULL_FACE);
     // Discord RPC (необязательно, если Discord не запущен — ничего не будет)
        try {
            DiscordRPC.start();
        } catch (Exception e) {
            // Discord не доступен
        }
        
        lastFpsTime = System.currentTimeMillis();
        GameLogger.log("Client ready");
    }

    public void connectToClassicServer(String ip, int port, String username) {
        this.serverIp = ip;
        this.serverPort = port;
        
        // Используем сохранённый ник если не передан
        if (username == null || username.isEmpty()) {
            username = this.playerName;
        }
        
        final String finalUsername = username;
        
        // Сбрасываем читы
        player.flyMode = false;
        player.noClipMode = false;
        
        new Thread(() -> {
            try {
                GameLogger.log("Connecting to " + ip + ":" + port + " ...");
                network.connect(ip, port, finalUsername, "");
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
        int fpsLimit = 0;

        while (running) {
            if (Display.isCloseRequested()) running = false;

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

            int ticks = 0;
            while (delta >= 1 && ticks < 10) {
                tick();
                delta--;
                ticks++;
            }

            // Обработка мыши – всегда, если захвачена
            if (Mouse.isGrabbed()) {
                float dx = (float) Mouse.getDX();
                float dy = (float) Mouse.getDY();
                if (dx != 0 || dy != 0) {
                    player.turn(dx * 0.45f, dy * 0.45f);
                }
            }

            render((float) delta);
            updateFps();

            fpsLimit = settingsMenu != null ? settingsMenu.getFpsLimit() : 120;
            if (fpsLimit > 0) {
                Display.sync(fpsLimit);
            }

            Display.update();
        }
        destroy();
    }

    public void tick() {
        if (needsRenderUpdate && pendingLevel != null && !isMultiplayer) {
            // Применяем новый уровень сразу, не ждём render()
        	tabMenu.update();
        	this.level = pendingLevel;
            this.renderer = new Renderer(pendingLevel, player);
            this.player.setLevel(pendingLevel);
            this.player.setPosition(level.width / 2f, level.height / 2f + 2, level.depth / 2f);
            if (particleEngine != null) {
                particleEngine = new ParticleEngine(pendingLevel);
            }
            pendingLevel = null;
            needsRenderUpdate = false;
            GameLogger.log("=== LEVEL APPLIED IN TICK ===");
        }
    	// Обработка чата
    	if (chat.isOpen()) {
    	    chat.handleInput();
    	    if (!chat.isOpen()) {
    	        String msg = chat.getInput();
    	        if (!msg.isEmpty()) {
    	            if (isMultiplayer && network.isConnected()) {
    	                // В мультиплеере НЕ добавляем "You:", сервер вернёт с ником
    	                network.sendMessage(msg);
    	            } else {
    	                // В одиночной игре добавляем "You:"
    	                chat.addMessage("You: " + msg);
    	            }
    	            chat.clearLastSent();
    	        }
    	    }
    	    return;
    	}
        
        if (serverGui != null && serverGui.isVisible()) {
            serverGui.handleInput();
            if (!serverGui.isVisible()) {
                Mouse.setGrabbed(true);
                guiOpen = false;
            }
            return;
        }
        
        if (tabMenu != null) {
            tabMenu.update();
            if (isMultiplayer) {
                tabMenu.setServerInfo(serverName, serverMotd);
                tabMenu.setMaxPlayers(16);
                tabMenu.updatePlayers(remotePlayers, ping, isOp);
            }
        }
        
     // В конце tick(), после отправки позиции:
        for (PlayerEntity entity : remotePlayers.values()) {
            entity.updateAnimation();
        }
        
        // Меню настроек
        if (settingsMenu != null && settingsMenu.isVisible()) {
            settingsMenu.handleInput();
            if (!settingsMenu.isVisible()) {
                Mouse.setGrabbed(true);
            }
            return;
        }

        // Инвентарь
        if (blockInventory != null && blockInventory.isVisible()) {
            while (Keyboard.next()) {
                if (Keyboard.getEventKeyState()) {
                    int key = Keyboard.getEventKey();
                    if (key == Keyboard.KEY_E || key == Keyboard.KEY_ESCAPE) {
                        blockInventory.hide();
                        Mouse.setGrabbed(true);
                    }
                    if (key == Keyboard.KEY_1) blockInventory.setSelectedSlot(0);
                    if (key == Keyboard.KEY_2) blockInventory.setSelectedSlot(1);
                    if (key == Keyboard.KEY_3) blockInventory.setSelectedSlot(2);
                    if (key == Keyboard.KEY_4) blockInventory.setSelectedSlot(3);
                    if (key == Keyboard.KEY_5) blockInventory.setSelectedSlot(4);
                    if (key == Keyboard.KEY_6) blockInventory.setSelectedSlot(5);
                    if (key == Keyboard.KEY_7) blockInventory.setSelectedSlot(6);
                }
            }
            int wheel = Mouse.getDWheel();
            if (wheel > 0) blockInventory.nextSlot();
            if (wheel < 0) blockInventory.prevSlot();
        }
        
        // Основной ввод с клавиатуры
        while (Keyboard.next()) {
            if (Keyboard.getEventKeyState()) {
                int key = Keyboard.getEventKey();

                if (key == Keyboard.KEY_T) {
                    chat.openChat();
                }
                
                if (key == Keyboard.KEY_ESCAPE) Mouse.setGrabbed(!Mouse.isGrabbed());
                if (key == Keyboard.KEY_GRAVE) showDebugMenu = !showDebugMenu;

                if (key == Keyboard.KEY_E) {
                    blockInventory.toggle();
                }

                if (key == Keyboard.KEY_F4) {
                    settingsMenu.toggle();
                    Mouse.setGrabbed(!settingsMenu.isVisible());
                }
                
                if (key == Keyboard.KEY_F) {
                    float mx = player.x + (float)Math.sin(Math.toRadians(player.yRot)) * 2;
                    float mz = player.z - (float)Math.cos(Math.toRadians(player.yRot)) * 2;
                    float my = player.y;
                    
                    for (int ty = level.height - 1; ty > 0; ty--) {
                        if (level.getBlock((int)mx, ty, (int)mz) != 0) {
                            my = ty + 2;
                            break;
                        }
                    }
                    
                    Mob mob = new Mob(level, mx, my, mz);
                    mob.setTexture(charTextureID);
                    mobs.add(mob);
                    GameLogger.log("Mob spawned!");
                }
                
                if (key == Keyboard.KEY_T) {
                    chat.openChat();
                    Mouse.setGrabbed(false);
                }
                
                if (key == Keyboard.KEY_1) blockInventory.setSelectedSlot(0);
                if (key == Keyboard.KEY_2) blockInventory.setSelectedSlot(1);
                if (key == Keyboard.KEY_3) blockInventory.setSelectedSlot(2);
                if (key == Keyboard.KEY_4) blockInventory.setSelectedSlot(3);
                if (key == Keyboard.KEY_5) blockInventory.setSelectedSlot(4);
                if (key == Keyboard.KEY_6) blockInventory.setSelectedSlot(5);
                if (key == Keyboard.KEY_7) blockInventory.setSelectedSlot(6);
                if (key == Keyboard.KEY_8) blockInventory.setSelectedSlot(7);
                if (key == Keyboard.KEY_9) blockInventory.setSelectedSlot(8);

                int wheel = Mouse.getDWheel();
                if (wheel > 0) blockInventory.nextSlot();
                if (wheel < 0) blockInventory.prevSlot();

                if (showDebugMenu) {
                    if (!isMultiplayer) {
                    if (key == Keyboard.KEY_F1) if (isOp) player.flyMode = !player.flyMode;
                    if (key == Keyboard.KEY_F2) if (isOp) player.noClipMode = !player.noClipMode;
                    }
                    if (key == Keyboard.KEY_F3) toggleFullscreen();
                    if (key == Keyboard.KEY_F5) if (isOp) WorldIO.saveWorld(level);
                {
                    if (key == Keyboard.KEY_F9) if (isOp) { if (WorldIO.loadWorld(level)) renderer.allChunksChanged(); }
                    if (key == Keyboard.KEY_F10) {
                        if (serverGui.isVisible()) { serverGui.hide(); Mouse.setGrabbed(true); guiOpen = false; }
                        else { serverGui.show(); Mouse.setGrabbed(false); guiOpen = true; }
                    }
                }
                if (key == Keyboard.KEY_F11) {
                    System.exit(0);
                }
                }
            }
}
        // Мышь (разрушение/установка)
        while (Mouse.next()) {
            if (Mouse.isGrabbed() && !showDebugMenu && !blockInventory.isVisible()) {
                // ЛКМ — разрушить блок
                if (Mouse.getEventButton() == 0 && Mouse.getEventButtonState()) {
                    Player.HitResult hit = player.rayTrace(5.0F);
                    if (hit != null) {
                        particleEngine.spawnBlockParticles(hit.x, hit.y, hit.z, level.getBlock(hit.x, hit.y, hit.z));
                        level.setBlock(hit.x, hit.y, hit.z, 0);
                        if (isMultiplayer && network.isConnected()) {
                            network.sendSetBlock(hit.x, hit.y, hit.z, (byte) 0x01, (byte) 0);
                        }
                    }
                }
                // ПКМ — поставить блок
                if (Mouse.getEventButton() == 1 && Mouse.getEventButtonState()) {
                    Player.HitResult hit2 = player.rayTrace(5.0F);
                    if (hit2 != null) {
                        int blockToPlace = blockInventory.getSelectedBlock();
                        int placeX = hit2.faceX;
                        int placeY = hit2.faceY;
                        int placeZ = hit2.faceZ;
                        
                        int px = (int) Math.floor(player.x);
                        int py = (int) Math.floor(player.y - 0.01f);
                        int pz = (int) Math.floor(player.z);
                        
                        if (placeX == px && placeZ == pz && placeY >= py - 1 && placeY <= py + 2) {
                            level.setBlock(placeX, placeY, placeZ, blockToPlace);
                            player.y = placeY + 1.8f;
                            player.yo = player.y;
                            player.yd = 0;
                            if (isMultiplayer && network.isConnected()) {
                                network.sendSetBlock(placeX, placeY, placeZ, (byte) 0x00, (byte) blockToPlace);
                            }
                        } else {
                            level.setBlock(placeX, placeY, placeZ, blockToPlace);
                            if (isMultiplayer && network.isConnected()) {
                                network.sendSetBlock(placeX, placeY, placeZ, (byte) 0x00, (byte) blockToPlace);
                            }
                        }
                    }
                }
            }
        }

        player.tick();
        level.tick();
        for (Mob mob : mobs) {
            mob.tick();
        }
        if (particleEngine != null) particleEngine.tick();
        blockRotation += 1.5f;
     // Отправляем пинг каждый 3 секунды
        if (isMultiplayer && network.isConnected()) {
            long now = System.currentTimeMillis();
            if (now - lastPingTime > 3000) {
                lastPingTime = now;
                ping = System.currentTimeMillis() - lastPingTime;
            }
        }
        if (isMultiplayer && network.isConnected()) {
            long now = System.currentTimeMillis();
            if (now - levelLoadTime > 2000 && now - lastPositionSend > 200) {
                lastPositionSend = now;
                network.sendPlayerPosition(player.x, player.y, player.z, player.yRot, player.xRot);
            }
        }
    }

    private void toggleFullscreen() {
        fullscreen = !fullscreen;
        try {
            if (fullscreen) {
                width = Display.getWidth();
                height = Display.getHeight();
                Display.setDisplayModeAndFullscreen(Display.getDesktopDisplayMode());
            } else {
                Display.setFullscreen(false);
                Display.setDisplayMode(new DisplayMode(width, height));
            }
            GL11.glViewport(0, 0, Display.getWidth(), Display.getHeight());
            GameLogger.log("Fullscreen: " + fullscreen);
        } catch (LWJGLException e) {
            GameLogger.log("Failed to toggle fullscreen: " + e.getMessage());
            fullscreen = !fullscreen;
        }
    }

    private void spawnMobs() {
        for (int i = 0; i < 5; i++) {
            float mx, my, mz;
            int attempts = 0;
            // Ищем свободное место
            do {
                mx = level.width / 2f + (float)(Math.random() - 0.5) * 30;
                mz = level.depth / 2f + (float)(Math.random() - 0.5) * 30;
                // Ищем поверхность
                my = level.height / 2f + 5;
                for (int ty = level.height - 1; ty > 0; ty--) {
                    if (level.getBlock((int)mx, ty, (int)mz) != 0) {
                        my = ty + 2;
                        break;
                    }
                }
                attempts++;
            } while (level.getBlock((int)mx, (int)(my - 1), (int)mz) == 0 && attempts < 20);
            
            Mob mob = new Mob(level, mx, my, mz);
            mob.setTexture(charTextureID);
            mobs.add(mob);
        }
    }
    
    // ===== NetworkListener =====

    @Override
    public void onLevelReceived(int width, int height, int depth, byte[] blocks) {
        GameLogger.log("=== LEVEL RECEIVED ===");
        
        // СОЗДАЁМ УРОВЕНЬ БЕЗ ГЕНЕРАЦИИ (false = не генерировать)
        Level newLevel = new Level(width, height, depth, false);
        
        // Копируем блоки
        System.arraycopy(blocks, 0, newLevel.getBlocks(), 0, 
            Math.min(blocks.length, newLevel.getBlocks().length));
        
        // НЕ ВЫЗЫВАЕМ generateMap() !
        
        this.pendingLevel = newLevel;
        this.needsRenderUpdate = true;
        
        isMultiplayer = true;
        isOp = false;
        GameLogger.log("=== LEVEL PENDING ===");
    }

    @Override
    public void onBlockChanged(int x, int y, int z, int blockType) {
        if (level != null) level.setBlock(x, y, z, blockType);
    }

    @Override
    public void onPlayerSpawned(byte playerId, String name, float x, float y, float z, byte yaw, byte pitch) {
        remotePlayers.put(playerId, new PlayerEntity(playerId, name, x, y, z,
                (yaw & 0xFF) * 360.0f / 256.0f, (pitch & 0xFF) * 360.0f / 256.0f));
    }
    
    @Override
    public void onOpStatusReceived(boolean isOp) {
        this.isOp = isOp;
        System.out.println("[Client] OP status: " + isOp);
    }
    
    @Override
    public void onFlyToggle() {
        if (player != null) {
            player.flyMode = !player.flyMode;
            System.out.println("[Client] Fly: " + player.flyMode);
        }
    }

    @Override
    public void onNoClipToggle() {
        if (player != null) {
            player.noClipMode = !player.noClipMode;
            System.out.println("[Client] NoClip: " + player.noClipMode);
        }
    }
    
    @Override
    public void onServerIdentified(String name, String motd) {
        this.serverName = name;
        this.serverMotd = motd;
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
        if (chat != null) {
            chat.addMessage(message);
        }
    }

    @Override
    public void onDisconnected(String reason) {
        System.out.println("=== DISCONNECTED: " + reason + " ===");
        System.exit(0);
    }

    // ===== Render =====

    private void renderDebugMenu() {
        if (!showDebugMenu) return;
        
        int dw = Display.getWidth();
        int dh = Display.getHeight();
        
        int menuW = 300;
        int menuH = 200;
        int mx = 5;
        int my = 30;
        
        // Затемнённый фон
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        GL11.glColor4f(0.0f, 0.0f, 0.0f, 0.7f);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(mx, my);
        GL11.glVertex2f(mx + menuW, my);
        GL11.glVertex2f(mx + menuW, my + menuH);
        GL11.glVertex2f(mx, my + menuH);
        GL11.glEnd();
        
        // Рамка
        GL11.glColor4f(0.5f, 0.5f, 0.5f, 1.0f);
        GL11.glLineWidth(1.5f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(mx, my);
        GL11.glVertex2f(mx + menuW, my);
        GL11.glVertex2f(mx + menuW, my + menuH);
        GL11.glVertex2f(mx, my + menuH);
        GL11.glEnd();
        
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        
        if (minecraftFont == null) return;
        
        int x = mx + 8;
        int y = my + 6;
        int lineH = 16;
        
        minecraftFont.drawShadow("=== DEBUG ===", x, y, 0xFFFF00); y += lineH;
        minecraftFont.drawShadow("[F1] Fly: " + (player.flyMode ? "ON" : "OFF"), x, y, 0x00FF00); y += lineH;
        minecraftFont.drawShadow("[F2] NoClip: " + (player.noClipMode ? "ON" : "OFF"), x, y, 0x00FF00); y += lineH;
        minecraftFont.drawShadow("[F3] Fullscreen: " + (fullscreen ? "ON" : "OFF"), x, y, 0x00FF00); y += lineH;
        minecraftFont.drawShadow("[F5] Save", x, y, 0x00FFFF); y += lineH;
        minecraftFont.drawShadow("[F9] Load", x, y, 0x00FFFF); y += lineH;
        minecraftFont.drawShadow("[F10] Connect", x, y, 0xFFFF00); y += lineH;
        minecraftFont.drawShadow("[F11] Disconnect", x, y, 0xFF5555); y += lineH;
        minecraftFont.drawShadow("[F] Spawn Mob", x, y, 0x00FF00); y += lineH;
        minecraftFont.drawShadow("[R] Reset Pos", x, y, 0x00FF00);
    }
    
    private void updateFps() {
        currentFps++;
        if (System.currentTimeMillis() - lastFpsTime >= 1000) {
            fpsDisplayString = currentFps + " fps, 0 chunk updates";
            currentFps = 0;
            lastFpsTime += 1000;
        }
    }

    public void render(float a) {
        GL11.glClearColor(0.5f, 0.8f, 1.0f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        
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

        for (Mob mob : mobs) {
            mob.render(a);
        }

        if (needsRenderUpdate && pendingLevel != null) {
            this.level = pendingLevel;
            this.renderer = new Renderer(pendingLevel, player);
            this.player.setLevel(pendingLevel);
            this.player.setPosition(level.width / 2f, level.height / 2f + 2, level.depth / 2f);
            if (particleEngine != null) {
                particleEngine = new ParticleEngine(pendingLevel);
            }
            pendingLevel = null;
            needsRenderUpdate = false;
            GameLogger.log("=== LEVEL APPLIED ===");
        }

        renderer.render();

        if (isMultiplayer && !remotePlayers.isEmpty()) {
            for (PlayerEntity entity : remotePlayers.values()) {
                entity.setTexture(charTextureID); // Можно установить один раз при создании
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
        GL11.glScalef(2.0f, 2.0f, 1.0f);

        if (minecraftFont != null) {
            minecraftFont.drawShadow("0.0.11a (rc-0.2c)", 4, 4, 0xFFFFFF);
            minecraftFont.drawShadow(fpsDisplayString, 4, 14, 0xFFFFFF);
            if (isMultiplayer && network.isConnected()) {
                // Название сервера (обрезанное до 16 символов)
                String serverDisplayName = serverName;
                if (serverDisplayName != null && serverDisplayName.length() > 32) {
                    serverDisplayName = serverDisplayName.substring(0, 32);
                }
                if (serverDisplayName != null && !serverDisplayName.isEmpty()) {
                    minecraftFont.drawShadow(serverDisplayName, 4, 24, 0xFFAA00);
                }
                // IP:Port
                minecraftFont.drawShadow("Server: " + serverIp + ":" + serverPort, 4, 34, 0x55FF55);
            }

            renderDebugMenu();

            if (disconnectReason != null) {
                long elapsed = System.currentTimeMillis() - disconnectMessageTime;
                if (elapsed < 5000) {
                    float alpha = 1.0f;
                    if (elapsed > 4000) alpha = 1.0f - (elapsed - 4000) / 1000.0f;
                    int tx = (Display.getWidth() / 2 - disconnectReason.length() * 8) / 2;
                    int ty = (Display.getHeight() / 2 + 40) / 2;
                    GL11.glDisable(GL11.GL_TEXTURE_2D);
                    GL11.glEnable(GL11.GL_BLEND);
                    GL11.glColor4f(0.0f, 0.0f, 0.0f, 0.7f * alpha);
                    GL11.glBegin(GL11.GL_QUADS);
                    GL11.glVertex2f(tx - 10, ty - 2);
                    GL11.glVertex2f(tx + disconnectReason.length() * 16 + 10, ty - 2);
                    GL11.glVertex2f(tx + disconnectReason.length() * 16 + 10, ty + 20);
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

        if (tabMenu != null) {
            tabMenu.render();
        }
        // Чат
        if (chat != null) {
            chat.render(Display.getWidth(), Display.getHeight());
        }

        if (settingsMenu != null && settingsMenu.isVisible()) {
            settingsMenu.render();
        }

        if (blockInventory != null && blockInventory.isVisible()) {
            blockInventory.render(Display.getWidth(), Display.getHeight(), renderer.getTextureID(), blockRotation);
        }

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
        BlockGuiRenderer.renderBlockInGui(blockInventory.getSelectedBlock(), blockRotation, renderer.getTextureID());
        GL11.glPopMatrix();
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        if (serverGui != null) serverGui.render();
    }

    public void onLightingChanged() {
        if (renderer != null) {
            renderer.allChunksChanged();
        }
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
        if (network != null) network.disconnect();
        GameLogger.close();
        DiscordRPC.stop();
        Keyboard.destroy();
        Mouse.destroy();
        Display.destroy();
    }

    public static void main(String[] args) {
        new ReplaceCraft().start();
    }
}