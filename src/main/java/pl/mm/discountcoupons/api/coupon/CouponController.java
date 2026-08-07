package pl.mm.discountcoupons.api.coupon;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;
import pl.mm.discountcoupons.api.CouponRedemptionsApi;
import pl.mm.discountcoupons.api.CouponsApi;
import pl.mm.discountcoupons.api.dto.CouponCreateRequest;
import pl.mm.discountcoupons.api.dto.CouponRedemptionRequest;
import pl.mm.discountcoupons.api.dto.CouponRedemptionResponse;
import pl.mm.discountcoupons.api.dto.CouponResponse;
import pl.mm.discountcoupons.application.CouponService;
import pl.mm.discountcoupons.ip.ClientIpResolver;

import java.net.URI;
import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
public class CouponController implements CouponsApi, CouponRedemptionsApi {

    private final CouponService couponService;
    private final ClientIpResolver clientIpResolver;
    private final HttpServletRequest request;

    @Override
    public ResponseEntity<CouponResponse> createCoupon(@Valid CouponCreateRequest couponCreateRequest) {
        CouponResponse response = couponService.createCoupon(couponCreateRequest);
        return ResponseEntity
                .created(couponUri(response.getCode()))
                .body(response);
    }

    @Override
    public ResponseEntity<CouponResponse> getCoupon(String code) {
        return ResponseEntity.ok(couponService.getCoupon(code));
    }

    @Override
    public ResponseEntity<CouponRedemptionResponse> redeemCoupon(
            @Valid CouponRedemptionRequest couponRedemptionRequest) {
        String clientIp = clientIpResolver.resolve(request);
        return ResponseEntity
                .created(URI.create("/coupon-redemptions"))
                .body(couponService.redeemCoupon(couponRedemptionRequest, clientIp));
    }

    private static URI couponUri(String code) {
        return URI.create("/coupons/" + UriUtils.encodePathSegment(code, StandardCharsets.UTF_8));
    }
}
