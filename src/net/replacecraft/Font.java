package net.replacecraft;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.imageio.ImageIO;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

public class Font {
    private int fontTexture;
    private int[] charWidths = new int[256];
    private int cellW, cellH, texW, texH;
    private final int[] colorTable = new int[32];

    public Font(String filename) {
        for (int i = 0; i < 32; ++i) {
            int j = (i >> 3 & 1) * 85;
            int k = (i >> 2 & 1) * 170 + j;
            int l = (i >> 1 & 1) * 170 + j;
            int i1 = (i & 1) * 170 + j;
            if (i == 6) k += 85;
            if (i >= 16) { k /= 4; l /= 4; i1 /= 4; }
            this.colorTable[i] = (k & 255) << 16 | (l & 255) << 8 | i1 & 255;
        }

        try {
            InputStream is = Font.class.getResourceAsStream("/textures/" + filename);
            if (is == null) {
                File f = new File(filename);
                if (f.exists()) is = new FileInputStream(f);
                else {
                    f = new File("res/textures/" + filename);
                    if (f.exists()) is = new FileInputStream(f);
                }
            }
            if (is == null) { System.out.println("Font not found: " + filename); return; }

            BufferedImage img = ImageIO.read(is);
            is.close();
            this.texW = img.getWidth();
            this.texH = img.getHeight();
            this.cellW = texW / 16;
            this.cellH = texH / 16;

            int[] pixels = new int[texW * texH];
            img.getRGB(0, 0, texW, texH, pixels, 0, texW);

            for (int i = 0; i < 256; i++) {
                int col = i % 16;
                int row = i / 16;
                int w = 0;
                boolean empty;
                do {
                    int px = col * cellW + w;
                    empty = true;
                    for (int y = 0; y < cellH && empty; y++) {
                        int py = (row * cellH + y) * texW;
                        int p = pixels[px + py];
                        int r = (p >> 16) & 0xFF;
                        int g = (p >> 8) & 0xFF;
                        int b = p & 0xFF;
                        if (r > 40 || g > 40 || b > 40) empty = false;
                    }
                    if (!empty) w++;
                } while (!empty && w < cellW);
                if (i == 32) charWidths[i] = cellW / 2;
                else if (w == 0) charWidths[i] = cellW / 2;
                else charWidths[i] = w + 1;
            }

            ByteBuffer buf = ByteBuffer.allocateDirect(texW * texH * 4).order(ByteOrder.nativeOrder());
            for (int y = 0; y < texH; y++) {
                for (int x = 0; x < texW; x++) {
                    int p = pixels[y * texW + x];
                    int r = (p >> 16) & 0xFF;
                    int g = (p >> 8) & 0xFF;
                    int b = p & 0xFF;
                    buf.put((byte) 255);
                    buf.put((byte) 255);
                    buf.put((byte) 255);
                    if (r < 30 && g < 30 && b < 30) buf.put((byte) 0);
                    else buf.put((byte) 255);
                }
            }
            buf.flip();

            fontTexture = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, fontTexture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, texW, texH, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf);
        } catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * Получить символ с клавиатуры (читает именно то, что напечатано)
     */
    public static char getKeyChar() {
        Keyboard.poll();
        while (Keyboard.next()) {
            if (Keyboard.getEventKeyState()) {
                return Keyboard.getEventCharacter();
            }
        }
        return '\0';
    }

    /**
     * Проверить, нажата ли конкретная клавиша
     */
    public static boolean isKeyDown(int keyCode) {
        return Keyboard.isKeyDown(keyCode);
    }

    private static int toCP437(char c) {
        if (c < 128) return c;
        if (c >= 'А' && c <= 'П') return 128 + (c - 'А');
        if (c >= 'Р' && c <= 'Я') return 144 + (c - 'Р');
        if (c >= 'а' && c <= 'п') return 160 + (c - 'а');
        if (c >= 'р' && c <= 'я') return 224 + (c - 'р');
        if (c == 'Ё') return 240;
        if (c == 'ё') return 241;
        return 63;
    }

    /**
     * Вычисляет ширину строки в пикселях с учетом индивидуальной ширины каждого символа.
     */
    public int getStringWidth(String s) {
        if (s == null || s.isEmpty()) return 0;
        int width = 0;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            // Пропускаем цветовые коды параграфа (§)
            if (ch == '\u00a7' && i + 1 < s.length()) {
                i++;
                continue;
            }
            int idx = toCP437(ch);
            width += (charWidths[idx] * 8) / cellW;
        }
        return width;
    }

    
    public void drawShadow(String s, int x, int y, int color) {
        if (s == null || s.isEmpty()) return;
        render(s, x + 1, y + 1, color, true);
        render(s, x, y, color, false);
    }

    public void draw(String s, int x, int y, int color) {
        if (s == null || s.isEmpty()) return;
        render(s, x, y, color, false);
    }

    private void render(String s, int x, int y, int color, boolean shadow) {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fontTexture);

        if ((color & 0xFF000000) == 0) color |= 0xFF000000;
        if (shadow) color = (color & 0xFCFCFC) >> 2 | (color & 0xFF000000);

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        GL11.glColor3f(r, g, b);

        GL11.glBegin(GL11.GL_QUADS);
        int xo = 0;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\u00a7' && i + 1 < s.length()) {
                int ci = "0123456789abcdef".indexOf(Character.toLowerCase(s.charAt(i + 1)));
                if (ci >= 0) {
                    int col = colorTable[shadow ? ci + 16 : ci];
                    GL11.glColor3f(((col >> 16) & 0xFF) / 255f, ((col >> 8) & 0xFF) / 255f, (col & 0xFF) / 255f);
                }
                i++;
                continue;
            }

            int idx = toCP437(ch);
            int col = idx % 16;
            int row = idx / 16;
            int px = col * cellW;
            int py = row * cellH;

            float u0 = (float) px / texW;
            float v0 = (float) py / texH;
            float u1 = (float) (px + cellW) / texW;
            float v1 = (float) (py + cellH) / texH;

            float x0 = x + xo;
            float x1 = x + xo + 8;
            float y0 = y;
            float y1 = y + 8;

            GL11.glTexCoord2f(u0, v0); GL11.glVertex2f(x0, y0);
            GL11.glTexCoord2f(u0, v1); GL11.glVertex2f(x0, y1);
            GL11.glTexCoord2f(u1, v1); GL11.glVertex2f(x1, y1);
            GL11.glTexCoord2f(u1, v0); GL11.glVertex2f(x1, y0);

            xo += (charWidths[idx] * 8) / cellW;
        }
        GL11.glEnd();
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }
}