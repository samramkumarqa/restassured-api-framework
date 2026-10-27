package com.qa.api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/** A booking as sent to and returned by /booking. Jackson serialises the record in both directions. */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record Booking(
        String firstname,
        String lastname,
        int totalprice,
        boolean depositpaid,
        BookingDates bookingdates,
        String additionalneeds) {

    public Booking withNames(String first, String last) {
        return new Booking(first, last, totalprice, depositpaid, bookingdates, additionalneeds);
    }

    public Booking withTotalPrice(int price) {
        return new Booking(firstname, lastname, price, depositpaid, bookingdates, additionalneeds);
    }
}
