package by.academy.project.service.business;

import by.academy.project.exception.UserRepositoryException;
import by.academy.project.models.User;
import by.academy.project.models.UserLevel;
import java.util.List;

public interface UserServiceInterface {
    User register(String login, String password, UserLevel level) throws UserRepositoryException;
    User login(String login, String password) throws UserRepositoryException;
    List<User> getAllUsers();
    boolean deleteUser(int userId, String adminLogin) throws UserRepositoryException;
    boolean changeLevel(int userId, UserLevel newLevel, String adminLogin) throws UserRepositoryException;
    void addWatchedMovie(int userId, int movieId) throws UserRepositoryException;
}