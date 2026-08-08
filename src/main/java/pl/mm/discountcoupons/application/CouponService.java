package pl.mm.discountcoupons.application;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.mm.discountcoupons.api.dto.CouponCreateRequest;
import pl.mm.discountcoupons.api.dto.CouponRedemptionRequest;
import pl.mm.discountcoupons.api.dto.CouponRedemptionResponse;
import pl.mm.discountcoupons.api.dto.CouponResponse;
import pl.mm.discountcoupons.domain.CouponAlreadyExistsException;
import pl.mm.discountcoupons.domain.CouponAlreadyUsedException;
import pl.mm.discountcoupons.domain.CouponCountryMismatchException;
import pl.mm.discountcoupons.domain.CouponExhaustedException;
import pl.mm.discountcoupons.domain.CouponNotFoundException;
import pl.mm.discountcoupons.ip.IpCountryResolver;
import pl.mm.discountcoupons.persistence.Coupon;
import pl.mm.discountcoupons.persistence.CouponRedemption;
import pl.mm.discountcoupons.persistence.CouponRedemptionRepository;
import pl.mm.discountcoupons.persistence.CouponRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository couponRedemptionRepository;
    private final CouponCodeNormalizer codeNormalizer;
    private final IpCountryResolver ipCountryResolver;
    private final Clock clock;

    @Transactional
    public CouponResponse createCoupon(CouponCreateRequest request) {
        String code = request.getCode().trim();
        String normalizedCode = codeNormalizer.normalize(code);
        String countryCode = normalizeCountryCode(request.getCountryCode());
        Instant createdAt = now();
        Coupon coupon = new Coupon(null, code, normalizedCode, createdAt, request.getMaxUses(), 0, countryCode);

        try {
            return toResponse(couponRepository.save(coupon));
        } catch (DataIntegrityViolationException e) {
            throw new CouponAlreadyExistsException(code + " coupon already exists");
        }
    }

    public CouponResponse getCoupon(String code) {
        return couponRepository.findByNormalizedCode(codeNormalizer.normalize(code))
                .map(this::toResponse)
                .orElseThrow(() -> new CouponNotFoundException(code + " coupon not found"));
    }

    @Transactional
    public CouponRedemptionResponse redeemCoupon(CouponRedemptionRequest request, String clientIp) {
        String code = request.getCode().trim();
        String userId = request.getUserId().trim();
        Coupon coupon = getCouponForUpdate(code);
        String resolvedCountryCode = ipCountryResolver.resolveCountryCode(clientIp);
        validateCouponCountry(coupon, resolvedCountryCode);

        Instant usedAt = now();
        try {
            CouponRedemption couponRedemption =
                    new CouponRedemption(null, coupon.id(), userId, clientIp, resolvedCountryCode, usedAt);
            couponRedemptionRepository.save(couponRedemption);
        } catch (DataIntegrityViolationException e) {
            throw new CouponAlreadyUsedException(userId + " already used " + coupon.code() + " coupon");
        }

        int updatedRows = couponRepository.incrementCurrentUsesIfAvailable(coupon.id());
        if (updatedRows == 0) {
            throw new CouponExhaustedException(coupon.code() + " coupon has reached its maximum number of uses");
        }

        Coupon updatedCoupon = couponRepository.findById(coupon.id())
                .orElseThrow(() -> new CouponNotFoundException(coupon.code() + " coupon not found"));
        return toRedemptionResponse(updatedCoupon, userId, usedAt);
    }

    private Coupon getCouponForUpdate(String code) {
        return couponRepository.findByNormalizedCode(codeNormalizer.normalize(code))
                .orElseThrow(() -> new CouponNotFoundException(code + " coupon not found"));
    }

    private static void validateCouponCountry(Coupon coupon, String resolvedCountryCode) {
        if (!coupon.countryCode().equals(resolvedCountryCode)) {
            throw new CouponCountryMismatchException(
                    coupon.code() + " coupon is not available in " + resolvedCountryCode);
        }
    }

    private CouponResponse toResponse(Coupon coupon) {
        return new CouponResponse(
                coupon.code(),
                toOffsetDateTime(coupon.createdAt()),
                coupon.maxUses(),
                coupon.currentUses(),
                coupon.countryCode());
    }

    private CouponRedemptionResponse toRedemptionResponse(Coupon coupon, String userId, Instant usedAt) {
        return new CouponRedemptionResponse(
                coupon.code(),
                userId,
                toOffsetDateTime(usedAt),
                coupon.currentUses(),
                coupon.maxUses(),
                coupon.countryCode());
    }

    private static String normalizeCountryCode(String countryCode) {
        return countryCode.trim().toUpperCase(Locale.ROOT);
    }

    private Instant now() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
    }

    private static OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
