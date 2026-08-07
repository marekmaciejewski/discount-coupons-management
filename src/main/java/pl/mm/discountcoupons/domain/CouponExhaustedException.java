package pl.mm.discountcoupons.domain;

public class CouponExhaustedException extends RuntimeException {

    public CouponExhaustedException(String message) {
        super(message);
    }
}
