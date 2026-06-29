package net.replacecraft;

public class Vertex {
    public Vec3 pos;
    public float u, v;

    public Vertex(float x, float y, float z, float u, float v) {
        this(new Vec3(x, y, z), u, v);
    }

    public Vertex(Vec3 pos, float u, float v) {
        this.pos = pos;
        this.u = u;
        this.v = v;
    }

    public Vertex remap(float u, float v) {
        return new Vertex(this.pos, u, v);
    }
}