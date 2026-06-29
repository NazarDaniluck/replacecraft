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

    public void spawnBlockParticles(int bx, int by, int bz, int blockType) {
        if (blockType == 0) return;

        for (int x = 0; x < 4; ++x) {
            for (int y = 0; y < 4; ++y) {
                for (int z = 0; z < 4; ++z) {
                    float px = (float) bx + ((float) x + 0.5F) / 4.0F;
                    float py = (float) by + ((float) y + 0.5F) / 4.0F;
                    float pz = (float) bz + ((float) z + 0.5F) / 4.0F;

                    float xd = px - ((float) bx + 0.5F);
                    float yd = py - ((float) by + 0.5F);
                    float zd = pz - ((float) bz + 0.5F);

                    particles.add(new SingleParticle(level, px, py, pz, xd, yd, zd, blockType));
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
        for (SingleParticle p : particles) {
            p.render(xRot, yRot);
        }
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

    private static class SingleParticle {
        Level level;
        float x, y, z;
        float xd, yd, zd;
        float u0, v0, u1, v1;
        float size;
        int age = 0;
        int maxAge;
        boolean isDead = false;

        public SingleParticle(Level level, float x, float y, float z, float xd, float yd, float zd, int blockType) {
            this.level = level;
            this.x = x;
            this.y = y;
            this.z = z;

            this.xd = xd + (float) (Math.random() * 2.0D - 1.0D) * 0.15F;
            this.yd = yd + (float) (Math.random() * 2.0D - 1.0D) * 0.1F;
            this.zd = zd + (float) (Math.random() * 2.0D - 1.0D) * 0.15F;

            float speedMultiplier = (float) (Math.random() * 0.1D + 0.15D);
            this.xd *= speedMultiplier;
            this.yd *= speedMultiplier;
            this.zd *= speedMultiplier;

            this.yd = (float) ((double) this.yd + 0.05D);

            this.maxAge = (int) (Math.random() * 8.0D) + 6;
            this.size = (float) (Math.random() * 0.02D + 0.02D);

            int texIndex = 2;
            if (blockType == 1) texIndex = 1;
            else if (blockType == 3) texIndex = 3;
            else if (blockType == 4) texIndex = 4;

            float texX = (float) (texIndex % 16) + (float) Math.random() * 0.94F;
            float texY = (float) (texIndex / 16) + (float) Math.random() * 0.94F;

            this.u0 = texX / 16.0F;
            this.v0 = texY / 16.0F;
            this.u1 = (texX + 0.02F) / 16.0F;
            this.v1 = (texY + 0.02F) / 16.0F;
        }

        public void tick() {
            if (level == null) return;
            this.age++;
            if (this.age >= this.maxAge) {
                this.isDead = true;
                return;
            }

            this.yd = (float) ((double) this.yd - 0.05D);

            float nextX = x + xd;
            float nextY = y + yd;
            float nextZ = z + zd;

            if (level.getBlock((int) Math.floor(nextX), (int) Math.floor(y), (int) Math.floor(z)) > 0) {
                xd = -xd * 0.2F;
            } else {
                x = nextX;
            }

            if (level.getBlock((int) Math.floor(x), (int) Math.floor(nextY), (int) Math.floor(z)) > 0) {
                yd = -yd * 0.2F;
                xd = (float) ((double) xd * 0.5D);
                zd = (float) ((double) zd * 0.5D);
            } else {
                y = nextY;
            }

            if (level.getBlock((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(nextZ)) > 0) {
                zd = -zd * 0.2F;
            } else {
                z = nextZ;
            }

            this.xd = (float) ((double) this.xd * 0.95D);
            this.yd = (float) ((double) this.yd * 0.95D);
            this.zd = (float) ((double) this.zd * 0.95D);
        }

        public void render(float xRot, float yRot) {
            float cosY = (float) Math.cos(Math.toRadians(yRot));
            float sinY = (float) Math.sin(Math.toRadians(yRot));
            float cosX = (float) Math.cos(Math.toRadians(xRot));
            float sinX = (float) Math.sin(Math.toRadians(xRot));

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
}