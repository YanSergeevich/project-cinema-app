package by.academy.project.repository;

import by.academy.project.exception.TicketRepositoryException;
import by.academy.project.models.Ticket;
import java.util.List;
import java.util.Set;

public interface TicketRepositoryInterface {
    void createTicket(int movieId, int seat, double price) throws TicketRepositoryException;
    List<Ticket> getAvailableTickets(int movieId) throws TicketRepositoryException;
    List<Ticket> getUserTickets(int userId) throws TicketRepositoryException;
    boolean buyTicket(int ticketId, int userId) throws TicketRepositoryException;
    boolean returnTicket(int ticketId) throws TicketRepositoryException;
    boolean returnUserTicket(int ticketId, int userId) throws TicketRepositoryException;
    int returnMultipleUserTickets(List<Integer> ticketIds, int userId) throws TicketRepositoryException;
    int returnAllUserTickets(int userId) throws TicketRepositoryException;
    void deleteFreeTicketsAboveSeatNumber(int movieId, int maxSeat) throws TicketRepositoryException;
    void addMissingTickets(int movieId, int requiredSeats, double price) throws TicketRepositoryException;
    void updatePriceForMovie(int movieId, double newPrice) throws TicketRepositoryException;
    Set<Integer> findSeatNumbersByMovieId(int movieId) throws TicketRepositoryException;
    void addNewSeats(int movieId, int startSeat, int endSeat, double price) throws TicketRepositoryException;
    boolean hasSoldSeatsAbove(int movieId, int seatNumber) throws TicketRepositoryException;
    void deleteFreeSeatsAbove(int movieId, int seatNumber) throws TicketRepositoryException;
    void deleteTicket(int movieId, int seatNumber) throws TicketRepositoryException;
}