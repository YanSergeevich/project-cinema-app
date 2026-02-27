package by.academy.project.service.pay;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class PaymentProcessing {
    private final Map<Integer, PaymentTransaction> transactionHistory = new HashMap<>();
    private final Random random = new Random();

    public PaymentTransaction processCardPayment(String cardNumber, String expiryDate,
                                                 String cvv, double amount, String description) {
        System.out.println("\n" + "=".repeat(40));
        System.out.println("   ОБРАБОТКА ПЛАТЕЖА ПО КАРТЕ");
        System.out.println("=".repeat(40));
        System.out.println("Карта: ****" + getLastFourDigits(cardNumber));
        System.out.println("Срок: " + expiryDate);
        System.out.println("Сумма: " + amount + " руб.");
        System.out.println("Описание: " + description);

        System.out.print("Обработка платежа");
        for (int i = 0; i < 3; i++) {
            try {
                Thread.sleep(500);
                System.out.print(".");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println();

        int transactionId = 100000 + random.nextInt(900000);
        PaymentTransaction transaction = new PaymentTransaction(
                transactionId, amount, "Банковская карта", description, true
        );

        transactionHistory.put(transactionId, transaction);

        System.out.println("✓ Платеж успешно проведен!");
        System.out.println("Номер транзакции: " + transactionId);
        System.out.println("=".repeat(40));

        return transaction;
    }

    public PaymentTransaction processYooMoneyPayment(String walletNumber, double amount, String description) {
        System.out.println("\n" + "=".repeat(40));
        System.out.println("   ОПЛАТА ЧЕРЕЗ ЮMONEY");
        System.out.println("=".repeat(40));
        System.out.println("Кошелек: " + walletNumber);
        System.out.println("Сумма: " + amount + " руб.");
        System.out.println("Описание: " + description);

        System.out.print("Перенаправление на страницу оплаты ЮMoney");
        for (int i = 0; i < 3; i++) {
            try {
                Thread.sleep(500);
                System.out.print(".");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println();

        int transactionId = 200000 + random.nextInt(900000);
        PaymentTransaction transaction = new PaymentTransaction(
                transactionId, amount, "ЮMoney", description, true
        );

        transactionHistory.put(transactionId, transaction);

        System.out.println("✓ Оплата через ЮMoney успешна!");
        System.out.println("Номер транзакции: " + transactionId);
        System.out.println("=".repeat(40));

        return transaction;
    }

    private String getLastFourDigits(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "0000";
        }
        return cardNumber.substring(cardNumber.length() - 4);
    }
}