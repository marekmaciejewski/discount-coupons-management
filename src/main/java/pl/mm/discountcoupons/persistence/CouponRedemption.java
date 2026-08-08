package pl.mm.discountcoupons.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("COUPON_REDEMPTION")
public record CouponRedemption(
        @Id
        @Column("ID")
        Long id,
        @Column("COUPON_ID")
        Long couponId,
        @Column("USER_ID")
        String userId,
        @Column("CLIENT_IP")
        String clientIp,
        @Column("RESOLVED_COUNTRY_CODE")
        String resolvedCountryCode,
        @Column("USED_AT")
        Instant usedAt) {
}
