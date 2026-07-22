package net.replacecraft;

import javax.swing.*;

public class UsernameDialog {
    
    private static final int MAX_LENGTH = 16;
    
    public static String askUsername() {
        String saved = loadSavedUsername();
        
        // Проверяем сохранённый ник
        if (saved != null && !saved.isEmpty()) {
            if (isValidUsername(saved)) {
                System.out.println("Username loaded: " + saved);
                return saved;
            } else {
                // Ник повреждён — удаляем файл и показываем окно
                System.err.println("Invalid username in file: '" + saved + "'. Resetting...");
                deleteSavedUsername();
            }
        }
        
        // Показываем окно ввода
        String name = null;
        while (name == null || !isValidUsername(name)) {
            JPanel panel = new JPanel();
            panel.add(new JLabel("Enter your username (3-16 chars, A-Z 0-9 _):"));
            JTextField nameField = new JTextField(name != null ? name : "Player", 16);
            panel.add(nameField);
            
            int result = JOptionPane.showConfirmDialog(null, panel, 
                "ReplaceCraft - Login", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            
            if (result != JOptionPane.OK_OPTION) {
                saveUsername("Player");
                return "Player";
            }
            
            name = nameField.getText().trim();
            
            if (!isValidUsername(name)) {
                JOptionPane.showMessageDialog(null,
                    "Invalid username!\n" +
                    "Length: 3-16 characters\n" +
                    "Allowed: A-Z, a-z, 0-9, _",
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        
        saveUsername(name);
        return name;
    }
    
    /**
     * Проверяет валидность никнейма
     */
    public static boolean isValidUsername(String name) {
        if (name == null || name.isEmpty()) return false;
        
        // Длина от 3 до 16 символов
        if (name.length() < 3 || name.length() > MAX_LENGTH) return false;
        
        // Только A-Z, a-z, 0-9, _
        for (char c : name.toCharArray()) {
            if (!(c >= 'A' && c <= 'Z') && 
                !(c >= 'a' && c <= 'z') && 
                !(c >= '0' && c <= '9') && 
                c != '_') {
                return false;
            }
        }
        
        return true;
    }
    
    private static String loadSavedUsername() {
        try {
            java.io.File file = new java.io.File("username.txt");
            if (file.exists()) {
                String content = new String(java.nio.file.Files.readAllBytes(file.toPath())).trim();
                // Убираем переводы строк (если вставили стих)
                content = content.replaceAll("[\\r\\n]", "");
                return content;
            }
        } catch (Exception e) {}
        return null;
    }
    
    private static void saveUsername(String name) {
        try {
            java.nio.file.Files.write(new java.io.File("username.txt").toPath(), name.getBytes());
            System.out.println("Username saved: " + name);
        } catch (Exception e) {
            System.err.println("Failed to save username: " + e.getMessage());
        }
    }
    
    private static void deleteSavedUsername() {
        try {
            java.io.File file = new java.io.File("username.txt");
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {}
    }
}