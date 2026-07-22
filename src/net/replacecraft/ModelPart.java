package net.replacecraft;

import java.util.ArrayList;
import java.util.List;

public class ModelPart {
    public float rotationPointX, rotationPointY, rotationPointZ;
    public float rotateAngleX, rotateAngleY, rotateAngleZ;
    public List<Box> boxes = new ArrayList<>();
    
    public int textureOffsetX, textureOffsetY;
    
    public ModelPart(int texOffX, int texOffY) {
        this.textureOffsetX = texOffX;
        this.textureOffsetY = texOffY;
    }
    
    public void addBox(float x, float y, float z, int width, int height, int depth) {
        boxes.add(new Box(x, y, z, width, height, depth, textureOffsetX, textureOffsetY));
    }
    
    public void setRotationPoint(float x, float y, float z) {
        this.rotationPointX = x;
        this.rotationPointY = y;
        this.rotationPointZ = z;
    }
    
    public static class Box {
        public float minX, minY, minZ;
        public float maxX, maxY, maxZ;
        public int width, height;
        public float u0, v0;
        
        public Box(float x, float y, float z, int w, int h, int d, int texX, int texY) {
            this.minX = x;
            this.minY = y;
            this.minZ = z;
            this.maxX = x + w;
            this.maxY = y + h;
            this.maxZ = z + d;
            this.width = w;
            this.height = h;
            this.u0 = texX;
            this.v0 = texY;
        }
    }
}