package by.academy.project.console;

import by.academy.project.exception.PromoCodeException;
import by.academy.project.exception.UserRepositoryException;
import by.academy.project.models.*;
import by.academy.project.repository.MovieRepository;
import by.academy.project.repository.PromoCodeRepository;
import by.academy.project.repository.TicketRepository;
import by.academy.project.repository.UserRepository;
import by.academy.project.service.business.PromoCodeService;
import by.academy.project.service.infra.DatabaseManager;

import by.academy.project.service.files.FileService;
import by.academy.project.service.business.MovieService;
import by.academy.project.service.business.TicketService;
import by.academy.project.service.business.UserService;
import by.academy.project.service.pay.PaymentProcessing;
import by.academy.project.service.pay.PaymentTransaction;
import by.academy.project.validation.LoginValidator;
import by.academy.project.validation.PasswordValidator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CinemaApp {
    private final Scanner scanner;
    private final UserService userService;
    private final MovieService movieService;
    private final TicketService ticketService;
    private final FileService fileService;
    private final PaymentProcessing paymentService;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final PromoCodeRepository promoCodeRepository;
    private final PromoCodeService promoCodeService;
    private User currentUser;
    public CinemaApp() {
        this.scanner = new Scanner(System.in);
        this.fileService = new FileService();

        UserRepository userRepository = new UserRepository();
        TicketRepository ticketRepository = new TicketRepository();
        MovieRepository movieRepository = new MovieRepository();

        this.userService = new UserService(userRepository, fileService);
        this.ticketService = new TicketService(ticketRepository, fileService);
        this.movieService = new MovieService(movieRepository, ticketRepository, userRepository, fileService);

        this.paymentService = new PaymentProcessing();
        this.currentUser = null;

        this.promoCodeRepository = new PromoCodeRepository();
        this.promoCodeService = new PromoCodeService(promoCodeRepository, fileService);
    }

    public void start() {
        DatabaseManager.initDB();

        while (true) {
            if (currentUser == null) {
                showLoginMenu();
            } else {
                movieService.checkAndAddFinishedMovies();
                showMainMenu();
            }
        }
    }

    // ==================== AUTHENTICATION SECTION ====================
    private void showLoginMenu() {
        while (currentUser == null) {
            System.out.println("\n" + "=".repeat(40));
            System.out.println("        ПРИЛОЖЕНИЕ КИНОТЕАТР");
            System.out.println("=".repeat(40));
            System.out.println("1. Войти в систему");
            System.out.println("2. Зарегистрироваться");
            System.out.println("3. Выйти из приложения");
            System.out.println("=".repeat(40));
            System.out.print("Выберите действие: ");

            int choice = getNumber();

            switch (choice) {
                case 1 -> login();
                case 2 -> register();
                case 3 -> {
                    System.out.println("\nДо свидания!");
                    System.exit(0);
                }
                default -> System.out.println(" Неправильный выбор. Попробуйте снова.");
            }
        }
    }

    private void login() {
        System.out.println("\n" + "-".repeat(40));
        System.out.println("             ВХОД В СИСТЕМУ");
        System.out.println("-".repeat(40));

        System.out.print("Введите логин: ");
        String login = scanner.nextLine();
        System.out.print("Введите пароль: ");
        String password = scanner.nextLine();

        try {
            currentUser = userService.login(login, password);
        } catch (UserRepositoryException e) {
            throw new RuntimeException(e);
        }
        if (currentUser != null) {
            System.out.println("\n Вход выполнен! Добро пожаловать, " + login + "!");
        } else {
            System.out.println("\n Неверный логин или пароль!");
        }
    }

    private void register() {
        System.out.println("\n" + "-".repeat(40));
        System.out.println("           РЕГИСТРАЦИЯ");
        System.out.println("-".repeat(40));

        System.out.print("Придумайте логин: ");
        String login = scanner.nextLine();

        if (!LoginValidator.isValid(login)) {
            System.out.println(" Ошибка: Логин не должен содержать слов admin или его вариаций");
            return;
        }

        String password;
        String passwordError;

        while (true) {
            System.out.print("Придумайте пароль (минимум 8 символов, хотя бы 1 буква): ");
            password = scanner.nextLine();

            passwordError = PasswordValidator.validateWithMessage(password);
            if (passwordError == null) {
                break;
            }
            System.out.println(" Ошибка: " + passwordError);
        }

        try {
            User user = userService.register(login, password, UserLevel.USER);
            if (user != null) {
                currentUser = user;
                System.out.println("\n Регистрация успешна! Добро пожаловать, " + login + "!");
            } else {
                System.out.println("\n Ошибка регистрации. Логин \"" + login + "\" уже занят.");
            }
        } catch (UserRepositoryException e) {
            System.out.println("Ошибка регистрации: " + e.getMessage());
        }
    }

    // ==================== MAIN MENU SECTION ====================
    private void showMainMenu() {
        System.out.println("\n=== ГЛАВНОЕ МЕНЮ ===");
        System.out.println("Пользователь: " + currentUser.getLogin() + " (" + currentUser.getUserLevel() + ")");
        System.out.println("1. Фильмы");

        if (currentUser.getUserLevel() == UserLevel.USER) {
            System.out.println("2. Мои билеты");
            System.out.println("3. Мои посещенные фильмы");
        }

        if (currentUser.getUserLevel() == UserLevel.MANAGER) {
            System.out.println("2. Управление фильмами");
            System.out.println("3. Управление билетами пользователей");
            System.out.println("4. Просмотр посещенных фильмов пользователей");
            System.out.println("5. Мой промокод");
        }

        if (currentUser.getUserLevel() == UserLevel.ADMIN) {
            System.out.println("2. Управление фильмами");
            System.out.println("3. Управление пользователями");
            System.out.println("4. Просмотр логов");
            System.out.println("5. Просмотр посещенных фильмов пользователей");
            System.out.println("6. Управление билетами пользователей");
            System.out.println("7. Мой промокод");
        }

        System.out.println("0. Выйти");
        System.out.print("Выберите: ");

        int choice = getNumber();

        switch (currentUser.getUserLevel()) {
            case USER -> handleUserMenu(choice);
            case MANAGER -> handleManagerMenu(choice);
            case ADMIN -> handleAdminMenu(choice);
        }
    }

    private void handleUserMenu(int choice) {
        switch (choice) {
            case 1 -> showMovies();
            case 2 -> showMyTickets();
            case 3 -> showWatchedMovies();
            case 0 -> {
                fileService.logExit(currentUser.getLogin());
                currentUser = null;
            }
            default -> System.out.println("Неправильный выбор");
        }
    }

    private void handleManagerMenu(int choice) {
        switch (choice) {
            case 1 -> showMovies();
            case 2 -> manageMovies();
            case 3 -> manageUserTickets();
            case 4 -> showUserWatchedMovies();
            case 5 -> showMyPromoCode();
            case 0 -> {
                fileService.logExit(currentUser.getLogin());
                currentUser = null;
            }
            default -> System.out.println("Неправильный выбор");
        }
    }

    private void handleAdminMenu(int choice) {
        switch (choice) {
            case 1 -> showMovies();
            case 2 -> manageMovies();
            case 3 -> manageUsers();
            case 4 -> viewLogs();
            case 5 -> showUserWatchedMovies();
            case 6 -> manageUserTickets();
            case 7 -> showMyPromoCode();
            case 0 -> {
                fileService.logExit(currentUser.getLogin());
                currentUser = null;
            }
            default -> System.out.println("Неправильный выбор");
        }
    }
    private void generateTestHall(Movie movie) {
        System.out.println("\n" + "=".repeat(40));
        System.out.println("СХЕМА ЗАЛА ");
        System.out.println("=".repeat(40));
        System.out.println("                 Экран");
        System.out.println("-".repeat(40));

        int totalSeats = movie.getTotalSeats();
        int rows = (int) Math.ceil(totalSeats / 5.0);

        Set<Integer> userSeats = new HashSet<>();
        List<Ticket> userTickets = ticketService.getUserTickets(currentUser.getId());
        for (Ticket ticket : userTickets) {
            if (ticket.getMovieId() == movie.getId()) {
                userSeats.add(ticket.getSeatNumber());
            }
        }

        Set<Integer> freeSeats = new HashSet<>();
        List<Ticket> availableTickets = ticketService.getAvailableTickets(movie.getId());
        for (Ticket ticket : availableTickets) {
            freeSeats.add(ticket.getSeatNumber());
        }

        for (int row = 1; row <= rows; row++) {
            System.out.print("Ряд " + row + ": ");
            int seatsInRow = (row == rows && totalSeats % 5 != 0) ? totalSeats % 5 : 5;

            for (int seatInRow = 1; seatInRow <= seatsInRow; seatInRow++) {
                int seatNumber = (row - 1) * 5 + seatInRow;

                if (seatNumber > totalSeats) {
                    System.out.print("[   ] ");
                    continue;
                }

                if (userSeats.contains(seatNumber)) {
                    System.out.print("[" + String.format("%02d", seatNumber) + "★] ");
                } else if (freeSeats.contains(seatNumber)) {
                    System.out.print("[" + String.format("%02d", seatNumber) + "▢] ");
                } else {
                    System.out.print("[" + String.format("%02d", seatNumber) + "x] ");
                }
            }
            System.out.println();
        }

        System.out.println("-".repeat(40));
        System.out.println("[▢] - Свободно  [x] - Занято  [★] - Ваше место");
        System.out.println("=".repeat(40));
    }
    // ==================== MOVIES SECTION ====================
    private void showMovies() {
        fileService.log(currentUser.getLogin(), "Смотрит список фильмов");
        List<Movie> movies = movieService.getAllMovies();

        if (movies.isEmpty()) {
            System.out.println("Нет доступных фильмов");
            return;
        }

        System.out.println("\n=== ДОСТУПНЫЕ ФИЛЬМЫ ===");

        int counter = 1;
        for (Movie movie : movies) {
            List<Ticket> availableTickets = ticketService.getAvailableTickets(movie.getId());

            System.out.println(counter + ". " +
                    movie.getTitle() + " - " + movie.getDateTime() +
                    " (" + movie.getDurationMinutes() + " мин., " +
                    availableTickets.size() + " свободных мест)");
            counter++;
        }

        System.out.print("\nВыберите фильм (0 - назад): ");
        int choice = getNumber();

        if (choice == 0) {
            return;
        }

        if (choice > 0 && choice <= movies.size()) {
            Movie movie = movies.get(choice - 1);
            showMovieDetails(movie);
        } else {
            System.out.println("Неверный выбор! Доступны фильмы с 1 по " + movies.size());
        }
    }

    private void showMovieDetails(Movie movie) {
        fileService.log(currentUser.getLogin(), "Смотрит фильм: " + movie.getTitle());
        List<Ticket> tickets = ticketService.getAvailableTickets(movie.getId());

        System.out.println("\n=== " + movie.getTitle() + " ===");
        System.out.println("Дата: " + movie.getDateTime());
        System.out.println("Длительность: " + movie.getDurationMinutes() + " минут");
        System.out.println("Свободных мест: " + tickets.size() + " из " + movie.getTotalSeats());

        if (!tickets.isEmpty()) {
            System.out.print("Доступные места: ");
            for (int i = 0; i < tickets.size(); i++) {
                Ticket ticket = tickets.get(i);
                System.out.print(ticket.getSeatNumber());
                if (i < tickets.size() - 1) {
                    System.out.print(", ");
                }
            }
            System.out.println("\nЦена за место: " + tickets.getFirst().getPrice() + " руб.");
        } else {
            System.out.println("Нет свободных мест");
        }

        System.out.println("\n1. Показать схему зала");

        if (currentUser.getUserLevel() == UserLevel.USER) {
            System.out.println("2. Купить билет(ы)");
            System.out.println("3. Вернуть билет");
        }

        System.out.println("0. Назад");
        System.out.print("Выберите: ");

        int choice = getNumber();

        switch (choice) {
            case 1 -> {
                generateTestHall(movie);

                if (currentUser.getUserLevel() == UserLevel.USER) {
                    System.out.println("\n1. Купить билет(ы)");
                    System.out.println("2. Вернуть билет");
                    System.out.println("0. Назад");
                    System.out.print("Выберите: ");

                    int secondChoice = getNumber();
                    switch (secondChoice) {
                        case 1 -> {
                            if (tickets.isEmpty()) {
                                System.out.println("Нет свободных мест для покупки!");
                            } else {
                                buyTickets(movie);
                            }
                        }
                        case 2 -> returnTicket(movie);
                        case 0 -> { }
                        default -> System.out.println("Неправильный выбор!");
                    }
                } else {
                    System.out.println("\n0. Назад");
                    System.out.print("Выберите: ");
                    int secondChoice = getNumber();
                    if (secondChoice != 0) {
                        System.out.println("Неправильный выбор!");
                    }
                }
            }
            case 2 -> {
                if (currentUser.getUserLevel() == UserLevel.USER) {
                    if (tickets.isEmpty()) {
                        System.out.println("Нет свободных мест для покупки!");
                    } else {
                        buyTickets(movie);
                    }
                } else {
                    System.out.println("Неправильный выбор!");
                }
            }
            case 3 -> {
                if (currentUser.getUserLevel() == UserLevel.USER) {
                    returnTicket(movie);
                } else {
                    System.out.println("Неправильный выбор!");
                }
            }
            case 0 -> { }
            default -> System.out.println("Неправильный выбор!");
        }
    }

    private void showWatchedMovies() {
        fileService.log(currentUser.getLogin(), "Смотрит свои посещенные фильмы");
        List<Movie> movies = movieService.getWatchedMovies(currentUser.getId());

        if (movies.isEmpty()) {
            System.out.println("Вы еще не посетили ни одного фильма");
            return;
        }

        System.out.println("\n=== МОИ ПОСЕЩЕННЫЕ ФИЛЬМЫ ===");
        for (Movie movie : movies) {
            System.out.println("Фильм: " + movie.getTitle() +
                    ", Дата просмотра: " + movie.getDateTime() +
                    ", Длительность: " + movie.getDurationMinutes() + " мин.");
        }

        System.out.println("\nВсего посещено фильмов: " + movies.size());
    }

    private void showUserWatchedMovies() {
        fileService.log(currentUser.getLogin(), "Смотрит посещенные фильмы пользователей");

        List<User> users = userService.getAllUsers();
        List<User> regularUsers = new java.util.ArrayList<>();

        for (User user : users) {
            if (user.getUserLevel() == UserLevel.USER) {
                regularUsers.add(user);
            }
        }

        if (regularUsers.isEmpty()) {
            System.out.println("Нет зарегистрированных пользователей");
            return;
        }

        System.out.println("\n=== ВЫБОР ПОЛЬЗОВАТЕЛЯ ===");
        System.out.println("Пользователи:");
        for (int i = 0; i < regularUsers.size(); i++) {
            System.out.println((i + 1) + ". " + regularUsers.get(i).getLogin());
        }

        System.out.print("Выберите пользователя (0 - назад): ");
        int userChoice = getNumber();

        if (userChoice == 0) return;

        if (userChoice > 0 && userChoice <= regularUsers.size()) {
            User selectedUser = regularUsers.get(userChoice - 1);
            List<Movie> movies = movieService.getWatchedMovies(selectedUser.getId());

            if (movies.isEmpty()) {
                System.out.println("\nПользователь " + selectedUser.getLogin() + " еще не посетил ни одного фильма");
                return;
            }

            System.out.println("\n=== ПОСЕЩЕННЫЕ ФИЛЬМЫ " + selectedUser.getLogin().toUpperCase() + " ===");
            for (Movie movie : movies) {
                System.out.println("Фильм: " + movie.getTitle() +
                        ", Дата просмотра: " + movie.getDateTime() +
                        ", Длительность: " + movie.getDurationMinutes() + " мин.");
            }

            System.out.println("\nВсего посещено фильмов: " + movies.size());
        } else {
            System.out.println("Неверный выбор! Доступны пользователи с 1 по " + regularUsers.size());
        }
    }

    // ==================== MOVIE MANAGEMENT SECTION ====================
    private void manageMovies() {
        fileService.log(currentUser.getLogin(), "Зашел в управление фильмами");

        while (true) {
            System.out.println("\n=== УПРАВЛЕНИЕ ФИЛЬМАМИ ===");

            System.out.println("1. Список всех фильмов");
            System.out.println("2. Редактировать фильм");

            if (currentUser.getUserLevel() == UserLevel.ADMIN) {
                System.out.println("3. Добавить фильм");
                System.out.println("4. Удалить фильм");
            }

            System.out.println("0. Назад");
            System.out.print("Выберите: ");

            int choice = getNumber();

            switch (choice) {
                case 1 -> listMovies();
                case 2 -> {
                    if (currentUser.getUserLevel() == UserLevel.MANAGER || currentUser.getUserLevel() == UserLevel.ADMIN) {
                        editMovie();
                    } else {
                        System.out.println("Доступ запрещен");
                    }
                }
                case 3 -> {
                    if (currentUser.getUserLevel() == UserLevel.ADMIN) {
                        addMovie();
                    } else {
                        System.out.println("Доступ запрещен. Только администратор может добавлять фильмы.");
                    }
                }
                case 4 -> {
                    if (currentUser.getUserLevel() == UserLevel.ADMIN) {
                        deleteMovie();
                    } else {
                        System.out.println("Доступ запрещен. Только администратор может удалять фильмы.");
                    }
                }
                case 0 -> { return; }
                default -> System.out.println("Неправильный выбор");
            }
        }
    }

    private void editMovie() {
        List<Movie> movies = movieService.getAllMoviesIncludingFinished();

        if (movies.isEmpty()) {
            System.out.println("Нет фильмов для редактирования");
            return;
        }

        System.out.println("\n=== ВЫБОР ФИЛЬМА ДЛЯ РЕДАКТИРОВАНИЯ ===");
        for (int i = 0; i < movies.size(); i++) {
            Movie movie = movies.get(i);
            String status = movie.isFinished() ? "ЗАВЕРШЕН" : "АКТИВЕН";
            System.out.println((i + 1) + ". " + movie.getTitle() + " (" + status + ")");
        }

        System.out.print("\nВыберите номер фильма (1-" + movies.size() + "): ");
        int choice = getNumber();

        if (choice < 1 || choice > movies.size()) {
            System.out.println("Неверный выбор!");
            return;
        }

        Movie movieToEdit = movies.get(choice - 1);

        if (movieToEdit.isFinished()) {
            System.out.println("Нельзя редактировать завершенный фильм!");
            return;
        }

        System.out.println("\n=== РЕДАКТИРОВАНИЕ ФИЛЬМА: " + movieToEdit.getTitle() + " ===");
        System.out.println("Что вы хотите изменить?");
        System.out.println("1. Название фильма");
        System.out.println("2. Дата и время сеанса");
        System.out.println("3. Продолжительность (минуты)");
        System.out.println("4. Количество мест");
        System.out.println("5. Цена билета");
        System.out.println("0. Отмена");
        System.out.print("Выберите: ");

        int editChoice = getNumber();

        switch (editChoice) {
            case 1 -> editMovieTitle(movieToEdit);
            case 2 -> editMovieDateTime(movieToEdit);
            case 3 -> editMovieDuration(movieToEdit);
            case 4 -> editMovieSeats(movieToEdit);
            case 5 -> editMoviePrice(movieToEdit);
            case 0 -> System.out.println("Редактирование отменено");
            default -> System.out.println("Неверный выбор");
        }
    }

    private void editMovieTitle(Movie movie) {
        System.out.print("Новое название фильма: ");
        String newTitle = scanner.nextLine();

        if (newTitle.trim().isEmpty()) {
            System.out.println("Название не может быть пустым!");
            return;
        }

        if (movieService.updateMovieTitle(movie.getId(), newTitle, currentUser.getLogin())) {
            System.out.println("Название фильма изменено на: " + newTitle);
        } else {
            System.out.println("Ошибка изменения названия фильма");
        }
    }

    private void editMovieDateTime(Movie movie) {
        LocalDateTime newDateTime = null;
        boolean validDate = false;

        while (!validDate) {
            System.out.print("Новая дата и время (гггг-мм-дд чч:мм): ");
            String dateStr = scanner.nextLine();

            try {
                newDateTime = LocalDateTime.parse(dateStr, dateFormatter);

                LocalDateTime now = LocalDateTime.now();

                if (newDateTime.isBefore(now)) {
                    System.out.println("Ошибка: Дата фильма не может быть в прошлом!");
                    System.out.println("Сегодня: " + now.format(dateFormatter));
                    continue;
                }

                validDate = true;

            } catch (Exception e) {
                System.out.println("Неправильный формат даты! Используйте формат: гггг-мм-дд чч:мм");
                System.out.println("Пример: " + LocalDateTime.now().format(dateFormatter));
            }
        }

        if (movieService.updateMovieDateTime(movie.getId(), newDateTime, currentUser.getLogin())) {
            System.out.println("Дата и время фильма изменены");
        } else {
            System.out.println("Ошибка изменения даты и времени фильма");
        }
    }

    private void editMovieDuration(Movie movie) {
        System.out.print("Новая продолжительность фильма (в минутах): ");
        int newDuration = getNumber();

        if (newDuration <= 0) {
            System.out.println("Продолжительность должна быть положительным числом!");
            return;
        }

        if (movieService.updateMovieDuration(movie.getId(), newDuration, currentUser.getLogin())) {
            System.out.println("Продолжительность фильма изменена на: " + newDuration + " минут");
        } else {
            System.out.println("Ошибка изменения продолжительности фильма");
        }
    }

    private void editMovieSeats(Movie movie) {
        System.out.print("Новое количество мест: ");
        int newSeats = getNumber();

        if (newSeats <= 0) {
            System.out.println("Количество мест должно быть положительным числом!");
            return;
        }

        int currentSeats = movie.getTotalSeats();

        if (newSeats == currentSeats) {
            System.out.println("Количество мест не изменилось");
            return;
        }

        if (movieService.updateMovieSeats(movie.getId(), newSeats, currentUser.getLogin())) {
            movie.setTotalSeats(newSeats);
            System.out.println(" Количество мест изменено с " + currentSeats + " на " + newSeats);
        } else {
            if (newSeats < currentSeats) {
                System.out.println(" Нельзя уменьшить места - есть проданные билеты!");
            } else {
                System.out.println(" Ошибка изменения количества мест");
            }
        }
    }

    private void editMoviePrice(Movie movie) {
        System.out.print("Новая цена билета: ");
        double newPrice = getDouble();

        if (newPrice <= 0) {
            System.out.println("Цена должна быть положительным числом!");
            return;
        }

        if (movieService.updateMoviePrice(movie.getId(), newPrice, currentUser.getLogin())) {
            System.out.println("Цена билета изменена на: " + newPrice + " руб.");
        } else {
            System.out.println("Ошибка изменения цены билета");
        }
    }

    private void addMovie() {
        System.out.print("Название фильма: ");
        String title = scanner.nextLine();

        LocalDateTime dateTime = null;
        boolean validDate = false;

        while (!validDate) {
            System.out.print("Дата и время (гггг-мм-дд чч:мм): ");
            String dateStr = scanner.nextLine();

            try {
                dateTime = LocalDateTime.parse(dateStr, dateFormatter);

                LocalDateTime now = LocalDateTime.now();

                if (dateTime.isBefore(now)) {
                    System.out.println("Ошибка: Дата фильма не может быть в прошлом!");
                    System.out.println("Сегодня: " + now.format(dateFormatter));
                    continue;
                }

                validDate = true;

            } catch (Exception e) {
                System.out.println("Неправильный формат даты! Используйте формат: гггг-мм-дд чч:мм");
                System.out.println("Пример: " + LocalDateTime.now().format(dateFormatter));
            }
        }

        System.out.print("Продолжительность фильма (в минутах): ");
        int duration = getNumber();

        System.out.print("Количество мест: ");
        int seats = getNumber();

        System.out.print("Цена билета: ");
        double price = getDouble();

        if (movieService.createMovie(title, dateTime, duration, seats, price, currentUser.getLogin()) != null) {
            System.out.println("Фильм добавлен");
        } else {
            System.out.println("Ошибка добавления");
        }
    }

    private void listMovies() {
        List<Movie> activeMovies = movieService.getAllMovies();

        if (activeMovies.isEmpty()) {
            System.out.println("Нет активных фильмов");
            return;
        }

        System.out.println("\n=== СПИСОК ФИЛЬМОВ ===");

        for (int i = 0; i < activeMovies.size(); i++) {
            Movie movie = activeMovies.get(i);
            List<Ticket> availableTickets = ticketService.getAvailableTickets(movie.getId());

            int totalSeats = movie.getTotalSeats();
            int freeSeats = availableTickets.size();
            int soldSeats = totalSeats - freeSeats;

            System.out.println((i + 1) + ". " + movie.getTitle() +
                    " - продано " + soldSeats + ", свободно " + freeSeats + " из " + totalSeats + " мест");
        }
    }
    private void deleteMovie() {
        List<Movie> movies = movieService.getAllMoviesIncludingFinished();

        if (movies.isEmpty()) {
            System.out.println("Нет фильмов для удаления");
            return;
        }

        System.out.println("\n=== ВЫБОР ФИЛЬМА ДЛЯ УДАЛЕНИЯ ===");
        for (int i = 0; i < movies.size(); i++) {
            Movie movie = movies.get(i);
            String status = movie.isFinished() ? "ЗАВЕРШЕН" : "АКТИВЕН";
            System.out.println((i + 1) + ". " + movie.getTitle() + " (" + status + ")");
        }

        System.out.print("\nВыберите номер фильма (1-" + movies.size() + "): ");
        int choice = getNumber();

        if (choice < 1 || choice > movies.size()) {
            System.out.println("Неверный выбор!");
            return;
        }

        Movie movieToDelete = movies.get(choice - 1);

        if (scanner.hasNextLine()) {
            scanner.nextLine();
        }

        System.out.print("Уверены, что хотите удалить фильм \"" + movieToDelete.getTitle() + "\"? (да/нет): ");
        String confirm = scanner.nextLine().toLowerCase().trim();

        if (confirm.equals("да")) {
            if (movieService.deleteMovie(movieToDelete.getId(), currentUser.getLogin())) {
                System.out.println("Фильм удален");
            } else {
                System.out.println("Ошибка удаления");
            }
        } else {
            System.out.println("Удаление отменено");
        }
    }

    // ==================== TICKETS SECTION ====================
    private void showMyTickets() {
        fileService.log(currentUser.getLogin(), "Смотрит свои билеты");
        List<Ticket> tickets = ticketService.getUserTickets(currentUser.getId());

        if (tickets.isEmpty()) {
            System.out.println("У вас нет билетов");
            scanner.nextLine();
            return;
        }

        System.out.println("\n=== ВАШИ АКТИВНЫЕ БИЛЕТЫ ===");
        boolean hasActiveTickets = false;

        for (Ticket ticket : tickets) {
            Movie movie = movieService.getById(ticket.getMovieId());

            if (!movie.isFinished()) {
                System.out.println("Фильм: " + movie.getTitle() +
                        ", Дата: " + movie.getDateTime() +
                        ", Место: " + ticket.getSeatNumber() +
                        ", Цена: " + ticket.getPrice() + " руб.");
                hasActiveTickets = true;
            }
        }

        if (!hasActiveTickets) {
            System.out.println("У вас нет активных билетов на будущие сеансы");
        }
        scanner.nextLine();
    }
    private void buyTickets(Movie movie) {
        List<Ticket> availableTickets = ticketService.getAvailableTickets(movie.getId());

        if (availableTickets.isEmpty()) {
            System.out.println("Нет свободных мест на этот фильм!");
            return;
        }

        System.out.print("\nДоступные места: ");
        for (int i = 0; i < availableTickets.size(); i++) {
            System.out.print(availableTickets.get(i).getSeatNumber());
            if (i < availableTickets.size() - 1) {
                System.out.print(", ");
            }
        }
        System.out.println("\nЦена за место: " + availableTickets.getFirst().getPrice() + " руб.");

        System.out.print("\nВведите номера мест через запятую или один номер: ");

        String seatsInput = scanner.nextLine().trim();

        if (seatsInput.isEmpty()) {
            System.out.print("Введите номера мест: ");
            seatsInput = scanner.nextLine().trim();
        }

        if (seatsInput.isEmpty()) {
            System.out.println("Ошибка: Не выбрано ни одного места!");
            return;
        }

        seatsInput = seatsInput.replaceAll("\\s+", "");

        if (seatsInput.isEmpty()) {
            System.out.println("Ошибка: Не выбрано ни одного места!");
            return;
        }

        if (seatsInput.endsWith(",")) {
            seatsInput = seatsInput.substring(0, seatsInput.length() - 1);
        }

        if (seatsInput.startsWith(",")) {
            seatsInput = seatsInput.substring(1);
        }

        String[] seatsArray = seatsInput.split(",");
        List<Integer> selectedSeats = new ArrayList<>();

        try {
            for (String seatStr : seatsArray) {
                seatStr = seatStr.trim();
                if (!seatStr.isEmpty()) {
                    int seatNum = Integer.parseInt(seatStr);
                    if (seatNum <= 0) {
                        System.out.println("Ошибка: Номер места должен быть положительным числом!");
                        return;
                    }
                    selectedSeats.add(seatNum);
                }
            }
        } catch (NumberFormatException e) {
            System.out.println("Ошибка ввода! Введите номера мест через запятую (например: 1,3,5) или один номер (например: 1)");
            return;
        }

        if (selectedSeats.isEmpty()) {
            System.out.println("Ошибка: Не выбрано ни одного места!");
            return;
        }

        Set<Integer> uniqueSeats = new HashSet<>(selectedSeats);
        if (uniqueSeats.size() < selectedSeats.size()) {
            System.out.println("Ошибка: Выбраны одинаковые места!");
            return;
        }

        List<Integer> availableSeatNumbers = new ArrayList<>();
        for (Ticket ticket : availableTickets) {
            availableSeatNumbers.add(ticket.getSeatNumber());
        }

        List<Integer> unavailableSeats = new ArrayList<>();
        for (int seat : selectedSeats) {
            if (!availableSeatNumbers.contains(seat)) {
                unavailableSeats.add(seat);
            }
        }

        if (!unavailableSeats.isEmpty()) {
            System.out.print("Места ");
            for (int i = 0; i < unavailableSeats.size(); i++) {
                System.out.print(unavailableSeats.get(i));
                if (i < unavailableSeats.size() - 1) {
                    System.out.print(", ");
                }
            }
            System.out.println(" недоступны!");
            return;
        }

        double totalPrice = 0;
        for (Ticket ticket : availableTickets) {
            for (int seat : selectedSeats) {
                if (ticket.getSeatNumber() == seat) {
                    totalPrice += ticket.getPrice();
                }
            }
        }

        System.out.println("\n" + "=".repeat(40));
        System.out.println("      ПОДТВЕРЖДЕНИЕ ПОКУПКИ");
        System.out.println("=".repeat(40));
        System.out.println("Фильм: " + movie.getTitle());
        System.out.println("Дата: " + movie.getDateTime());
        System.out.println("Места: " + selectedSeats);
        System.out.println("Количество билетов: " + selectedSeats.size());
        System.out.println("Общая стоимость: " + totalPrice + " руб.");

        System.out.println("\n" + "-".repeat(40));
        System.out.println("Есть промокод? Введите его или нажмите Enter для пропуска:");
        System.out.print("Промокод: ");
        String promoCodeInput = scanner.nextLine().trim();
        String appliedPromoCode = null;

        if (!promoCodeInput.isEmpty()) {
            double priceAfterDiscount;
            try {
                priceAfterDiscount = promoCodeService.applyPromoCode(promoCodeInput, totalPrice, currentUser.getLogin());
            } catch (PromoCodeException e) {
                throw new RuntimeException(e);
            }

            if (priceAfterDiscount < totalPrice) {
                appliedPromoCode = promoCodeInput;
                System.out.println("\n Промокод применен!");
                System.out.printf("Скидка: %.2f руб.\n", totalPrice - priceAfterDiscount);
                System.out.printf("Итоговая сумма: %.2f руб.\n", priceAfterDiscount);
                totalPrice = priceAfterDiscount;
            }
        }

        System.out.println("=".repeat(40));

        System.out.print("Подтвердить покупку? (да/нет): ");
        String confirm = scanner.nextLine().toLowerCase();

        if (!confirm.equals("да")) {
            System.out.println("Покупка отменена.");
            return;
        }

        String description = "Билеты на фильм: " + movie.getTitle() +
                ", места: " + selectedSeats;

        if (!processPayment(totalPrice, description)) {
            System.out.println("Оплата не прошла. Покупка отменена.");
            return;
        }

        StringBuilder seatsList = new StringBuilder();
        boolean atLeastOnePurchased = false;

        for (int i = 0; i < selectedSeats.size(); i++) {
            int seat = selectedSeats.get(i);

            for (Ticket ticket : availableTickets) {
                if (ticket.getSeatNumber() == seat) {
                    if (ticketService.buyTicket(ticket.getId(), currentUser.getId(), currentUser.getLogin())) {
                        seatsList.append(seat);
                        if (i < selectedSeats.size() - 1) {
                            seatsList.append(", ");
                        }
                        atLeastOnePurchased = true;
                    }
                    break;
                }
            }
        }

        if (atLeastOnePurchased) {
            System.out.println("\n✓ Покупка успешна!");
            System.out.println("Куплены билеты на места: " + seatsList);
            System.out.println("Количество билетов: " + selectedSeats.size());
            System.out.println("Общая стоимость: " + totalPrice + " руб.");

            if (appliedPromoCode != null) {
                System.out.println("Применен промокод: " + appliedPromoCode);
            }

            fileService.log(currentUser.getLogin(),
                    "Купил " + selectedSeats.size() + " билет(ов) на места: " + seatsList +
                            " на фильм: " + movie.getTitle() + ". Сумма: " + totalPrice + " руб." +
                            (appliedPromoCode != null ? " Промокод: " + appliedPromoCode : ""));

            if (movie.isFinished()) {
                try {
                    userService.addWatchedMovie(currentUser.getId(), movie.getId());
                } catch (UserRepositoryException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            System.out.println("Не удалось купить ни одного билета!");
            System.out.println("Средства будут возвращены автоматически.");
        }
    }

    private void returnTicket(Movie movie) {
        List<Ticket> userTickets = ticketService.getUserTickets(currentUser.getId());
        List<Ticket> movieTickets = new java.util.ArrayList<>();

        for (Ticket ticket : userTickets) {
            if (ticket.getMovieId() == movie.getId()) {
                movieTickets.add(ticket);
            }
        }

        if (movieTickets.isEmpty()) {
            System.out.println("У вас нет билетов на этот фильм");
            return;
        }

        System.out.println("Ваши билеты:");
        for (int i = 0; i < movieTickets.size(); i++) {
            Ticket ticket = movieTickets.get(i);
            System.out.println((i + 1) + ". Место " + ticket.getSeatNumber());
        }

        System.out.print("Выберите билет для возврата (0 - отмена): ");
        int choice = getNumber();

        if (choice == 0) return;

        if (choice > 0 && choice <= movieTickets.size()) {
            Ticket ticket = movieTickets.get(choice - 1);
            if (ticketService.returnTicket(ticket.getId(), currentUser.getLogin())) {
                System.out.println("Билет возвращен");
                System.out.println("Ваши средства в размере " + ticket.getPrice() + " руб. были возвращены.");

                fileService.log(currentUser.getLogin(),
                        "Вернул билет на фильм '" + movie.getTitle() +
                                "' (место " + ticket.getSeatNumber() +
                                "). Возвращено " + ticket.getPrice() + " руб.");
            } else {
                System.out.println("Ошибка возврата");
            }
        } else {
            System.out.println("Неверный выбор! Доступны билеты с 1 по " + movieTickets.size());
        }
    }

    // ==================== TICKET MANAGEMENT SECTION ====================
    private void manageUserTickets() {
        List<User> users = userService.getAllUsers();
        List<User> regularUsers = new ArrayList<>();

        for (User user : users) {
            if (user.getUserLevel() == UserLevel.USER) {
                regularUsers.add(user);
            }
        }

        if (regularUsers.isEmpty()) {
            System.out.println("Нет зарегистрированных пользователей");
            return;
        }

        while (true) {
            System.out.println("\n=== УПРАВЛЕНИЕ БИЛЕТАМИ ПОЛЬЗОВАТЕЛЕЙ ===");
            System.out.println("Пользователи:");
            for (int i = 0; i < regularUsers.size(); i++) {
                System.out.println((i + 1) + ". " + regularUsers.get(i).getLogin());
            }
            System.out.println("0. Назад");
            System.out.print("Выберите пользователя: ");

            int userChoice = getNumber();

            if (userChoice == 0) return;

            if (userChoice < 1 || userChoice > regularUsers.size()) {
                System.out.println("Неверный выбор!");
                continue;
            }

            User selectedUser = regularUsers.get(userChoice - 1);
            manageSingleUserTickets(selectedUser);
        }
    }

    private void manageSingleUserTickets(User user) {
        while (true) {
            List<Ticket> tickets = ticketService.getUserTickets(user.getId());
            int totalTickets = tickets.size();

            System.out.println("\n" + "=".repeat(50));
            System.out.println("Управление билетами пользователя: " + user.getLogin());
            System.out.println("Всего билетов: " + totalTickets);
            System.out.println("=".repeat(50));

            if (totalTickets > 0) {
                System.out.println("1. Просмотреть все билеты");
                System.out.println("2. Просмотреть билеты по фильмам");
                System.out.println("3. Возвратить один билет");
                System.out.println("4. Возвратить несколько билетов");
                System.out.println("5. Возвратить все билеты");
                System.out.println("6. Купить билеты для этого пользователя");
            } else {
                System.out.println("У пользователя нет билетов");
                System.out.println("1. Купить билеты для этого пользователя");
            }
            System.out.println("0. Назад к выбору пользователя");
            System.out.print("Выберите действие: ");

            int choice = getNumber();

            if (totalTickets > 0) {
                switch (choice) {
                    case 1 -> showUserTickets(user);
                    case 2 -> showUserTicketsByMovies(user);
                    case 3 -> returnSingleUserTicket(user);
                    case 4 -> returnMultipleUserTickets(user);
                    case 5 -> returnAllUserTickets(user);
                    case 6 -> buyTicketsForUser(user);
                    case 0 -> { return; }
                    default -> System.out.println("Неправильный выбор!");
                }
            } else {
                switch (choice) {
                    case 1 -> buyTicketsForUser(user);
                    case 0 -> { return; }
                    default -> System.out.println("Неправильный выбор!");
                }
            }
        }
    }

    private void buyTicketsForUser(User user) {
        List<Movie> movies = movieService.getAllMovies();

        if (movies.isEmpty()) {
            System.out.println("Нет доступных фильмов");
            return;
        }

        System.out.println("\nДоступные фильмы для пользователя " + user.getLogin() + ":");
        for (int i = 0; i < movies.size(); i++) {
            List<Ticket> availableTickets = ticketService.getAvailableTickets(movies.get(i).getId());
            System.out.println((i + 1) + ". " + movies.get(i).getTitle() +
                    " (" + availableTickets.size() + " свободных мест)");
        }

        System.out.print("Выберите фильм (0 - назад): ");
        int movieChoice = getNumber();

        if (movieChoice == 0) return;

        if (movieChoice < 1 || movieChoice > movies.size()) {
            System.out.println("Неверный выбор! Доступны фильмы с 1 по " + movies.size());
            return;
        }

        Movie movie = movies.get(movieChoice - 1);
        List<Ticket> tickets = ticketService.getAvailableTickets(movie.getId());

        if (tickets.isEmpty()) {
            System.out.println("Нет свободных мест на этот фильм");
            return;
        }

        System.out.print("Свободные места: ");
        for (int i = 0; i < tickets.size(); i++) {
            System.out.print(tickets.get(i).getSeatNumber());
            if (i < tickets.size() - 1) {
                System.out.print(", ");
            }
        }
        System.out.println("\nЦена за место: " + tickets.getFirst().getPrice() + " руб.");

        System.out.print("\nВведите номера мест через запятую или один номер: ");

        String seatsInput = scanner.nextLine().trim();

        if (seatsInput.isEmpty()) {
            System.out.print("Введите номера мест: ");
            seatsInput = scanner.nextLine().trim();
        }

        if (seatsInput.isEmpty()) {
            System.out.println("Ошибка: Не выбрано ни одного места!");
            return;
        }

        seatsInput = seatsInput.replaceAll("\\s+", "");

        if (seatsInput.isEmpty()) {
            System.out.println("Ошибка: Не выбрано ни одного места!");
            return;
        }

        if (seatsInput.endsWith(",")) {
            seatsInput = seatsInput.substring(0, seatsInput.length() - 1);
        }

        if (seatsInput.startsWith(",")) {
            seatsInput = seatsInput.substring(1);
        }

        String[] seatsArray = seatsInput.split(",");
        List<Integer> selectedSeats = new ArrayList<>();

        try {
            for (String seatStr : seatsArray) {
                seatStr = seatStr.trim();
                if (!seatStr.isEmpty()) {
                    int seatNum = Integer.parseInt(seatStr);
                    if (seatNum <= 0) {
                        System.out.println("Ошибка: Номер места должен быть положительным числом!");
                        return;
                    }
                    selectedSeats.add(seatNum);
                }
            }
        } catch (NumberFormatException e) {
            System.out.println("Ошибка ввода! Введите номера мест через запятую (например: 1,3,5) или один номер (например: 1)");
            return;
        }

        if (selectedSeats.isEmpty()) {
            System.out.println("Ошибка: Не выбрано ни одного места!");
            return;
        }

        Set<Integer> uniqueSeats = new HashSet<>(selectedSeats);
        if (uniqueSeats.size() < selectedSeats.size()) {
            System.out.println("Ошибка: Выбраны одинаковые места!");
            return;
        }

        List<Integer> availableSeatNumbers = new ArrayList<>();
        for (Ticket ticket : tickets) {
            availableSeatNumbers.add(ticket.getSeatNumber());
        }

        List<Integer> unavailableSeats = new ArrayList<>();
        for (int seat : selectedSeats) {
            if (!availableSeatNumbers.contains(seat)) {
                unavailableSeats.add(seat);
            }
        }

        if (!unavailableSeats.isEmpty()) {
            System.out.print("Места ");
            for (int i = 0; i < unavailableSeats.size(); i++) {
                System.out.print(unavailableSeats.get(i));
                if (i < unavailableSeats.size() - 1) {
                    System.out.print(", ");
                }
            }
            System.out.println(" недоступны!");
            return;
        }

        double totalPrice = 0;
        for (Ticket ticket : tickets) {
            for (int seat : selectedSeats) {
                if (ticket.getSeatNumber() == seat) {
                    totalPrice += ticket.getPrice();
                }
            }
        }

        System.out.println("\n" + "=".repeat(40));
        System.out.println("      ПОДТВЕРЖДЕНИЕ ПОКУПКИ");
        System.out.println("=".repeat(40));
        System.out.println("Покупатель: " + user.getLogin());
        System.out.println("Фильм: " + movie.getTitle());
        System.out.println("Дата: " + movie.getDateTime());
        System.out.println("Места: " + selectedSeats);
        System.out.println("Количество билетов: " + selectedSeats.size());
        System.out.println("Общая стоимость: " + totalPrice + " руб.");

        System.out.println("\n" + "-".repeat(40));
        System.out.println("Есть промокод? Введите его или нажмите Enter для пропуска:");
        System.out.print("Промокод: ");
        String promoCodeInput = scanner.nextLine().trim();
        String appliedPromoCode = null;

        if (!promoCodeInput.isEmpty()) {
            double priceAfterDiscount;
            try {
                priceAfterDiscount = promoCodeService.applyPromoCode(promoCodeInput, totalPrice, currentUser.getLogin());
            } catch (PromoCodeException e) {
                throw new RuntimeException(e);
            }

            if (priceAfterDiscount < totalPrice) {
                appliedPromoCode = promoCodeInput;
                System.out.println("\n Промокод применен!");
                System.out.printf("Скидка: %.2f руб.\n", totalPrice - priceAfterDiscount);
                System.out.printf("Итоговая сумма: %.2f руб.\n", priceAfterDiscount);
                totalPrice = priceAfterDiscount;
            }
        }

        System.out.println("=".repeat(40));
        System.out.print("Подтвердить покупку? (да/нет): ");
        String confirm = scanner.nextLine().toLowerCase();

        if (!confirm.equals("да")) {
            System.out.println("Покупка отменена.");
            return;
        }

        String description = "Билеты для " + user.getLogin() +
                " на фильм: " + movie.getTitle() +
                ", места: " + selectedSeats;

        if (!processPayment(totalPrice, description)) {
            System.out.println("Оплата не прошла. Покупка отменена.");
            return;
        }

        StringBuilder seatsList = new StringBuilder();
        boolean atLeastOnePurchased = false;

        for (int i = 0; i < selectedSeats.size(); i++) {
            int seat = selectedSeats.get(i);

            for (Ticket ticket : tickets) {
                if (ticket.getSeatNumber() == seat) {
                    if (ticketService.buyTicket(ticket.getId(), user.getId(),
                            currentUser.getLogin() + " для " + user.getLogin())) {
                        seatsList.append(seat);
                        if (i < selectedSeats.size() - 1) {
                            seatsList.append(", ");
                        }
                        atLeastOnePurchased = true;
                        System.out.println("Билет на место " + seat + " куплен для " + user.getLogin());
                    }
                    break;
                }
            }
        }

        if (atLeastOnePurchased) {
            System.out.println("\n✓ Покупка успешна!");
            System.out.println("Для пользователя " + user.getLogin() + " куплены билеты на места: " + seatsList);
            System.out.println("Количество билетов: " + selectedSeats.size());
            System.out.println("Общая стоимость: " + totalPrice + " руб.");

            if (appliedPromoCode != null) {
                System.out.println("Применен промокод: " + appliedPromoCode);
            }

            if (!seatsList.isEmpty()) {
                fileService.log(currentUser.getLogin(),
                        "Купил " + selectedSeats.size() + " билет(ов) на места: " + seatsList +
                                " для пользователя " + user.getLogin() + " на фильм: " + movie.getTitle() +
                                ". Сумма: " + totalPrice + " руб." +
                                (appliedPromoCode != null ? " Промокод: " + appliedPromoCode : ""));
            }

            if (movie.isFinished()) {
                try {
                    userService.addWatchedMovie(user.getId(), movie.getId());
                } catch (UserRepositoryException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            System.out.println("Не удалось купить ни одного билета!");
            System.out.println("Средства будут возвращены автоматически.");
        }
    }

    private void showUserTickets(User user) {
        List<Ticket> tickets = ticketService.getUserTickets(user.getId());

        if (tickets.isEmpty()) {
            System.out.println("У пользователя " + user.getLogin() + " нет билетов");
            return;
        }

        System.out.println("\n=== ВСЕ БИЛЕТЫ ПОЛЬЗОВАТЕЛЯ " + user.getLogin().toUpperCase() + " ===");
        double totalSum = 0;

        for (int i = 0; i < tickets.size(); i++) {
            Ticket ticket = tickets.get(i);
            Movie movie = movieService.getById(ticket.getMovieId());

            System.out.printf("%2d. Фильм: %-30s Дата: %-16s Место: %-3d Цена: %.2f руб.\n",
                    i + 1,
                    movie.getTitle(),
                    movie.getDateTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                    ticket.getSeatNumber(),
                    ticket.getPrice());

            totalSum += ticket.getPrice();
        }

        System.out.println("-".repeat(70));
        System.out.printf("Итого: %d билетов, Общая стоимость: %.2f руб.\n", tickets.size(), totalSum);
    }

    private void showUserTicketsByMovies(User user) {
        List<Ticket> allTickets = ticketService.getUserTickets(user.getId());

        if (allTickets.isEmpty()) {
            System.out.println("У пользователя " + user.getLogin() + " нет билетов");
            return;
        }

        Map<Integer, List<Ticket>> ticketsByMovie = new HashMap<>();
        Map<Integer, Movie> moviesMap = new HashMap<>();

        for (Ticket ticket : allTickets) {
            int movieId = ticket.getMovieId();
            ticketsByMovie.computeIfAbsent(movieId,k -> new ArrayList<>()).add(ticket);

            if (!moviesMap.containsKey(movieId)) {
                Movie movie = movieService.getById(movieId);
                if (movie != null) {
                    moviesMap.put(movieId, movie);
                }
            }
        }

        System.out.println("\n=== БИЛЕТЫ ПОЛЬЗОВАТЕЛЯ " + user.getLogin().toUpperCase() + " ПО ФИЛЬМАМ ===");

        int totalTickets = 0;
        double totalSum = 0;

        for (Map.Entry<Integer, List<Ticket>> entry : ticketsByMovie.entrySet()) {
            int movieId = entry.getKey();
            List<Ticket> movieTickets = entry.getValue();
            Movie movie = moviesMap.get(movieId);

            if (movie != null) {
                System.out.println("\nФильм: " + movie.getTitle());
                System.out.println("Дата: " + movie.getDateTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
                System.out.print("Места: ");

                double movieTotal = 0;
                for (int i = 0; i < movieTickets.size(); i++) {
                    Ticket ticket = movieTickets.get(i);
                    System.out.print(ticket.getSeatNumber());
                    if (i < movieTickets.size() - 1) System.out.print(", ");
                    movieTotal += ticket.getPrice();
                }

                System.out.printf("\nКоличество: %d, Сумма: %.2f руб.\n", movieTickets.size(), movieTotal);

                totalTickets += movieTickets.size();
                totalSum += movieTotal;
            }
        }

        System.out.println("-".repeat(70));
        System.out.printf("Итого по всем фильмам: %d билетов, Общая стоимость: %.2f руб.\n", totalTickets, totalSum);
    }

    private void returnSingleUserTicket(User user) {
        List<Ticket> tickets = ticketService.getUserTickets(user.getId());

        if (tickets.isEmpty()) {
            System.out.println("У пользователя " + user.getLogin() + " нет билетов");
            return;
        }

        showUserTickets(user);
        System.out.print("\nВведите номер билета для возврата (0 - отмена): ");
        int ticketChoice = getNumber();

        if (ticketChoice == 0) return;

        if (ticketChoice > 0 && ticketChoice <= tickets.size()) {
            Ticket ticket = tickets.get(ticketChoice - 1);
            Movie movie = movieService.getById(ticket.getMovieId());

            System.out.print("\nВы хотите вернуть билет:\n");
            System.out.printf("Фильм: %s\n", movie.getTitle());
            System.out.printf("Дата: %s\n", movie.getDateTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
            System.out.printf("Место: %d, Цена: %.2f руб.\n", ticket.getSeatNumber(), ticket.getPrice());

            System.out.print("Подтвердить возврат? (да/нет): ");
            String confirm = scanner.nextLine().trim().toLowerCase();

            if (confirm.equals("да")) {
                if (ticketService.returnUserTicket(ticket.getId(), user.getId(), currentUser.getLogin())) {
                    System.out.println("Билет успешно возвращен!");
                    System.out.println("Средства в размере " + ticket.getPrice() + " руб. были возвращены пользователю " + user.getLogin());

                    fileService.log(currentUser.getLogin(),
                            "Вернул билет пользователя " + user.getLogin() +
                                    " на фильм '" + movie.getTitle() + "' (место " + ticket.getSeatNumber() +
                                    "). Возвращено " + ticket.getPrice() + " руб.");
                } else {
                    System.out.println("Ошибка возврата билета!");
                }
            } else {
                System.out.println("Возврат отменен");
            }
        } else {
            System.out.println("Неверный номер билета!");
        }
    }

    private void returnMultipleUserTickets(User user) {
        List<Ticket> tickets = ticketService.getUserTickets(user.getId());

        if (tickets.isEmpty()) {
            System.out.println("У пользователя " + user.getLogin() + " нет билетов");
            return;
        }

        showUserTickets(user);
        System.out.print("\nВведите номера билетов через запятую: ");

        String input = scanner.nextLine().trim();

        if (input.isEmpty()) {
            System.out.println("Не выбрано ни одного билета!");
            return;
        }

        input = input.replaceAll("\\s+", "");
        if (input.startsWith(",")) {
            input = input.substring(1);
        }
        if (input.endsWith(",")) {
            input = input.substring(0, input.length() - 1);
        }

        String[] parts = input.split(",");
        List<Integer> ticketNumbers = new ArrayList<>();

        try {
            for (String part : parts) {
                if (!part.isEmpty()) {
                    int num = Integer.parseInt(part.trim());
                    if (num > 0 && num <= tickets.size()) {
                        ticketNumbers.add(num);
                    }
                }
            }
        } catch (NumberFormatException e) {
            System.out.println("Ошибка ввода! Используйте формат: 1,2,3");
            return;
        }

        if (ticketNumbers.isEmpty()) {
            System.out.println("Не выбрано корректных номеров билетов!");
            return;
        }

        Set<Integer> uniqueNumbers = new TreeSet<>(ticketNumbers);
        List<Integer> selectedNumbers = new ArrayList<>(uniqueNumbers);

        System.out.println("\nВы выбрали билеты:");
        double totalSum = 0;
        for (int num : selectedNumbers) {
            Ticket ticket = tickets.get(num - 1);
            Movie movie = movieService.getById(ticket.getMovieId());
            System.out.printf("  %d. %s - место %d (%.2f руб.)\n",
                    num, movie.getTitle(), ticket.getSeatNumber(), ticket.getPrice());
            totalSum += ticket.getPrice();
        }

        System.out.printf("Количество: %d, Общая сумма: %.2f руб.\n", selectedNumbers.size(), totalSum);
        System.out.print("\nПодтвердить возврат этих билетов? (да/нет): ");
        String confirm = scanner.nextLine().trim().toLowerCase();

        if (!confirm.equals("да")) {
            System.out.println("Возврат отменен");
            return;
        }

        List<Integer> ticketIds = new ArrayList<>();
        for (int num : selectedNumbers) {
            ticketIds.add(tickets.get(num - 1).getId());
        }

        int returnedCount = ticketService.returnMultipleUserTickets(ticketIds, user.getId(), currentUser.getLogin());

        if (returnedCount > 0) {
            System.out.println("Успешно возвращено " + returnedCount + " из " + selectedNumbers.size() + " билетов");

            double returnedSum = 0;
            for (int i = 0; i < returnedCount; i++) {
                returnedSum += tickets.get(selectedNumbers.get(i) - 1).getPrice();
            }
            System.out.printf("Возвращено средств: %.2f руб.\n", returnedSum);

            StringBuilder seats = new StringBuilder();
            for (int i = 0; i < returnedCount; i++) {
                if (i > 0) seats.append(", ");
                seats.append(tickets.get(selectedNumbers.get(i) - 1).getSeatNumber());
            }

            fileService.log(currentUser.getLogin(),
                    "Вернул " + returnedCount + " билет(ов) пользователя " + user.getLogin() +
                            " (места: " + seats + "). Сумма возврата: " +
                            String.format("%.2f", returnedSum) + " руб.");
        } else {
            System.out.println("Не удалось вернуть ни одного билета!");
        }
    }

    private void returnAllUserTickets(User user) {
        List<Ticket> tickets = ticketService.getUserTickets(user.getId());

        if (tickets.isEmpty()) {
            System.out.println("У пользователя " + user.getLogin() + " нет билетов");
            return;
        }

        System.out.println("\n=== ВОЗВРАТ ВСЕХ БИЛЕТОВ ===");
        System.out.println("Пользователь: " + user.getLogin());
        System.out.println("Всего билетов: " + tickets.size());

        double totalSum = 0;
        Map<String, List<Integer>> ticketsByMovie = new HashMap<>();

        for (Ticket ticket : tickets) {
            Movie movie = movieService.getById(ticket.getMovieId());
            String movieInfo = movie.getTitle() + " (" +
                    movie.getDateTime().format(DateTimeFormatter.ofPattern("dd.MM.yy HH:mm")) + ")";

            ticketsByMovie.computeIfAbsent(movieInfo, k -> new ArrayList<>())
                    .add(ticket.getSeatNumber());
            totalSum += ticket.getPrice();
        }

        System.out.println("\nСписок билетов:");
        for (Map.Entry<String, List<Integer>> entry : ticketsByMovie.entrySet()) {
            System.out.printf("  %s: места %s\n",
                    entry.getKey(),
                    entry.getValue().stream()
                            .map(String::valueOf)
                            .collect(java.util.stream.Collectors.joining(", ")));
        }

        System.out.printf("\nОбщая сумма к возврату: %.2f руб.\n", totalSum);
        System.out.print("\nВНИМАНИЕ: Это действие необратимо!\n");
        System.out.print("Подтвердите возврат ВСЕХ билетов пользователя " + user.getLogin() + "? (да/нет): ");

        String confirm = scanner.nextLine().trim().toLowerCase();

        if (!confirm.equals("да")) {
            System.out.println("Возврат отменен");
            return;
        }

        int returnedCount = ticketService.returnAllUserTickets(user.getId(), currentUser.getLogin());

        if (returnedCount > 0) {
            System.out.println("Успешно возвращено " + returnedCount + " билетов!");
            System.out.printf("Возвращено средств: %.2f руб.\n", totalSum);
            fileService.log(currentUser.getLogin(),
                    "Вернул ВСЕ билеты (" + returnedCount + " шт.) пользователя " +
                            user.getLogin() + " на сумму " + totalSum + " руб.");
        } else {
            System.out.println("Не удалось вернуть билеты!");
        }
    }
    // ==================== USER MANAGEMENT SECTION ====================
    private void manageUsers() {
        fileService.log(currentUser.getLogin(), "Зашел в управление пользователями");

        while (true) {
            System.out.println("\n=== УПРАВЛЕНИЕ ПОЛЬЗОВАТЕЛЯМИ ===");
            System.out.println("1. Список пользователей");
            System.out.println("2. Создать пользователя");
            System.out.println("3. Удалить пользователя");
            System.out.println("4. Изменить уровень");
            System.out.println("0. Назад");
            System.out.print("Выберите: ");

            int choice = getNumber();

            switch (choice) {
                case 1 -> showAllUsers();
                case 2 -> createUser();
                case 3 -> deleteUser();
                case 4 -> changeUserLevel();
                case 0 -> { return; }
                default -> System.out.println("Неправильный выбор");
            }
        }
    }

    private void showAllUsers() {
        List<User> users = userService.getAllUsers();

        if (users.isEmpty()) {
            System.out.println("Нет пользователей");
            return;
        }

        System.out.println("\nСписок пользователей:");
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            System.out.println((i + 1) + ". " + user.getLogin() + " - " + user.getUserLevel());
        }
    }

    private void createUser() {
        System.out.println("\n=== СОЗДАНИЕ ПОЛЬЗОВАТЕЛЯ ===");

        System.out.print("Логин: ");
        String login = scanner.nextLine();

        if (!LoginValidator.isValid(login)) {
            System.out.println("Ошибка: Логин не должен содержать слов admin, админ или их вариаций");
            return;
        }

        String password;
        String passwordError;

        while (true) {
            System.out.print("Пароль (минимум 8 символов, хотя бы 1 буква): ");
            password = scanner.nextLine();

            passwordError = PasswordValidator.validateWithMessage(password);
            if (passwordError == null) {
                break;
            }
            System.out.println("Ошибка: " + passwordError);
        }

        System.out.println("Уровень доступа:");
        System.out.println("1. USER");
        System.out.println("2. MANAGER");
        System.out.println("3. ADMIN");
        System.out.print("Выберите уровень: ");

        int levelChoice = getNumber();
        UserLevel userLevel;

        switch (levelChoice) {
            case 1 -> userLevel = UserLevel.USER;
            case 2 -> userLevel = UserLevel.MANAGER;
            case 3 -> userLevel = UserLevel.ADMIN;
            default -> {
                System.out.println("Неправильный выбор, установлен уровень USER");
                userLevel = UserLevel.USER;
            }
        }

        try {
            User user = userService.register(login, password, userLevel);
            if (user != null) {
                System.out.println("Пользователь " + login + " успешно создан с уровнем " + userLevel);
                fileService.log(currentUser.getLogin(), "Создал пользователя: " + login + " с уровнем " + userLevel);
            } else {
                System.out.println("Ошибка создания пользователя. Логин \"" + login + "\" уже занят.");
            }
        } catch (UserRepositoryException e) {
            System.out.println("Ошибка при создании пользователя: " + e.getMessage());
        }
    }

    private void deleteUser() {
        showAllUsers();

        if (userService.getAllUsers().isEmpty()) {
            return;
        }

        System.out.print("Номер пользователя для удаления: ");
        int choice = getNumber();

        List<User> users = userService.getAllUsers();

        if (choice > 0 && choice <= users.size()) {
            User userToDelete = users.get(choice - 1);

            if (userToDelete.getId() == currentUser.getId()) {
                System.out.println("Нельзя удалить самого себя!");
                return;
            }

            List<Ticket> userTickets = ticketService.getUserTickets(userToDelete.getId());

            System.out.print("Уверены, что хотите удалить пользователя \"" +
                    userToDelete.getLogin() + "\"? ");

            if (!userTickets.isEmpty()) {
                System.out.print("У пользователя есть " + userTickets.size() +
                        " билет(ов). Они будут освобождены. ");
            }

            System.out.print("(да/нет): ");
            String confirm = scanner.nextLine().toLowerCase();

            if (confirm.equals("да")) {
                int returnedTickets = 0;
                if (!userTickets.isEmpty()) {
                    for (Ticket ticket : userTickets) {
                        if (ticketService.returnTicket(ticket.getId(),
                                currentUser.getLogin() + " (удаление пользователя)")) {
                            returnedTickets++;
                        }
                    }
                }

                try {
                    if (userService.deleteUser(userToDelete.getId(), currentUser.getLogin())) {
                        System.out.println("Пользователь \"" + userToDelete.getLogin() +
                                "\" удален. Освобождено билетов: " + returnedTickets);
                    } else {
                        System.out.println("Ошибка удаления пользователя!");
                    }
                } catch (UserRepositoryException e) {
                    throw new RuntimeException(e);
                }
            } else {
                System.out.println("Удаление отменено");
            }
        } else {
            System.out.println("Неверный выбор!");
        }
    }

    private void changeUserLevel() {
        showAllUsers();

        if (userService.getAllUsers().isEmpty()) {
            return;
        }

        System.out.print("Номер пользователя: ");
        int choice = getNumber();

        List<User> users = userService.getAllUsers();

        if (choice > 0 && choice <= users.size()) {
            User user = users.get(choice - 1);

            if (user.getId() == currentUser.getId()) {
                System.out.println("Нельзя изменить уровень самому себе!");
                return;
            }

            System.out.println("Новый уровень для " + user.getLogin() + ":");
            System.out.println("1. USER");
            System.out.println("2. MANAGER");
            System.out.println("3. ADMIN");
            System.out.print("Выберите: ");

            int levelChoice = getNumber();
            UserLevel newLevel;

            switch (levelChoice) {
                case 1 -> newLevel = UserLevel.USER;
                case 2 -> newLevel = UserLevel.MANAGER;
                case 3 -> newLevel = UserLevel.ADMIN;
                default -> {
                    System.out.println("Неправильный выбор");
                    return;
                }
            }

            try {
                if (userService.changeLevel(user.getId(), newLevel, currentUser.getLogin())) {
                    System.out.println("Уровень изменен");
                } else {
                    System.out.println("Ошибка изменения");
                }
            } catch (UserRepositoryException e) {
                throw new RuntimeException(e);
            }
        } else {
            System.out.println("Неверный выбор!");
        }
    }

    private void viewLogs() {
        List<User> users = userService.getAllUsers();

        if (users.isEmpty()) {
            System.out.println("Нет зарегистрированных пользователей");
            return;
        }

        System.out.println("\nПользователи:");
        for (int i = 0; i < users.size(); i++) {
            System.out.println((i + 1) + ". " + users.get(i).getLogin());
        }

        System.out.print("Выберите пользователя (0 - назад): ");
        int choice = getNumber();

        if (choice == 0) return;

        if (choice > 0 && choice <= users.size()) {
            fileService.showLogs(users.get(choice - 1).getLogin());
        } else {
            System.out.println("Неверный выбор! Доступны пользователи с 1 по " + users.size());
        }
    }

    // ==================== NUMBER SECTION ====================
    private int getNumber() {
        while (true) {
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (Exception e) {
                System.out.print("Введите число: ");
            }
        }
    }

    private double getDouble() {
        while (true) {
            try {
                return Double.parseDouble(scanner.nextLine().trim());
            } catch (Exception e) {
                System.out.print("Введите число: ");
            }
        }
    }

    // ==================== PAYMENT SECTION ====================
    private boolean processPayment(double amount, String description) {
        System.out.println("\n" + "=".repeat(40));
        System.out.println("          ОПЛАТА БИЛЕТОВ");
        System.out.println("=".repeat(40));
        System.out.println("Сумма к оплате: " + amount + " руб.");
        System.out.println("Описание: " + description);

        while (true) {
            System.out.println("\nВыберите способ оплаты:");
            System.out.println("1. Банковская карта");
            System.out.println("2. ЮMoney");
            System.out.println("0. Отмена покупки");
            System.out.print("Выберите: ");

            int choice = getNumber();

            switch (choice) {
                case 1 -> {
                    return processCardPayment(amount, description);
                }
                case 2 -> {
                    return processYooMoneyPayment(amount, description);
                }
                case 0 -> {
                    System.out.println("Оплата отменена");
                    return false;
                }
                default -> System.out.println("Неверный выбор! Попробуйте снова.");
            }
        }
    }

    private boolean processCardPayment(double amount, String description) {
        System.out.println("\n" + "=".repeat(40));
        System.out.println("      ОПЛАТА БАНКОВСКОЙ КАРТОЙ");
        System.out.println("=".repeat(40));

        String cardNumber;
        while (true) {
            System.out.print("Номер карты (16 цифр без пробелов): ");
            cardNumber = scanner.nextLine().replaceAll("\\s+", "");

            if (cardNumber.matches("\\d{16}")) {
                break;
            }
            System.out.println("Ошибка! Номер карты должен содержать 16 цифр.");
        }

        String expiryDate;
        while (true) {
            System.out.print("Срок действия (ММ/ГГ): ");
            expiryDate = scanner.nextLine();

            if (expiryDate.matches("\\d{2}/\\d{2}")) {
                break;
            }
            System.out.println("Ошибка! Формат: ММ/ГГ (например: 12/28)");
        }

        String cvv;
        while (true) {
            System.out.print("CVV/CVC (3 цифры): ");
            cvv = scanner.nextLine();

            if (cvv.matches("\\d{3}")) {
                break;
            }
            System.out.println("Ошибка! CVV должен содержать 3 цифры.");
        }

        PaymentTransaction transaction = paymentService.processCardPayment(
                cardNumber, expiryDate, cvv, amount, description
        );

        if (transaction.isSuccess()) {
            generateReceipt(transaction);
            fileService.log(currentUser.getLogin(),
                    "Оплатил " + amount + " руб. картой. Транзакция: " + transaction.getId());
            return true;
        }

        return false;
    }

    private boolean processYooMoneyPayment(double amount, String description) {
        System.out.println("\n" + "=".repeat(40));
        System.out.println("        ОПЛАТА ЧЕРЕЗ ЮMONEY");
        System.out.println("=".repeat(40));

        String walletNumber;
        while (true) {
            System.out.print("Номер кошелька ЮMoney (11 цифр): ");
            walletNumber = scanner.nextLine().replaceAll("\\s+", "");

            if (walletNumber.matches("\\d{11}")) {
                break;
            }
            System.out.println("Ошибка! Номер кошелька должен содержать 11 цифр.");
        }

        System.out.println("\nИнициируем оплату через ЮMoney...");

        PaymentTransaction transaction = paymentService.processYooMoneyPayment(
                walletNumber, amount, description
        );

        if (transaction.isSuccess()) {
            generateReceipt(transaction);
            fileService.log(currentUser.getLogin(),
                    "Оплатил " + amount + " руб. через ЮMoney. Транзакция: " + transaction.getId());
            return true;
        }

        return false;
    }

    private void generateReceipt(PaymentTransaction transaction) {
        System.out.println("\n" + "=".repeat(40));
        System.out.println("       ЭЛЕКТРОННЫЙ ЧЕК");
        System.out.println("=".repeat(40));
        System.out.println("Кинотеатр: 'Cinema App'");
        System.out.println("Кассир: Система");
        System.out.println("Покупатель: " + currentUser.getLogin());
        System.out.println("Транзакция: #" + transaction.getId());
        System.out.println("Способ оплаты: " + transaction.getPaymentMethod());
        System.out.println("Сумма: " + transaction.getAmount() + " руб.");
        System.out.println("Описание: " + transaction.getDescription());
        System.out.println("Время: " + transaction.getTimestamp().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));
        System.out.println("Статус: УСПЕШНО");
        System.out.println("=".repeat(40));
        System.out.println("Чек сохранен в системе.");
        System.out.println("Спасибо за покупку!");
        System.out.println("=".repeat(40));
    }

    // ==================== PROMO CODE SECTION ====================
    private void showMyPromoCode() {
        System.out.println("\n" + "=".repeat(40));
        System.out.println("          ПРОМОКОД СОТРУДНИКА");
        System.out.println("=".repeat(40));

        try {
            promoCodeRepository.deactivateExpiredPromoCodes();
            promoCodeService.showMyPromoCodes(currentUser);

        } catch (PromoCodeException e) {
            System.out.println(e.getMessage());

            System.out.print("\nХотите сгенерировать новый промокод? (да/нет): ");
            String answer = scanner.nextLine().toLowerCase();

            if (answer.equals("да")) {
                try {
                    PromoCode newPromoCode = promoCodeService.generatePromoCode(currentUser);

                    System.out.println("\n" + "=".repeat(20));
                    System.out.println(" НОВЫЙ ПРОМОКОД СГЕНЕРИРОВАН!");
                    System.out.println("=".repeat(20));
                    System.out.println(newPromoCode);

                    if (currentUser.getUserLevel() == UserLevel.ADMIN) {
                        System.out.println(" Скидка администратора: 25%");
                    } else {
                        System.out.println(" Скидка менеджера: 20%");
                    }

                    System.out.println(" Действует: 24 часа с момента создания");
                    System.out.println(" Осталось использований: 10");
                    System.out.println("=".repeat(20));

                    fileService.log(currentUser.getLogin(),
                            "Сгенерировал промокод: " + newPromoCode.getCode());

                } catch (PromoCodeException ex) {
                    System.out.println(" Ошибка: " + ex.getMessage());
                }
            }
        }
        scanner.nextLine();
    }
}