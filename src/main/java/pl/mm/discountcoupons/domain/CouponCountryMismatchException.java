package pl.mm.discountcoupons.domain;

public class CouponCountryMismatchException extends RuntimeException {

    public CouponCountryMismatchException(String message) {
        super(message);
    }
}
