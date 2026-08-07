package pl.mm.discountcoupons.persistence;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

@Repository
public class CouponRepository {

    private final JdbcClient jdbcClient;

    public CouponRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Coupon insert(Coupon coupon) {
        jdbcClient.sql("""
                        INSERT INTO COUPON (
                            CODE,
                            NORMALIZED_CODE,
                            CREATED_AT,
                            MAX_USES,
                            CURRENT_USES,
                            COUNTRY_CODE
                        )
                        VALUES (:code, :normalizedCode, :createdAt, :maxUses, :currentUses, :countryCode)
                        """)
                .param("code", coupon.code())
                .param("normalizedCode", coupon.normalizedCode())
                .param("createdAt", Timestamp.from(coupon.createdAt()))
                .param("maxUses", coupon.maxUses())
                .param("currentUses", coupon.currentUses())
                .param("countryCode", coupon.countryCode())
                .update();

        return findByNormalizedCode(coupon.normalizedCode()).orElseThrow();
    }

    public Optional<Coupon> findById(long id) {
        return jdbcClient.sql("""
                        SELECT ID, CODE, NORMALIZED_CODE, CREATED_AT, MAX_USES, CURRENT_USES, COUNTRY_CODE
                        FROM COUPON
                        WHERE ID = :id
                        """)
                .param("id", id)
                .query(this::toCoupon)
                .optional();
    }

    public Optional<Coupon> findByNormalizedCode(String normalizedCode) {
        return jdbcClient.sql("""
                        SELECT ID, CODE, NORMALIZED_CODE, CREATED_AT, MAX_USES, CURRENT_USES, COUNTRY_CODE
                        FROM COUPON
                        WHERE NORMALIZED_CODE = :normalizedCode
                        """)
                .param("normalizedCode", normalizedCode)
                .query(this::toCoupon)
                .optional();
    }

    public void insertRedemption(
            long couponId,
            String userId,
            String clientIp,
            String resolvedCountryCode,
            Instant usedAt) {
        jdbcClient.sql("""
                        INSERT INTO COUPON_REDEMPTION (
                            COUPON_ID,
                            USER_ID,
                            CLIENT_IP,
                            RESOLVED_COUNTRY_CODE,
                            USED_AT
                        )
                        VALUES (:couponId, :userId, :clientIp, :resolvedCountryCode, :usedAt)
                        """)
                .param("couponId", couponId)
                .param("userId", userId)
                .param("clientIp", clientIp)
                .param("resolvedCountryCode", resolvedCountryCode)
                .param("usedAt", Timestamp.from(usedAt))
                .update();
    }

    public int incrementCurrentUsesIfAvailable(long couponId) {
        return jdbcClient.sql("""
                        UPDATE COUPON
                        SET CURRENT_USES = CURRENT_USES + 1
                        WHERE ID = :couponId
                          AND CURRENT_USES < MAX_USES
                        """)
                .param("couponId", couponId)
                .update();
    }

    private Coupon toCoupon(ResultSet rs, int rowNumber) throws SQLException {
        return new Coupon(
                rs.getLong("ID"),
                rs.getString("CODE"),
                rs.getString("NORMALIZED_CODE"),
                rs.getTimestamp("CREATED_AT").toInstant(),
                rs.getInt("MAX_USES"),
                rs.getInt("CURRENT_USES"),
                rs.getString("COUNTRY_CODE"));
    }
}
