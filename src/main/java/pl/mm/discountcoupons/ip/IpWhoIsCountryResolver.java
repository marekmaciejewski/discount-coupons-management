package pl.mm.discountcoupons.ip;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import pl.mm.discountcoupons.domain.CountryResolutionException;

import java.util.Locale;

@Component
public class IpWhoIsCountryResolver implements IpCountryResolver {

    private final RestClient restClient;

    public IpWhoIsCountryResolver(@Value("${app.geo.ipwhois.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public String resolveCountryCode(String ipAddress) {
        try {
            IpWhoIsResponse response = restClient.get()
                    .uri("/{ipAddress}", ipAddress)
                    .retrieve()
                    .body(IpWhoIsResponse.class);

            if (response == null || !Boolean.TRUE.equals(response.success()) || !isCountryCode(response.countryCode())) {
                throw new CountryResolutionException("Could not resolve country for client IP address");
            }
            return response.countryCode().toUpperCase(Locale.ROOT);
        } catch (RestClientException e) {
            throw new CountryResolutionException("Could not resolve country for client IP address", e);
        }
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
