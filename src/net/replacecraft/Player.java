package net.replacecraft;

import org.lwjgl.input.Keyboard;

import net.replacecraft.level.Level;

public class Player {
    private Level level;
    
    // Координаты позиции глаз игрока
    public float x, y, z;
    public float xo, yo, zo; 
    
    // Вектор скорости (моментум)
    public float xd, yd, zd; 
    public float xRot, yRot;
    
    // Хитбокс (bb.y0 - это уровень ног)
    public float x0, y0, z0; 
    
    public float heightOffset = 1.62F; 
    public float width = 0.6F;
    public float height = 1.8F;
    public boolean onGround = false;

    public boolean flyMode = false;
    public boolean noClipMode = false;
    
    public Player(Level level) {
        this.level = level;
        resetPos();
    }

    public void resetPos() {
        float startX = (float)level.width / 2.0F;
        float startZ = (float)level.depth / 2.0F;
        
        // Ищем верхний блок земли на координатах спавна, чтобы не застрять в холме
        float startY = level.height - 1;
        while (startY > 0 && level.getBlock((int)startX, (int)startY, (int)startZ) == 0) {
            startY--;
        }
        startY += 6.0F; // Встаем чуть выше травы
        
        this.x0 = startX - width / 2.0F;
        this.y0 = startY;
        this.z0 = startZ - width / 2.0F;
        // ... (оставшаяся часть метода resetPos без изменений)
        
        this.x = startX;
        this.y = this.y0 + heightOffset;
        this.z = startZ;
        
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.xd = this.yd = this.zd = 0.0F;
    }

    public void turn(float xo, float yo) {
        this.yRot = (float) ((double) this.yRot + (double) xo * 0.15D);
        this.xRot = (float) ((double) this.xRot - (double) yo * 0.15D);
        
        if (this.xRot < -90.0F) this.xRot = -90.0F;
        if (this.xRot > 90.0F) this.xRot = 90.0F;
    }

    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        float xa = 0.0F;
        float ya = 0.0F;

        if (Keyboard.isKeyDown(Keyboard.KEY_R)) {
            this.resetPos();
        }
        
