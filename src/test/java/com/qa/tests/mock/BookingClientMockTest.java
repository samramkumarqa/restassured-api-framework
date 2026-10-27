package com.qa.tests.mock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import com.qa.api.client.BookingClient;
import com.qa.api.data.BookingFactory;
import com.qa.api.models.Booking;
import com.qa.api.spec.Specs;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.specification.RequestSpecification;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.SocketTimeoutException;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.not;

/**
 * Tests the framework's own client against a local WireMock server: the requests it builds, and how
 * responses a live API can't produce on demand (server errors, broken JSON, slow replies) surface.
 * Runs fully offline.
 */
public class BookingClientMockTest {

    private static final String TOKEN = "mock-token-123";

    private WireMockServer server;
    private BookingClient client;

    @BeforeClass(alwaysRun = true)
    public void startServer() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
        client = new BookingClient(Specs.forBaseUri(server.baseUrl()), TOKEN);
    }

    @AfterClass(alwaysRun = true)
    public void stopServer() {
        if (server != null) {
            server.stop();
        }
    }

    @BeforeMethod(alwaysRun = true)
    public void resetStubs() {
        server.resetAll();
    }

    @Test(groups = {"smoke", "mock"}, description = "Client sends the exact Accept header the API requires and maps the body")
    public void getSendsExactAcceptAndMapsBooking() {
        server.stubFor(get(urlEqualTo("/booking/7"))
                .withHeader("Accept", equalTo("application/json"))
                .willReturn(okJson("""
                        {"firstname":"Ada","lastname":"Lovelace","totalprice":120,"depositpaid":true,
                         "bookingdates":{"checkin":"2027-01-10","checkout":"2027-01-12"},"additionalneeds":"Tea"}
                        """)));

        Booking booking = client.get(7).then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/booking.json"))
                .extract().as(Booking.class);

        Assert.assertEquals(booking.firstname(), "Ada");
        Assert.assertEquals(booking.bookingdates().checkout(), "2027-01-12");
    }

    @Test(groups = "mock", description = "Create sends the booking as JSON matching the model")
    public void createSendsBookingJson() {
        server.stubFor(post(urlEqualTo("/booking")).willReturn(okJson("{\"bookingid\":42,\"booking\":{}}")));
        Booking booking = BookingFactory.booking();

        client.create(booking).then().statusCode(200);

        server.verify(postRequestedFor(urlEqualTo("/booking"))
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(matchingJsonPath("$.firstname", equalTo(booking.firstname())))
                .withRequestBody(matchingJsonPath("$.bookingdates.checkin", equalTo(booking.bookingdates().checkin())))
                .withRequestBody(matchingJsonPath("$.totalprice", equalTo(String.valueOf(booking.totalprice())))));
    }

    @Test(groups = "mock", description = "Write calls authenticate with the token cookie")
    public void writesSendTokenCookie() {
        server.stubFor(any(urlPathEqualTo("/booking/5"))
                .willReturn(aResponse().withStatus(201)));

        client.patch(5, Map.of("totalprice", 10));
        client.delete(5);

        server.verify(patchRequestedFor(urlEqualTo("/booking/5"))
                .withCookie("token", equalTo(TOKEN))
                .withRequestBody(equalToJson("{\"totalprice\":10}")));
        server.verify(deleteRequestedFor(urlEqualTo("/booking/5")).withCookie("token", equalTo(TOKEN)));
    }

    @Test(groups = "mock", description = "Read calls don't leak the token")
    public void readsDoNotSendToken() {
        server.stubFor(get(urlEqualTo("/booking/1")).willReturn(okJson("{}")));
        client.get(1);
        server.verify(getRequestedFor(urlEqualTo("/booking/1"))
                .withoutHeader("Cookie"));
    }

    @Test(groups = "mock", description = "A 503 from the server is surfaced as-is, not swallowed")
    public void serverErrorIsSurfaced() {
        server.stubFor(get(urlEqualTo("/booking/1"))
                .willReturn(aResponse().withStatus(503).withBody("Service Unavailable")));

        client.get(1).then().statusCode(503);
    }

    @Test(groups = "mock", description = "A schema check catches a contract break (wrong type, missing field)")
    public void schemaCatchesContractBreak() {
        server.stubFor(get(urlEqualTo("/booking/3")).willReturn(okJson("""
                {"firstname":"Ada","lastname":"Lovelace","totalprice":"120","depositpaid":true}
                """)));

        client.get(3).then()
                .statusCode(200)
                .body(not(matchesJsonSchemaInClasspath("schemas/booking.json")));
    }

    @Test(groups = "mock", description = "Malformed JSON fails deserialisation with a clear error")
    public void malformedJsonFailsMapping() {
        server.stubFor(get(urlEqualTo("/booking/4")).willReturn(okJson("{\"firstname\": \"Ada\", ")));

        Assert.expectThrows(Exception.class, () -> client.get(4).then().extract().as(Booking.class));
    }

    @Test(groups = "mock", description = "A dropped connection is reported as an I/O error")
    public void connectionResetIsReported() {
        server.stubFor(get(urlEqualTo("/booking/8")).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        Assert.expectThrows(Exception.class, () -> client.get(8));
    }

    @Test(groups = "mock", description = "A slow response trips the configured read timeout")
    public void slowResponseTimesOut() {
        server.stubFor(get(urlEqualTo("/booking/9")).willReturn(okJson("{}").withFixedDelay(3_000)));

        RequestSpecification shortTimeout = new RequestSpecBuilder()
                .addRequestSpecification(Specs.forBaseUri(server.baseUrl()))
                .setConfig(RestAssuredConfig.config().httpClient(
                        HttpClientConfig.httpClientConfig().setParam("http.socket.timeout", 1_000)))
                .build();
        BookingClient impatient = new BookingClient(shortTimeout, TOKEN);

        Throwable error = Assert.expectThrows(Exception.class, () -> impatient.get(9));
        Assert.assertTrue(rootCause(error) instanceof SocketTimeoutException, "Expected a socket timeout, got " + error);
    }

    private static Throwable rootCause(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }
}
