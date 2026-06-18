package net.replacecraft.debug;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.lwjgl.input.Mouse;
import net.replacecraft.level.Level;
public class WorldIO {

    // Сохранение мира через диалоговое окно
    public static void saveWorld(Level level) {
        boolean wasGrabbed = Mouse.isGrabbed();
        if (wasGrabbed) Mouse.setGrabbed(false);

        try {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Сохранить карту ReplaceCraft");
            fileChooser.setFileFilter(new FileNameExtensionFilter("Файлы карт ReplaceCraft (*.rc)", "rc"));
            
            int userSelection = fileChooser.showSaveDialog(null);
            
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                String filePath = fileToSave.getAbsolutePath();
                
                if (!filePath.toLowerCase().endsWith(".rc")) {
                    fileToSave = new File(filePath + ".rc");
                }

                // Прямой доступ к блокам без рефлексии!
                byte[] blocks = level.getBlocks();

                try (FileOutputStream fos = new FileOutputStream(fileToSave)) {
                    fos.write(blocks);
                }
                System.out.println("Мир успешно сохранен в: " + fileToSave.getAbsolutePath());
            }
        } catch (Exception e) {
            System.out.println("Ошибка сохранения мира через окно!");
            e.printStackTrace();
        } finally {
            if (wasGrabbed) Mouse.setGrabbed(true);
        }
    }

    // Загрузка мира через диалоговое окно
    public static boolean loadWorld(Level level) {
        boolean wasGrabbed = Mouse.isGrabbed();
        if (wasGrabbed) Mouse.setGrabbed(false);

        try {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Открыть карту ReplaceCraft");
            fileChooser.setFileFilter(new FileNameExtensionFilter("Файлы карт ReplaceCraft (*.rc)", "rc"));
            
            int userSelection = fileChooser.showOpenDialog(null);
            
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToLoad = fileChooser.getSelectedFile();
                
                if (!fileToLoad.exists()) {
                    System.out.println("Выбранный файл не существует!");
                    return false;
                }

                byte[] blocks = level.getBlocks();

                try (FileInputStream fis = new FileInputStream(fileToLoad)) {
                    int bytesRead = fis.read(blocks);
                    if (bytesRead != blocks.length) {
                        System.out.println("Внимание: Размер файла (" + bytesRead + 
                                         ") не совпадает с размером мира (" + blocks.length + ")!");
                    }
                }
                
                // Оповещаем рендерер о перестройке
                level.loadBlocks(blocks);
                
                System.out.println("Мир успешно загружен из: " + fileToLoad.getAbsolutePath());
                return true;
            }
        } catch (Exception e) {
            System.out.println("Ошибка загрузки мира через окно!");
            e.printStackTrace();
        } finally {
            if (wasGrabbed) Mouse.setGrabbed(true);
        }
        return false;
    }
}