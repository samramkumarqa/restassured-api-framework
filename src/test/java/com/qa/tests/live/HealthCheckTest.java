package com.qa.tests.live;

import com.qa.tests.base.BaseApiTest;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.lessThan;

public class HealthCheckTest extends BaseApiTest {

    @Test(groups = {"smoke", "live"}, description = "Service is up (Restful-Booker answers /ping with 201)")
    public void pingReturns201() {
        bookings.ping()
                .then()
                .statusCode(201)
                .time(lessThan(10_000L));
    }
}
