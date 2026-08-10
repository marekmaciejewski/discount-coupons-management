package pl.mm.discountcoupons.api;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@EnableWireMock(@ConfigureWireMock(
        name = "ipwhois",
        baseUrlProperties = "app.geo.ipwhois.base-url"))
@Sql(scripts = "/sql/clear-coupons.sql")
class CouponIpValidationIT {

    private static final String POLISH_IP = "203.0.113.10";
    private static final String POLISH_IPV6 = "2001:db8::8";

    @LocalServerPort
    private int port;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void redeemCoupon_acceptsForwardedHeaderAddress_withQuotesBracketsAndPort() {
        createCoupon("FORWARDED");

        request()
                .header("Forwarded", "proto=https;for=\"[%s]:4711\"".formatted(POLISH_IP))
                .contentType(ContentType.JSON)
                .body(redemptionBody("FORWARDED", "forwarded-user"))
        .when()
                .post("/coupon-redemptions")
        .then()
                .statusCode(201)
                .header("Content-Type", startsWith("application/json"))
                .body("code", equalTo("FORWARDED"))
                .body("userId", equalTo("forwarded-user"))
                .body("countryCode", equalTo("PL"));
    }

    @Test
    void redeemCoupon_acceptsXForwarded_forIpv4AddressWithPort() {
        createCoupon("PORTIP");

        request()
                .header("X-Forwarded-For", POLISH_IP + ":4711")
                .contentType(ContentType.JSON)
                .body(redemptionBody("PORTIP", "port-user"))
        .when()
                .post("/coupon-redemptions")
        .then()
                .statusCode(201)
                .header("Content-Type", startsWith("application/json"))
                .body("code", equalTo("PORTIP"))
                .body("userId", equalTo("port-user"))
                .body("countryCode", equalTo("PL"));
    }

    @Test
    void redeemCoupon_skipsInvalidIpCandidates_untilXRealIp() {
        createCoupon("SKIPBAD");

        request()
                .header("Forwarded", "for=2001:db8:::1")
                .header("X-Forwarded-For", "not-an-ip")
                .header("X-Real-IP", POLISH_IP)
                .contentType(ContentType.JSON)
                .body(redemptionBody("SKIPBAD", "skip-user"))
        .when()
                .post("/coupon-redemptions")
        .then()
                .statusCode(201)
                .header("Content-Type", startsWith("application/json"))
                .body("code", equalTo("SKIPBAD"))
                .body("userId", equalTo("skip-user"))
                .body("countryCode", equalTo("PL"));
    }

    @Test
    void redeemCoupon_skipsBlankXForwarded_forValuesUntilXRealIp() {
        createCoupon("BLANKIP");

        request()
                .header("X-Forwarded-For", " , ")
                .header("X-Real-IP", POLISH_IP)
                .contentType(ContentType.JSON)
                .body(redemptionBody("BLANKIP", "blank-user"))
        .when()
                .post("/coupon-redemptions")
        .then()
                .statusCode(201)
                .header("Content-Type", startsWith("application/json"))
                .body("code", equalTo("BLANKIP"))
                .body("userId", equalTo("blank-user"))
                .body("countryCode", equalTo("PL"));
    }

    @Test
    void redeemCoupon_skipsBlankXForwarded_forEntryUntilNextEntry() {
        createCoupon("NEXTIP");

        request()
                .header("X-Forwarded-For", ", " + POLISH_IP)
                .contentType(ContentType.JSON)
                .body(redemptionBody("NEXTIP", "next-ip-user"))
        .when()
                .post("/coupon-redemptions")
        .then()
                .statusCode(201)
                .header("Content-Type", startsWith("application/json"))
                .body("code", equalTo("NEXTIP"))
                .body("userId", equalTo("next-ip-user"))
                .body("countryCode", equalTo("PL"));
    }

