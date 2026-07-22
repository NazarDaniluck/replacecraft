package net.replacecraft;

import java.io.*;
import java.util.Properties;
import net.replacecraft.level.Lighting;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

public class SettingsMenu {
    private boolean visible = false;
    private int selectedOption = 0;
    private Font font;
    
    // Текущие настройки
    private int fpsLimit = 0;
    private boolean discordRPC = true;
    private boolean smoothLighting = true;
    
    // Временные настройки
    private int tempFpsLimit;
    private boolean tempDiscordRPC;
    private boolean tempSmoothLighting;
    
    private int[] fpsOptions = {30, 60, 120, 144, 240, 0};
    private int fpsIndex = 2;
    private int tempFpsIndex;
    
    private long lastKeyTime = 0;
    private ReplaceCraft parent;
    
    private static final float TEXT_SCALE = 3.0f;
    private static final int OPTIONS_COUNT = 5;
    private static final File SETTINGS_FILE = new File("settings.properties");

    public SettingsMenu(Font font, ReplaceCraft parent) {
        this.font = font;
        this.parent = parent;
        loadSettings();
    }

    public boolean isVisible() { return visible; }
    
    public void show() {
        visible = true;
        selectedOption = 0;
        tempFpsLimit = fpsLimit;
        tempFpsIndex = fpsIndex;
        tempDiscordRPC = discordRPC;
        tempSmoothLighting = smoothLighting;
    }
    
    public void hide() { 
        visible = false; 
    }
    
    public void toggle() { 
        if (visible) hide(); 
        else show(); 
    }
    
    public int getFpsLimit() { return fpsLimit; }
    public boolean isDiscordRPC() { return discordRPC; }
    public boolean isSmoothLighting() { return smoothLighting; }
    
    public void setFpsLimit(int fps) {
        this.fpsLimit = fps;
        for (int i = 0; i < fpsOptions.length; i++) {
            if (fpsOptions[i] == fps) {
                fpsIndex = i;
                break;
            }
        }
    }
    
    public void setDiscordRPC(boolean on) { 
        this.discordRPC = on; 
    }
    
    public void setSmoothLighting(boolean on) {
        this.smoothLighting = on;
    }

    public void handleInput() {
        if (!visible) return;
        
        while (Keyboard.next()) {
            if (!Keyboard.getEventKeyState()) continue;
            
            long now = System.currentTimeMillis();
            if (now - lastKeyTime < 150) continue;
            lastKeyTime = now;
            
            int key = Keyboard.getEventKey();
            
            if (key == Keyboard.KEY_ESCAPE) {
                hide();
                return;
            }
            
            if (key == Keyboard.KEY_UP || key == Keyboard.KEY_W) {
                selectedOption = (selectedOption - 1 + OPTIONS_COUNT) % OPTIONS_COUNT;
            }
            
            if (key == Keyboard.KEY_DOWN || key == Keyboard.KEY_S) {
                selectedOption = (selectedOption + 1) % OPTIONS_COUNT;
            }
            
            if (key == Keyboard.KEY_LEFT || key == Keyboard.KEY_A) {
                changeTempOption(-1);
            }
            
            if (key == Keyboard.KEY_RIGHT || key == Keyboard.KEY_D) {
                changeTempOption(1);
            }
            
            if (key == Keyboard.KEY_RETURN) {
                applySelectedOption();
            }
        }
    }

    private void changeTempOption(int delta) {
        switch (selectedOption) {
            case 0:
                tempFpsIndex = (tempFpsIndex + delta + fpsOptions.length) % fpsOptions.length;
                tempFpsLimit = fpsOptions[tempFpsIndex];
                break;
                
            case 1:
                tempSmoothLighting = !tempSmoothLighting;
                break;
                
            case 2:
                tempDiscordRPC = !tempDiscordRPC;
                break;
        }
    }

    private void applySelectedOption() {
        switch (selectedOption) {
            case 3:
                applySettings();
                hide();
                break;
                
            case 4:
                hide();
                break;
        }
    }
    
