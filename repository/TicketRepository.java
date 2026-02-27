package by.academy.project.repository;

import by.academy.project.exception.TicketRepositoryException;
import by.academy.project.models.Ticket;
import by.academy.project.service.infra.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TicketRepository implements TicketRepositoryInterface {

    public void createTicket(int movieId, int seat, double price) throws TicketRepositoryException {
        String sql = "INSERT INTO tickets (movie_id, seat_number, price) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE movie_id = movie_id";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, movieId);
            stmt.setInt(2, seat);
            stmt.setDouble(3, price);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new TicketRepositoryException("Ошибка создания билета: " + e.getMessage());
        }
    }

    public List<Ticket> getAvailableTickets(int movieId) throws TicketRepositoryException {
        List<Ticket> tickets = new ArrayList<>();
        String sql = "SELECT * FROM tickets WHERE movie_id = ? AND is_purchased = FALSE ORDER BY seat_number";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, movieId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Ticket ticket = new Ticket();
                ticket.setId(rs.getInt("id"));
                ticket.setMovieId(movieId);
                ticket.setSeatNumber(rs.getInt("seat_number"));
                ticket.setPrice(rs.getDouble("price"));
                ticket.setPurchased(rs.getBoolean("is_purchased"));
                tickets.add(ticket);
            }
            return tickets;

        } catch (SQLException e) {
            throw new TicketRepositoryException("Ошибка получения доступных билетов: " + e.getMessage());
        }
    }

    public List<Ticket> getUserTickets(int userId) throws TicketRepositoryException {
        List<Ticket> tickets = new ArrayList<>();
        String sql = "SELECT * FROM tickets WHERE user_id = ? AND is_purchased = TRUE";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Ticket ticket = new Ticket();
                ticket.setId(rs.getInt("id"));
                ticket.setMovieId(rs.getInt("movie_id"));
                ticket.setSeatNumber(rs.getInt("seat_number"));
                ticket.setPrice(rs.getDouble("price"));
                ticket.setPurchased(true);
                ticket.setUserId(userId);
                tickets.add(ticket);
            }
            return tickets;

        } catch (SQLException e) {
            throw new TicketRepositoryException("Ошибка получения билетов пользователя: " + e.getMessage());
        }
    }

    public boolean buyTicket(int ticketId, int userId) throws TicketRepositoryException {
        String sql = "UPDATE tickets SET is_purchased = TRUE, user_id = ? WHERE id = ? AND is_purchased = FALSE";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, ticketId);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new TicketRepositoryException("Ошибка покупки билета: " + e.getMessage());
        }
    }

    public boolean returnTicket(int ticketId) throws TicketRepositoryException {
        String sql = "UPDATE tickets SET is_purchased = FALSE, user_id = NULL WHERE id = ? AND is_purchased = TRUE";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, ticketId);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new TicketRepositoryException("Ошибка возврата билета: " + e.getMessage());
        }
    }

    public boolean returnUserTicket(int ticketId, int userId) throws TicketRepositoryException {
        String sql = "UPDATE tickets SET user_id = NULL, is_purchased = FALSE WHERE id = ? AND user_id = ? AND is_purchased = TRUE";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, ticketId);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new TicketRepositoryException("Ошибка возврата билета пользователя: " + e.getMessage());
        }
    }

    public int returnMultipleUserTickets(List<Integer> ticketIds, int userId) throws TicketRepositoryException {
        if (ticketIds == null || ticketIds.isEmpty()) {
            return 0;
        }

        int returnedCount = 0;
        String sql = "UPDATE tickets SET user_id = NULL, is_purchased = FALSE WHERE id = ? AND user_id = ? AND is_purchased = TRUE";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (int ticketId : ticketIds) {
                stmt.setInt(1, ticketId);
                stmt.setInt(2, userId);
                stmt.addBatch();
            }

            int[] results = stmt.executeBatch();

            for (int result : results) {
                if (result > 0) {
                    returnedCount++;
                }
            }
            return returnedCount;

        } catch (SQLException e) {
            throw new TicketRepositoryException("Ошибка возврата нескольких билетов: " + e.getMessage());
        }
    }

    public int returnAllUserTickets(int userId) throws TicketRepositoryException {
        String sql = "UPDATE tickets SET user_id = NULL, is_purchased = FALSE WHERE user_id = ? AND is_purchased = TRUE";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            return stmt.executeUpdate();

        } catch (SQLException e) {
            throw new TicketRepositoryException("Ошибка возврата всех билетов: " + e.getMessage());
        }
    }

    public void deleteFreeTicketsAboveSeatNumber(int movieId, int maxSeat) throws TicketRepositoryException {
        String sql = "DELETE FROM tickets WHERE movie_id = ? AND seat_number > ? AND is_purchased = FALSE";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, movieId);
            stmt.setInt(2, maxSeat);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new TicketRepositoryException("Ошибка удаления свободных билетов: " + e.getMessage());
        }
    }

    public void addMissingTickets(int movieId, int requiredSeats, double price) throws TicketRepositoryException {
        String sql = "INSERT INTO tickets (movie_id, seat_number, price, is_purchased) VALUES (?, ?, ?, FALSE)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (int seat = 1; seat <= requiredSeats; seat++) {
                stmt.setInt(1, movieId);
                stmt.setInt(2, seat);
                stmt.setDouble(3, price);
                stmt.addBatch();
            }
            int[] results = stmt.executeBatch();

        } catch (SQLException e) {
            throw new TicketRepositoryException("Ошибка добавления недостающих билетов: " + e.getMessage());
        }
    }

    public void updatePriceForMovie(int movieId, double newPrice) throws TicketRepositoryException {
        String sql = "UPDATE tickets SET price = ? WHERE movie_id = ? AND is_purchased = FALSE";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, newPrice);
            stmt.setInt(2, movieId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new TicketRepositoryException("Ошибка обновления цены билетов: " + e.getMessage());
        }
    }
    @Override
    public Set<Integer> findSeatNumbersByMovieId(int movieId) throws TicketRepositoryException {
        String sql = "SELECT seat_number FROM tickets WHERE movie_id = ?";
        Set<Integer> seats = new HashSet<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, movieId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                seats.add(rs.getInt("seat_number"));
            }
            return seats;

        } catch (SQLException e) {
            throw new TicketRepositoryException("Ошибка получения мест: " + e.getMessage());
        }
    }

    @Override
    public void addNewSeats(int movieId, int startSeat, int endSeat, double price) throws TicketRepositoryException {
        String sql = "INSERT INTO tickets (movie_id, seat_number, price, is_purchased) VALUES (?, ?, ?, FALSE)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (int seatNum = startSeat; seatNum <= endSeat; seatNum++) {
                stmt.setInt(1, movieId);
                stmt.setInt(2, seatNum);
                stmt.setDouble(3, price);
                stmt.addBatch();
            }
            stmt.executeBatch();

        } catch (SQLException e) {
            throw new TicketRepositoryException("Ошибка добавления мест: " + e.getMessage());
        }
    }

    @Override
    public boolean hasSoldSeatsAbove(int movieId, int seatNumber) throws TicketRepositoryException {
        String sql = "SELECT COUNT(*) FROM tickets WHERE movie_id = ? AND is_purchased = TRUE AND seat_number > ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, movieId);
            stmt.setInt(2, seatNumber);
            ResultSet rs = stmt.executeQuery();

            return rs.next() && rs.getInt(1) > 0;

        } catch (SQLException e) {
            throw new TicketRepositoryException("Ошибка проверки проданных мест: " + e.getMessage());
        }
    }

    @Override
    public void deleteFreeSeatsAbove(int movieId, int seatNumber) throws TicketRepositoryException {
        String sql = "DELETE FROM tickets WHERE movie_id = ? AND seat_number > ? AND is_purchased = FALSE";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, movieId);
            stmt.setInt(2, seatNumber);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new TicketRepositoryException("Ошибка удаления мест: " + e.getMessage());
        }
    }
    public void deleteTicket(int movieId, int seatNumber) throws TicketRepositoryException {
        String sql = "DELETE FROM tickets WHERE movie_id = ? AND seat_number = ? AND is_purchased = FALSE";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, movieId);
            stmt.setInt(2, seatNumber);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new TicketRepositoryException("Ошибка удаления билета: " + e.getMessage());
        }
    }
}