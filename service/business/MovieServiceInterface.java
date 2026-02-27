package by.academy.project.service.business;

import by.academy.project.models.Movie;
import java.time.LocalDateTime;
import java.util.List;

public interface MovieServiceInterface {
    Movie createMovie(String title, LocalDateTime dateTime, int durationMinutes, int seats, double price, String username);
    List<Movie> getAllMovies();
    List<Movie> getAllMoviesIncludingFinished();
    Movie getById(int id);
    boolean deleteMovie(int id, String username);
    void checkAndAddFinishedMovies();
    List<Movie> getWatchedMovies(int userId);
    boolean updateMovieTitle(int movieId, String newTitle, String username);
    boolean updateMovieDateTime(int movieId, LocalDateTime newDateTime, String username);
    boolean updateMovieDuration(int movieId, int newDuration, String username);
    boolean updateMovieSeats(int movieId, int newSeats, String username);
    boolean updateMoviePrice(int movieId, double newPrice, String username);
}