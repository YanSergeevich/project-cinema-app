package by.academy.project.service.business;

import by.academy.project.exception.TicketRepositoryException;
import by.academy.project.models.Ticket;
import java.util.List;

public interface TicketServiceInterface {
    List<Ticket> getAvailableTickets(int movieId);
    List<Ticket> getUserTickets(int userId);
    boolean buyTicket(int ticketId, int userId, String buyerUsername);
    boolean returnTicket(int ticketId, String returnerUsername);
    boolean returnUserTicket(int ticketId, int userId, String managerName);
    int returnMultipleUserTickets(List<Integer> ticketIds, int userId, String managerName);
    int returnAllUserTickets(int userId, String managerName);
    void createTicket(int movieId, int seat, double price);
    boolean addSeatsForMovie(int movieId, int newTotalSeats, double price) throws TicketRepositoryException;
}