package pl.mm.discountcoupons.api.coupon;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import pl.mm.discountcoupons.api.CouponRedemptionsApi;
import pl.mm.discountcoupons.api.dto.CouponRedemptionRequest;
import pl.mm.discountcoupons.api.dto.CouponRedemptionResponse;
import pl.mm.discountcoupons.application.CouponService;
import pl.mm.discountcoupons.ip.ClientIpResolver;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class CouponRedemptionController implements CouponRedemptionsApi {

    private final CouponService couponService;
    private final ClientIpResolver clientIpResolver;
    private final HttpServletRequest request;

    @Override
    public ResponseEntity<CouponRedemptionResponse> redeemCoupon(
            @Valid CouponRedemptionRequest couponRedemptionRequest) {
        String clientIp = clientIpResolver.resolve(request);
        return ResponseEntity
                .created(URI.create("/coupon-redemptions"))
                .body(couponService.redeemCoupon(couponRedemptionRequest, clientIp));
    }
}
