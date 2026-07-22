package net.replacecraft;

import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

public class Tesselator {
    private FloatBuffer buffer = BufferUtils.createFloatBuffer(524288);
    private float[] array = new float[524288];
    private int vertices = 0;
    private float u, v;
    private float r = 1, g = 1, b = 1;
    private boolean hasColor = true;
    private boolean hasTexture = false;
    private int p = 0;
    
    public static Tesselator instance = new Tesselator();
    
    public void init() {
        vertices = 0;
        p = 0;
        hasColor = true;
        hasTexture = false;
    }
    
    public void tex(float u, float v) {
        hasTexture = true;
        this.u = u;
        this.v = v;
    }
    
    public void color(float r, float g, float b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }
    
    public void vertex(float x, float y, float z) {
        if (hasTexture) {
            array[p++] = u;
            array[p++] = v;
        }
        if (hasColor) {
            array[p++] = r;
            array[p++] = g;
            array[p++] = b;
        }
        array[p++] = x;
        array[p++] = y;
        array[p++] = z;
        vertices++;
    }
    
    public void flush() {
        if (vertices == 0) return;
        
        buffer.clear();
        buffer.put(array, 0, p);
        buffer.flip();
        
        int stride = (hasTexture ? 2 : 0) + (hasColor ? 3 : 0) + 3;
        int format = hasColor ? (hasTexture ? GL11.GL_T2F_C3F_V3F : GL11.GL_C3F_V3F) : GL11.GL_V3F;
        
        GL11.glInterleavedArrays(format, stride * 4, buffer);
        
        GL11.glDrawArrays(GL11.GL_QUADS, 0, vertices);
        
        init();
    }
}