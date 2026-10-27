package com.qa.api.spec;

import com.qa.api.config.Config;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.EncoderConfig;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

/**
 * Shared request settings, built once and reused by every client.
 *
 * Restful-Booker compares the Accept header with an exact string. Rest Assured's ContentType.JSON
 * sends a list ("application/json, application/javascript, ..."), which this API answers with 418,
 * so Accept is set to the literal value.
 */
public final class Specs {

    public static final String JSON = "application/json";
    public static final String XML = "application/xml";

    private Specs() {
    }

    public static RequestSpecification forBaseUri(String baseUri) {
        return new RequestSpecBuilder()
                .setBaseUri(baseUri)
                .setConfig(config())
                .setContentType(ContentType.JSON)
                .setAccept(JSON)
                .addFilter(new AllureRestAssured())
                .build();
    }

    /** The live API from config (base.url). */
    public static RequestSpecification live() {
        return forBaseUri(Config.get("base.url").replaceAll("/+$", ""));
    }

    private static RestAssuredConfig config() {
        return RestAssuredConfig.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", Config.getInt("connect.timeout.ms"))
                        .setParam("http.socket.timeout", Config.getInt("read.timeout.ms")))
                // Send "application/json", not "application/json; charset=ISO-8859-1".
                .encoderConfig(EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false));
    }
}
