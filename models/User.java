package by.academy.project.models;

import java.util.ArrayList;
import java.util.List;

public class User {
    private int id;
    private String login;
    private UserLevel userLevel;
    private final List<Movie> watchedMovies = new ArrayList<>();

    public User() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public UserLevel getUserLevel() { return userLevel; }
    public void setUserLevel(UserLevel userLevel) { this.userLevel = userLevel; }

    public List<Movie> getWatchedMovies() { return watchedMovies; }
    public void addWatchedMovie(Movie movie) { watchedMovies.add(movie); }

    @Override
    public String toString() {
        return login + " (" + userLevel + ")";
    }
}