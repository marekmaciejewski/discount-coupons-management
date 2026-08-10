package pl.mm.discountcoupons.api.coupon;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;
import pl.mm.discountcoupons.api.CouponsApi;
import pl.mm.discountcoupons.api.dto.CouponCreateRequest;
import pl.mm.discountcoupons.api.dto.CouponResponse;
import pl.mm.discountcoupons.application.CouponService;

import java.net.URI;
import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
public class CouponController implements CouponsApi {

    private final CouponService couponService;

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

    private static URI couponUri(String code) {
        return URI.create("/coupons/" + UriUtils.encodePathSegment(code, StandardCharsets.UTF_8));
    }
}
