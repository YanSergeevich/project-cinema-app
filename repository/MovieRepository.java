package by.academy.project.repository;

import by.academy.project.exception.MovieRepositoryException;
import by.academy.project.models.Movie;
import by.academy.project.service.infra.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MovieRepository implements MovieRepositoryInterface  {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Movie create(String title, LocalDateTime dateTime, int durationMinutes, int seats, double price) throws MovieRepositoryException {
        String sql = "INSERT INTO movies (title, date_time, duration_minutes, total_seats, ticket_price) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, title);
            stmt.setString(2, dateTime.format(formatter));
            stmt.setInt(3, durationMinutes);
            stmt.setInt(4, seats);
            stmt.setDouble(5, price);

            if (stmt.executeUpdate() > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    int movieId = rs.getInt(1);
                    Movie movie = new Movie(title, dateTime, durationMinutes, seats);
                    movie.setId(movieId);
                    return movie;
                }
            }
            return null;
        } catch (SQLException e) {
            throw new MovieRepositoryException("Ошибка создания фильма: " + e.getMessage());
        }
    }

    public List<Movie> getAll() throws MovieRepositoryException {
        List<Movie> movies = new ArrayList<>();
        String sql = "SELECT * FROM movies WHERE DATE_ADD(date_time, INTERVAL duration_minutes MINUTE) > NOW() ORDER BY date_time";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Movie movie = new Movie();
                movie.setId(rs.getInt("id"));
                movie.setTitle(rs.getString("title"));
                movie.setDateTime(rs.getTimestamp("date_time").toLocalDateTime());
                movie.setDurationMinutes(rs.getInt("duration_minutes"));
                movie.setTotalSeats(rs.getInt("total_seats"));
                movies.add(movie);
            }
            return movies;
        } catch (SQLException e) {
            throw new MovieRepositoryException("Ошибка получения фильмов: " + e.getMessage());
        }
    }

    public List<Movie> getAllIncludingFinished() throws MovieRepositoryException {
        List<Movie> movies = new ArrayList<>();
        String sql = "SELECT * FROM movies ORDER BY date_time";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Movie movie = new Movie();
                movie.setId(rs.getInt("id"));
                movie.setTitle(rs.getString("title"));
                movie.setDateTime(rs.getTimestamp("date_time").toLocalDateTime());
                movie.setDurationMinutes(rs.getInt("duration_minutes"));
                movie.setTotalSeats(rs.getInt("total_seats"));
                movies.add(movie);
            }
            return movies;
        } catch (SQLException e) {
            throw new MovieRepositoryException("Ошибка получения всех фильмов: " + e.getMessage());
        }
    }

    public Movie getById(int id) throws MovieRepositoryException {
        String sql = "SELECT * FROM movies WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Movie movie = new Movie();
                movie.setId(rs.getInt("id"));
                movie.setTitle(rs.getString("title"));
                movie.setDateTime(rs.getTimestamp("date_time").toLocalDateTime());
                movie.setDurationMinutes(rs.getInt("duration_minutes"));
                movie.setTotalSeats(rs.getInt("total_seats"));
                return movie;
            }
            return null;
        } catch (SQLException e) {
            throw new MovieRepositoryException("Ошибка поиска фильма: " + e.getMessage());
        }
    }

    public boolean delete(int id) throws MovieRepositoryException {
        String sql = "DELETE FROM movies WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new MovieRepositoryException("Ошибка удаления фильма: " + e.getMessage());
        }
    }

    public List<Object[]> getFinishedMoviesToAdd() throws MovieRepositoryException {
        List<Object[]> results = new ArrayList<>();
        String sql = "SELECT t.user_id, t.movie_id " +
                "FROM tickets t " +
                "JOIN movies m ON t.movie_id = m.id " +
                "WHERE t.is_purchased = TRUE " +
                "AND DATE_ADD(m.date_time, INTERVAL m.duration_minutes MINUTE) < NOW() " +
                "AND NOT EXISTS (" +
                "    SELECT 1 FROM watched_movies wm " +
                "    WHERE wm.user_id = t.user_id AND wm.movie_id = t.movie_id" +
                ")";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int userId = rs.getInt("user_id");
                int movieId = rs.getInt("movie_id");
                results.add(new Object[]{userId, movieId});
            }
            return results;

        } catch (SQLException e) {
            throw new MovieRepositoryException("Ошибка проверки завершенных фильмов: " + e.getMessage());
        }
    }

    public List<Movie> getWatchedMovies(int userId) throws MovieRepositoryException {
        List<Movie> movies = new ArrayList<>();
        String sql = "SELECT m.* FROM watched_movies wm " +
                "JOIN movies m ON wm.movie_id = m.id " +
                "WHERE wm.user_id = ? ORDER BY wm.watched_date DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Movie movie = new Movie();
                movie.setId(rs.getInt("id"));
                movie.setTitle(rs.getString("title"));
                movie.setDateTime(rs.getTimestamp("date_time").toLocalDateTime());
                movie.setDurationMinutes(rs.getInt("duration_minutes"));
                movie.setTotalSeats(rs.getInt("total_seats"));
                movies.add(movie);
            }
            return movies;

        } catch (SQLException e) {
            throw new MovieRepositoryException("Ошибка получения просмотренных фильмов: " + e.getMessage());
        }
    }

    public boolean updateTitle(int movieId, String newTitle) throws MovieRepositoryException {
        String sql = "UPDATE movies SET title = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newTitle);
            stmt.setInt(2, movieId);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new MovieRepositoryException("Ошибка обновления названия фильма: " + e.getMessage());
        }
    }

    public boolean updateDateTime(int movieId, LocalDateTime newDateTime) throws MovieRepositoryException {
        String sql = "UPDATE movies SET date_time = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newDateTime.format(formatter));
            stmt.setInt(2, movieId);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new MovieRepositoryException("Ошибка обновления даты фильма: " + e.getMessage());
        }
    }

    public boolean updateDuration(int movieId, int newDuration) throws MovieRepositoryException {
        String sql = "UPDATE movies SET duration_minutes = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, newDuration);
            stmt.setInt(2, movieId);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new MovieRepositoryException("Ошибка обновления длительности фильма: " + e.getMessage());
        }
    }

    public boolean updateSeats(int movieId, int newSeats) throws MovieRepositoryException {
        String sql = "UPDATE movies SET total_seats = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, newSeats);
            stmt.setInt(2, movieId);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new MovieRepositoryException("Ошибка обновления количества мест: " + e.getMessage());
        }
    }

    public boolean updatePrice(int movieId, double newPrice) throws MovieRepositoryException {
        String sql = "UPDATE movies SET ticket_price = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, newPrice);
            stmt.setInt(2, movieId);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new MovieRepositoryException("Ошибка обновления цены фильма: " + e.getMessage());
        }
    }

    public Set<Integer> getSoldSeats(int movieId) throws MovieRepositoryException {
        Set<Integer> soldSeats = new HashSet<>();
        String sql = "SELECT seat_number FROM tickets WHERE movie_id = ? AND is_purchased = TRUE";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, movieId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                soldSeats.add(rs.getInt("seat_number"));
            }
            return soldSeats;
        } catch (SQLException e) {
            throw new MovieRepositoryException("Ошибка получения проданных мест: " + e.getMessage());
        }
    }

    public Set<Integer> getAllSeats(int movieId) throws MovieRepositoryException {
        Set<Integer> allSeats = new HashSet<>();
        String sql = "SELECT seat_number FROM tickets WHERE movie_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, movieId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                allSeats.add(rs.getInt("seat_number"));
            }
            return allSeats;
        } catch (SQLException e) {
            throw new MovieRepositoryException("Ошибка получения всех мест: " + e.getMessage());
        }
    }

    public double getTicketPrice(int movieId) throws MovieRepositoryException {
        String sql = "SELECT price FROM tickets WHERE movie_id = ? LIMIT 1";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, movieId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getDouble("price");
            }
            return 100.0;
        } catch (SQLException e) {
            throw new MovieRepositoryException("Ошибка получения цены: " + e.getMessage());
        }
    }
}