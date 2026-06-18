package net.replacecraft.level;

import org.lwjgl.opengl.GL11;

public class Chunk {
    private Level level;
    public final int x0, y0, z0;
    public final int x1, y1, z1;
    private int displayListID;
    private boolean isDirty = true;

    public Chunk(Level level, int x0, int y0, int z0, int x1, int y1, int z1) {
        this.level = level;
        this.x0 = x0; this.y0 = y0; this.z0 = z0;
        this.x1 = x1; this.y1 = y1; this.z1 = z1;
        this.displayListID = GL11.glGenLists(1);
    }

    public void render() {
        if (isDirty) {
            System.out.println("Chunk rebuilding: " + x0 + "," + y0 + "," + z0);
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
        GL11.glBegin(GL11.GL_QUADS);

        for (int x = x0; x < x1; x++) {
            for (int y = y0; y < y1; y++) {
                for (int z = z0; z < z1; z++) {
                    int type = level.getBlock(x, y, z);
                    if (type > 0) {
                        if (type == 7) {
                            GL11.glEnd(); // Временно прерываем GL_QUADS для включения блендинга
                            GL11.glEnable(GL11.GL_BLEND);
                            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                            GL11.glEnable(GL11.GL_ALPHA_TEST);
                            GL11.glAlphaFunc(GL11.GL_GREATER, 0.01f);
                            
                            GL11.glBegin(GL11.GL_QUADS);
                            renderSpriteFaces(x, y, z, type);
                            GL11.glEnd();
                            
                            GL11.glDisable(GL11.GL_BLEND);
                            GL11.glDisable(GL11.GL_ALPHA_TEST);
                            GL11.glBegin(GL11.GL_QUADS); // Возвращаем режим для остальных блоков
                        } else {
                            renderBlockFaces(x, y, z, type);
                        }
                    }
                }
            }
        }

        GL11.glEnd();
        GL11.glEndList();
    }

    public void forceDirty() {
        this.isDirty = true;
    }
    
    private void renderSpriteFaces(int x, int y, int z, int type) {
        int texID = 15; 
        GL11.glColor3f(1.0f, 1.0f, 1.0f);

        float u0 = (texID % 16) / 16.0f;
        float v0 = (texID / 16) / 16.0f;
        float u1 = u0 + (1.0f / 16.0f);
        float v1 = v0 + (1.0f / 16.0f);

        float x0 = x, x1 = x + 1.0f;
        float y0 = y, y1 = y + 1.0f;
        float z0 = z, z1 = z + 1.0f;

        GL11.glTexCoord2f(u0, v1); GL11.glVertex3f(x0, y0, z0);
        GL11.glTexCoord2f(u0, v0); GL11.glVertex3f(x0, y1, z0);
        GL11.glTexCoord2f(u1, v0); GL11.glVertex3f(x1, y1, z1);
        GL11.glTexCoord2f(u1, v1); GL11.glVertex3f(x1, y0, z1);

        GL11.glTexCoord2f(u1, v1); GL11.glVertex3f(x1, y0, z1);
        GL11.glTexCoord2f(u1, v0); GL11.glVertex3f(x1, y1, z1);
        GL11.glTexCoord2f(u0, v0); GL11.glVertex3f(x0, y1, z0);
        GL11.glTexCoord2f(u0, v1); GL11.glVertex3f(x0, y0, z0);

        GL11.glTexCoord2f(u0, v1); GL11.glVertex3f(x0, y0, z1);
        GL11.glTexCoord2f(u0, v0); GL11.glVertex3f(x0, y1, z1);
        GL11.glTexCoord2f(u1, v0); GL11.glVertex3f(x1, y1, z0);
        GL11.glTexCoord2f(u1, v1); GL11.glVertex3f(x1, y0, z0);

        GL11.glTexCoord2f(u1, v1); GL11.glVertex3f(x1, y0, z0);
        GL11.glTexCoord2f(u1, v0); GL11.glVertex3f(x1, y1, z0);
        GL11.glTexCoord2f(u0, v0); GL11.glVertex3f(x0, y1, z1);
        GL11.glTexCoord2f(u0, v1); GL11.glVertex3f(x0, y0, z1);
    }

    

    private void renderBlockFaces(int x, int y, int z, int type) {
        int texTop = 2, texBottom = 2, texSide = 2;

        if (type == 3)      { texTop = 0;  texBottom = 2; texSide = 3; }  // Трава
        else if (type == 2) { texTop = 2;  texBottom = 2; texSide = 2; }  // Земля
        else if (type == 1) { texTop = 1;  texBottom = 1; texSide = 1; }  // Камень
        else if (type == 4) { texTop = 4;  texBottom = 4; texSide = 4; }  // Доски
        else if (type == 5) { texTop = 14; texBottom = 14; texSide = 14; } // Дерево (бревно)
        else if (type == 6) { texTop = 13; texBottom = 13; texSide = 13; } // Листва

        float x0 = x, x1 = x + 1.0f;
        float y0 = y, y1 = y + 1.0f;
        float z0 = z, z1 = z + 1.0f;

        // Расчёт UV координат для текущих текстур по атласу 16х16
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

        // ВЕРХНЯЯ ГРАНЬ (Светлая)
        if (level.getBlock(x, y + 1, z) == 0) {
            GL11.glColor3f(1.0f, 1.0f, 1.0f);
            GL11.glTexCoord2f(u0Top, v0Top); GL11.glVertex3f(x0, y1, z0);
            GL11.glTexCoord2f(u0Top, v1Top); GL11.glVertex3f(x0, y1, z1);
            GL11.glTexCoord2f(u1Top, v1Top); GL11.glVertex3f(x1, y1, z1);
            GL11.glTexCoord2f(u1Top, v0Top); GL11.glVertex3f(x1, y1, z0);
        }
        
        // НИЖНЯЯ ГРАНЬ (Тёмная)
        if (level.getBlock(x, y - 1, z) == 0) {
            GL11.glColor3f(0.5f, 0.5f, 0.5f);
            GL11.glTexCoord2f(u0Bot, v0Bot); GL11.glVertex3f(x0, y0, z0);
            GL11.glTexCoord2f(u1Bot, v0Bot); GL11.glVertex3f(x1, y0, z0);
            GL11.glTexCoord2f(u1Bot, v1Bot); GL11.glVertex3f(x1, y0, z1);
            GL11.glTexCoord2f(u0Bot, v1Bot); GL11.glVertex3f(x0, y0, z1);
        }
        
        GL11.glColor3f(0.8f, 0.8f, 0.8f);
        // СЕВЕРНАЯ ГРАНЬ (Z-)
        if (level.getBlock(x, y, z - 1) == 0) {
            GL11.glTexCoord2f(u1Side, v1Side); GL11.glVertex3f(x0, y0, z0);
            GL11.glTexCoord2f(u1Side, v0Side); GL11.glVertex3f(x0, y1, z0);
            GL11.glTexCoord2f(u0Side, v0Side); GL11.glVertex3f(x1, y1, z0);
            GL11.glTexCoord2f(u0Side, v1Side); GL11.glVertex3f(x1, y0, z0);
        }
        // ЮЖНАЯ ГРАНЬ (Z+)
        if (level.getBlock(x, y, z + 1) == 0) {
            GL11.glTexCoord2f(u0Side, v1Side); GL11.glVertex3f(x1, y0, z1);
            GL11.glTexCoord2f(u0Side, v0Side); GL11.glVertex3f(x1, y1, z1);
            GL11.glTexCoord2f(u1Side, v0Side); GL11.glVertex3f(x0, y1, z1);
            GL11.glTexCoord2f(u1Side, v1Side); GL11.glVertex3f(x0, y0, z1);
        }
        
        GL11.glColor3f(0.6f, 0.6f, 0.6f);
        // ЗАПАДНАЯ ГРАНЬ (X-)
        if (level.getBlock(x - 1, y, z) == 0) {
            GL11.glTexCoord2f(u0Side, v1Side); GL11.glVertex3f(x0, y0, z1);
            GL11.glTexCoord2f(u0Side, v0Side); GL11.glVertex3f(x0, y1, z1);
            GL11.glTexCoord2f(u1Side, v0Side); GL11.glVertex3f(x0, y1, z0);
            GL11.glTexCoord2f(u1Side, v1Side); GL11.glVertex3f(x0, y0, z0);
        }
        // ВОСТОЧНАЯ ГРАНЬ (X+)
        if (level.getBlock(x + 1, y, z) == 0) {
            GL11.glTexCoord2f(u1Side, v1Side); GL11.glVertex3f(x1, y0, z0);
            GL11.glTexCoord2f(u1Side, v0Side); GL11.glVertex3f(x1, y1, z0);
            GL11.glTexCoord2f(u0Side, v0Side); GL11.glVertex3f(x1, y1, z1);
            GL11.glTexCoord2f(u0Side, v1Side); GL11.glVertex3f(x1, y0, z1);
        }
    }
}   