        if (Keyboard.isKeyDown(Keyboard.KEY_UP) || Keyboard.isKeyDown(Keyboard.KEY_W)) {
            --ya;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_S) || Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
            ++ya;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_LEFT) || Keyboard.isKeyDown(Keyboard.KEY_A)) {
            --xa;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT) || Keyboard.isKeyDown(Keyboard.KEY_D)) {
            ++xa;
        }

        // Прыжок ровно на 0.12F из оригинального Player.java
        if ((Keyboard.isKeyDown(Keyboard.KEY_SPACE) || Keyboard.isKeyDown(Keyboard.KEY_LMETA)) && this.onGround) {
            this.yd = 0.12F;
        }

        if (flyMode) {
            this.yd = 0.0F;
            if (Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
                this.yd = 0.25F;
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
                this.yd = -0.25F;
            }
            this.moveRelative(xa, ya, 0.08F);
        } else {
            if ((Keyboard.isKeyDown(Keyboard.KEY_SPACE) || Keyboard.isKeyDown(Keyboard.KEY_LMETA)) && this.onGround) {
                this.yd = 0.12F;
            }
            this.moveRelative(xa, ya, this.onGround ? 0.02F : 0.005F);
            this.yd = (float)((double)this.yd - 0.005D);
        }

        this.move(this.xd, this.yd, this.zd);
        
        float factor = flyMode ? 0.6F : 0.91F;
        this.xd *= factor;
        this.yd *= 0.98F;
        this.zd *= factor;
        
        if (this.onGround && !flyMode) {
            this.xd *= 0.7F;
            this.zd *= 0.7F;
        }
    }
    private void moveRelative(float xa, float ya, float speed) {
        float dist = xa * xa + ya * ya;
        if (dist >= 0.01F) {
            dist = (float)Math.sqrt(dist);
            if (dist < 1.0F) dist = 1.0F;
            
            dist = speed / dist;
            xa *= dist;
            ya *= dist;
            
            float sin = (float)Math.sin(Math.toRadians(this.yRot));
            float cos = (float)Math.cos(Math.toRadians(this.yRot));
            
            this.xd += xa * cos - ya * sin;
            this.zd += ya * cos + xa * sin;
        }
    }

    // Метод перемещения хитбокса AABB из RubyDung
    public void move(float dx, float dy, float dz) {
        float oldX = x0;
        float oldY = y0;
        float oldZ = z0;

        x0 += dx;
        if (!noClipMode && isColliding()) x0 = oldX;

        y0 += dy;
        onGround = false;
        if (isColliding()) {
            if (!noClipMode) {
                if (dy < 0) onGround = true;
                y0 = oldY;
                yd = 0.0F;
            }
        }

        z0 += dz;
        if (!noClipMode && isColliding()) z0 = oldZ;

        this.x = x0 + width / 2.0F;
        this.y = y0 + heightOffset;
        this.z = z0 + width / 2.0F;
    }

    public void setLevel(Level level) {
        this.level = level;
    }
    
    public void setPosition(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }
    
    
    // Метод трассировки луча взгляда игрока (Raycast)
    public HitResult rayTrace(float maxDistance) {
        float step = 0.05F; // Размер шага проверки
        
        // Переводим углы поворота камеры в вектор направления взгляда
        float cosX = (float) Math.cos(Math.toRadians(xRot));
        float sinX = (float) Math.sin(Math.toRadians(xRot));
        float cosY = (float) Math.cos(Math.toRadians(yRot));
        float sinY = (float) Math.sin(Math.toRadians(yRot));
        
        // Вектор направления взгляда (инвертируем Z, как в OpenGL)
        float dirX = sinY * cosX;
        float dirY = -sinX;
        float dirZ = -cosY * cosX;

        // Стартуем луч строго из позиции глаз игрока
        float currentX = this.x;
        float currentY = this.y;
        float currentZ = this.z;

        int lastBlockX = (int) Math.floor(currentX);
        int lastBlockY = (int) Math.floor(currentY);
        int lastBlockZ = (int) Math.floor(currentZ);

        for (float d = 0; d < maxDistance; d += step) {
            currentX += dirX * step;
            currentY += dirY * step;
            currentZ += dirZ * step;

            int blockX = (int) Math.floor(currentX);
            int blockY = (int) Math.floor(currentY);
            int blockZ = (int) Math.floor(currentZ);

            // Если луч наткнулся на твердый блок (не воздух)
            if (level.getBlock(blockX, blockY, blockZ) > 0) {
                // Возвращаем координаты блока столкновения и координаты свободного соседа перед ним
                return new HitResult(blockX, blockY, blockZ, lastBlockX, lastBlockY, lastBlockZ);
            }

            // Запоминаем текущие координаты воздуха на случай, если на следующем шаге упремся в блок
            lastBlockX = blockX;
            lastBlockY = blockY;
            lastBlockZ = blockZ;
        }

        return null; // Никуда не попали
    }

    // Вспомогательный класс для хранения результатов попадания луча
    public static class HitResult {
        public int x, y, z;          // Координаты блока, в который попали (для разрушения)
        public int faceX, faceY, faceZ; // Координаты соседнего воздуха (для установки)

        public HitResult(int x, int y, int z, int fx, int fy, int fz) {
            this.x = x; this.y = y; this.z = z;
            this.faceX = fx; this.faceY = fy; this.faceZ = fz;
        }
    }

    private boolean isColliding() {
        int minX = (int) Math.floor(x0);
        int maxX = (int) Math.floor(x0 + width);
        int minY = (int) Math.floor(y0);
        int maxY = (int) Math.floor(y0 + height);
        int minZ = (int) Math.floor(z0);
        int maxZ = (int) Math.floor(z0 + width);

        for (int bx = minX; bx <= maxX; bx++) {
            for (int by = minY; by <= maxY; by++) {
                for (int bz = minZ; bz <= maxZ; bz++) {
                    int blockType = level.getBlock(bx, by, bz);
                    if (blockType > 0 && blockType != 7) {
                        return true; 
                    }
                }
            }
        }
        return false;
    }
}