package by.academy.project.service.business;

import by.academy.project.exception.PromoCodeException;
import by.academy.project.models.PromoCode;
import by.academy.project.models.User;
import by.academy.project.models.UserLevel;
import by.academy.project.repository.PromoCodeRepository;
import by.academy.project.service.files.FileService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

public class PromoCodeService implements PromoCodeServiceInterface {
    private final PromoCodeRepository promoCodeRepository;
    private final FileService fileService;
    private final Random random = new Random();

    public PromoCodeService(PromoCodeRepository promoCodeRepository, FileService fileService) {
        this.promoCodeRepository = promoCodeRepository;
        this.fileService = fileService;
    }

    public PromoCode generatePromoCode(User user) throws PromoCodeException {
        if (user.getUserLevel() != UserLevel.MANAGER && user.getUserLevel() != UserLevel.ADMIN) {
            throw new PromoCodeException("Только менеджеры и администраторы могут генерировать промокоды");
        }

        promoCodeRepository.deactivateExpiredPromoCodes();

        List<PromoCode> activePromoCodes = promoCodeRepository.getActivePromoCodesByUserId(user.getId());
        for (PromoCode code : activePromoCodes) {
            if (code.isValid()) {
                throw new PromoCodeException("У вас уже есть активный промокод. Используйте его перед созданием нового.");
            }
        }

        LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);
        int promoCodesThisYear = promoCodeRepository.getPromoCodeCountByUserIdSince(user.getId(), oneYearAgo);

        if (promoCodesThisYear >= 10) {
            throw new PromoCodeException("Вы исчерпали годовой лимит промокодов (10/10). Новый промокод будет доступен через год.");
        }

        int remainingPromoCodes = 10 - promoCodesThisYear;

        String code = generateUniqueCode(user);
        double discountPercent = user.getUserLevel() == UserLevel.ADMIN ? 25.0 : 20.0;
        int maxUses = 10;

        PromoCode promoCode = new PromoCode(code, user.getId(), discountPercent, maxUses);

        if (promoCodeRepository.createPromoCode(promoCode)) {
            fileService.log(user.getLogin(),
                    String.format("Сгенерировал новый промокод: %s (скидка %.0f%%, действует 24 часа, осталось промокодов в этом году: %d)",
                            code, discountPercent, remainingPromoCodes - 1));
            return promoCode;
        }

        throw new PromoCodeException("Не удалось создать промокод в базе данных");
    }

    public double applyPromoCode(String code, double originalPrice, String userLogin) throws PromoCodeException {
        PromoCode promoCode = promoCodeRepository.getPromoCodeByCode(code);

        if (promoCode == null) {
            throw new PromoCodeException("Промокод '" + code + "' не найден!");
        }

        if (!promoCode.isValid()) {
            if (!promoCode.isActive()) {
                throw new PromoCodeException("Промокод '" + code + "' деактивирован!");
            } else if (promoCode.getUsedCount() >= promoCode.getMaxUses()) {
                throw new PromoCodeException("Промокод '" + code + "' исчерпал лимит использований!");
            } else if (LocalDateTime.now().isAfter(promoCode.getExpiresAt())) {
                promoCodeRepository.deactivatePromoCode(promoCode.getId());
                throw new PromoCodeException("Срок действия промокода '" + code + "' истек!");
            }
        }

        double discountAmount = originalPrice * (promoCode.getDiscountPercent() / 100);
        double finalPrice = originalPrice - discountAmount;

        if (promoCodeRepository.incrementUsedCount(promoCode.getId())) {
            fileService.log(userLogin,
                    String.format("Применил промокод: %s (скидка %.0f%%, сэкономлено %.2f руб.)",
                            code, promoCode.getDiscountPercent(), discountAmount));

            if (promoCode.getUsedCount() + 1 >= promoCode.getMaxUses()) {
                promoCodeRepository.deactivatePromoCode(promoCode.getId());
            }
            return finalPrice;
        }

        throw new PromoCodeException("Не удалось обновить счетчик использований промокода");
    }

    public void showMyPromoCodes(User user) throws PromoCodeException {
        List<PromoCode> promoCodes = promoCodeRepository.getActivePromoCodesByUserId(user.getId());

        if (promoCodes.isEmpty()) {
            throw new PromoCodeException("У вас нет активных промокодов.");
        }

        System.out.println("\n" + "=".repeat(40));
        System.out.println("        ВАШИ АКТИВНЫЕ ПРОМОКОДЫ");
        System.out.println("=".repeat(40));

        boolean hasValidCode = false;
        for (PromoCode promoCode : promoCodes) {
            if (promoCode.isValid()) {
                System.out.println(promoCode);
                hasValidCode = true;
            }
        }

        if (!hasValidCode) {
            throw new PromoCodeException("У вас нет активных промокодов.");
        }

        System.out.println("=".repeat(50));
    }

    private String generateUniqueCode(User user) throws PromoCodeException {
        String prefix = user.getUserLevel() == UserLevel.ADMIN ? "ADMIN" : "MNG";
        String uniqueCode;
        int attempts = 0;
        int maxAttempts = 100;

        do {
            if (attempts++ > maxAttempts) {
                throw new PromoCodeException("Не удалось сгенерировать уникальный промокод");
            }
            int randomNum = random.nextInt(10000, 99999);
            uniqueCode = prefix + randomNum;
        } while (promoCodeRepository.getPromoCodeByCode(uniqueCode) != null);

        return uniqueCode;
    }
}