    private void applySettings() {
        boolean needsChunkRebuild = false;
        
        if (fpsLimit != tempFpsLimit) {
            fpsLimit = tempFpsLimit;
            fpsIndex = tempFpsIndex;
        }
        
        if (smoothLighting != tempSmoothLighting) {
            smoothLighting = tempSmoothLighting;
            Lighting.enabled = smoothLighting;
            needsChunkRebuild = true;
        }
        
        if (discordRPC != tempDiscordRPC) {
            discordRPC = tempDiscordRPC;
            if (discordRPC) {
                DiscordRPC.start();
            } else {
                DiscordRPC.stop();
            }
        }
        
        saveSettings();
        
        if (needsChunkRebuild && parent != null) {
            parent.onLightingChanged();
        }
        
        System.out.println("Settings applied: FPS=" + (fpsLimit == 0 ? "Unlimited" : fpsLimit) + 
                          ", Lighting=" + smoothLighting + ", DiscordRPC=" + discordRPC);
    }
    
    private void loadSettings() {
        if (!SETTINGS_FILE.exists()) {
            createDefaultSettings();
            return;
        }
        
        Properties props = new Properties();
        FileInputStream fis = null;
        
        try {
            fis = new FileInputStream(SETTINGS_FILE);
            props.load(fis);
            
            String fpsStr = props.getProperty("fpsLimit", "120");
            try {
                fpsLimit = Integer.parseInt(fpsStr);
                for (int i = 0; i < fpsOptions.length; i++) {
                    if (fpsOptions[i] == fpsLimit) {
                        fpsIndex = i;
                        break;
                    }
                }
            } catch (NumberFormatException e) {
                fpsLimit = 120;
                fpsIndex = 2;
            }
            
            smoothLighting = Boolean.parseBoolean(props.getProperty("smoothLighting", "true"));
            discordRPC = Boolean.parseBoolean(props.getProperty("discordRPC", "true"));
            
            // Применяем настройку освещения после загрузки значений
            Lighting.enabled = smoothLighting;
            
            System.out.println("Settings loaded from: " + SETTINGS_FILE.getAbsolutePath());
            System.out.println("  FPS Limit: " + (fpsLimit == 0 ? "Unlimited" : fpsLimit));
            System.out.println("  Smooth Lighting: " + smoothLighting);
            System.out.println("  Discord RPC: " + discordRPC);
            
        } catch (IOException e) {
            System.err.println("Failed to load settings: " + e.getMessage());
            createDefaultSettings();
        } finally {
            if (fis != null) {
                try { fis.close(); } catch (IOException e) {}
            }
        }
    }
    
    private void createDefaultSettings() {
        fpsLimit = 0;
        fpsIndex = 2;
        smoothLighting = true;
        discordRPC = true;
        
        // Применяем настройку освещения
        Lighting.enabled = smoothLighting;
        
        Properties props = new Properties();
        props.setProperty("fpsLimit", "120");
        props.setProperty("smoothLighting", "true");
        props.setProperty("discordRPC", "true");
        
        FileOutputStream fos = null;
        
        try {
            if (SETTINGS_FILE.getParentFile() != null) {
                SETTINGS_FILE.getParentFile().mkdirs();
            }
            SETTINGS_FILE.createNewFile();
            
            fos = new FileOutputStream(SETTINGS_FILE);
            props.store(fos, "ReplaceCraft Default Settings");
            
            System.out.println("Default settings created: " + SETTINGS_FILE.getAbsolutePath());
            System.out.println("  FPS Limit: 120");
            System.out.println("  Smooth Lighting: true");
            System.out.println("  Discord RPC: true");
            
        } catch (IOException e) {
            System.err.println("Failed to create default settings: " + e.getMessage());
        } finally {
            if (fos != null) {
                try { fos.close(); } catch (IOException e) {}
            }
        }
    }
    
