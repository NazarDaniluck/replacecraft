package net.replacecraft;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import java.util.ArrayList;
import java.util.List;

public class Chat {
    private boolean open = false;
    private String input = "";
    private List<String> messages = new ArrayList<>();
    private Font font;
    private int maxMessages = 100;
    private long backspaceTimer = 0;
    private boolean backspaceHeld = false;
    private String lastSentMessage = "";
    private boolean russianLayout = true;

    public Chat(Font font) {
        this.font = font;
    }

    public boolean isOpen() { return open; }

    public void openChat() {
        open = true;
        input = "";
    }

    public void closeChat() {
        open = false;
        input = "";
    }

    public void addMessage(String msg) {
        messages.add(msg);
        if (messages.size() > maxMessages) {
            messages.remove(0);
        }
    }

    public void handleInput() {
        if (!open) return;

        while (Keyboard.next()) {
            if (Keyboard.getEventKeyState()) {
                int key = Keyboard.getEventKey();

                if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
                    if (!input.trim().isEmpty()) {
                        lastSentMessage = input.trim();
                    }
                    open = false;
                    return;
                }

                if (key == Keyboard.KEY_ESCAPE) {
                    closeChat();
                    return;
                }

                // Переключение раскладки Alt+Shift
                if (key == Keyboard.KEY_LMENU || key == Keyboard.KEY_RMENU) {
                    if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
                        russianLayout = !russianLayout;
                        continue;
                    }
                }

             // Переключение раскладки (Alt слева или справа)
                if (key == Keyboard.KEY_LMENU || key == Keyboard.KEY_RMENU) {
                    russianLayout = !russianLayout;
                    continue;
                }

                if (key == Keyboard.KEY_BACK) {
                    backspaceHeld = true;
                    backspaceTimer = System.currentTimeMillis();
                    doBackspace();
                    continue;
                }

                char c = getCharFromKey(key);
                if (c != 0 && input.length() < 100) {
                    input += c;
                }
            } else {
                if (Keyboard.getEventKey() == Keyboard.KEY_BACK) {
                    backspaceHeld = false;
                }
            }
        }

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

    private char getCharFromKey(int key) {
        boolean shift = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);

        if (russianLayout) {
            if (key == Keyboard.KEY_Q) return shift ? 'Й' : 'й';
            if (key == Keyboard.KEY_W) return shift ? 'Ц' : 'ц';
            if (key == Keyboard.KEY_E) return shift ? 'У' : 'у';
            if (key == Keyboard.KEY_R) return shift ? 'К' : 'к';
            if (key == Keyboard.KEY_T) return shift ? 'Е' : 'е';
            if (key == Keyboard.KEY_Y) return shift ? 'Н' : 'н';
            if (key == Keyboard.KEY_U) return shift ? 'Г' : 'г';
            if (key == Keyboard.KEY_I) return shift ? 'Ш' : 'ш';
            if (key == Keyboard.KEY_O) return shift ? 'Щ' : 'щ';
            if (key == Keyboard.KEY_P) return shift ? 'З' : 'з';
            if (key == Keyboard.KEY_LBRACKET) return shift ? 'Х' : 'х';
            if (key == Keyboard.KEY_RBRACKET) return shift ? 'Ъ' : 'ъ';
            if (key == Keyboard.KEY_A) return shift ? 'Ф' : 'ф';
            if (key == Keyboard.KEY_S) return shift ? 'Ы' : 'ы';
            if (key == Keyboard.KEY_D) return shift ? 'В' : 'в';
            if (key == Keyboard.KEY_F) return shift ? 'А' : 'а';
            if (key == Keyboard.KEY_G) return shift ? 'П' : 'п';
            if (key == Keyboard.KEY_H) return shift ? 'Р' : 'р';
            if (key == Keyboard.KEY_J) return shift ? 'О' : 'о';
            if (key == Keyboard.KEY_K) return shift ? 'Л' : 'л';
            if (key == Keyboard.KEY_L) return shift ? 'Д' : 'д';
            if (key == Keyboard.KEY_SEMICOLON) return shift ? 'Ж' : 'ж';
            if (key == Keyboard.KEY_APOSTROPHE) return shift ? 'Э' : 'э';
            if (key == Keyboard.KEY_Z) return shift ? 'Я' : 'я';
            if (key == Keyboard.KEY_X) return shift ? 'Ч' : 'ч';
            if (key == Keyboard.KEY_C) return shift ? 'С' : 'с';
            if (key == Keyboard.KEY_V) return shift ? 'М' : 'м';
            if (key == Keyboard.KEY_B) return shift ? 'И' : 'и';
            if (key == Keyboard.KEY_N) return shift ? 'Т' : 'т';
            if (key == Keyboard.KEY_M) return shift ? 'Ь' : 'ь';
            if (key == Keyboard.KEY_COMMA) return shift ? 'Б' : 'б';
            if (key == Keyboard.KEY_PERIOD) return shift ? 'Ю' : 'ю';
            if (key == Keyboard.KEY_SLASH) return shift ? ',' : '.';
        } else {
            if (key >= Keyboard.KEY_A && key <= Keyboard.KEY_Z) {
                char c = (char) ('a' + (key - Keyboard.KEY_A));
                return shift ? Character.toUpperCase(c) : c;
            }
            if (key == Keyboard.KEY_COMMA) return shift ? '<' : ',';
            if (key == Keyboard.KEY_PERIOD) return shift ? '>' : '.';
            if (key == Keyboard.KEY_SLASH) return shift ? '?' : '/';
            if (key == Keyboard.KEY_SEMICOLON) return shift ? ':' : ';';
            if (key == Keyboard.KEY_APOSTROPHE) return shift ? '"' : '\'';
            if (key == Keyboard.KEY_LBRACKET) return shift ? '{' : '[';
            if (key == Keyboard.KEY_RBRACKET) return shift ? '}' : ']';
            if (key == Keyboard.KEY_BACKSLASH) return shift ? '|' : '\\';
            if (key == Keyboard.KEY_GRAVE) return shift ? '~' : '`';
        }

        if (key >= Keyboard.KEY_0 && key <= Keyboard.KEY_9) {
            if (shift) {
                char[] shifted = {')', '!', '"', '№', ';', '%', ':', '?', '*', '('};
                return shifted[key - Keyboard.KEY_0];
            }
            return (char) ('0' + (key - Keyboard.KEY_0));
        }

        if (key == Keyboard.KEY_SPACE) return ' ';
        if (key == Keyboard.KEY_MINUS) return shift ? '_' : '-';
        if (key == Keyboard.KEY_EQUALS) return shift ? '+' : '=';

        return 0;
    }

    public void render(int screenWidth, int screenHeight) {
        if (font == null) return;

        int scale = 2;
        GL11.glPushMatrix();
        GL11.glScalef(scale, scale, 1.0f);

        int sx = screenWidth / scale;
        int sy = screenHeight / scale;

        int chatX = 2;
        int chatY = sy - 100;
        int lineHeight = 11;

        int msgY = chatY;
        int startIdx = Math.max(0, messages.size() - 14);
        for (int i = messages.size() - 1; i >= startIdx && msgY > 10; i--) {
            font.drawShadow(messages.get(i), chatX, msgY, 0xFFFFFF);
            msgY -= lineHeight;
        }

        if (open) {
            int inputY = chatY + 4;
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

            String display = "> " + input + (System.currentTimeMillis() % 1000 < 500 ? "_" : "");
            font.drawShadow(display, chatX + 2, inputY, 0xFFFF55);
        }

        GL11.glPopMatrix();
    }
}