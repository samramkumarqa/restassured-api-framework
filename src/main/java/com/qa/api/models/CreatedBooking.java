package com.qa.api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Response of POST /booking. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CreatedBooking(int bookingid, Booking booking) {
}
