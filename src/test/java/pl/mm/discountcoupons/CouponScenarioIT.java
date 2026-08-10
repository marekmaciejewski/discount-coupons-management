package pl.mm.discountcoupons;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.jdbc.Sql;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableWireMock(@ConfigureWireMock(
        name = "ipwhois",
        baseUrlProperties = "app.geo.ipwhois.base-url"))
@Sql(scripts = "/sql/clear-coupons.sql")
class CouponScenarioIT {

    private static final Instant SCENARIO_INSTANT = Instant.parse("2026-08-07T12:00:00Z");
    private static final String POLISH_IP = "203.0.113.10";
    private static final String GERMAN_IP = "198.51.100.20";

    @LocalServerPort
    private int port;

    @TestFactory
    Stream<DynamicNode> couponScenario() {
        return Stream.of(
                dynamicContainer("Create coupon", Stream.of(
                        dynamicTest("create WIOSNA coupon for Poland", this::createCoupon),
                        dynamicTest("read WIOSNA coupon case-insensitively", () -> checkCoupon(0)),
                        dynamicTest("reject duplicate code with different case", this::rejectDuplicateCode))),
                dynamicContainer("Redeem coupon", Stream.of(
                        dynamicTest("redeem for first Polish user", () -> redeem("user-1", 1)),
                        dynamicTest("reject second redemption by the same user", this::rejectDuplicateUser),
                        dynamicTest("reject redemption from another country", this::rejectWrongCountry),
                        dynamicTest("redeem for second Polish user", () -> redeem("user-2", 2)),
                        dynamicTest("reject redemption after max uses is reached", this::rejectExhaustedCoupon),
                        dynamicTest("read final coupon usage", () -> checkCoupon(2)))));
    }

    private void createCoupon() {
        request()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "code": "WIOSNA",
                          "maxUses": 2,
                          "countryCode": "pl"
                        }
                        """)
        .when()
                .post("/coupons")
        .then()
                .statusCode(201)
                .header("Location", "/coupons/WIOSNA")
                .header("Content-Type", startsWith("application/json"))
                .body("code", equalTo("WIOSNA"))
                .body("createdAt", equalTo("2026-08-07T12:00:00Z"))
                .body("maxUses", equalTo(2))
                .body("currentUses", equalTo(0))
                .body("countryCode", equalTo("PL"));
    }

    private void checkCoupon(int expectedCurrentUses) {
        request()
        .when()
                .get("/coupons/{code}", "wiosna")
        .then()
                .statusCode(200)
                .header("Content-Type", startsWith("application/json"))
                .body("code", equalTo("WIOSNA"))
                .body("maxUses", equalTo(2))
                .body("currentUses", equalTo(expectedCurrentUses))
                .body("countryCode", equalTo("PL"));
    }

    private void rejectDuplicateCode() {
        request()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "code": "wiosna",
                          "maxUses": 10,
                          "countryCode": "PL"
                        }
                        """)
        .when()
                .post("/coupons")
        .then()
                .statusCode(409)
                .header("Content-Type", startsWith("application/problem+json"))
                .body("detail", equalTo("wiosna coupon already exists"));
    }

    private void redeem(String userId, int expectedCurrentUses) {
        request()
                .header("X-Forwarded-For", POLISH_IP)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "code": "wiosna",
                          "userId": "%s"
                        }
                        """.formatted(userId))
        .when()
                .post("/coupon-redemptions")
        .then()
                .statusCode(201)
                .header("Content-Type", startsWith("application/json"))
                .body("code", equalTo("WIOSNA"))
                .body("userId", equalTo(userId))
                .body("usedAt", equalTo("2026-08-07T12:00:00Z"))
                .body("currentUses", equalTo(expectedCurrentUses))
                .body("maxUses", equalTo(2))
                .body("countryCode", equalTo("PL"));
    }

    private void rejectDuplicateUser() {
        request()
                .header("X-Forwarded-For", POLISH_IP)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "code": "WIOSNA",
                          "userId": "user-1"
                        }
                        """)
        .when()
                .post("/coupon-redemptions")
        .then()
                .statusCode(409)
                .header("Content-Type", startsWith("application/problem+json"))
                .body("detail", equalTo("user-1 already used WIOSNA coupon"));
    }

    private void rejectWrongCountry() {
        request()
                .header("X-Forwarded-For", GERMAN_IP)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "code": "WIOSNA",
                          "userId": "user-de"
                        }
                        """)
        .when()
                .post("/coupon-redemptions")
        .then()
                .statusCode(403)
                .header("Content-Type", startsWith("application/problem+json"))
                .body("detail", equalTo("WIOSNA coupon is not available in DE"));
    }

    private void rejectExhaustedCoupon() {
        request()
                .header("X-Forwarded-For", POLISH_IP)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "code": "WIOSNA",
                          "userId": "user-3"
                        }
                        """)
        .when()
                .post("/coupon-redemptions")
        .then()
                .statusCode(409)
                .header("Content-Type", startsWith("application/problem+json"))
                .body("detail", equalTo("WIOSNA coupon has reached its maximum number of uses"));
    }

    private RequestSpecification request() {
        return given()
                .baseUri("http://localhost")
                .port(port);
    }

    @TestConfiguration
    static class ScenarioConfiguration {

        @Bean
        @Primary
        Clock scenarioClock() {
            return Clock.fixed(SCENARIO_INSTANT, ZoneOffset.UTC);
        }
    }
}
