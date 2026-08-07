package pl.mm.discountcoupons.ip;

import jakarta.servlet.http.HttpServletRequest;

public interface ClientIpResolver {

    String resolve(HttpServletRequest request);
}
