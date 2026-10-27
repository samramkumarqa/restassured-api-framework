package com.qa.tests.live;

import com.qa.api.data.BookingFactory;
import com.qa.api.models.Booking;
import com.qa.api.spec.Specs;
import com.qa.tests.base.BaseApiTest;
import io.restassured.path.xml.XmlPath;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.containsString;

public class ContentNegotiationTest extends BaseApiTest {

    @Test(groups = "live", description = "The same booking can be read as XML")
    public void bookingAsXml() {
        Booking booking = BookingFactory.booking();
        int id = createBooking(booking).bookingid();

        String xml = bookings.get(id, Specs.XML)
                .then()
                .statusCode(200)
                .contentType(containsString("xml"))
                .extract().asString();

        XmlPath path = new XmlPath(xml);
        Assert.assertEquals(path.getString("booking.firstname"), booking.firstname());
        Assert.assertEquals(path.getInt("booking.totalprice"), booking.totalprice());
        Assert.assertEquals(path.getString("booking.bookingdates.checkin"), booking.bookingdates().checkin());
    }
}
