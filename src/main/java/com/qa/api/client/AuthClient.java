package com.qa.api.client;

import com.qa.api.config.Config;
import com.qa.api.models.AuthRequest;
import com.qa.api.models.AuthResponse;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

public class AuthClient {

    private static volatile String cachedToken;

    private final RequestSpecification spec;

    public AuthClient(RequestSpecification spec) {
        this.spec = spec;
    }

    public Response createToken(String username, String password) {
        return given(spec)
                .body(new AuthRequest(username, password))
                .when()
                .post("/auth");
    }

    /** A token for the configured user, requested once and shared by all tests in the run. */
    public String token() {
        if (cachedToken == null) {
            synchronized (AuthClient.class) {
                if (cachedToken == null) {
                    AuthResponse auth = createToken(Config.get("api.username"), Config.get("api.password"))
                            .then().statusCode(200)
                            .extract().as(AuthResponse.class);
                    if (auth.token() == null || auth.token().isBlank()) {
                        throw new IllegalStateException("Login failed: " + auth.reason());
                    }
                    cachedToken = auth.token();
                }
            }
        }
        return cachedToken;
    }
}
