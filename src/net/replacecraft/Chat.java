package net.replacecraft;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import java.util.ArrayList;
import java.util.List;

public class Chat {
    private boolean open = false;
    private String input = "";
    private List<ChatMessage> messages = new ArrayList<>();
    private Font font;
    private long backspaceTimer = 0;
    private boolean backspaceHeld = false;
    private String lastSentMessage = "";
    private long lastMessageTime = 0;
    private static final long MESSAGE_TIMEOUT = 5000;

    public Chat(Font font) {
        this.font = font;
    }

    public boolean isOpen() {
        return open;
    }

    public void openChat() {
        open = true;
        input = "";
    }

    public void closeChat() {
        open = false;
        input = "";
    }

    public void clearMessages() {
        messages.clear();
    }
    
    public void addMessage(String msg) {
        messages.add(new ChatMessage(msg, System.currentTimeMillis()));
        lastMessageTime = System.currentTimeMillis();
    }

    public void handleInput() {
        if (!open) return;

        while (Keyboard.next()) {
            if (Keyboard.getEventKeyState()) {
                int key = Keyboard.getEventKey();

                // Enter - отправить сообщение
                if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
                    if (!input.trim().isEmpty()) {
                        lastSentMessage = input.trim();
                    }
                    open = false;
                    return;
                }

                // Escape - закрыть чат
                if (key == Keyboard.KEY_ESCAPE) {
                    closeChat();
                    return;
                }

                // Backspace - удалить символ
                if (key == Keyboard.KEY_BACK) {
                    backspaceHeld = true;
                    backspaceTimer = System.currentTimeMillis();
                    doBackspace();
                    continue;
                }

                // Получаем символ напрямую с клавиатуры
                char c = Keyboard.getEventCharacter();
                
                // Игнорируем непечатаемые символы
                if (c != '\0' && c != '\n' && c != '\r' && c != '\b' && c != '\t' && input.length() < 100) {
                    input += c;
                }
            } else {
                // Клавиша отпущена
                if (Keyboard.getEventKey() == Keyboard.KEY_BACK) {
                    backspaceHeld = false;
                }
            }
        }

        // Автоповтор backspace при удержании
        if (backspaceHeld && System.currentTimeMillis() - backspaceTimer > 80) {
            backspaceTimer = System.currentTimeMillis();
            doBackspace();
        }
    }

    private void doBackspace() {
        if (input.length() > 0) {
            input = input.substring(0, input.length() - 1);
        }
    }

    public String getInput() {
        return lastSentMessage.isEmpty() ? input : lastSentMessage;
    }

    public void clearLastSent() {
        lastSentMessage = "";
    }

    public void update() {
        long now = System.currentTimeMillis();
        messages.removeIf(msg -> now - msg.time > MESSAGE_TIMEOUT);
    }

    public void render(int screenWidth, int screenHeight) {
        if (font == null) return;

        update();

        int scale = 2;
        GL11.glPushMatrix();
        GL11.glScalef(scale, scale, 1.0f);

        int sx = screenWidth / scale;
        int sy = screenHeight / scale;

        int chatX = 2;
        int chatY = sy - 60;
        int lineHeight = 11;

        // Рендер сообщений
        int msgY = chatY;
        int count = 0;
        for (int i = messages.size() - 1; i >= 0 && count < 10; i--) {
            font.drawShadow(messages.get(i).text, chatX, msgY, 0xFFFFFF);
            msgY -= lineHeight;
            count++;
        }

        // Рендер строки ввода
        if (open) {
            int inputY = chatY + 4;
            
            // Фон строки ввода
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glColor4f(0.0f, 0.0f, 0.0f, 0.6f);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2f(chatX, inputY - 2);
            GL11.glVertex2f(sx - 4, inputY - 2);
            GL11.glVertex2f(sx - 4, inputY + 12);
            GL11.glVertex2f(chatX, inputY + 12);
            GL11.glEnd();
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_TEXTURE_2D);

            // Текст ввода с мигающим курсором
            String display = "> " + input + (System.currentTimeMillis() % 1000 < 500 ? "_" : "");
            font.drawShadow(display, chatX + 2, inputY, 0xFFFF55);
        }

        GL11.glPopMatrix();
    }

    private static class ChatMessage {
        String text;
        long time;

        ChatMessage(String text, long time) {
            this.text = text;
            this.time = time;
        }
    }
}