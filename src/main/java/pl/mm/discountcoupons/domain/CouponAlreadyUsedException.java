package pl.mm.discountcoupons.domain;

public class CouponAlreadyUsedException extends RuntimeException {

    public CouponAlreadyUsedException(String message) {
        super(message);
    }
}
