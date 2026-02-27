package by.academy.project.service.files;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileService implements FileServiceInterface{
    private static final String LOGS_DIR = "src/by/academy/project/logs/";

    public FileService() {
        new File(LOGS_DIR).mkdirs();
    }

    public void createFile(String username) {
        String filename = LOGS_DIR + username + ".txt";
        try {
            File file = new File(filename);
            boolean created = file.createNewFile();
            if (created) {
                addFileToGit(filename);
            }
        } catch (IOException e) {
            System.out.println("Не создал файл: " + e.getMessage());
        }
    }

    public void showLogs(String username) {
        String filename = LOGS_DIR + username + ".txt";
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            System.out.println("\n=== Логи " + username + " ===");
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
            System.out.println("===================\n");
        } catch (IOException e) {
            System.out.println("Нет логов у " + username);
        }
    }

    public void log(String username, String action) {
        String filename = LOGS_DIR + username + ".txt";
        try (PrintWriter out = new PrintWriter(new FileWriter(filename, true))) {
            LocalDateTime now = LocalDateTime.now();
            String time = now.format(DateTimeFormatter.ofPattern("dd.MM.yy / HH:mm"));
            out.println("\t" + action + " (Время: " + time + ")");
            commitFileToGit(filename);
        } catch (IOException e) {
            System.out.println("Не записал лог: " + e.getMessage());
        }
    }

    public void logRegister(String username) {
        log(username, "Зарегистрировался");
    }

    public void logLogin(String username) {
        log(username, "Вошел в систему");
    }

    public void logExit(String username) {
        log(username, "Вышел из системы");
    }

    // Git
    private void addFileToGit(String filePath) {
        try {
            String absolutePath = new File(filePath).getAbsolutePath();
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("cmd.exe", "/c", "git", "add", absolutePath);

            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                System.err.println("Git add failed for: " + filePath);
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Ошибка при добавлении файла в Git: " + e.getMessage());
        }
    }

    private void commitFileToGit(String filePath) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            String message = "Update log file: " + new File(filePath).getName();
            processBuilder.command("cmd.exe", "/c", "git", "commit", "-m", message, filePath);

            processBuilder.start();
        } catch (IOException e) {
            // Игнорируем ошибки
        }
    }
}