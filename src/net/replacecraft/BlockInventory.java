package net.replacecraft;

import org.lwjgl.opengl.GL11;

public class BlockInventory {
	public static final int[] BLOCKS = {3, 2, 1, 4, 5, 6, 7, 8};
	public static final int SLOTS = BLOCKS.length;    
	private int selected = 0;
    private boolean visible = false;
    private Font font;
    
    public BlockInventory(Font font) {
        this.font = font;
    }
    
    public int getSelectedBlock() {
        return BLOCKS[selected];
    }
    
    public int getSelectedSlot() {
        return selected;
    }
    
    public void setSelectedSlot(int slot) {
        if (slot >= 0 && slot < SLOTS) {
            selected = slot;
        }
    }
    
    public void nextSlot() {
        selected = (selected + 1) % SLOTS;
    }
    
    public void prevSlot() {
        selected = (selected - 1 + SLOTS) % SLOTS;
    }
    
    public void toggle() {
        visible = !visible;
    }
    
    public boolean isVisible() {
        return visible;
    }
    
    public void show() {
        visible = true;
    }
    
    public void hide() {
        visible = false;
    }
    
    public void render(int screenWidth, int screenHeight, int textureID, float rotation) {
        if (!visible) return;

        int invWidth = SLOTS * 52 + 16;
        int invHeight = 70;
        int x = (screenWidth - invWidth) / 2;
        int y = screenHeight - invHeight - 10;

        // Фон инвентаря
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glColor4f(0.3f, 0.3f, 0.3f, 0.85f);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + invWidth, y);
        GL11.glVertex2f(x + invWidth, y + invHeight);
        GL11.glVertex2f(x, y + invHeight);
        GL11.glEnd();

        GL11.glColor4f(0.6f, 0.6f, 0.6f, 1.0f);
        GL11.glLineWidth(2.0f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + invWidth, y);
        GL11.glVertex2f(x + invWidth, y + invHeight);
        GL11.glVertex2f(x, y + invHeight);
        GL11.glEnd();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);

        // Слоты с блоками
        for (int i = 0; i < SLOTS; i++) {
            int slotX = x + 10 + i * 52;
            int slotY = y + 10;

            // Подсветка выбранного слота
            if (i == selected) {
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.4f);
                GL11.glBegin(GL11.GL_QUADS);
                GL11.glVertex2f(slotX - 2, slotY - 2);
                GL11.glVertex2f(slotX + 44, slotY - 2);
                GL11.glVertex2f(slotX + 44, slotY + 44);
                GL11.glVertex2f(slotX - 2, slotY + 44);
                GL11.glEnd();
                GL11.glDisable(GL11.GL_BLEND);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
            }

            // Отрисовка блока — используем BlockGuiRenderer
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glPushMatrix();
            GL11.glTranslatef(slotX + 22, slotY + 22, 0);
            GL11.glScalef(32.0f, 32.0f, 32.0f);
            BlockGuiRenderer.renderBlockInGui(BLOCKS[i], rotation, textureID);
            GL11.glPopMatrix();
            GL11.glDisable(GL11.GL_DEPTH_TEST);

            // Название блока
            if (font != null) {
                String name = getBlockName(BLOCKS[i]);
                int nameX = slotX + 22 - name.length() * 4;
                font.drawShadow(name, nameX, slotY + 44, i == selected ? 0xFFFFFF : 0xAAAAAA);
            }
        }
    }
    
    private String getBlockName(int id) {
        switch (id) {
            case 1: return "Stone";
            case 2: return "Dirt";
            case 3: return "Grass";
            case 4: return "Planks";
            case 5: return "Log";
            case 6: return "Leaves";
            case 7: return "Sapling";
            case 8: return "Cobblestone";
            default: return "Block";
        }
    }
}