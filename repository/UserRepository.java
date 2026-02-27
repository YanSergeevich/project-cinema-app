package by.academy.project.repository;

import by.academy.project.exception.UserRepositoryException;
import by.academy.project.models.User;
import by.academy.project.models.UserLevel;
import by.academy.project.service.infra.PasswordHasher;
import by.academy.project.service.infra.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserRepository implements UserRepositoryInterface {

    public User register(String login, String password, UserLevel level) throws UserRepositoryException {
        String sql = "INSERT INTO users (login, password, user_level) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, login);
            stmt.setString(2, PasswordHasher.hash(password));
            stmt.setString(3, level.toString());

            if (stmt.executeUpdate() > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt(1));
                    user.setLogin(login);
                    user.setUserLevel(level);
                    return user;
                }
            }
        } catch (SQLException e) {
            throw new UserRepositoryException("Ошибка регистрации: " + e.getMessage());
        }
        return null;
    }

    public boolean isLoginTaken(String login) throws UserRepositoryException {
        String sql = "SELECT id FROM users WHERE login = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, login);
            ResultSet rs = stmt.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            throw new UserRepositoryException("Ошибка проверки логина: " + e.getMessage());
        }
    }

    public User login(String login, String password) throws UserRepositoryException {
        String sql = "SELECT * FROM users WHERE login = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, login);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password");
                if (PasswordHasher.check(password, storedHash)) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setLogin(rs.getString("login"));
                    user.setUserLevel(UserLevel.valueOf(rs.getString("user_level")));
                    return user;
                }
            }
            return null;
        } catch (SQLException e) {
            throw new UserRepositoryException("Ошибка входа: " + e.getMessage());
        }
    }

    public void loadWatchedMovies(User user) throws UserRepositoryException {
        String sql = "SELECT m.* FROM watched_movies wm " +
                "JOIN movies m ON wm.movie_id = m.id " +
                "WHERE wm.user_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, user.getId());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                by.academy.project.models.Movie movie = new by.academy.project.models.Movie();
                movie.setId(rs.getInt("id"));
                movie.setTitle(rs.getString("title"));
                movie.setDateTime(rs.getTimestamp("date_time").toLocalDateTime());
                movie.setDurationMinutes(rs.getInt("duration_minutes"));
                movie.setTotalSeats(rs.getInt("total_seats"));

                user.addWatchedMovie(movie);
            }

        } catch (SQLException e) {
            throw new UserRepositoryException("Ошибка загрузки просмотренных фильмов: " + e.getMessage());
        }
    }

    public List<User> getAllUsers() throws UserRepositoryException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setLogin(rs.getString("login"));
                user.setUserLevel(UserLevel.valueOf(rs.getString("user_level")));
                users.add(user);
            }
            return users;
        } catch (SQLException e) {
            throw new UserRepositoryException("Ошибка получения пользователей: " + e.getMessage());
        }
    }

    public boolean deleteUser(int userId) throws UserRepositoryException {
        String sql = "DELETE FROM users WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new UserRepositoryException("Ошибка удаления: " + e.getMessage());
        }
    }

    public boolean changeLevel(int userId, UserLevel newLevel) throws UserRepositoryException {
        String sql = "UPDATE users SET user_level = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newLevel.toString());
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new UserRepositoryException("Ошибка изменения уровня: " + e.getMessage());
        }
    }

    public void addWatchedMovie(int userId, int movieId) throws UserRepositoryException {
        String sql = "INSERT IGNORE INTO watched_movies (user_id, movie_id) VALUES (?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, movieId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new UserRepositoryException("Ошибка добавления просмотренного фильма: " + e.getMessage());
        }
    }
}