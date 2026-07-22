package net.replacecraft.level;

import net.replacecraft.Player;
import org.lwjgl.opengl.GL11;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Chunk {
    private Level level;
    public final int x0, y0, z0;
    public final int x1, y1, z1;
    private int displayListID;
    private boolean isDirty = true;
    private Lighting lighting;

    
    public Chunk(Level level, int x0, int y0, int z0, int x1, int y1, int z1) {
        this.level = level;
        this.lighting = new Lighting(level);
        this.x0 = x0; this.y0 = y0; this.z0 = z0;
        this.x1 = x1; this.y1 = y1; this.z1 = z1;
        this.displayListID = GL11.glGenLists(1);
    }

    public void render() {
        if (isDirty) {
//            System.out.println("Chunk rebuilding: " + x0 + "," + y0 + "," + z0);
            rebuild();
            isDirty = false;
        }
        GL11.glCallList(displayListID);
    }

    public void setDirty() {
        this.isDirty = true;
    }

    private void rebuild() {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);

        GL11.glNewList(displayListID, GL11.GL_COMPILE);
        
        // Если освещение выключено — все блоки рисуем с полной яркостью
        if (!Lighting.enabled) {
            GL11.glColor3f(1.0f, 1.0f, 1.0f);
        }
        
        // Сначала рендерим все обычные блоки
        GL11.glBegin(GL11.GL_QUADS);
        for (int x = x0; x < x1; x++) {
            for (int y = y0; y < y1; y++) {
                for (int z = z0; z < z1; z++) {
                    int type = level.getBlock(x, y, z);
                    if (type > 0 && type != 7) {
                        renderBlockFaces(x, y, z, type);
                    }
                }
            }
        }
        GL11.glEnd();
        
        // Потом рендерим прозрачные блоки (саженцы)
        renderTransparentBlocks();
        
        GL11.glEndList();
    }

    private void renderTransparentBlocks() {
        boolean hasTransparent = false;
        
        // Проверяем, есть ли прозрачные блоки
        for (int x = x0; x < x1 && !hasTransparent; x++) {
            for (int y = y0; y < y1 && !hasTransparent; y++) {
                for (int z = z0; z < z1 && !hasTransparent; z++) {
                    if (level.getBlock(x, y, z) == 7) {
                        hasTransparent = true;
                    }
                }
            }
        }
        
        if (!hasTransparent) return;
        
        // Рендерим прозрачные блоки
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1f);
        GL11.glDepthMask(false);
        
        GL11.glBegin(GL11.GL_QUADS);
        for (int x = x0; x < x1; x++) {
            for (int y = y0; y < y1; y++) {
                for (int z = z0; z < z1; z++) {
                    if (level.getBlock(x, y, z) == 7) {
                        renderSaplingFaces(x, y, z);
                    }
                }
            }
        }
        GL11.glEnd();
        
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_BLEND);
    }
    
    private void renderSaplingFaces(int x, int y, int z) {
        int texID = 15;
        
        // Получаем яркость от освещения
        float brightness = lighting.getVertexBrightness(x, y, z, 0, 0);
        GL11.glColor4f(brightness, brightness, brightness, 0.9f); // Используем color4f с альфой

        float u0 = (texID % 16) / 16.0f;
        float v0 = (texID / 16) / 16.0f;
        float u1 = u0 + (1.0f / 16.0f);
        float v1 = v0 + (1.0f / 16.0f);

        float x0 = x, x1 = x + 1.0f;
        float y0 = y, y1 = y + 1.0f;
        float z0 = z, z1 = z + 1.0f;

        // Первая диагональная плоскость (X-образная)
        GL11.glTexCoord2f(u0, v1); GL11.glVertex3f(x0, y0, z0);
        GL11.glTexCoord2f(u0, v0); GL11.glVertex3f(x0, y1, z0);
        GL11.glTexCoord2f(u1, v0); GL11.glVertex3f(x1, y1, z1);
        GL11.glTexCoord2f(u1, v1); GL11.glVertex3f(x1, y0, z1);

        GL11.glTexCoord2f(u1, v1); GL11.glVertex3f(x1, y0, z1);
        GL11.glTexCoord2f(u1, v0); GL11.glVertex3f(x1, y1, z1);
        GL11.glTexCoord2f(u0, v0); GL11.glVertex3f(x0, y1, z0);
        GL11.glTexCoord2f(u0, v1); GL11.glVertex3f(x0, y0, z0);

        // Вторая диагональная плоскость (Z-образная)
        GL11.glTexCoord2f(u0, v1); GL11.glVertex3f(x0, y0, z1);
        GL11.glTexCoord2f(u0, v0); GL11.glVertex3f(x0, y1, z1);
        GL11.glTexCoord2f(u1, v0); GL11.glVertex3f(x1, y1, z0);
        GL11.glTexCoord2f(u1, v1); GL11.glVertex3f(x1, y0, z0);

        GL11.glTexCoord2f(u1, v1); GL11.glVertex3f(x1, y0, z0);
        GL11.glTexCoord2f(u1, v0); GL11.glVertex3f(x1, y1, z0);
        GL11.glTexCoord2f(u0, v0); GL11.glVertex3f(x0, y1, z1);
        GL11.glTexCoord2f(u0, v1); GL11.glVertex3f(x0, y0, z1);
    }

    public void forceDirty() {
        this.isDirty = true;
    }

    private void renderBlockFaces(int x, int y, int z, int type) {
        int texTop = 2, texBottom = 2, texSide = 2;

        if (type == 3)      { texTop = 0;  texBottom = 2; texSide = 3; }
        else if (type == 2) { texTop = 2;  texBottom = 2; texSide = 2; }
        else if (type == 1) { texTop = 1;  texBottom = 1; texSide = 1; }
        else if (type == 4) { texTop = 4;  texBottom = 4; texSide = 4; }
        else if (type == 5) { texTop = 17; texBottom = 17; texSide = 14; }
        else if (type == 6) { texTop = 13; texBottom = 13; texSide = 13; }
        else if (type == 8) { texTop = 16; texBottom = 16; texSide = 16; }
        else if (type == 9) { texTop = 0;  texBottom = 0;  texSide = 0; }

        float x0 = x, x1 = x + 1.0f;
        float y0 = y, y1 = y + 1.0f;
        float z0 = z, z1 = z + 1.0f;

        float u0Top = (texTop % 16) / 16.0f;
        float v0Top = (texTop / 16) / 16.0f;
        float u1Top = u0Top + (1.0f / 16.0f);
        float v1Top = v0Top + (1.0f / 16.0f);

        float u0Bot = (texBottom % 16) / 16.0f;
        float v0Bot = (texBottom / 16) / 16.0f;
        float u1Bot = u0Bot + (1.0f / 16.0f);
        float v1Bot = v0Bot + (1.0f / 16.0f);

        float u0Side = (texSide % 16) / 16.0f;
        float v0Side = (texSide / 16) / 16.0f;
        float u1Side = u0Side + (1.0f / 16.0f);
        float v1Side = v0Side + (1.0f / 16.0f);

        float b0, b1, b2, b3;
        boolean useLighting = Lighting.enabled;

        // ВЕРХНЯЯ ГРАНЬ
        if (level.getBlock(x, y + 1, z) == 0 || level.getBlock(x, y + 1, z) == 7) {
            if (useLighting) {
                b0 = lighting.getVertexBrightness(x, y, z, 0, 0);
                b1 = lighting.getVertexBrightness(x, y, z, 0, 1);
                b2 = lighting.getVertexBrightness(x, y, z, 0, 2);
                b3 = lighting.getVertexBrightness(x, y, z, 0, 3);
            } else {
                b0 = b1 = b2 = b3 = 1.0f;
            }

            GL11.glColor3f(b0, b0, b0);
            GL11.glTexCoord2f(u0Top, v0Top); GL11.glVertex3f(x0, y1, z0);
            GL11.glColor3f(b1, b1, b1);
            GL11.glTexCoord2f(u0Top, v1Top); GL11.glVertex3f(x0, y1, z1);
            GL11.glColor3f(b2, b2, b2);
            GL11.glTexCoord2f(u1Top, v1Top); GL11.glVertex3f(x1, y1, z1);
            GL11.glColor3f(b3, b3, b3);
            GL11.glTexCoord2f(u1Top, v0Top); GL11.glVertex3f(x1, y1, z0);
        }

        // НИЖНЯЯ ГРАНЬ
        if (level.getBlock(x, y - 1, z) == 0 || level.getBlock(x, y - 1, z) == 7) {
            if (useLighting) {
                b0 = lighting.getVertexBrightness(x, y, z, 1, 0);
                b1 = lighting.getVertexBrightness(x, y, z, 1, 1);
                b2 = lighting.getVertexBrightness(x, y, z, 1, 2);
                b3 = lighting.getVertexBrightness(x, y, z, 1, 3);
            } else {
                b0 = b1 = b2 = b3 = 1.0f;
            }

            GL11.glColor3f(b0, b0, b0);
            GL11.glTexCoord2f(u0Bot, v0Bot); GL11.glVertex3f(x0, y0, z0);
            GL11.glColor3f(b1, b1, b1);
            GL11.glTexCoord2f(u1Bot, v0Bot); GL11.glVertex3f(x1, y0, z0);
            GL11.glColor3f(b2, b2, b2);
            GL11.glTexCoord2f(u1Bot, v1Bot); GL11.glVertex3f(x1, y0, z1);
            GL11.glColor3f(b3, b3, b3);
            GL11.glTexCoord2f(u0Bot, v1Bot); GL11.glVertex3f(x0, y0, z1);
        }

        // СЕВЕРНАЯ ГРАНЬ (Z-)
        if (level.getBlock(x, y, z - 1) == 0 || level.getBlock(x, y, z - 1) == 7) {
            if (useLighting) {
                b0 = lighting.getVertexBrightness(x, y, z, 2, 0);
                b1 = lighting.getVertexBrightness(x, y, z, 2, 1);
                b2 = lighting.getVertexBrightness(x, y, z, 2, 2);
                b3 = lighting.getVertexBrightness(x, y, z, 2, 3);
            } else {
                b0 = b1 = b2 = b3 = 1.0f;
            }

            GL11.glColor3f(b0, b0, b0);
            GL11.glTexCoord2f(u1Side, v1Side); GL11.glVertex3f(x0, y0, z0);
            GL11.glColor3f(b1, b1, b1);
            GL11.glTexCoord2f(u1Side, v0Side); GL11.glVertex3f(x0, y1, z0);
            GL11.glColor3f(b2, b2, b2);
            GL11.glTexCoord2f(u0Side, v0Side); GL11.glVertex3f(x1, y1, z0);
            GL11.glColor3f(b3, b3, b3);
            GL11.glTexCoord2f(u0Side, v1Side); GL11.glVertex3f(x1, y0, z0);
        }

        // ЮЖНАЯ ГРАНЬ (Z+)
        if (level.getBlock(x, y, z + 1) == 0 || level.getBlock(x, y, z + 1) == 7) {
            if (useLighting) {
                b0 = lighting.getVertexBrightness(x, y, z, 3, 0);
                b1 = lighting.getVertexBrightness(x, y, z, 3, 1);
                b2 = lighting.getVertexBrightness(x, y, z, 3, 2);
                b3 = lighting.getVertexBrightness(x, y, z, 3, 3);
            } else {
                b0 = b1 = b2 = b3 = 1.0f;
            }

            GL11.glColor3f(b0, b0, b0);
            GL11.glTexCoord2f(u0Side, v1Side); GL11.glVertex3f(x1, y0, z1);
            GL11.glColor3f(b1, b1, b1);
            GL11.glTexCoord2f(u0Side, v0Side); GL11.glVertex3f(x1, y1, z1);
            GL11.glColor3f(b2, b2, b2);
            GL11.glTexCoord2f(u1Side, v0Side); GL11.glVertex3f(x0, y1, z1);
            GL11.glColor3f(b3, b3, b3);
            GL11.glTexCoord2f(u1Side, v1Side); GL11.glVertex3f(x0, y0, z1);
        }

        // ЗАПАДНАЯ ГРАНЬ (X-)
        if (level.getBlock(x - 1, y, z) == 0 || level.getBlock(x - 1, y, z) == 7) {
            if (useLighting) {
                b0 = lighting.getVertexBrightness(x, y, z, 4, 0);
                b1 = lighting.getVertexBrightness(x, y, z, 4, 1);
                b2 = lighting.getVertexBrightness(x, y, z, 4, 2);
                b3 = lighting.getVertexBrightness(x, y, z, 4, 3);
            } else {
                b0 = b1 = b2 = b3 = 1.0f;
            }

            GL11.glColor3f(b0, b0, b0);
            GL11.glTexCoord2f(u0Side, v1Side); GL11.glVertex3f(x0, y0, z1);
            GL11.glColor3f(b1, b1, b1);
            GL11.glTexCoord2f(u0Side, v0Side); GL11.glVertex3f(x0, y1, z1);
            GL11.glColor3f(b2, b2, b2);
            GL11.glTexCoord2f(u1Side, v0Side); GL11.glVertex3f(x0, y1, z0);
            GL11.glColor3f(b3, b3, b3);
            GL11.glTexCoord2f(u1Side, v1Side); GL11.glVertex3f(x0, y0, z0);
        }

        // ВОСТОЧНАЯ ГРАНЬ (X+)
        if (level.getBlock(x + 1, y, z) == 0 || level.getBlock(x + 1, y, z) == 7) {
            if (useLighting) {
                b0 = lighting.getVertexBrightness(x, y, z, 5, 0);
                b1 = lighting.getVertexBrightness(x, y, z, 5, 1);
                b2 = lighting.getVertexBrightness(x, y, z, 5, 2);
                b3 = lighting.getVertexBrightness(x, y, z, 5, 3);
            } else {
                b0 = b1 = b2 = b3 = 1.0f;
            }

            GL11.glColor3f(b0, b0, b0);
            GL11.glTexCoord2f(u1Side, v1Side); GL11.glVertex3f(x1, y0, z0);
            GL11.glColor3f(b1, b1, b1);
            GL11.glTexCoord2f(u1Side, v0Side); GL11.glVertex3f(x1, y1, z0);
            GL11.glColor3f(b2, b2, b2);
            GL11.glTexCoord2f(u0Side, v0Side); GL11.glVertex3f(x1, y1, z1);
            GL11.glColor3f(b3, b3, b3);
            GL11.glTexCoord2f(u0Side, v1Side); GL11.glVertex3f(x1, y0, z1);
        }
    }
    
    private static class TransparentBlock {
        int x, y, z, type;
        float distance;
        
        TransparentBlock(int x, int y, int z, int type, float distance) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.type = type;
            this.distance = distance;
        }
    }
}