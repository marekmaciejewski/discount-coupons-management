package pl.mm.discountcoupons.domain;

public class CountryResolutionException extends RuntimeException {

    public CountryResolutionException(String message) {
        super(message);
    }

    public CountryResolutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
