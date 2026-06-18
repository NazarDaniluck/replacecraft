package net.replacecraft.particle;

import java.util.ArrayList;
import java.util.List;
import org.lwjgl.opengl.GL11;
import net.replacecraft.level.Level;


public class ParticleEngine {
    private Level level;
    private List<SingleParticle> particles = new ArrayList<>();

    public ParticleEngine(Level level) {
        this.level = level;
    }

    // Оригинальный метод спавна: разбивает блок на сетку мелких партиклов 4х4х4 (всего 64 частицы)
    public void spawnBlockParticles(int bx, int by, int bz, int blockType) {
        if (blockType == 0) return;
        
        // В c0.0.11a блок дробился системно по сетке, чтобы создать плотное облако крошки
        for (int x = 0; x < 4; ++x) {
            for (int y = 0; y < 4; ++y) {
                for (int z = 0; z < 4; ++z) {
                    float px = (float)bx + ((float)x + 0.5F) / 4.0F;
                    float py = (float)by + ((float)y + 0.5F) / 4.0F;
                    float pz = (float)bz + ((float)z + 0.5F) / 4.0F;
                    
                    float xd = px - ((float)bx + 0.5F);
                    float yd = py - ((float)by + 0.5F);
                    float zd = pz - ((float)bz + 0.5F);
                    
                    particles.add(new SingleParticle(px, py, pz, xd, yd, zd, blockType));
                }
            }
        }
    }

    public void tick() {
        for (int i = 0; i < particles.size(); i++) {
            SingleParticle p = particles.get(i);
            p.tick();
            if (p.isDead) {
                particles.remove(i);
                i--;
            }
        }
    }

    public void render(int textureID, float xRot, float yRot) {
        if (particles.isEmpty()) return;

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        GL11.glDisable(GL11.GL_CULL_FACE); 
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);
        
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1f);

        GL11.glBegin(GL11.GL_QUADS);
        // ... (весь код цикла отрисовки частиц оставляем без изменений) ...
        GL11.glEnd();
        
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_CULL_FACE);
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public void clear() {
        particles.clear();
    }

    private class SingleParticle {
        float x, y, z;
        float xd, yd, zd;
        float u0, v0, u1, v1;
        float size;
        int age = 0;
        int maxAge;
        boolean isDead = false;

        public SingleParticle(float x, float y, float z, float xd, float yd, float zd, int blockType) {
            this.x = x; this.y = y; this.z = z;
            
            // Зажимаем разлёт, чтобы частицы оставались в районе сломанного блока
            this.xd = xd + (float)(Math.random() * 2.0D - 1.0D) * 0.15F;
            this.yd = yd + (float)(Math.random() * 2.0D - 1.0D) * 0.1F;
            this.zd = zd + (float)(Math.random() * 2.0D - 1.0D) * 0.15F;
            
            // ЖЕСТКИЙ ФИКС СКОРОСТИ: уменьшаем силу взрыва в 4 раза, чтобы убрать эффект попкорна!
            float speedMultiplier = (float)(Math.random() * 0.1D + 0.15D);
            this.xd *= speedMultiplier;
            this.yd *= speedMultiplier;
            this.zd *= speedMultiplier;
            
            // Минимальный толчок вверх, чтобы они плавно осыпались
            this.yd = (float)((double)this.yd + 0.05D);

            // Время жизни делаем коротким, чтобы они быстро исчезали (от 6 до 14 тиков)
            this.maxAge = (int)(Math.random() * 8.0D) + 6;
            
            // ЖЕСТКИЙ ФИКС РАЗМЕРА: Делаем частицы крошечными пикселями (в 3 раза меньше оригинала)
            this.size = (float)(Math.random() * 0.02D + 0.02D); 

            // Нарезка UV-координат под твои честные ID
            int texIndex = 2; // Земля
            if (blockType == 1) texIndex = 1;      // Камень
            else if (blockType == 3) texIndex = 3; // Трава
            else if (blockType == 4) texIndex = 4; // Доски

            float texX = (float)(texIndex % 16) + (float)Math.random() * 0.94F;
            float texY = (float)(texIndex / 16) + (float)Math.random() * 0.94F;
            
            this.u0 = texX / 16.0F;
            this.v0 = texY / 16.0F;
            this.u1 = (texX + 0.02F) / 16.0F; // Сужаем вырез под новый размер
            this.v1 = (texY + 0.02F) / 16.0F;
        }

        private Level level;
        
        public void tick() {
            if (level == null) return;
        	this.age++;
            if (this.age >= this.maxAge) {
                this.isDead = true;
                return;
            }

            // Тяжёлая гравитация, чтобы они падали камнем вниз
            this.yd = (float)((double)this.yd - 0.05D);

            float nextX = x + xd;
            float nextY = y + yd;
            float nextZ = z + zd;

            // Коллизии со скольжением
            if (level.getBlock((int) Math.floor(nextX), (int) Math.floor(y), (int) Math.floor(z)) > 0) {
                xd = -xd * 0.2F;
            } else {
                x = nextX;
            }

            if (level.getBlock((int) Math.floor(x), (int) Math.floor(nextY), (int) Math.floor(z)) > 0) {
                yd = -yd * 0.2F;
                xd = (float)((double)xd * 0.5D); // Сильное трение о пол
                zd = (float)((double)zd * 0.5D);
            } else {
                y = nextY;
            }

            if (level.getBlock((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(nextZ)) > 0) {
                zd = -zd * 0.2F;
            } else {
                z = nextZ;
            }

            this.xd = (float)((double)this.xd * 0.95D);
            this.yd = (float)((double)this.yd * 0.95D);
            this.zd = (float)((double)this.zd * 0.95D);
        }
    }
}
