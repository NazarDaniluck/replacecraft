package net.replacecraft.network;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

public class ServerConnectGUI {
    
    private boolean visible = false;
    private String ipInput = "127.0.0.1";
    private String portInput = "25565";
    private String nameInput = "Player";
    private int selectedField = 0;
    private long backspaceTimer = 0;
    private boolean backspaceHeld = false;
    private long lastKeyTime = 0;
    
    private ConnectCallback callback;
    private Object fontRef;
    private boolean hasFont = false;
    
    public interface ConnectCallback {
        void onConnect(String ip, int port, String name);
    }
    
    public ServerConnectGUI(Object font, String defaultName) {
        this.fontRef = font;
        this.hasFont = (font != null);
        this.nameInput = (defaultName != null && !defaultName.isEmpty()) ? defaultName : "Player";
    }
    
    public void setCallback(ConnectCallback callback) {
        this.callback = callback;
    }
    
    public boolean isVisible() {
        return visible;
    }
    
    public void show() {
        visible = true;
        selectedField = 0;
        backspaceHeld = false;
        lastKeyTime = System.currentTimeMillis();
    }
    
    public void hide() {
        visible = false;
        backspaceHeld = false;
    }
    
    public void toggle() {
        if (visible) hide();
        else show();
    }
    
    public void handleInput() {
        if (!visible) return;
        
        // Обработка зажатого BACKSPACE
        if (backspaceHeld && System.currentTimeMillis() - backspaceTimer > 80) {
            backspaceTimer = System.currentTimeMillis();
            doBackspace();
        }
        
        while (Keyboard.next()) {
            long now = System.currentTimeMillis();
            // Защита от двойного срабатывания (игнорируем события чаще 30 мс)
            if (now - lastKeyTime < 30 && !Keyboard.getEventKeyState()) {
                continue;
            }
            
            if (Keyboard.getEventKeyState()) {
                lastKeyTime = now;
                int key = Keyboard.getEventKey();
                
                // Отладочный вывод
                System.out.println("[GUI] Key pressed: " + key + " (" + getKeyName(key) + ")");
                
                // ENTER — подключиться
                if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
                    doConnect();
                    return;
                }
                
                // ESC — закрыть меню
                if (key == Keyboard.KEY_ESCAPE) {
                    hide();
                    return;
                }
                
                // TAB — переключение полей
                if (key == Keyboard.KEY_TAB) {
                    selectedField = (selectedField + 1) % 4;
                    continue;
                }
                
                // Стрелка вверх
                if (key == Keyboard.KEY_UP) {
                    selectedField = (selectedField - 1 + 4) % 4;
                    continue;
                }
                // Стрелка вниз
                if (key == Keyboard.KEY_DOWN) {
                    selectedField = (selectedField + 1) % 4;
                    continue;
                }
                
                // BACKSPACE
                if (key == Keyboard.KEY_BACK) {
                    doBackspace();
                    backspaceHeld = true;
                    backspaceTimer = System.currentTimeMillis();
                    continue;
                }
                
                // Текстовый ввод
                if (selectedField < 3) {
                    char c = getCharFromKey(key);
                    if (c != 0) {
                        String current = getCurrentField();
                        if (current.length() < 20) {
                            setCurrentField(current + c);
                            System.out.println("[GUI] Field " + selectedField + " = " + getCurrentField());
                        }
                    }
                }
            } else {
                // Клавиша отпущена
                int key = Keyboard.getEventKey();
                if (key == Keyboard.KEY_BACK) {
                    backspaceHeld = false;
                }
            }
        }
    }
    
    private String getKeyName(int key) {
        // Для отладки
        switch (key) {
            case Keyboard.KEY_0: return "0";
            case Keyboard.KEY_1: return "1";
            case Keyboard.KEY_2: return "2";
            case Keyboard.KEY_3: return "3";
            case Keyboard.KEY_4: return "4";
            case Keyboard.KEY_5: return "5";
            case Keyboard.KEY_6: return "6";
            case Keyboard.KEY_7: return "7";
            case Keyboard.KEY_8: return "8";
            case Keyboard.KEY_9: return "9";
            case Keyboard.KEY_A: return "A";
            case Keyboard.KEY_B: return "B";
            case Keyboard.KEY_PERIOD: return ".";
            case Keyboard.KEY_BACK: return "BACKSPACE";
            case Keyboard.KEY_RETURN: return "ENTER";
            case Keyboard.KEY_TAB: return "TAB";
            case Keyboard.KEY_ESCAPE: return "ESC";
            default: return "Code=" + key;
        }
    }
    
    private void doBackspace() {
        String current = getCurrentField();
        if (current.length() > 0) {
            setCurrentField(current.substring(0, current.length() - 1));
        }
    }
    
    private String getCurrentField() {
        switch (selectedField) {
            case 0: return ipInput;
            case 1: return portInput;
            case 2: return nameInput;
            default: return "";
        }
    }
    
    private void setCurrentField(String value) {
        switch (selectedField) {
            case 0: ipInput = value; break;
            case 1: portInput = value; break;
            case 2: nameInput = value; break;
        }
    }
    
    private void doConnect() {
        try {
            int port = Integer.parseInt(portInput);
            if (port < 1 || port > 65535) port = 25565;
            if (callback != null) {
                callback.onConnect(ipInput, port, nameInput);
            }
            hide();
        } catch (NumberFormatException e) {
            portInput = "25565";
        }
    }
    
    private char getCharFromKey(int key) {
        // Цифры с ОСНОВНОЙ клавиатуры
        if (key == Keyboard.KEY_0) return '0';
        if (key == Keyboard.KEY_1) return '1';
        if (key == Keyboard.KEY_2) return '2';
        if (key == Keyboard.KEY_3) return '3';
        if (key == Keyboard.KEY_4) return '4';
        if (key == Keyboard.KEY_5) return '5';
        if (key == Keyboard.KEY_6) return '6';
        if (key == Keyboard.KEY_7) return '7';
        if (key == Keyboard.KEY_8) return '8';
        if (key == Keyboard.KEY_9) return '9';
        
        // Цифры с NUMPAD
        if (key == Keyboard.KEY_NUMPAD0) return '0';
        if (key == Keyboard.KEY_NUMPAD1) return '1';
        if (key == Keyboard.KEY_NUMPAD2) return '2';
        if (key == Keyboard.KEY_NUMPAD3) return '3';
        if (key == Keyboard.KEY_NUMPAD4) return '4';
        if (key == Keyboard.KEY_NUMPAD5) return '5';
        if (key == Keyboard.KEY_NUMPAD6) return '6';
        if (key == Keyboard.KEY_NUMPAD7) return '7';
        if (key == Keyboard.KEY_NUMPAD8) return '8';
        if (key == Keyboard.KEY_NUMPAD9) return '9';
        
        // Буквы A-Z
        if (key == Keyboard.KEY_A) return 'a';
        if (key == Keyboard.KEY_B) return 'b';
        if (key == Keyboard.KEY_C) return 'c';
        if (key == Keyboard.KEY_D) return 'd';
        if (key == Keyboard.KEY_E) return 'e';
        if (key == Keyboard.KEY_F) return 'f';
        if (key == Keyboard.KEY_G) return 'g';
        if (key == Keyboard.KEY_H) return 'h';
        if (key == Keyboard.KEY_I) return 'i';
        if (key == Keyboard.KEY_J) return 'j';
        if (key == Keyboard.KEY_K) return 'k';
        if (key == Keyboard.KEY_L) return 'l';
        if (key == Keyboard.KEY_M) return 'm';
        if (key == Keyboard.KEY_N) return 'n';
        if (key == Keyboard.KEY_O) return 'o';
        if (key == Keyboard.KEY_P) return 'p';
        if (key == Keyboard.KEY_Q) return 'q';
        if (key == Keyboard.KEY_R) return 'r';
        if (key == Keyboard.KEY_S) return 's';
        if (key == Keyboard.KEY_T) return 't';
        if (key == Keyboard.KEY_U) return 'u';
        if (key == Keyboard.KEY_V) return 'v';
        if (key == Keyboard.KEY_W) return 'w';
        if (key == Keyboard.KEY_X) return 'x';
        if (key == Keyboard.KEY_Y) return 'y';
        if (key == Keyboard.KEY_Z) return 'z';
        
        // Точка
        if (key == Keyboard.KEY_PERIOD || key == Keyboard.KEY_DECIMAL) return '.';
        
        // Минус
        if (key == Keyboard.KEY_MINUS) return '-';
        
        // Подчёркивание
        if (key == Keyboard.KEY_UNDERLINE) return '_';
        
        return 0;
    }
    
    public void render() {
        if (!visible) return;
        
        int dw = Display.getWidth();
        int dh = Display.getHeight();
        
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        GL11.glColor4f(0.0f, 0.0f, 0.0f, 0.6f);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(0, 0);
        GL11.glVertex2f(dw, 0);
        GL11.glVertex2f(dw, dh);
        GL11.glVertex2f(0, dh);
        GL11.glEnd();
        
        int menuW = 340;
        int menuH = 220;
        int mx = (dw - menuW) / 2;
        int my = (dh - menuH) / 2;
        
        GL11.glColor4f(0.2f, 0.2f, 0.2f, 0.95f);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(mx, my);
        GL11.glVertex2f(mx + menuW, my);
        GL11.glVertex2f(mx + menuW, my + menuH);
        GL11.glVertex2f(mx, my + menuH);
        GL11.glEnd();
        
        GL11.glColor4f(0.5f, 0.5f, 0.5f, 1.0f);
        GL11.glLineWidth(2.0f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(mx, my);
        GL11.glVertex2f(mx + menuW, my);
        GL11.glVertex2f(mx + menuW, my + menuH);
        GL11.glVertex2f(mx, my + menuH);
        GL11.glEnd();
        
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        
        int scale = 2;
        GL11.glPushMatrix();
        GL11.glScalef(scale, scale, 1.0f);
        
        int sx = mx / scale;
        int sy = my / scale;
        
        drawText("Connect to Server", sx + 20, sy + 10, 0xFFFF00);
        
        int yBase = sy + 35;
        int lineH = 28;
        
        drawField("IP Address:", ipInput, sx + 15, yBase, 0);
        drawField("Port:", portInput, sx + 15, yBase + lineH, 1);
        drawField("Username:", nameInput, sx + 15, yBase + lineH * 2, 2);
        
        int btnY = yBase + lineH * 3 + 10;
        boolean btnSelected = (selectedField == 3);
        drawText(btnSelected ? "> [ CONNECT ] <" : "  [ CONNECT ]  ", 
                sx + 75, btnY, btnSelected ? 0x55FF55 : 0xAAAAAA);
        
        drawText("TAB/Arrows - navigate | ENTER - connect | ESC - back", 
                sx + 10, btnY + 25, 0x888888);
        
        GL11.glPopMatrix();
    }
    
    private void drawField(String label, String value, int sx, int sy, int fieldIndex) {
        boolean selected = (selectedField == fieldIndex);
        drawText(label, sx, sy, selected ? 0xFFFFFF : 0xAAAAAA);
        
        String display = value;
        if (selected && System.currentTimeMillis() % 1000 < 500) {
            display = value + "_";
        }
        
        int color = selected ? 0xFFFFFF : 0xCCCCCC;
        drawText(display, sx + 100, sy, color);
    }
    
    private void drawText(String text, int x, int y, int color) {
        if (hasFont && fontRef != null) {
            try {
                java.lang.reflect.Method method = fontRef.getClass().getMethod("drawShadow", 
                    String.class, int.class, int.class, int.class);
                method.invoke(fontRef, text, x, y, color);
            } catch (Exception e) {}
        }
    }
}