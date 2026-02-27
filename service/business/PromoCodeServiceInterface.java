package by.academy.project.service.business;

import by.academy.project.exception.PromoCodeException;
import by.academy.project.models.PromoCode;
import by.academy.project.models.User;

public interface PromoCodeServiceInterface {
    PromoCode generatePromoCode(User user) throws PromoCodeException;
    double applyPromoCode(String code, double originalPrice, String userLogin) throws PromoCodeException;
    void showMyPromoCodes(User user) throws PromoCodeException;
}