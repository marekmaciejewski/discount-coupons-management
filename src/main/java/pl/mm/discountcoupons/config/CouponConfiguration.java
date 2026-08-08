package pl.mm.discountcoupons.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.web.client.RestClient;
import pl.mm.discountcoupons.api.dto.CouponCreateRequest;
import pl.mm.discountcoupons.api.dto.CouponRedemptionRequest;
import pl.mm.discountcoupons.api.dto.CouponRedemptionResponse;
import pl.mm.discountcoupons.api.dto.CouponResponse;
import pl.mm.discountcoupons.persistence.Coupon;
import pl.mm.discountcoupons.persistence.CouponRedemption;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@Configuration
@RequiredArgsConstructor
public class CouponConfiguration {

    private final Clock clock;

    @Bean
    static Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public RestClient ipwhoisRestClient(@Value("${app.geo.ipwhois.base-url}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public Coupon coupon(CouponCreateRequest request) {
        String code = request.getCode().trim();
        return new Coupon(
                null,
                code,
                normalizeCode(code),
                now(),
                request.getMaxUses(),
                0,
                normalizeCountryCode(request.getCountryCode()));
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public CouponRedemption couponRedemption(
            Coupon coupon,
            CouponRedemptionRequest request,
            String clientIp,
            String resolvedCountryCode) {
        return new CouponRedemption(
                null,
                coupon.id(),
                request.getUserId().trim(),
                clientIp,
                resolvedCountryCode,
                now());
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public CouponResponse couponResponse(Coupon coupon) {
        return new CouponResponse(
                coupon.code(),
                toOffsetDateTime(coupon.createdAt()),
                coupon.maxUses(),
                coupon.currentUses(),
                coupon.countryCode());
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public CouponRedemptionResponse couponRedemptionResponse(Coupon coupon, CouponRedemption couponRedemption) {
        return new CouponRedemptionResponse(
                coupon.code(),
                couponRedemption.userId(),
                toOffsetDateTime(couponRedemption.usedAt()),
                coupon.currentUses(),
                coupon.maxUses(),
                coupon.countryCode());
    }

    private static OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    public String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    public Instant now() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
    }

    private static String normalizeCountryCode(String countryCode) {
        return countryCode.trim().toUpperCase(Locale.ROOT);
    }
}
