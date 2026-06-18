package net.replacecraft.particle;

import org.lwjgl.opengl.GL11;
import net.replacecraft.level.Level;

public class Particle {
    private Level level;
    public float x, y, z;
    public float xd, yd, zd;
    private int texID;
    private float u0, v0, u1, v1;
    private int age;
    private int maxAge;
    private float size;

    public Particle(Level level, float x, float y, float z, float xd, float yd, float zd, int blockType) {
        this.level = level;
        this.x = x;
        this.y = y;
        this.z = z;
        
        // Случайный разлет частиц
        this.xd = xd + (float)(Math.random() * 2.0 - 1.0) * 0.4f;
        this.yd = yd + (float)(Math.random() * 2.0 - 1.0) * 0.4f + 0.5f; // Импульс вверх
        this.zd = zd + (float)(Math.random() * 2.0 - 1.0) * 0.4f;

        this.age = 0;
        this.maxAge = (int)(Math.random() * 30.0) + 20; // Время жизни в тиках
        this.size = (float)(Math.random() * 0.1) + 0.1f; // Размер частицы

        // Привязка текстуры к типу разрушенного блока (вырезаем кусочек)
        int texIndex = 2; // По умолчанию земля
        if (blockType == 1) texIndex = 1;      // Камень
        else if (blockType == 2) texIndex = 2; // Земля
        else if (blockType == 3) texIndex = 3; // Трава сбоку
        else if (blockType == 4) texIndex = 4; // Доски

        this.u0 = (texIndex % 16) / 16.0f + (float)Math.random() * 0.04f;
        this.v0 = (texIndex / 16) / 16.0f + (float)Math.random() * 0.04f;
        this.u1 = this.u0 + 0.01f;
        this.v1 = this.v0 + 0.01f;
    }

    public void tick() {
        this.age++;
        if (this.age >= this.maxAge) return;

        // Физика гравитации и трения воздуха
        this.yd -= 0.04f;
        
        // Движение с простейшей коллизией, чтобы частицы не падали сквозь пол
        float nextX = x + xd;
        float nextY = y + yd;
        float nextZ = z + zd;

        if (level.getBlock((int)Math.floor(nextX), (int)Math.floor(y), (int)Math.floor(z)) > 0) xd = -xd * 0.4f;
        else x = nextX;

        if (level.getBlock((int)Math.floor(x), (int)Math.floor(nextY), (int)Math.floor(z)) > 0) {
            yd = -yd * 0.4f; // Отскок от пола
            xd *= 0.7f;      // Трение о пол
            zd *= 0.7f;
        } else y = nextY;

        if (level.getBlock((int)Math.floor(x), (int)Math.floor(y), (int)Math.floor(nextZ)) > 0) zd = -zd * 0.4f;
        else z = nextZ;

        this.xd *= 0.98f;
        this.yd *= 0.98f;
        this.zd *= 0.98f;
    }

    public boolean isDead() {
        return this.age >= this.maxAge;
    }

    public void setLevel(Level level) {
        this.level = level;
    }
    
    // Отрисовка частицы в виде плоского квадрата (Billboard), повернутого к камере
    public void render(float xRot, float yRot) {
        float cosY = (float) Math.cos(Math.toRadians(yRot));
        float sinY = (float) Math.sin(Math.toRadians(yRot));
        float cosX = (float) Math.cos(Math.toRadians(xRot));
        float sinX = (float) Math.sin(Math.toRadians(xRot));

        // Векторы камеры для создания Billboard эффекта (частица всегда смотрит на игрока)
        float xa = cosY * size;
        float ya = -sinX * sinY * size;
        float za = -cosX * sinY * size;
        
        float xb = 0.0f;
        float yb = cosX * size;
        float zb = -sinX * size;

        GL11.glTexCoord2f(u0, v0); GL11.glVertex3f(x - xa - xb, y - ya - yb, z - za - zb);
        GL11.glTexCoord2f(u0, v1); GL11.glVertex3f(x - xa + xb, y - ya + yb, z - za + zb);
        GL11.glTexCoord2f(u1, v1); GL11.glVertex3f(x + xa + xb, y + ya + yb, z + za + zb);
        GL11.glTexCoord2f(u1, v0); GL11.glVertex3f(x + xa - xb, y + ya - yb, z + za - zb);
    }
}
