package pl.mm.discountcoupons.api;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.jdbc.Sql;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.startsWith;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/sql/clear-coupons.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class CouponApiValidationRestAssuredIT {

    @LocalServerPort
    private int port;

    @Test
    void createCouponReturnsBadRequestForInvalidBody() {
        request()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "code": " ",
                          "maxUses": 0,
                          "countryCode": "POL"
                        }
                        """)
        .when()
                .post("/coupons")
        .then()
                .statusCode(400)
                .header("Content-Type", startsWith("application/problem+json"))
                .body("detail", equalTo("Request validation failed"))
                .body("instance", equalTo("/coupons"))
                .body("errors.field", hasItems("code", "maxUses", "countryCode"));
    }

    @Test
    void redeemCouponReturnsBadRequestForMissingRequiredFields() {
        request()
                .contentType(ContentType.JSON)
                .body("{}")
        .when()
                .post("/coupon-redemptions")
        .then()
                .statusCode(400)
                .header("Content-Type", startsWith("application/problem+json"))
                .body("detail", equalTo("Request validation failed"))
                .body("instance", equalTo("/coupon-redemptions"))
                .body("errors.field", hasItems("code", "userId"));
    }

    @Test
    void createCouponReturnsBadRequestForMalformedBody() {
        request()
                .contentType(ContentType.JSON)
                .body("{")
        .when()
                .post("/coupons")
        .then()
                .statusCode(400)
                .header("Content-Type", startsWith("application/problem+json"))
                .body("detail", equalTo("Request body is invalid"))
                .body("instance", equalTo("/coupons"));
    }

    @Test
    void createCouponReturnsConflictForCaseInsensitiveDuplicateCode() {
        createCoupon("WIOSNA", 2, "PL");

        request()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "code": "wiosna",
                          "maxUses": 5,
                          "countryCode": "pl"
                        }
                        """)
        .when()
                .post("/coupons")
        .then()
                .statusCode(409)
                .header("Content-Type", startsWith("application/problem+json"))
                .body("detail", equalTo("wiosna coupon already exists"))
                .body("instance", equalTo("/coupons"));
    }

    @Test
    void getCouponReturnsNotFoundForUnknownCode() {
        request()
        .when()
                .get("/coupons/{code}", "UNKNOWN")
        .then()
                .statusCode(404)
                .header("Content-Type", startsWith("application/problem+json"))
                .body("detail", equalTo("UNKNOWN coupon not found"))
                .body("instance", equalTo("/coupons/UNKNOWN"));
    }

    private void createCoupon(String code, int maxUses, String countryCode) {
        request()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "code": "%s",
                          "maxUses": %d,
                          "countryCode": "%s"
                        }
                        """.formatted(code, maxUses, countryCode))
        .when()
                .post("/coupons")
        .then()
                .statusCode(201)
                .header("Location", "/coupons/" + code);
    }

    private RequestSpecification request() {
        return given()
                .baseUri("http://localhost")
                .port(port);
    }
}
