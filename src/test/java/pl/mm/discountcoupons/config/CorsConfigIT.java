package pl.mm.discountcoupons.config;

import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CorsConfigIT {

    private static final String FRONTEND_ORIGIN = "http://localhost:5173";
    private static final String GITHUB_PAGES_ORIGIN = "https://marekmaciejewski.github.io";

    @LocalServerPort
    private int port;

    @Test
    void preflight_allowsConfiguredOrigin() {
        request()
                .header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
        .when()
                .options("/coupons")
        .then()
                .statusCode(200)
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, equalTo(FRONTEND_ORIGIN))
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("GET"))
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("POST"));
    }

    @Test
    void preflight_allowsGitHubPagesOrigin() {
        request()
                .header(HttpHeaders.ORIGIN, GITHUB_PAGES_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
        .when()
                .options("/coupons/WIOSNA")
        .then()
                .statusCode(200)
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, equalTo(GITHUB_PAGES_ORIGIN));
    }

    @Test
    void preflight_rejectsUnconfiguredOrigin() {
        request()
                .header(HttpHeaders.ORIGIN, "https://example.invalid")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
        .when()
                .options("/coupons/WIOSNA")
        .then()
                .statusCode(403);
    }

    private RequestSpecification request() {
        return given()
                .baseUri("http://localhost")
                .port(port);
    }
}