    @Test
    void redeemCoupon_skipsMalformedWrappedIpCandidates_untilXRealIp() {
        createCoupon("BADWRAP");

        request()
                .header("Forwarded", "for=\"not-an-ip")
                .header("X-Forwarded-For", "[" + POLISH_IP)
                .header("X-Real-IP", POLISH_IP)
                .contentType(ContentType.JSON)
                .body(redemptionBody("BADWRAP", "bad-wrap-user"))
        .when()
                .post("/coupon-redemptions")
        .then()
                .statusCode(201)
                .header("Content-Type", startsWith("application/json"))
                .body("code", equalTo("BADWRAP"))
                .body("userId", equalTo("bad-wrap-user"))
                .body("countryCode", equalTo("PL"));
    }

    @Test
    void redeemCoupon_skipsSingleQuoteIpCandidate_untilXRealIp() {
        createCoupon("QUOTEIP");

        request()
                .header("Forwarded", "for=\"")
                .header("X-Real-IP", POLISH_IP)
                .contentType(ContentType.JSON)
                .body(redemptionBody("QUOTEIP", "quote-ip-user"))
        .when()
                .post("/coupon-redemptions")
        .then()
                .statusCode(201)
                .header("Content-Type", startsWith("application/json"))
                .body("code", equalTo("QUOTEIP"))
                .body("userId", equalTo("quote-ip-user"))
                .body("countryCode", equalTo("PL"));
    }

    @Test
    void redeemCoupon_acceptsIpv6Address() {
        createCoupon("IPV6OK");

        request()
                .header("X-Forwarded-For", POLISH_IPV6)
                .contentType(ContentType.JSON)
                .body(redemptionBody("IPV6OK", "ipv6-user"))
        .when()
                .post("/coupon-redemptions")
        .then()
                .statusCode(201)
                .header("Content-Type", startsWith("application/json"))
                .body("code", equalTo("IPV6OK"))
                .body("userId", equalTo("ipv6-user"))
                .body("countryCode", equalTo("PL"));
    }

    @Test
    void redeemCoupon_skipsIpv6Candidate_withInvalidCharactersUntilXRealIp() {
        createCoupon("BADIPV6");

        request()
                .header("X-Forwarded-For", "2001:db8::zz")
                .header("X-Real-IP", POLISH_IP)
                .contentType(ContentType.JSON)
                .body(redemptionBody("BADIPV6", "bad-ipv6-user"))
        .when()
                .post("/coupon-redemptions")
        .then()
                .statusCode(201)
                .header("Content-Type", startsWith("application/json"))
                .body("code", equalTo("BADIPV6"))
                .body("userId", equalTo("bad-ipv6-user"))
                .body("countryCode", equalTo("PL"));
    }

    @Test
    void redeemCoupon_returnsNotFound_forUnknownCoupon() {
        request()
                .header("X-Forwarded-For", POLISH_IP)
                .contentType(ContentType.JSON)
                .body(redemptionBody("UNKNOWN", "unknown-coupon-user"))
        .when()
                .post("/coupon-redemptions")
        .then()
                .statusCode(404)
                .header("Content-Type", startsWith("application/problem+json"))
                .body("detail", equalTo("UNKNOWN coupon not found"))
                .body("instance", equalTo("/coupon-redemptions"));
    }

    @Test
    void redeemCoupon_returnsBadRequest_whenNoClientIpCanBeResolved() throws Exception {
        mockMvc.perform(post("/coupon-redemptions")
                        .with(request -> {
                            request.setRemoteAddr("not-an-ip");
                            return request;
                        })
                        .header("Forwarded", "for=2001:db8:::1")
                        .header("X-Forwarded-For", "not-an-ip")
                        .header("X-Real-IP", "also-not-an-ip")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(redemptionBody("UNKNOWN", "bad-ip-user")))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", startsWith("application/problem+json")))
                .andExpect(jsonPath("$.detail", equalTo("Could not resolve client IP address")))
                .andExpect(jsonPath("$.instance", equalTo("/coupon-redemptions")));
    }

