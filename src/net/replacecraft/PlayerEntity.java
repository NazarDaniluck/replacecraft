package net.replacecraft;

import org.lwjgl.opengl.GL11;

public class PlayerEntity {
    public byte playerId;
    public String name;
    public float x, y, z;
    public float yaw, pitch;
    public boolean visible = true;
    
    private static PlayerModel model = new PlayerModel();
    private float animTime = 0;
    private int textureID = -1;

    public PlayerEntity(byte id, String name, float x, float y, float z, float yaw, float pitch) {
        this.playerId = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }
    
    public void setPosition(float x, float y, float z, float yaw, float pitch) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }
    
    public void setTexture(int texID) {
        this.textureID = texID;
    }
    
    public void updateAnimation() {
    }
    
    public void render(Font font, float playerYaw) {
        if (!visible) return;

        GL11.glPushMatrix();
        GL11.glTranslatef(x, y - 1.5f, z);

        // Модель всегда лицом к камере (как никнейм)
        GL11.glRotatef(-playerYaw + 180, 0.0f, 1.0f, 0.0f);

        float size = 0.058333334f;
        GL11.glScalef(1.0f, -1.0f, 1.0f);
        GL11.glScalef(size, size, size);
        GL11.glTranslatef(0.0f, -24.0f, 0.0f);

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        if (textureID >= 0) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        } else {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glColor3f(0.8f, 0.6f, 0.4f);
        }

        if (model != null) {
            model.render(animTime);
        }
        
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor3f(1.0f, 1.0f, 1.0f);
        GL11.glPopMatrix();

        // Никнейм
        if (font != null && name != null) {
            GL11.glPushMatrix();
            GL11.glTranslatef(x, y + 0.8f, z);
            GL11.glRotatef(-playerYaw, 0.0f, 1.0f, 0.0f);
            
            float scale = 0.025f;
            GL11.glScalef(scale, -scale, scale);
            
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);

            int textWidth = font.getStringWidth(name);
            int padding = 2;

            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDisable(GL11.GL_TEXTURE_2D);

            GL11.glColor4f(0.0f, 0.0f, 0.0f, 0.5f);
            
            GL11.glBegin(GL11.GL_QUADS);
                GL11.glVertex2f(-textWidth / 2.0f - padding, -padding);
                GL11.glVertex2f(-textWidth / 2.0f - padding, 8.0f + padding);
                GL11.glVertex2f(textWidth / 2.0f + padding, 8.0f + padding);
                GL11.glVertex2f(textWidth / 2.0f + padding, -padding);
            GL11.glEnd();

            GL11.glDisable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

            font.drawShadow(name, -textWidth / 2, 0, 0xFFFFFF);
            
            GL11.glDepthMask(true);
            GL11.glEnable(GL11.GL_CULL_FACE);
            
            GL11.glPopMatrix();
        }
    }
}