package by.academy.project.service.business;

import by.academy.project.exception.UserRepositoryException;
import by.academy.project.models.User;
import by.academy.project.models.UserLevel;
import by.academy.project.repository.UserRepository;
import by.academy.project.service.files.FileService;

import java.util.List;

public class UserService implements UserServiceInterface  {
    private final UserRepository userRepository;
    private final FileService fileService;

    public UserService(UserRepository userRepository, FileService fileService) {
        this.userRepository = userRepository;
        this.fileService = fileService;
    }

    public User register(String login, String password, UserLevel level)
     throws UserRepositoryException
    {
        if (userRepository.isLoginTaken(login)) {
            return null;
        }

        User user = userRepository.register(login, password, level);

        if (user != null) {
            fileService.createFile(login);
            fileService.logRegister(login);
        }

        return user;
    }

    public User login(String login, String password) throws UserRepositoryException {
        User user = userRepository.login(login, password);

        if (user != null) {
            userRepository.loadWatchedMovies(user);
            fileService.logLogin(login);
        }

        return user;
    }

    public List<User> getAllUsers() {
        try {
            return userRepository.getAllUsers();
        } catch (UserRepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean deleteUser(int userId, String adminLogin) throws UserRepositoryException {
        boolean result = userRepository.deleteUser(userId);
        if (result) {
            fileService.log(adminLogin, "Удалил пользователя ID " + userId);
        }
        return result;
    }

    public boolean changeLevel(int userId, UserLevel newLevel, String adminLogin) throws UserRepositoryException {
        boolean result = userRepository.changeLevel(userId, newLevel);
        if (result) {
            fileService.log(adminLogin, "Изменил уровень пользователя " + userId + " на " + newLevel);
        }
        return result;
    }

    public void addWatchedMovie(int userId, int movieId) throws UserRepositoryException {
        userRepository.addWatchedMovie(userId, movieId);
    }
}