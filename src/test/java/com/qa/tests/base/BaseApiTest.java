package com.qa.tests.base;

import com.qa.api.client.AuthClient;
import com.qa.api.client.BookingClient;
import com.qa.api.models.Booking;
import com.qa.api.models.CreatedBooking;
import com.qa.api.spec.Specs;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Live-API base: one shared spec and token, and automatic cleanup of every booking a test creates,
 * so the shared public API isn't left full of test data even when an assertion fails.
 */
public abstract class BaseApiTest {

    protected RequestSpecification spec;
    protected AuthClient auth;
    protected BookingClient bookings;

    private final List<Integer> created = Collections.synchronizedList(new ArrayList<>());

    @BeforeSuite(alwaysRun = true)
    public void logOnlyFailures() {
        // Full request/response is printed only for failed assertions; Allure records every call anyway.
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @BeforeClass(alwaysRun = true)
    public void setUpClients() {
        spec = Specs.live();
        auth = new AuthClient(spec);
        bookings = new BookingClient(spec, auth.token());
    }

    @AfterMethod(alwaysRun = true)
    public void deleteCreatedBookings() {
        synchronized (created) {
            for (int id : created) {
                bookings.delete(id); // 201 deleted, or 405 if the test already deleted it
            }
            created.clear();
        }
    }

    /** Creates a booking, asserts it worked, and registers it for cleanup. */
    protected CreatedBooking createBooking(Booking booking) {
        CreatedBooking result = bookings.create(booking)
                .then().statusCode(200)
                .extract().as(CreatedBooking.class);
        created.add(result.bookingid());
        return result;
    }
}
