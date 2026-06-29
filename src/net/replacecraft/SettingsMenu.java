package net.replacecraft;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.LWJGLException;

public class SettingsMenu {
    private boolean visible = false;
    private int selectedOption = 0;
    private Font font;
    private boolean fullscreen;
    private int fpsLimit = 0;
    private int[] fpsOptions = {30, 60, 120, 144, 240, 0};
    private int fpsIndex = 2;
    private long lastKeyTime = 0;

    public SettingsMenu(Font font) {
        this.font = font;
    }

    public boolean isVisible() { return visible; }
    public void show() { visible = true; selectedOption = 0; }
    public void hide() { visible = false; }
    public void toggle() { if (visible) hide(); else show(); }
    public boolean isFullscreen() { return fullscreen; }
    public int getFpsLimit() { return fpsLimit; }

    public void handleInput() {
        if (!visible) return;
        while (Keyboard.next()) {
            if (!Keyboard.getEventKeyState()) continue;
            long now = System.currentTimeMillis();
            if (now - lastKeyTime < 150) continue;
            lastKeyTime = now;
            int key = Keyboard.getEventKey();
            if (key == Keyboard.KEY_ESCAPE) { hide(); return; }
            if (key == Keyboard.KEY_UP || key == Keyboard.KEY_W) selectedOption = (selectedOption - 1 + 3) % 3;
            if (key == Keyboard.KEY_DOWN || key == Keyboard.KEY_S) selectedOption = (selectedOption + 1) % 3;
            if (key == Keyboard.KEY_LEFT || key == Keyboard.KEY_A) changeOption(-1);
            if (key == Keyboard.KEY_RIGHT || key == Keyboard.KEY_D) changeOption(1);
            if (key == Keyboard.KEY_RETURN) applyFullscreen();
        }
    }

    private void changeOption(int delta) {
        switch (selectedOption) {
            case 0: fullscreen = !fullscreen; break;
            case 1: fpsIndex = (fpsIndex + delta + fpsOptions.length) % fpsOptions.length;
                    fpsLimit = fpsOptions[fpsIndex]; break;
            case 2: break;
        }
    }

    private void applyFullscreen() {
        try {
            if (fullscreen) {
                Display.setDisplayModeAndFullscreen(Display.getDesktopDisplayMode());
            } else {
                Display.setFullscreen(false);
                Display.setDisplayMode(new DisplayMode(854, 480));
            }
        } catch (LWJGLException e) {
            System.err.println("Fullscreen error: " + e.getMessage());
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
        GL11.glVertex2f(0, 0); GL11.glVertex2f(dw, 0);
        GL11.glVertex2f(dw, dh); GL11.glVertex2f(0, dh);
        GL11.glEnd();

        int menuW = 400, menuH = 250;
        int mx = (dw - menuW) / 2, my = (dh - menuH) / 2;

        GL11.glColor4f(0.15f, 0.15f, 0.15f, 0.95f);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(mx, my); GL11.glVertex2f(mx + menuW, my);
        GL11.glVertex2f(mx + menuW, my + menuH); GL11.glVertex2f(mx, my + menuH);
        GL11.glEnd();

        GL11.glColor4f(0.5f, 0.5f, 0.5f, 1.0f);
        GL11.glLineWidth(2.0f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(mx, my); GL11.glVertex2f(mx + menuW, my);
        GL11.glVertex2f(mx + menuW, my + menuH); GL11.glVertex2f(mx, my + menuH);
        GL11.glEnd();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);

        if (font != null) {
            font.drawShadow("Settings", mx + 70, my + 12, 0xFFFF00);
            int yOff = my + 50;
            font.drawShadow("Fullscreen: " + (fullscreen ? "ON" : "OFF"), mx + 20, yOff, selectedOption == 0 ? 0x55FF55 : 0xCCCCCC);
            yOff += 24;
            font.drawShadow("FPS Limit: " + (fpsLimit == 0 ? "Unlimited" : fpsLimit), mx + 20, yOff, selectedOption == 1 ? 0x55FF55 : 0xCCCCCC);
            yOff += 24;
            font.drawShadow("[ Apply & Close ]", mx + 50, yOff + 10, selectedOption == 2 ? 0x55FF55 : 0xAAAAAA);
            font.drawShadow("Arrows/WASD - navigate | Left/Right - change | Enter - apply",
                    mx + 10, yOff + 35, 0x888888);
        }
    }
}