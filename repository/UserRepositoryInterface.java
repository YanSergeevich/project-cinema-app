package by.academy.project.repository;

import by.academy.project.exception.UserRepositoryException;
import by.academy.project.models.User;
import by.academy.project.models.UserLevel;
import java.util.List;

public interface UserRepositoryInterface {
    User register(String login, String password, UserLevel level) throws UserRepositoryException;
    boolean isLoginTaken(String login) throws UserRepositoryException;
    User login(String login, String password) throws UserRepositoryException;
    void loadWatchedMovies(User user) throws UserRepositoryException;
    List<User> getAllUsers() throws UserRepositoryException;
    boolean deleteUser(int userId) throws UserRepositoryException;
    boolean changeLevel(int userId, UserLevel newLevel) throws UserRepositoryException;
    void addWatchedMovie(int userId, int movieId) throws UserRepositoryException;
}