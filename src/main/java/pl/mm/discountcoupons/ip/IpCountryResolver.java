package pl.mm.discountcoupons.ip;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import pl.mm.discountcoupons.domain.CountryResolutionException;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class IpCountryResolver {

    private final RestClient ipwhoisRestClient;

    public String resolveCountryCode(String ipAddress) {
        IpWhoIsResponse response;
        try {
            response = ipwhoisRestClient.get()
                    .uri("/{ipAddress}", ipAddress)
                    .retrieve()
                    .body(IpWhoIsResponse.class);
        } catch (RestClientException e) {
            throw new CountryResolutionException("Could not resolve country for client IP address", e);
        }
        if (response == null || !Boolean.TRUE.equals(response.success()) || !isCountryCode(response.countryCode())) {
            throw new CountryResolutionException("Could not resolve country for client IP address");
        }
        return response.countryCode().toUpperCase(Locale.ROOT);
    }

    private static boolean isCountryCode(String value) {
        return value != null && value.matches("^[A-Za-z]{2}$");
    }

    private record IpWhoIsResponse(
            Boolean success,
            @JsonProperty("country_code") String countryCode,
            String message) {
    }
}
