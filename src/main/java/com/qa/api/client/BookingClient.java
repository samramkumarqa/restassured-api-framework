package com.qa.api.client;

import com.qa.api.models.Booking;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * Thin wrapper over /booking. Returns raw Responses so tests decide what to assert
 * (status codes, headers, body, schema); the client only owns paths, auth and payloads.
 * Write calls send the token as the "token" cookie, which is how Restful-Booker authenticates.
 */
public class BookingClient {

    private final RequestSpecification spec;
    private final String token;

    public BookingClient(RequestSpecification spec, String token) {
        this.spec = spec;
        this.token = token;
    }

    public Response ping() {
        return given(spec).when().get("/ping");
    }

    public Response list(Map<String, ?> filters) {
        return given(spec).queryParams(filters).when().get("/booking");
    }

    public Response get(int id) {
        return given(spec).when().get("/booking/{id}", id);
    }

    /** GET with an explicit Accept header, e.g. application/xml. */
    public Response get(int id, String accept) {
        return given(spec).accept(accept).when().get("/booking/{id}", id);
    }

    public Response create(Object booking) {
        return given(spec).body(booking).when().post("/booking");
    }

    /** PUT replaces the whole booking. Accepts any body so negative tests can send incomplete ones. */
    public Response update(int id, Object booking) {
        return authed().body(booking).when().put("/booking/{id}", id);
    }

    public Response patch(int id, Map<String, ?> changes) {
        return authed().body(changes).when().patch("/booking/{id}", id);
    }

    public Response delete(int id) {
        return authed().when().delete("/booking/{id}", id);
    }

    /** Same calls without credentials, for negative auth tests. */
    public Response updateWithoutAuth(int id, Booking booking) {
        return given(spec).body(booking).when().put("/booking/{id}", id);
    }

    public Response deleteWithoutAuth(int id) {
        return given(spec).when().delete("/booking/{id}", id);
    }

    private RequestSpecification authed() {
        return given(spec).cookie("token", token);
    }
}