    private void saveSettings() {
        Properties props = new Properties();
        props.setProperty("fpsLimit", String.valueOf(fpsLimit));
        props.setProperty("smoothLighting", String.valueOf(smoothLighting));
        props.setProperty("discordRPC", String.valueOf(discordRPC));
        
        FileOutputStream fos = null;
        
        try {
            fos = new FileOutputStream(SETTINGS_FILE);
            props.store(fos, "ReplaceCraft Settings");
            System.out.println("Settings saved to: " + SETTINGS_FILE.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to save settings: " + e.getMessage());
        } finally {
            if (fos != null) {
                try { fos.close(); } catch (IOException e) {}
            }
        }
    }

    public void render() {
        if (!visible) return;
        
        int dw = Display.getWidth();
        int dh = Display.getHeight();

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glColor4f(0.0f, 0.0f, 0.0f, 0.7f);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(0, 0);
        GL11.glVertex2f(dw, 0);
        GL11.glVertex2f(dw, dh);
        GL11.glVertex2f(0, dh);
        GL11.glEnd();

        int menuW = 500;
        int menuH = 300;
        int mx = (dw - menuW) / 2;
        int my = (dh - menuH) / 2;

        GL11.glColor4f(0.1f, 0.1f, 0.1f, 0.95f);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(mx, my);
        GL11.glVertex2f(mx + menuW, my);
        GL11.glVertex2f(mx + menuW, my + menuH);
        GL11.glVertex2f(mx, my + menuH);
        GL11.glEnd();

        GL11.glColor4f(0.4f, 0.4f, 0.4f, 1.0f);
        GL11.glLineWidth(3.0f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(mx, my);
        GL11.glVertex2f(mx + menuW, my);
        GL11.glVertex2f(mx + menuW, my + menuH);
        GL11.glVertex2f(mx, my + menuH);
        GL11.glEnd();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);

        if (font != null) {
            GL11.glPushMatrix();
            GL11.glScalef(TEXT_SCALE, TEXT_SCALE, 1.0f);
            
            int sx = (int) (mx / TEXT_SCALE);
            int sy = (int) (my / TEXT_SCALE);
            int sw = (int) (menuW / TEXT_SCALE);
            int lineHeight = 14;
            int centerX = sx + sw / 2;
            
            drawCentered("=== SETTINGS ===", centerX, sy + 8, 0xFFD700);
            
            int yOff = sy + 34;
            
            String fpsText = "FPS Limit: " + (tempFpsLimit == 0 ? "Unlimited" : String.valueOf(tempFpsLimit));
            drawCentered(fpsText, centerX, yOff, selectedOption == 0 ? 0x55FF55 : 0xCCCCCC);
            yOff += lineHeight;
            
            String lightText = "Smooth Lighting: " + (tempSmoothLighting ? "ON" : "OFF");
            drawCentered(lightText, centerX, yOff, selectedOption == 1 ? 0x55FF55 : 0xCCCCCC);
            yOff += lineHeight;
            
            String rpcText = "Discord RPC: " + (tempDiscordRPC ? "ON" : "OFF");
            drawCentered(rpcText, centerX, yOff, selectedOption == 2 ? 0x55FF55 : 0xCCCCCC);
            yOff += lineHeight;
            
            drawCentered("[ Apply & Close ]", centerX, yOff, selectedOption == 3 ? 0x55FF55 : 0xAAAAAA);
            yOff += lineHeight;
            
            drawCentered("[ Close (Discard) ]", centerX, yOff, selectedOption == 4 ? 0xFF5555 : 0xAAAAAA);
            yOff += lineHeight;
            
            yOff += 4;
            boolean hasChanges = (fpsLimit != tempFpsLimit) || (smoothLighting != tempSmoothLighting) || (discordRPC != tempDiscordRPC);
            drawCentered(hasChanges ? "* Changes not saved" : "* Settings saved", 
                        centerX, yOff, hasChanges ? 0xFFAA00 : 0x55FF55);
            
            GL11.glPopMatrix();
            
            GL11.glPushMatrix();
            float hintScale = 1.5f;
            GL11.glScalef(hintScale, hintScale, 1.0f);
            
            String hint = "W/S - select | A/D - change | Enter - confirm";
            int hintW = getTextWidth(hint);
            int hintX = (int) ((dw - hintW * hintScale) / 2 / hintScale);
            int hintY = (int) ((my + menuH + 20) / hintScale);
            font.drawShadow(hint, hintX, hintY, 0x666666);
            
            GL11.glPopMatrix();
        }
    }

    private void drawCentered(String text, int centerX, int y, int color) {
        int textWidth = getTextWidth(text);
        font.drawShadow(text, centerX - textWidth / 2, y, color);
    }

    private int getTextWidth(String text) {
        if (text == null || text.isEmpty()) return 0;
        String clean = text.replaceAll("\u00a7.", "");
        return clean.length() * 8;
    }
}