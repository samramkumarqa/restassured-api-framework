package com.qa.tests.live;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qa.api.data.BookingFactory;
import com.qa.api.models.Booking;
import com.qa.api.models.BookingDates;
import com.qa.api.models.CreatedBooking;
import com.qa.tests.base.BaseApiTest;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Rows from testdata/bookings.json: each one is created, read back and compared field by field. */
public class BookingDataDrivenTest extends BaseApiTest {

    public record Row(String id, int totalprice, boolean depositpaid, int nights, String additionalneeds) {
        @Override
        public String toString() {
            return id;
        }
    }

    @DataProvider(name = "rows")
    public Object[][] rows() throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("testdata/bookings.json")) {
            List<Row> rows = new ObjectMapper().readValue(in, new TypeReference<List<Row>>() { });
            return rows.stream().map(r -> new Object[] {r}).toArray(Object[][]::new);
        }
    }

    @Test(dataProvider = "rows", groups = "live", description = "Each data row round-trips through the API unchanged")
    public void bookingRoundTrips(Row row) {
        Booking base = BookingFactory.booking();
        LocalDate checkin = LocalDate.parse(base.bookingdates().checkin());
        Booking booking = new Booking(
                base.firstname(), base.lastname(), row.totalprice(), row.depositpaid(),
                new BookingDates(checkin.toString(), checkin.plusDays(row.nights()).toString()),
                row.additionalneeds());

        CreatedBooking created = createBooking(booking);

        Booking fetched = bookings.get(created.bookingid()).then().statusCode(200).extract().as(Booking.class);
        Assert.assertEquals(fetched, booking, row.id());
    }

    @Test(groups = "live", description = "Search by first and last name finds exactly the booking created")
    public void searchByName() {
        Booking booking = BookingFactory.booking();
        int id = createBooking(booking).bookingid();

        List<Integer> ids = bookings.list(Map.of("firstname", booking.firstname(), "lastname", booking.lastname()))
                .then().statusCode(200)
                .extract().jsonPath().getList("bookingid", Integer.class);

        Assert.assertEquals(ids, List.of(id), "names are unique per run, so exactly one match");
    }
}
