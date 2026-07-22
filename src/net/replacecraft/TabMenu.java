package net.replacecraft;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import java.util.*;

public class TabMenu {
    private boolean visible = false;
    private Font font;
    private String serverName = "";
    private String motd = "";
    private String localPlayerName = "";
    private int maxPlayers = 16;
    private List<TabPlayer> players = new ArrayList<>();
    
    private static final int MAX_PER_COLUMN = 8;
    private static final int COLUMN_WIDTH = 160;
    private static final int LINE_HEIGHT = 14;
    private static final int PADDING = 12;
    
    public TabMenu(Font font) {
        this.font = font;
        loadLocalName();
    }
    
    private void loadLocalName() {
        try {
            java.io.File file = new java.io.File("username.txt");
            if (file.exists()) {
                localPlayerName = new String(java.nio.file.Files.readAllBytes(file.toPath())).trim();
            }
        } catch (Exception e) {}
        if (localPlayerName.isEmpty()) localPlayerName = "Player";
    }
    
    public boolean isVisible() { return visible; }
    
    public void update() {
        visible = Keyboard.isKeyDown(Keyboard.KEY_TAB);
    }
    
    public void setServerInfo(String name, String motd) {
        this.serverName = name;
        this.motd = motd;
    }
    
    public void setMaxPlayers(int max) {
        this.maxPlayers = max;
    }
    
    public void updatePlayers(Map<Byte, PlayerEntity> remotePlayers, long localPing, boolean isOp) {
        players.clear();
        players.add(new TabPlayer(localPlayerName, localPing, isOp, true));
        for (PlayerEntity p : remotePlayers.values()) {
            players.add(new TabPlayer(p.name, 0, false, false));
        }
    }
    
