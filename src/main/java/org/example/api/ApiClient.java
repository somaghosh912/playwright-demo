package org.example.api;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.example.config.ConfigManager;

import java.util.Map;

public final class ApiClient {

    private ApiClient() {}
    private static RequestSpecification base() {
        return RestAssured.given()
                .baseUri(ConfigManager.apiBaseUrl())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }

    public static Response get(String url) {
        return RestAssured
                .given()
                .when()
                .get(url);
    }

    public static Response get(String url, Map<String, ?> queryParams) {
        return RestAssured
                .given()
                .queryParams(queryParams)
                .when()
                .get(url);
    }

    public static Response post(String url, Object body) {
        return RestAssured
                .given()
                .contentType("application/json")
                .body(body)
                .when()
                .post(url);
    }

    public static Response put(String url, Object body) {
        return RestAssured
                .given()
                .contentType("application/json")
                .body(body)
                .when()
                .put(url);
    }

    public static Response delete(String url) {
        return RestAssured
                .given()
                .when()
                .delete(url);
    }
}
