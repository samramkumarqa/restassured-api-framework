package com.qa.api.data;

import com.qa.api.models.Booking;
import com.qa.api.models.BookingDates;
import net.datafaker.Faker;

import java.time.LocalDate;
import java.util.Locale;

/**
 * Builds valid, unique bookings. The public API is shared, so names carry a random suffix
 * to make search assertions unambiguous.
 */
public final class BookingFactory {

    private static final Faker FAKER = new Faker(Locale.ENGLISH);

    private BookingFactory() {
    }

    public static Booking booking() {
        LocalDate checkin = LocalDate.now().plusDays(FAKER.number().numberBetween(10, 200));
        LocalDate checkout = checkin.plusDays(FAKER.number().numberBetween(1, 14));
        return new Booking(
                FAKER.name().firstName() + FAKER.regexify("[A-Z]{4}"),
                FAKER.name().lastName() + FAKER.regexify("[A-Z]{4}"),
                FAKER.number().numberBetween(50, 2000),
                FAKER.bool().bool(),
                new BookingDates(checkin.toString(), checkout.toString()),
                FAKER.options().option("Breakfast", "Late checkout", "Airport transfer", "Extra bed"));
    }
}
