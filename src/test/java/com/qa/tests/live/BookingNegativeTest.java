package com.qa.tests.live;

import com.qa.api.data.BookingFactory;
import com.qa.api.models.Booking;
import com.qa.tests.base.BaseApiTest;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;

/**
 * Error handling. Restful-Booker's status codes are unusual (500 for a missing field, 405 for a missing
 * booking on update, 418 for an unsupported Accept). These tests pin the actual behaviour so any change is noticed.
 */
public class BookingNegativeTest extends BaseApiTest {

    @Test(groups = "live", description = "GET of an unknown booking returns 404")
    public void unknownBookingIs404() {
        bookings.get(999_999_999).then().statusCode(404);
    }

    @Test(groups = "live", description = "PUT and DELETE without a token are forbidden and change nothing")
    public void writesRequireAuth() {
        Booking booking = BookingFactory.booking();
        int id = createBooking(booking).bookingid();

        bookings.updateWithoutAuth(id, booking.withTotalPrice(1)).then().statusCode(403);
        bookings.deleteWithoutAuth(id).then().statusCode(403);

        bookings.get(id).then().statusCode(200).body("totalprice", equalTo(booking.totalprice()));
    }

    @Test(groups = "live", description = "Creating a booking without required fields is rejected (API returns 500)")
    public void createWithMissingFieldIsRejected() {
        Map<String, Object> body = new HashMap<>();
        body.put("firstname", "OnlyAFirstName");
        bookings.create(body).then().statusCode(500);
    }

    @Test(groups = "live", description = "PUT with an incomplete body returns 400")
    public void updateWithIncompleteBodyIs400() {
        int id = createBooking(BookingFactory.booking()).bookingid();
        bookings.update(id, Map.of("firstname", "NoOtherFields")).then().statusCode(400);
    }

    @Test(groups = "live", description = "Updating or deleting a booking that doesn't exist returns 405")
    public void writesOnUnknownBookingAre405() {
        bookings.update(999_999_999, BookingFactory.booking()).then().statusCode(405);
        bookings.delete(999_999_999).then().statusCode(405);
    }

    @Test(groups = "live", description = "An unsupported Accept header returns 418")
    public void unsupportedAcceptIs418() {
        int id = createBooking(BookingFactory.booking()).bookingid();
        bookings.get(id, "text/plain").then().statusCode(418);
    }
}
