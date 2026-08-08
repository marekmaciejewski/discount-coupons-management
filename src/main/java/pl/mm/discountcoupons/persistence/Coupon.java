package pl.mm.discountcoupons.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("COUPON")
public record Coupon(
        @Id
        @Column("ID")
        Long id,
        @Column("CODE")
        String code,
        @Column("NORMALIZED_CODE")
        String normalizedCode,
        @Column("CREATED_AT")
        Instant createdAt,
        @Column("MAX_USES")
        int maxUses,
        @Column("CURRENT_USES")
        int currentUses,
        @Column("COUNTRY_CODE")
        String countryCode) {
}
