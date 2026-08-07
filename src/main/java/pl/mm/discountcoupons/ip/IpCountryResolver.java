package pl.mm.discountcoupons.ip;

public interface IpCountryResolver {

    String resolveCountryCode(String ipAddress);
}
