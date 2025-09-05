package com.kas.promoservice.exception;

import com.kas.promoservice.model.Promo;

public class PromoNotFoundException extends RuntimeException {
    public static final String PROMO_NOT_FOUND_WITH_ID = "Promo not found with id: ";

    public PromoNotFoundException(String message) {
        super(message);
    }

    public PromoNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
