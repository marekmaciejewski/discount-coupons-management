package pl.mm.discountcoupons.ip;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import pl.mm.discountcoupons.domain.ClientIpResolutionException;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Component
public class ClientIpResolver {

    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.|$)){4}$");
    private static final Pattern IPV6_CHARS_PATTERN = Pattern.compile("^[0-9a-fA-F:.%]+$");

    public String resolve(HttpServletRequest request) {
        return Stream.of(
                        forwardedHeader(request),
                        commaSeparatedHeader(request, "X-Forwarded-For"),
                        commaSeparatedHeader(request, "X-Real-IP"),
                        Optional.ofNullable(request.getRemoteAddr()))
                .flatMap(Optional::stream)
                .map(ClientIpResolver::cleanCandidate)
                .filter(ClientIpResolver::isValidIpAddress)
                .findFirst()
                .orElseThrow(() -> new ClientIpResolutionException("Could not resolve client IP address"));
    }

    private static Optional<String> forwardedHeader(HttpServletRequest request) {
        return Collections.list(request.getHeaders("Forwarded")).stream()
                .flatMap(header -> Stream.of(header.split(",")))
                .map(String::trim)
                .flatMap(entry -> Stream.of(entry.split(";")))
                .map(String::trim)
                .filter(part -> part.regionMatches(true, 0, "for=", 0, 4))
                .map(part -> part.substring(4))
                .findFirst();
    }

    private static Optional<String> commaSeparatedHeader(HttpServletRequest request, String name) {
        return Collections.list(request.getHeaders(name)).stream()
                .flatMap(header -> Stream.of(header.split(",")))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .findFirst();
    }

    private static String cleanCandidate(String value) {
        String candidate = value.trim();
        if (candidate.startsWith("\"") && candidate.endsWith("\"") && candidate.length() > 1) {
            candidate = candidate.substring(1, candidate.length() - 1);
        }
        if (candidate.startsWith("[") && candidate.contains("]")) {
            return candidate.substring(1, candidate.indexOf(']'));
        }
        int colon = candidate.lastIndexOf(':');
        if (candidate.contains(".") && colon > -1) {
            return candidate.substring(0, colon);
        }
        return candidate;
    }

    private static boolean isValidIpAddress(String value) {
        if (IPV4_PATTERN.matcher(value).matches()) {
            return true;
        }
        if (!value.contains(":") || !IPV6_CHARS_PATTERN.matcher(value).matches()) {
            return false;
        }
        try {
            InetAddress.getByName(value);
            return true;
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
