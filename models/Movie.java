package by.academy.project.models;

import java.time.LocalDateTime;

public class Movie {
    private int id;
    private String title;
    private LocalDateTime dateTime;
    private int durationMinutes;
    private int totalSeats;

    public Movie() {}

    public Movie(String title, LocalDateTime dateTime, int durationMinutes, int totalSeats) {
        this.title = title;
        this.dateTime = dateTime;
        this.durationMinutes = durationMinutes;
        this.totalSeats = totalSeats;
    }

    public boolean isFinished() {
        return dateTime.plusMinutes(durationMinutes).isBefore(LocalDateTime.now());
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public LocalDateTime getDateTime() { return dateTime; }
    public void setDateTime(LocalDateTime dateTime) { this.dateTime = dateTime; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public int getTotalSeats() { return totalSeats; }
    public void setTotalSeats(int totalSeats) { this.totalSeats = totalSeats; }

    @Override
    public String toString() {
        return title + " - " + dateTime + " (" + durationMinutes + " мин., " + totalSeats + " мест)";
    }
}