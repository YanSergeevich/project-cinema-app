package by.academy.project.service.pay;

import java.time.LocalDateTime;

public class PaymentTransaction {
    private final int id;
    private final double amount;
    private final String paymentMethod;
    private final String description;
    private final boolean success;
    private final LocalDateTime timestamp;

    public PaymentTransaction(int id, double amount, String paymentMethod,
                              String description, boolean success) {
        this.id = id;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.description = description;
        this.success = success;
        this.timestamp = LocalDateTime.now();
    }

    public int getId() { return id; }
    public double getAmount() { return amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getDescription() { return description; }
    public boolean isSuccess() { return success; }
    public LocalDateTime getTimestamp() { return timestamp; }
}