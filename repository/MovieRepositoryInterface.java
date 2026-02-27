package by.academy.project.repository;

import by.academy.project.exception.MovieRepositoryException;
import by.academy.project.models.Movie;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface MovieRepositoryInterface {
    Movie create(String title, LocalDateTime dateTime, int durationMinutes, int seats, double price) throws MovieRepositoryException;
    List<Movie> getAll() throws MovieRepositoryException;
    List<Movie> getAllIncludingFinished() throws MovieRepositoryException;
    Movie getById(int id) throws MovieRepositoryException;
    boolean delete(int id) throws MovieRepositoryException;
    List<Object[]> getFinishedMoviesToAdd() throws MovieRepositoryException;
    List<Movie> getWatchedMovies(int userId) throws MovieRepositoryException;
    boolean updateTitle(int movieId, String newTitle) throws MovieRepositoryException;
    boolean updateDateTime(int movieId, LocalDateTime newDateTime) throws MovieRepositoryException;
    boolean updateDuration(int movieId, int newDuration) throws MovieRepositoryException;
    boolean updateSeats(int movieId, int newSeats) throws MovieRepositoryException;
    boolean updatePrice(int movieId, double newPrice) throws MovieRepositoryException;
    Set<Integer> getSoldSeats(int movieId) throws MovieRepositoryException;
    Set<Integer> getAllSeats(int movieId) throws MovieRepositoryException;
    double getTicketPrice(int movieId) throws MovieRepositoryException;
}