package by.academy.project.service.business;

import by.academy.project.exception.TicketRepositoryException;
import by.academy.project.models.Ticket;
import by.academy.project.repository.TicketRepository;
import by.academy.project.service.files.FileService;
import java.util.List;
import java.util.Set;

public class TicketService implements TicketServiceInterface{
    private final TicketRepository ticketRepository;
    private final FileService fileService;

    public TicketService(TicketRepository ticketRepository, FileService fileService) {
        this.ticketRepository = ticketRepository;
        this.fileService = fileService;
    }

    public void createTicket(int movieId, int seat, double price) {
        try {
            ticketRepository.createTicket(movieId, seat, price);
        } catch (TicketRepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Ticket> getAvailableTickets(int movieId) {
        try {
            return ticketRepository.getAvailableTickets(movieId);
        } catch (TicketRepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Ticket> getUserTickets(int userId) {
        try {
            return ticketRepository.getUserTickets(userId);
        } catch (TicketRepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean buyTicket(int ticketId, int userId, String buyerUsername) {
        boolean result;
        try {
            result = ticketRepository.buyTicket(ticketId, userId);
        } catch (TicketRepositoryException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public boolean returnTicket(int ticketId, String returnerUsername) {
        boolean result;
        try {
            result = ticketRepository.returnTicket(ticketId);
        } catch (TicketRepositoryException e) {
            throw new RuntimeException(e);
        }
        if (result) {
            fileService.log(returnerUsername, "Вернул билет ID " + ticketId);
        }
        return result;
    }

    public boolean returnUserTicket(int ticketId, int userId, String managerName) {
        boolean result;
        try {
            result = ticketRepository.returnUserTicket(ticketId, userId);
        } catch (TicketRepositoryException e) {
            throw new RuntimeException(e);
        }
        if (result) {
            fileService.log(managerName, "Вернул билет " + ticketId + " у пользователя " + userId);
        }
        return result;
    }

    public int returnMultipleUserTickets(List<Integer> ticketIds, int userId, String managerName) {
        int returnedCount;
        try {
            returnedCount = ticketRepository.returnMultipleUserTickets(ticketIds, userId);
        } catch (TicketRepositoryException e) {
            throw new RuntimeException(e);
        }
        if (returnedCount > 0) {
            fileService.log(managerName, "Вернул " + returnedCount + " билетов у пользователя " + userId);
        }
        return returnedCount;
    }

    public int returnAllUserTickets(int userId, String managerName) {
        int returnedCount;
        try {
            returnedCount = ticketRepository.returnAllUserTickets(userId);
        } catch (TicketRepositoryException e) {
            throw new RuntimeException(e);
        }
        if (returnedCount > 0) {
            fileService.log(managerName, "Вернул все билеты (" + returnedCount + " шт.) у пользователя " + userId);
        }
        return returnedCount;
    }
    @Override
    public boolean addSeatsForMovie(int movieId, int newTotalSeats, double price) throws TicketRepositoryException {
        Set<Integer> existingSeats = ticketRepository.findSeatNumbersByMovieId(movieId);

        if (existingSeats.isEmpty()) {
            ticketRepository.addNewSeats(movieId, 1, newTotalSeats, price);
            return true;
        }

        int maxExistingSeat = existingSeats.stream().max(Integer::compareTo).orElse(0);

        if (newTotalSeats > maxExistingSeat) {
            ticketRepository.addNewSeats(movieId, maxExistingSeat + 1, newTotalSeats, price);
            return true;
        }
        return false;
    }
}