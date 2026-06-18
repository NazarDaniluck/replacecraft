package net.replacecraft;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.imageio.ImageIO;
import org.lwjgl.opengl.GL11;

public class Font {
    private int fontTexture;
    private int[] charWidths = new int[256];

    public Font(String filename) {
        try {
            File file = new File(filename);
            if (!file.exists()) {
                System.out.println("КРИТИЧЕСКАЯ ОШИБКА: Шрифт " + filename + " не найден в корне!");
                return;
            }

            BufferedImage image = ImageIO.read(new FileInputStream(file));
            int w = image.getWidth();
            int h = image.getHeight();
            int[] rawPixels = new int[w * h];
            image.getRGB(0, 0, w, h, rawPixels, 0, w);

            // Автоматический расчет ширины букв по пикселям (алгоритм Нотча)
            for (int i = 0; i < 128; ++i) {
                int xt = i % 16;
                int yt = i / 16;
                int x = 0;
                for (boolean emptyColumn = false; x < 8 && !emptyColumn; ++x) {
                    int xPixel = xt * 8 + x;
                    emptyColumn = true;
                    for (int y = 0; y < 8 && emptyColumn; ++y) {
                        int yPixel = (yt * 8 + y) * w;
                        int pixel = rawPixels[xPixel + yPixel] & 255;
                        if (pixel > 128) {
                            emptyColumn = false;
                        }
                    }
                }
                if (i == 32) x = 4; // Пробел
                this.charWidths[i] = x;
            }

            // Загрузка текстуры шрифта в OpenGL
            ByteBuffer buffer = ByteBuffer.allocateDirect(w * h * 4).order(ByteOrder.nativeOrder());
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int pixel = rawPixels[y * w + x];
                    buffer.put((byte) ((pixel >> 16) & 0xFF));
                    buffer.put((byte) ((pixel >> 8) & 0xFF));
                    buffer.put((byte) (pixel & 0xFF));
                    buffer.put((byte) ((pixel >> 24) & 0xFF));
                }
            }
            buffer.flip();

            this.fontTexture = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.fontTexture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, w, h, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void drawShadow(String str, int x, int y, int color) {
        // Тень (затемненный цвет, смещенный на 1 пиксель)
        int r = (color >> 16 & 255) / 4;
        int g = (color >> 8 & 255) / 4;
        int b = (color & 255) / 4;
        int shadowColor = (r << 16) | (g << 8) | b;

        this.draw(str, x + 1, y + 1, shadowColor);
        this.draw(str, x, y, color);
    }

    public void draw(String str, int x, int y, int color) {
        char[] chars = str.toCharArray();
        
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.fontTexture);

        // Раскладываем цвет на составляющие для glColor
        float r = (float) (color >> 16 & 255) / 255.0f;
        float g = (float) (color >> 8 & 255) / 255.0f;
        float b = (float) (color & 255) / 255.0f;
        GL11.glColor3f(r, g, b);

        GL11.glBegin(GL11.GL_QUADS);
        int xo = 0;
        for (int i = 0; i < chars.length; ++i) {
            if (chars[i] >= 256) continue;
            
            int ix = chars[i] % 16 * 8;
            int iy = chars[i] / 16 * 8;

            float u0 = (float) ix / 128.0f;
            float v0 = (float) iy / 128.0f;
            float u1 = (float) (ix + 8) / 128.0f;
            float v1 = (float) (iy + 8) / 128.0f;

            float x0 = x + xo;
            float x1 = x + xo + 8;
            float y0 = y;
            float y1 = y + 8;

            GL11.glTexCoord2f(u0, v0); GL11.glVertex2f(x0, y0);
            GL11.glTexCoord2f(u0, v1); GL11.glVertex2f(x0, y1);
            GL11.glTexCoord2f(u1, v1); GL11.glVertex2f(x1, y1);
            GL11.glTexCoord2f(u1, v0); GL11.glVertex2f(x1, y0);

            xo += this.charWidths[chars[i]];
        }
        GL11.glEnd();
        
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }
}