    @Test
    void redeemCoupon_returnsServiceUnavailable_whenCountryResponseHasNoCountryCodeForValidIp() {
        createCoupon("NOCOUNTRY");

        request()
                .header("X-Forwarded-For", "192.0.2.99")
                .contentType(ContentType.JSON)
                .body(redemptionBody("NOCOUNTRY", "no-country-user"))
        .when()
                .post("/coupon-redemptions")
        .then()
                .statusCode(503)
                .header("Content-Type", startsWith("application/problem+json"))
                .body("detail", equalTo("Could not resolve country for client IP address"))
                .body("instance", equalTo("/coupon-redemptions"));
    }

    @Test
    void redeemCoupon_returnsServiceUnavailable_whenCountryResponseHasInvalidCountryCodeForValidIp() {
        createCoupon("BADCOUNTRY");

        request()
                .header("X-Forwarded-For", "192.0.2.103")
                .contentType(ContentType.JSON)
                .body(redemptionBody("BADCOUNTRY", "bad-country-user"))
        .when()
                .post("/coupon-redemptions")
        .then()
                .statusCode(503)
                .header("Content-Type", startsWith("application/problem+json"))
                .body("detail", equalTo("Could not resolve country for client IP address"))
                .body("instance", equalTo("/coupon-redemptions"));
    }

    @Test
    void redeemCoupon_returnsServiceUnavailable_whenCountryResponseHasSuccessFalseForValidIp() {
        createCoupon("GEOFAIL");

        request()
                .header("X-Forwarded-For", "192.0.2.100")
                .contentType(ContentType.JSON)
                .body(redemptionBody("GEOFAIL", "geo-fail-user"))
        .when()
                .post("/coupon-redemptions")
        .then()
                .statusCode(503)
                .header("Content-Type", startsWith("application/problem+json"))
                .body("detail", equalTo("Could not resolve country for client IP address"))
                .body("instance", equalTo("/coupon-redemptions"));
    }

    @Test
    void redeemCoupon_returnsServiceUnavailable_whenCountryResponseHasNoBodyForValidIp() {
        createCoupon("NOBODY");

        request()
                .header("X-Forwarded-For", "192.0.2.101")
                .contentType(ContentType.JSON)
                .body(redemptionBody("NOBODY", "no-body-user"))
        .when()
                .post("/coupon-redemptions")
        .then()
                .statusCode(503)
                .header("Content-Type", startsWith("application/problem+json"))
                .body("detail", equalTo("Could not resolve country for client IP address"))
                .body("instance", equalTo("/coupon-redemptions"));
    }

    @Test
    void redeemCoupon_returnsServiceUnavailable_whenCountryServiceFailsForValidIp() {
        createCoupon("GEOERROR");

        request()
                .header("X-Forwarded-For", "192.0.2.102")
                .contentType(ContentType.JSON)
                .body(redemptionBody("GEOERROR", "geo-error-user"))
        .when()
                .post("/coupon-redemptions")
        .then()
                .statusCode(503)
                .header("Content-Type", startsWith("application/problem+json"))
                .body("detail", equalTo("Could not resolve country for client IP address"))
                .body("instance", equalTo("/coupon-redemptions"));
    }

    private void createCoupon(String code) {
        request()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "code": "%s",
                          "maxUses": 5,
                          "countryCode": "PL"
                        }
                        """.formatted(code))
        .when()
                .post("/coupons")
        .then()
                .statusCode(201)
                .header("Location", "/coupons/" + code);
    }

    private static String redemptionBody(String code, String userId) {
        return """
                {
                  "code": "%s",
                  "userId": "%s"
                }
                """.formatted(code, userId);
    }

    private RequestSpecification request() {
        return given()
                .baseUri("http://localhost")
                .port(port);
    }
}
