package com.qa.api.models;

/** Dates as the API sends them: ISO yyyy-MM-dd strings. */
public record BookingDates(String checkin, String checkout) {
}
