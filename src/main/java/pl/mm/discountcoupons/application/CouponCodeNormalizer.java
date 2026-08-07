package pl.mm.discountcoupons.application;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class CouponCodeNormalizer {

    public String normalize(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
