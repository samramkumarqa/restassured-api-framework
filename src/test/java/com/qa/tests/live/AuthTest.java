package com.qa.tests.live;

import com.qa.api.config.Config;
import com.qa.api.models.AuthResponse;
import com.qa.tests.base.BaseApiTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

public class AuthTest extends BaseApiTest {

    @Test(groups = {"smoke", "live"}, description = "Valid credentials return a token")
    public void validCredentialsReturnToken() {
        auth.createToken(Config.get("api.username"), Config.get("api.password"))
                .then()
                .statusCode(200)
                .body("token", not(emptyOrNullString()))
                .body("token", matchesPattern("^[a-f0-9]+$"))
                .body("reason", nullValue());
    }

    @DataProvider(name = "badCredentials")
    public Object[][] badCredentials() {
        return new Object[][] {
                {"admin", "wrong-password"},
                {"not-a-user", "password123"},
                {"", ""},
        };
    }

    /** The API reports a failed login with HTTP 200 and a reason, not 401. The test pins that contract. */
    @Test(dataProvider = "badCredentials", groups = "live", description = "Bad credentials return a reason and no token")
    public void badCredentialsAreRejected(String username, String password) {
        AuthResponse response = auth.createToken(username, password)
                .then()
                .statusCode(200)
                .body("reason", equalTo("Bad credentials"))
                .extract().as(AuthResponse.class);

        org.testng.Assert.assertNull(response.token(), "No token should be issued");
    }
}
