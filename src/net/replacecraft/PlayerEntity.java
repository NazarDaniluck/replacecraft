package net.replacecraft;

import org.lwjgl.opengl.GL11;

public class PlayerEntity {
    public byte playerId;
    public String name;
    public float x, y, z;
    public float yaw, pitch;
    public boolean visible = true;

    public PlayerEntity(byte id, String name, float x, float y, float z, float yaw, float pitch) {
        this.playerId = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public void render(Font font, float playerYaw) {
        if (!visible) return;

        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, z);

        float w = 0.5f;
        float h = 1.7f;
        float d = 0.5f;

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(0.5f, 0.5f, 0.5f, 0.8f);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3f(-w, 0, -d); GL11.glVertex3f(-w, h, -d);
        GL11.glVertex3f(w, h, -d);   GL11.glVertex3f(w, 0, -d);
        GL11.glVertex3f(w, 0, d);    GL11.glVertex3f(w, h, d);
        GL11.glVertex3f(-w, h, d);   GL11.glVertex3f(-w, 0, d);
        GL11.glVertex3f(-w, 0, d);   GL11.glVertex3f(-w, h, d);
        GL11.glVertex3f(-w, h, -d);  GL11.glVertex3f(-w, 0, -d);
        GL11.glVertex3f(w, 0, -d);   GL11.glVertex3f(w, h, -d);
        GL11.glVertex3f(w, h, d);    GL11.glVertex3f(w, 0, d);
        GL11.glVertex3f(-w, h, -d);  GL11.glVertex3f(-w, h, d);
        GL11.glVertex3f(w, h, d);    GL11.glVertex3f(w, h, -d);
        GL11.glVertex3f(-w, 0, d);   GL11.glVertex3f(-w, 0, -d);
        GL11.glVertex3f(w, 0, -d);   GL11.glVertex3f(w, 0, d);
        GL11.glEnd();

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        if (font != null && name != null) {
            GL11.glPushMatrix();
            GL11.glRotatef(-playerYaw, 0.0f, 1.0f, 0.0f);
            float scale = 0.02f;
            GL11.glScalef(scale, scale, scale);
            GL11.glTranslatef(0, h / scale + 5, 0);
            font.drawShadow(name, -name.length() * 4, 0, 0xFFFFFF);
            GL11.glPopMatrix();
        }

        GL11.glPopMatrix();
    }
}