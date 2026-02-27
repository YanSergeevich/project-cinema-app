package by.academy.project.models;

import java.time.LocalDateTime;

public class PromoCode {
    private int id;
    private String code;
    private int ownerUserId;
    private double discountPercent;
    private int maxUses;
    private int usedCount;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private boolean isActive;

    public PromoCode() {
    }

    public PromoCode(String code, int ownerUserId, double discountPercent, int maxUses) {
        this.code = code;
        this.ownerUserId = ownerUserId;
        this.discountPercent = discountPercent;
        this.maxUses = maxUses;
        this.usedCount = 0;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusHours(24);
        this.isActive = true;
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getCode() {
        return code;
    }
    public void setCode(String code) {
        this.code = code;
    }
    public int getOwnerUserId() {
        return ownerUserId;
    }
    public void setOwnerUserId(int ownerUserId) {
        this.ownerUserId = ownerUserId;
    }
    public double getDiscountPercent() {
        return discountPercent;
    }
    public void setDiscountPercent(double discountPercent) {
        this.discountPercent = discountPercent;
    }
    public int getMaxUses() {
        return maxUses;
    }
    public void setMaxUses(int maxUses) {
        this.maxUses = maxUses;
    }
    public int getUsedCount() {
        return usedCount;
    }
    public void setUsedCount(int usedCount) {
        this.usedCount = usedCount;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    public boolean isActive() {
        return isActive;
    }
    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isValid() {
        return isActive &&
                usedCount < maxUses &&
                LocalDateTime.now().isBefore(expiresAt);
    }

    public int getRemainingUses() {
        return maxUses - usedCount;
    }

    @Override
    public String toString() {
        return String.format("Промокод: %s | Скидка: %.0f%% | Осталось использований: %d | Действует до: %s",
                code, discountPercent, getRemainingUses(),
                expiresAt.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
    }
}