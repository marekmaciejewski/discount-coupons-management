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

    @LocalServerPort
    private int port;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void redeemCoupon_acceptsForwardedHeaderAddressWithQuotesBracketsAndPort() {
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
    void redeemCoupon_acceptsXForwardedForIpv4AddressWithPort() {
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
    void redeemCoupon_skipsInvalidIpCandidatesUntilXRealIp() {
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
    void redeemCoupon_skipsBlankXForwardedForValuesUntilXRealIp() {
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
    void redeemCoupon_returnsBadRequestWhenNoClientIpCanBeResolved() throws Exception {
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
    void redeemCoupon_returnsServiceUnavailableWhenCountryResponseHasNoCountryCodeForValidIp() {
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
