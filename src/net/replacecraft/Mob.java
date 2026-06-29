package net.replacecraft;

import net.replacecraft.level.Level;
import org.lwjgl.opengl.GL11;

public class Mob {
    public float x, y, z;
    public float xo, yo, zo;
    public float rot;
    public float rotA;
    public float timeOffs;
    public float speed = 1.0f;
    public boolean onGround = false;
    public float xd, yd, zd;
    public boolean visible = true;

    private Level level;
    private static MobModel model = new MobModel();
    private int textureID = -1;

    public Mob(Level level, float x, float y, float z) {
        this.level = level;
        setPos(x, y, z);
        this.timeOffs = (float) Math.random() * 1239813.0f;
        this.rot = (float) (Math.random() * Math.PI * 2.0);
        this.rotA = (float) (Math.random() + 1.0) * 0.003f; // БЫЛО 0.01 — медленнее поворот
    }

    public void setTexture(int texID) {
        this.textureID = texID;
    }

    public void setPos(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    public void tick() {
        xo = x;
        yo = y;
        zo = z;

        if (y < -100.0f) {
            setPos(level.width / 2f, level.height / 2f + 10, level.depth / 2f);
        }

        rot += rotA;
        rotA *= 0.99f;
        rotA += (float) ((Math.random() - Math.random()) * Math.random() * Math.random() * 0.02);

        float xa = (float) Math.sin(rot);
        float ya = (float) Math.cos(rot);

        // ПРЫЖОК: если на земле и впереди блок — прыгаем
        if (onGround) {
            // Проверяем блок впереди на высоте ног и головы
            int frontX = (int)(x + xa * 0.5f);
            int frontZ = (int)(z + ya * 0.5f);
            int feetY = (int)(y - 0.5f);
            int headY = (int)(y + 1.0f);
            
            boolean blockAhead = level.getBlock(frontX, feetY, frontZ) != 0 
                              || level.getBlock(frontX, feetY + 1, frontZ) != 0
                              || level.getBlock(frontX, headY, frontZ) != 0;
            
            // Прыгаем если впереди блок или случайно
            if (blockAhead || Math.random() < 0.01f) {
                yd = 0.42f; // Сила прыжка (как у игрока)
            }
        }

        // Движение
        xd += xa * (onGround ? 0.03f : 0.01f);
        zd += ya * (onGround ? 0.03f : 0.01f);
        yd -= 0.08f; // Гравитация

        // Коллизия
        float newX = x + xd;
        float newY = y + yd;
        float newZ = z + zd;

        // Проверка по X (две точки: ноги и голова)
        if (checkCollision(newX, y - 0.5f, z) || checkCollision(newX, y + 0.5f, z) || checkCollision(newX, y + 1.0f, z)) {
            xd = 0;
            newX = x;
        }
        // Проверка по Y
        if (checkCollision(x, newY - 0.5f, z)) {
            yd = 0;
            newY = y;
            onGround = true;
        } else {
            onGround = false;
        }
        // Проверка по Z
        if (checkCollision(x, y - 0.5f, newZ) || checkCollision(x, y + 0.5f, newZ) || checkCollision(x, y + 1.0f, newZ)) {
            zd = 0;
            newZ = z;
        }

        x = newX;
        y = newY;
        z = newZ;

        // Трение
        xd *= 0.91f;
        yd *= 0.98f;
        zd *= 0.91f;

        if (onGround) {
            xd *= 0.7f;
            zd *= 0.7f;
        }

        // Границы мира
        if (x < 2) { x = 2; xd = 0; }
        if (x > level.width - 2) { x = level.width - 2; xd = 0; }
        if (z < 2) { z = 2; zd = 0; }
        if (z > level.depth - 2) { z = level.depth - 2; zd = 0; }
    }

    private boolean checkCollision(float px, float py, float pz) {
        int bx = (int) px;
        int by = (int) py;
        int bz = (int) pz;
        return level.getBlock(bx, by, bz) != 0;
    }

    public void render(float a) {
        if (!visible) return;

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        if (textureID >= 0) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        } else {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
        }

        GL11.glPushMatrix();

        double time = System.nanoTime() / 1.0E9 * 8.0 * speed + timeOffs; // БЫЛО 10.0 — медленнее анимация
        float size = 0.058333334f;
        float yy = (float) (-Math.abs(Math.sin(time * 0.6662)) * 5.0 - 23.0);

        float rx = xo + (x - xo) * a;
        float ry = yo + (y - yo) * a;
        float rz = zo + (z - zo) * a;

        GL11.glTranslatef(rx, ry, rz);
        GL11.glScalef(1.0f, -1.0f, 1.0f);
        GL11.glScalef(size, size, size);
        GL11.glTranslatef(0.0f, yy, 0.0f);

        GL11.glRotatef(rot * 57.29578f + 180.0f, 0.0f, 1.0f, 0.0f);

        if (model != null) {
            model.render((float) time);
        }

        GL11.glPopMatrix();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }
}