    public void render() {
        if (!visible || font == null || players.isEmpty()) return;
        
        int dw = Display.getWidth();
        int dh = Display.getHeight();
        
        int guiScale = 1;
        if (dw >= 1920) guiScale = 4;
        else if (dw >= 1280) guiScale = 3;
        else if (dw >= 800) guiScale = 2;
        
        // Сортируем: локальный игрок первый, OP игроки выше
        players.sort((a, b) -> {
            if (a.isLocal) return -1;
            if (b.isLocal) return 1;
            if (a.isOp && !b.isOp) return -1;
            if (!a.isOp && b.isOp) return 1;
            return 0;
        });
        
        int totalPlayers = players.size();
        int maxPerColumn = 5;
        int columns = Math.max(1, (totalPlayers + maxPerColumn - 1) / maxPerColumn);
        int rows = Math.min(totalPlayers, maxPerColumn);
        
        int columnWidth = 160;
        int padding = 20;
        
        int contentW = Math.max(280, columns * columnWidth + padding * 2);
        
        int headerH = 0;
        if (!serverName.isEmpty()) headerH += 12;
        if (!motd.isEmpty()) headerH += 12;
        headerH += 14;
        
        int cellH = LINE_HEIGHT + 4;
        int playersH = rows * cellH + 4;
        int footerH = LINE_HEIGHT + 6;
        
        int contentH = padding * 2 + headerH + playersH + footerH;
        
        int boxW = contentW * guiScale;
        int boxH = contentH * guiScale;
        int boxX = (dw - boxW) / 2;
        int boxY = (dh - boxH) / 2;
        
        // --- Фон + тень ---
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        GL11.glColor4f(0.0f, 0.0f, 0.0f, 0.15f);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(boxX + 3, boxY + 3);
        GL11.glVertex2f(boxX + boxW + 3, boxY + 3);
        GL11.glVertex2f(boxX + boxW + 3, boxY + boxH + 3);
        GL11.glVertex2f(boxX + 3, boxY + boxH + 3);
        GL11.glEnd();
        
        GL11.glColor4f(0.05f, 0.05f, 0.05f, 0.85f);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(boxX, boxY);
        GL11.glVertex2f(boxX + boxW, boxY);
        GL11.glVertex2f(boxX + boxW, boxY + boxH);
        GL11.glVertex2f(boxX, boxY + boxH);
        GL11.glEnd();
        
        GL11.glColor4f(0.25f, 0.25f, 0.25f, 1.0f);
        GL11.glLineWidth(1.5f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(boxX, boxY);
        GL11.glVertex2f(boxX + boxW, boxY);
        GL11.glVertex2f(boxX + boxW, boxY + boxH);
        GL11.glVertex2f(boxX, boxY + boxH);
        GL11.glEnd();
        
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        
        // --- Текст ---
        GL11.glPushMatrix();
        GL11.glScalef(guiScale, guiScale, 1.0f);
        
        int sx = boxX / guiScale;
        int sy = boxY / guiScale;
        int sw = contentW;
        int centerX = sx + sw / 2;
        
        int cy = sy + padding;
        
        if (!serverName.isEmpty()) {
            drawCentered(serverName, centerX, cy, 0xFFD700);
            cy += 12;
        }
        
        if (!motd.isEmpty()) {
            String m = motd.length() > 45 ? motd.substring(0, 45) : motd;
            drawCentered(m, centerX, cy, 0xAAAAAA);
            cy += 12;
        }
        
        cy += 4;
        
        drawCentered("Connected players:", centerX, cy, 0xCCCCCC);
        cy += 16;
        
        // Сетка игроков
        int gridW = columns * columnWidth;
        int gridX = sx + (sw - gridW) / 2;
        
        for (int col = 0; col < columns; col++) {
            int start = col * maxPerColumn;
            int end = Math.min(start + maxPerColumn, totalPlayers);
            int px = gridX + col * columnWidth;
            
            for (int i = start; i < end; i++) {
                TabPlayer p = players.get(i);
                int row = i - start;
                int py = cy + row * cellH;
                
                int cx1 = px + 2;
                int cx2 = px + columnWidth - 2;
                int cy1 = py;
                int cy2 = py + LINE_HEIGHT;
                
                // Полоски
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glEnable(GL11.GL_BLEND);
                if (p.isOp) {
                    GL11.glColor4f(1.0f, 0.85f, 0.0f, 0.08f); // золотистый фон для OP
                } else {
                    GL11.glColor4f(1.0f, 1.0f, 1.0f, row % 2 == 0 ? 0.06f : 0.02f);
                }
                GL11.glBegin(GL11.GL_QUADS);
                GL11.glVertex2f(cx1, cy1);
                GL11.glVertex2f(cx2, cy1);
                GL11.glVertex2f(cx2, cy2);
                GL11.glVertex2f(cx1, cy2);
                GL11.glEnd();
                GL11.glDisable(GL11.GL_BLEND);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
                
                // [OP] префикс
                int nickX = cx1 + 4;
                if (p.isOp) {
                    font.drawShadow("[OP] ", nickX, py + 2, 0xFF5555);
                    nickX += font.getStringWidth("[OP] ");
                }
                
                // Ник
                String nick = p.name;
                int maxNickW = (cx2 - nickX - 60);
                // Обрезаем если не влезает
                while (font.getStringWidth(nick) > maxNickW && nick.length() > 1) {
                    nick = nick.substring(0, nick.length() - 1);
                }
                font.drawShadow(nick, nickX, py + 2, p.isLocal ? 0xFFFF55 : 0xFFFFFF);
                
                // Пинг
                String pingStr = p.ping + "ms";
                int pingColor = p.ping < 60 ? 0x55FF55 : (p.ping < 120 ? 0xFFFF55 : (p.ping < 200 ? 0xFFAA00 : 0xFF5555));
                int pw = font.getStringWidth(pingStr);
                font.drawShadow(pingStr, cx2 - pw - 4, py + 2, pingColor);
            }
        }
        
        // Разделитель
        int divY = sy + contentH - footerH - 2;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(0.3f, 0.3f, 0.3f, 0.5f);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(sx + 10, divY);
        GL11.glVertex2f(sx + sw - 10, divY);
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        
        // Онлайн
        String onlineLabel = "Online: ";
        String onlineValue = totalPlayers + " / " + maxPlayers;
        int labelW = font.getStringWidth(onlineLabel);
        int valueW = font.getStringWidth(onlineValue);
        int totalW = labelW + valueW;
        int onlineX = centerX - totalW / 2;
        
        font.drawShadow(onlineLabel, onlineX, divY + 2, 0xAAAAAA);
        font.drawShadow(onlineValue, onlineX + labelW, divY + 2, 0xFFFFFF);
        
        GL11.glPopMatrix();
    }

    
    private String formatPing(long ping) {
        if (ping > 99999) return "verybig";
        if (ping < 10) return ping + "ms  ";
        if (ping < 100) return ping + "ms ";
        return ping + "ms";
    }
    
    private int getPingColor(long ping) {
        if (ping < 60) return 0x55FF55;
        if (ping < 120) return 0xFFFF55;
        if (ping < 200) return 0xFFAA00;
        return 0xFF5555;
    }
    
    private void drawCentered(String text, int centerX, int y, int color) {
        if (text == null || text.isEmpty()) return;
        int textWidth = font.getStringWidth(text);
        font.drawShadow(text, centerX - textWidth / 2, y, color);
    }
    
    private static class TabPlayer {
        String name;
        long ping;
        boolean isOp;
        boolean isLocal;
        
        TabPlayer(String name, long ping, boolean isOp, boolean isLocal) {
            this.name = name;
            this.ping = ping;
            this.isOp = isOp;
            this.isLocal = isLocal;
        }
    }
}