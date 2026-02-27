package by.academy.project.models;

public class Ticket {
    private int id;
    private int userId;
    private int movieId;
    private int seatNumber;
    private double price;
    private boolean purchased;

    public Ticket() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getMovieId() { return movieId; }
    public void setMovieId(int movieId) { this.movieId = movieId; }

    public int getSeatNumber() { return seatNumber; }
    public void setSeatNumber(int seatNumber) { this.seatNumber = seatNumber; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public boolean isPurchased() { return purchased; }
    public void setPurchased(boolean purchased) { this.purchased = purchased; }

    @Override
    public String toString() {
        return "Билет " + id + ": место " + seatNumber + ", цена " + price + " руб.";
    }
}