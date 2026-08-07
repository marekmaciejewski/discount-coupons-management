package pl.mm.discountcoupons.domain;

public class CouponAlreadyExistsException extends RuntimeException {

    public CouponAlreadyExistsException(String message) {
        super(message);
    }
}
