package net.replacecraft;

import org.lwjgl.opengl.GL11;

public class BlockGuiRenderer {
    
    public static void renderBlockInGui(int type, float rotation, int textureID) {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);

        GL11.glPushMatrix();
        // Наклоняем куб «лицом к зрителю» — классический изометрический вид GUI Minecraft
        GL11.glRotatef(30.0f, 1.0f, 0.0f, 0.0f);   // наклон вперёд
        GL11.glRotatef(-45.0f, 0.0f, 1.0f, 0.0f);  // поворот на 45° против часовой
        GL11.glRotatef(rotation, 0.0f, 1.0f, 0.0f); // анимация вращения

        float x0 = -0.5f, x1 = 0.5f;
        float y0 = -0.5f, y1 = 0.5f;
        float z0 = -0.5f, z1 = 0.5f;

        if (type == 7) {
            // Саженец — крестовая модель
            int texID = 15;
            GL11.glColor3f(1.0f, 1.0f, 1.0f);

            float u0 = (texID % 16) / 16.0f;
            float v0 = (texID / 16) / 16.0f;
            float u1 = u0 + (1.0f / 16.0f);
            float v1 = v0 + (1.0f / 16.0f);

            GL11.glBegin(GL11.GL_QUADS);
            // Плоскость X
            GL11.glTexCoord2f(u0, v0); GL11.glVertex3f(x0, y0, z0);
            GL11.glTexCoord2f(u0, v1); GL11.glVertex3f(x0, y1, z0);
            GL11.glTexCoord2f(u1, v1); GL11.glVertex3f(x1, y1, z1);
            GL11.glTexCoord2f(u1, v0); GL11.glVertex3f(x1, y0, z1);

            GL11.glTexCoord2f(u1, v0); GL11.glVertex3f(x1, y0, z1);
            GL11.glTexCoord2f(u1, v1); GL11.glVertex3f(x1, y1, z1);
            GL11.glTexCoord2f(u0, v1); GL11.glVertex3f(x0, y1, z0);
            GL11.glTexCoord2f(u0, v0); GL11.glVertex3f(x0, y0, z0);

            // Плоскость Z
            GL11.glTexCoord2f(u0, v0); GL11.glVertex3f(x0, y0, z1);
            GL11.glTexCoord2f(u0, v1); GL11.glVertex3f(x0, y1, z1);
            GL11.glTexCoord2f(u1, v1); GL11.glVertex3f(x1, y1, z0);
            GL11.glTexCoord2f(u1, v0); GL11.glVertex3f(x1, y0, z0);

            GL11.glTexCoord2f(u1, v0); GL11.glVertex3f(x1, y0, z0);
            GL11.glTexCoord2f(u1, v1); GL11.glVertex3f(x1, y1, z0);
            GL11.glTexCoord2f(u0, v1); GL11.glVertex3f(x0, y1, z1);
            GL11.glTexCoord2f(u0, v0); GL11.glVertex3f(x0, y0, z1);
            GL11.glEnd();

        } else {
            // Обычный блок — ПРАВИЛЬНЫЕ UV-координаты
            int texTop = 2, texBottom = 2, texSide = 2;

            if (type == 3)      { texTop = 0;  texBottom = 0; texSide = 3; }  // Трава
            else if (type == 2) { texTop = 2;  texBottom = 2; texSide = 2; }  // Земля
            else if (type == 1) { texTop = 1;  texBottom = 1; texSide = 1; }  // Камень
            else if (type == 4) { texTop = 4;  texBottom = 4; texSide = 4; }  // Доски
            else if (type == 5) { texTop = 14; texBottom = 17; texSide = 14; } // Дерево (бревно)
            else if (type == 6) { texTop = 13; texBottom = 13; texSide = 13; } // Листва
            else if (type == 8) { texTop = 16; texBottom = 16; texSide = 16; } // Cobblestone
            else if (type == 9) { texTop = 0; texBottom = 0; texSide = 0; } // FullGrass
            
            // Вычисляем UV для каждой текстуры
            float uTop0 = (texTop % 16) / 16.0f;
            float vTop0 = (texTop / 16) / 16.0f;
            float uTop1 = uTop0 + (1.0f / 16.0f);
            float vTop1 = vTop0 + (1.0f / 16.0f);

            float uBot0 = (texBottom % 16) / 16.0f;
            float vBot0 = (texBottom / 16) / 16.0f;
            float uBot1 = uBot0 + (1.0f / 16.0f);
            float vBot1 = vBot0 + (1.0f / 16.0f);

            float uSide0 = (texSide % 16) / 16.0f;
            float vSide0 = (texSide / 16) / 16.0f;
            float uSide1 = uSide0 + (1.0f / 16.0f);
            float vSide1 = vSide0 + (1.0f / 16.0f);

            GL11.glBegin(GL11.GL_QUADS);

            // ВЕРХ (Y+) — СВЕТЛЫЙ
            GL11.glColor3f(1.0f, 1.0f, 1.0f);
            GL11.glTexCoord2f(uTop0, vTop1); GL11.glVertex3f(x0, y1, z1);
            GL11.glTexCoord2f(uTop0, vTop0); GL11.glVertex3f(x0, y1, z0);
            GL11.glTexCoord2f(uTop1, vTop0); GL11.glVertex3f(x1, y1, z0);
            GL11.glTexCoord2f(uTop1, vTop1); GL11.glVertex3f(x1, y1, z1);

            // НИЗ (Y-) — ТЁМНЫЙ
            GL11.glColor3f(0.4f, 0.4f, 0.4f);
            GL11.glTexCoord2f(uBot0, vBot0); GL11.glVertex3f(x0, y0, z0);
            GL11.glTexCoord2f(uBot0, vBot1); GL11.glVertex3f(x0, y0, z1);
            GL11.glTexCoord2f(uBot1, vBot1); GL11.glVertex3f(x1, y0, z1);
            GL11.glTexCoord2f(uBot1, vBot0); GL11.glVertex3f(x1, y0, z0);

            // ПЕРЕД (Z-) — средний свет
            GL11.glColor3f(0.7f, 0.7f, 0.7f);
            GL11.glTexCoord2f(uSide0, vSide0); GL11.glVertex3f(x0, y0, z0);
            GL11.glTexCoord2f(uSide0, vSide1); GL11.glVertex3f(x0, y1, z0);
            GL11.glTexCoord2f(uSide1, vSide1); GL11.glVertex3f(x1, y1, z0);
            GL11.glTexCoord2f(uSide1, vSide0); GL11.glVertex3f(x1, y0, z0);

            // ЗАД (Z+) — средний свет
            GL11.glColor3f(0.7f, 0.7f, 0.7f);
            GL11.glTexCoord2f(uSide1, vSide0); GL11.glVertex3f(x1, y0, z1);
            GL11.glTexCoord2f(uSide1, vSide1); GL11.glVertex3f(x1, y1, z1);
            GL11.glTexCoord2f(uSide0, vSide1); GL11.glVertex3f(x0, y1, z1);
            GL11.glTexCoord2f(uSide0, vSide0); GL11.glVertex3f(x0, y0, z1);

            // ЛЕВО (X-) — темнее
            GL11.glColor3f(0.5f, 0.5f, 0.5f);
            GL11.glTexCoord2f(uSide0, vSide0); GL11.glVertex3f(x0, y0, z1);
            GL11.glTexCoord2f(uSide0, vSide1); GL11.glVertex3f(x0, y1, z1);
            GL11.glTexCoord2f(uSide1, vSide1); GL11.glVertex3f(x0, y1, z0);
            GL11.glTexCoord2f(uSide1, vSide0); GL11.glVertex3f(x0, y0, z0);

            // ПРАВО (X+) — темнее
            GL11.glColor3f(0.5f, 0.5f, 0.5f);
            GL11.glTexCoord2f(uSide1, vSide0); GL11.glVertex3f(x1, y0, z0);
            GL11.glTexCoord2f(uSide1, vSide1); GL11.glVertex3f(x1, y1, z0);
            GL11.glTexCoord2f(uSide0, vSide1); GL11.glVertex3f(x1, y1, z1);
            GL11.glTexCoord2f(uSide0, vSide0); GL11.glVertex3f(x1, y0, z1);

            GL11.glEnd();
        }

        GL11.glPopMatrix();
        GL11.glDisable(GL11.GL_BLEND);
    }
}