package by.academy.project.service.files;

import java.util.List;

public interface FileServiceInterface {
    void log(String username, String action);
    void logRegister(String username);
    void logLogin(String username);
    void logExit(String username);
    void createFile(String username);
    void showLogs(String username);
}