package pl.mm.discountcoupons.persistence;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface CouponRepository extends CrudRepository<Coupon, Long> {

    Optional<Coupon> findByNormalizedCode(String normalizedCode);

    @Modifying
    @Query("""
            UPDATE COUPON
            SET CURRENT_USES = CURRENT_USES + 1
            WHERE ID = :couponId
              AND CURRENT_USES < MAX_USES
            """)
    int incrementCurrentUsesIfAvailable(long couponId);
}
