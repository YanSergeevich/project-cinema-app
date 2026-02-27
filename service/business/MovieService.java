package by.academy.project.service.business;

import by.academy.project.exception.MovieRepositoryException;
import by.academy.project.exception.TicketRepositoryException;
import by.academy.project.exception.UserRepositoryException;
import by.academy.project.models.Movie;
import by.academy.project.repository.MovieRepository;
import by.academy.project.repository.TicketRepository;
import by.academy.project.repository.UserRepository;
import by.academy.project.service.files.FileService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;

public class MovieService implements MovieServiceInterface  {
    private final MovieRepository movieRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final FileService fileService;

    public MovieService(MovieRepository movieRepository,
                        TicketRepository ticketRepository,
                        UserRepository userRepository,
                        FileService fileService) {
        this.movieRepository = movieRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.fileService = fileService;
    }

    public Movie createMovie(String title, LocalDateTime dateTime, int durationMinutes, int seats, double price, String username) {
        Movie movie;
        try {
            movie = movieRepository.create(title, dateTime, durationMinutes, seats, price);
        } catch (MovieRepositoryException e) {
            throw new RuntimeException(e);
        }

        if (movie != null) {
            for (int i = 1; i <= seats; i++) {
                try {
                    ticketRepository.createTicket(movie.getId(), i, price);
                } catch (TicketRepositoryException e) {
                    throw new RuntimeException(e);
                }
            }

            fileService.log(username, "Создал фильм: " + title + " с ценой " + price + " руб.");
        }

        return movie;
    }

    public List<Movie> getAllMovies() {
        try {
            return movieRepository.getAll();
        } catch (MovieRepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Movie> getAllMoviesIncludingFinished() {
        try {
            return movieRepository.getAllIncludingFinished();
        } catch (MovieRepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    public Movie getById(int id) {
        try {
            return movieRepository.getById(id);
        } catch (MovieRepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean deleteMovie(int id, String username) {
        boolean result;
        try {
            result = movieRepository.delete(id);
        } catch (MovieRepositoryException e) {
            throw new RuntimeException(e);
        }
        if (result) {
            fileService.log(username, "Удалил фильм ID " + id);
        }
        return result;
    }

    public void checkAndAddFinishedMovies() {
        List<Object[]> finishedMovies;
        try {
            finishedMovies = movieRepository.getFinishedMoviesToAdd();
        } catch (MovieRepositoryException e) {
            throw new RuntimeException(e);
        }

        for (Object[] row : finishedMovies) {
            int userId = (int) row[0];
            int movieId = (int) row[1];
            try {
                userRepository.addWatchedMovie(userId, movieId);
            } catch (UserRepositoryException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public List<Movie> getWatchedMovies(int userId) {
        try {
            return movieRepository.getWatchedMovies(userId);
        } catch (MovieRepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean updateMovieTitle(int movieId, String newTitle, String username) {
        boolean result;
        try {
            result = movieRepository.updateTitle(movieId, newTitle);
        } catch (MovieRepositoryException e) {
            throw new RuntimeException(e);
        }
        if (result) {
            fileService.log(username, "Изменил название фильма ID " + movieId + " на: " + newTitle);
        }
        return result;
    }

    public boolean updateMovieDateTime(int movieId, LocalDateTime newDateTime, String username) {
        boolean result;
        try {
            result = movieRepository.updateDateTime(movieId, newDateTime);
        } catch (MovieRepositoryException e) {
            throw new RuntimeException(e);
        }
        if (result) {
            fileService.log(username, "Изменил дату фильма ID " + movieId + " на: " + newDateTime);
        }
        return result;
    }

    public boolean updateMovieDuration(int movieId, int newDuration, String username) {
        boolean result;
        try {
            result = movieRepository.updateDuration(movieId, newDuration);
        } catch (MovieRepositoryException e) {
            throw new RuntimeException(e);
        }
        if (result) {
            fileService.log(username, "Изменил длительность фильма ID " + movieId + " на: " + newDuration + " минут");
        }
        return result;
    }

    public boolean updateMovieSeats(int movieId, int newSeats, String username) {
        try {
            Movie movie = movieRepository.getById(movieId);
            if (movie == null) {
                return false;
            }

            int currentSeats = movie.getTotalSeats();

            if (newSeats == currentSeats) {
                return true;
            }

            double ticketPrice = movieRepository.getTicketPrice(movieId);

            Set<Integer> allExistingSeats = movieRepository.getAllSeats(movieId);

            if (newSeats > currentSeats) {
                for (int seatNum = currentSeats + 1; seatNum <= newSeats; seatNum++) {
                    if (!allExistingSeats.contains(seatNum)) {
                        ticketRepository.createTicket(movieId, seatNum, ticketPrice);
                    }
                }
            } else if (newSeats < currentSeats) {
                Set<Integer> soldSeats = movieRepository.getSoldSeats(movieId);

                List<Integer> seatsToRemove = new ArrayList<>();
                List<Integer> soldSeatsToRemove = new ArrayList<>();

                for (int seatNum = newSeats + 1; seatNum <= currentSeats; seatNum++) {
                    if (soldSeats.contains(seatNum)) {
                        soldSeatsToRemove.add(seatNum);
                    } else if (allExistingSeats.contains(seatNum)) {
                        seatsToRemove.add(seatNum);
                    }
                }

                if (!soldSeatsToRemove.isEmpty()) {
                    System.out.println(" Нельзя удалить места: " + soldSeatsToRemove + " - они проданы!");
                    return false;
                }

                for (int seatNum : seatsToRemove) {
                    ticketRepository.deleteTicket(movieId, seatNum);
                }
            }

            boolean updateResult = movieRepository.updateSeats(movieId, newSeats);

            if (updateResult) {
                fileService.log(username, "Изменил количество мест фильма ID " + movieId +
                        " с " + currentSeats + " на " + newSeats);
            }
            return updateResult;

        } catch (MovieRepositoryException | TicketRepositoryException e) {
            throw new RuntimeException("Ошибка при изменении количества мест: " + e.getMessage(), e);
        }
    }

    public boolean updateMoviePrice(int movieId, double newPrice, String username) {
        boolean result;
        try {
            result = movieRepository.updatePrice(movieId, newPrice);
        } catch (MovieRepositoryException e) {
            throw new RuntimeException(e);
        }

        if (result) {
            try {
                ticketRepository.updatePriceForMovie(movieId, newPrice);
            } catch (TicketRepositoryException e) {
                throw new RuntimeException(e);
            }
            fileService.log(username, "Изменил цену билета фильма ID " + movieId + " на: " + newPrice + " руб.");
        }
        return result;
    }
}