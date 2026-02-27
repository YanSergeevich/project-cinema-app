package by.academy.project.repository;

import by.academy.project.exception.PromoCodeException;
import by.academy.project.models.PromoCode;
import java.time.LocalDateTime;
import java.util.List;

public interface PromoCodeRepositoryInterface {
    boolean createPromoCode(PromoCode promoCode) throws PromoCodeException;
    PromoCode getPromoCodeByCode(String code) throws PromoCodeException;
    List<PromoCode> getActivePromoCodesByUserId(int userId) throws PromoCodeException;
    boolean incrementUsedCount(int promoCodeId) throws PromoCodeException;
    void deactivatePromoCode(int promoCodeId) throws PromoCodeException;
    void deactivateExpiredPromoCodes() throws PromoCodeException;
    int getPromoCodeCountByUserIdSince(int userId, LocalDateTime since);
}