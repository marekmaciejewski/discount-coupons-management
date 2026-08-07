package pl.mm.discountcoupons.persistence;

import java.time.Instant;

public record Coupon(
        Long id,
        String code,
        String normalizedCode,
        Instant createdAt,
        int maxUses,
        int currentUses,
        String countryCode) {
}
