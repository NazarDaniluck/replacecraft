package net.replacecraft;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.imageio.ImageIO;
import org.lwjgl.opengl.GL11;

public class Texture {
    
    /** Папка с текстурами (в ресурсах) */
    public static String texturePath = "/textures/";
    
    public static int loadTexture(String filename) {
        try {
            // Пробуем загрузить из ресурсов (внутри JAR или classpath)
            InputStream is = Texture.class.getResourceAsStream(texturePath + filename);
            
            // Если не нашли — пробуем из файла (для разработки)
            if (is == null) {
                File file = new File(filename);
                if (file.exists()) {
                    is = new FileInputStream(file);
                } else {
                    file = new File("res" + texturePath + filename);
                    if (file.exists()) {
                        is = new FileInputStream(file);
                    }
                }
            }
            
            if (is == null) {
                System.out.println("ERROR: Texture " + filename + " not found!");
                return 0;
            }

            BufferedImage image = ImageIO.read(is);
            is.close();
            
            int w = image.getWidth();
            int h = image.getHeight();
            int[] pixels = new int[w * h];
            image.getRGB(0, 0, w, h, pixels, 0, w);

            ByteBuffer buffer = ByteBuffer.allocateDirect(w * h * 4);
            buffer.order(ByteOrder.nativeOrder());

            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int pixel = pixels[y * w + x];
                    
                    int a = (pixel >> 24) & 0xFF;
                    int r = (pixel >> 16) & 0xFF;
                    int g = (pixel >> 8) & 0xFF;
                    int b = pixel & 0xFF;

                    if (a == 0) {
                        r = 0;
                        g = 0;
                        b = 0;
                    }

                    buffer.put((byte) r);
                    buffer.put((byte) g);
                    buffer.put((byte) b);
                    buffer.put((byte) a);
                }
            }
            buffer.flip();

            int textureID = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, w, h, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
            
            System.out.println("Texture loaded: " + filename + " (ID=" + textureID + ")");
            return textureID;
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }
}