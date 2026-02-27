package by.academy.project.service.infra;

import java.sql.*;

public class DatabaseManager {
    private static final String URL = "jdbc:mysql://localhost/cinema";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "";
    private static Connection connection;

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("MySQL драйвер не найден!");
        }
    }

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
        }
        return connection;
    }

    public static void initDB() {
        Connection tempConnection;
        Statement stmt;

        try {
            tempConnection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            stmt = tempConnection.createStatement();

            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT," +
                    "login VARCHAR(50) UNIQUE NOT NULL," +
                    "password VARCHAR(255) NOT NULL," +
                    "user_level ENUM('USER', 'MANAGER', 'ADMIN') NOT NULL)");

            stmt.execute("CREATE TABLE IF NOT EXISTS movies (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT," +
                    "title VARCHAR(100) NOT NULL," +
                    "date_time DATETIME NOT NULL," +
                    "duration_minutes INT DEFAULT 120," +
                    "total_seats INT DEFAULT 10," +
                    "ticket_price DECIMAL(10,2) DEFAULT 0.00)");


            try {
                stmt.execute("SELECT duration_minutes FROM movies LIMIT 1");
            } catch (SQLException e) {
                stmt.execute("ALTER TABLE movies ADD COLUMN duration_minutes INT DEFAULT 120");
            }


            try {
                stmt.execute("SELECT ticket_price FROM movies LIMIT 1");
            } catch (SQLException e) {
                stmt.execute("ALTER TABLE movies ADD COLUMN ticket_price DECIMAL(10,2) DEFAULT 0.00");
            }

            stmt.execute("CREATE TABLE IF NOT EXISTS tickets (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT," +
                    "user_id INT," +
                    "movie_id INT NOT NULL," +
                    "seat_number INT NOT NULL," +
                    "price DECIMAL(10,2) NOT NULL," +
                    "is_purchased BOOLEAN DEFAULT FALSE," +
                    "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL," +
                    "FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE," +
                    "UNIQUE KEY unique_movie_seat (movie_id, seat_number))");

            stmt.execute("CREATE TABLE IF NOT EXISTS watched_movies (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT," +
                    "user_id INT NOT NULL," +
                    "movie_id INT NOT NULL," +
                    "watched_date DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE," +
                    "FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE," +
                    "UNIQUE KEY unique_user_movie (user_id, movie_id))");

            String adminHash = PasswordHasher.hash("adm1nadm1n");
            stmt.execute("DELETE FROM users WHERE login = 'admin'");
            stmt.execute("INSERT IGNORE INTO users (login, password, user_level) " +
                    "VALUES ('admin', '" + adminHash + "', 'ADMIN')");

            stmt.execute("CREATE TABLE IF NOT EXISTS promo_codes (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT," +
                    "code VARCHAR(20) UNIQUE NOT NULL," +
                    "owner_user_id INT NOT NULL," +
                    "discount_percent DECIMAL(5,2) NOT NULL," +
                    "max_uses INT NOT NULL DEFAULT 10," +
                    "used_count INT NOT NULL DEFAULT 0," +
                    "created_at DATETIME NOT NULL," +
                    "expires_at DATETIME NOT NULL," +
                    "is_active BOOLEAN DEFAULT TRUE," +
                    "FOREIGN KEY (owner_user_id) REFERENCES users(id) ON DELETE CASCADE)");

        } catch (Exception e) {
            System.out.println(" Ошибка БД: " + e.getMessage());
            e.printStackTrace();
        }
    }
}