package com.qa.tests.live;

import com.qa.api.data.BookingFactory;
import com.qa.api.models.Booking;
import com.qa.api.models.CreatedBooking;
import com.qa.tests.base.BaseApiTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.equalTo;

public class BookingCrudTest extends BaseApiTest {

    @Test(groups = {"smoke", "live"}, description = "Create, read, update (PUT), partially update (PATCH) and delete a booking")
    public void bookingLifecycle() {
        Booking original = BookingFactory.booking();

        // POST: response echoes the booking and matches the contract
        CreatedBooking created = bookings.create(original)
                .then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/created-booking.json"))
                .extract().as(CreatedBooking.class);
        int id = created.bookingid();
        Assert.assertEquals(created.booking(), original, "created booking");

        // GET
        Booking fetched = bookings.get(id)
                .then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/booking.json"))
                .extract().as(Booking.class);
        Assert.assertEquals(fetched, original, "fetched booking");

        // PUT replaces the whole booking
        Booking replaced = original.withNames(original.firstname() + "X", original.lastname() + "Y").withTotalPrice(1234);
        Booking afterPut = bookings.update(id, replaced).then().statusCode(200).extract().as(Booking.class);
        Assert.assertEquals(afterPut, replaced, "after PUT");

        // PATCH changes only the given fields
        bookings.patch(id, Map.of("totalprice", 777, "additionalneeds", "Dinner"))
                .then()
                .statusCode(200)
                .body("totalprice", equalTo(777))
                .body("additionalneeds", equalTo("Dinner"))
                .body("firstname", equalTo(replaced.firstname()));

        // DELETE answers 201, then the booking is gone
        bookings.delete(id).then().statusCode(201);
        bookings.get(id).then().statusCode(404);
    }

    @Test(groups = "live", description = "Booking ids list matches its schema")
    public void bookingIdsMatchSchema() {
        bookings.list(Map.of())
                .then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/booking-ids.json"));
    }
}
