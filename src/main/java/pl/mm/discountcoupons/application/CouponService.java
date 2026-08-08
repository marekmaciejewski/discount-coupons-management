package pl.mm.discountcoupons.application;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.mm.discountcoupons.api.dto.CouponCreateRequest;
import pl.mm.discountcoupons.api.dto.CouponRedemptionRequest;
import pl.mm.discountcoupons.api.dto.CouponRedemptionResponse;
import pl.mm.discountcoupons.api.dto.CouponResponse;
import pl.mm.discountcoupons.config.CouponPrototypeConfiguration;
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

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository couponRedemptionRepository;
    private final CouponPrototypeConfiguration couponPrototypes;
    private final IpCountryResolver ipCountryResolver;

    @Transactional
    public CouponResponse createCoupon(CouponCreateRequest request) {
        Coupon coupon = couponPrototypes.coupon(request);
        try {
            return couponPrototypes.couponResponse(couponRepository.save(coupon));
        } catch (DataIntegrityViolationException e) {
            throw new CouponAlreadyExistsException(coupon.code() + " coupon already exists");
        }
    }

    public CouponResponse getCoupon(String code) {
        return couponRepository.findByNormalizedCode(couponPrototypes.normalizeCode(code))
                .map(couponPrototypes::couponResponse)
                .orElseThrow(() -> new CouponNotFoundException(code + " coupon not found"));
    }

    @Transactional
    public CouponRedemptionResponse redeemCoupon(CouponRedemptionRequest request, String clientIp) {
        String code = request.getCode().trim();
        Coupon coupon = getCouponForUpdate(code);
        String resolvedCountryCode = ipCountryResolver.resolveCountryCode(clientIp);
        validateCouponCountry(coupon, resolvedCountryCode);

        CouponRedemption couponRedemption =
                couponPrototypes.couponRedemption(coupon, request, clientIp, resolvedCountryCode);
        try {
            couponRedemptionRepository.save(couponRedemption);
        } catch (DataIntegrityViolationException e) {
            throw new CouponAlreadyUsedException(
                    couponRedemption.userId() + " already used " + coupon.code() + " coupon");
        }

        int updatedRows = couponRepository.incrementCurrentUsesIfAvailable(coupon.id());
        if (updatedRows == 0) {
            throw new CouponExhaustedException(coupon.code() + " coupon has reached its maximum number of uses");
        }

        Coupon updatedCoupon = couponRepository.findById(coupon.id())
                .orElseThrow(() -> new CouponNotFoundException(coupon.code() + " coupon not found"));
        return couponPrototypes.couponRedemptionResponse(updatedCoupon, couponRedemption);
    }

    private Coupon getCouponForUpdate(String code) {
        return couponRepository.findByNormalizedCode(couponPrototypes.normalizeCode(code))
                .orElseThrow(() -> new CouponNotFoundException(code + " coupon not found"));
    }

    private static void validateCouponCountry(Coupon coupon, String resolvedCountryCode) {
        if (!coupon.countryCode().equals(resolvedCountryCode)) {
            throw new CouponCountryMismatchException(
                    coupon.code() + " coupon is not available in " + resolvedCountryCode);
        }
    }
}
