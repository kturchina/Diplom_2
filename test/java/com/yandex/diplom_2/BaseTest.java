package com.yandex.diplom_2;

import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jdk.jfr.StackTrace;
import org.apache.http.HttpHeaders;
import org.hamcrest.Matcher;
import org.json.JSONObject;
import org.junit.BeforeClass;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class BaseTest {
    public static final String USER_NAME = "stellarking123321";
    public static final String USER_EMAIL = USER_NAME +"@ya.ru";
    public static final String USER_PASSWORD = USER_NAME;

    public static final Matcher<Integer> BAD_REQUEST_403 = equalTo(403);
    public static final Matcher<Integer> BAD_REQUEST_400 = equalTo(400);
    public static final Matcher<Integer> UNAUTHORIZED_401 = equalTo(401);
    public static final Matcher<Integer> NOT_FOUND_404 = equalTo(404);

    public static final Matcher<Integer> ACCEPTED_202 = equalTo(202);
    public static final Matcher<Integer> OK_200 = equalTo(200);

    public static final Matcher<Integer> ERR_500 = equalTo(500);

    @BeforeClass
    public static void setup() {
        RestAssured.baseURI = "https://stellarburgers.nomoreparties.site/";
        RestAssured.port = 443;

        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
    }

    @Step
    public Response tryUserCreate(JSONObject body) {
        var rq = given().contentType(ContentType.JSON);
        if (body != null) {
            rq = rq.body(body.toString());
        }
        return rq.post("/api/auth/register");
    }

    @Step
    public Response tryUserLogin(JSONObject body) {
        var rq = given().contentType(ContentType.JSON);
        if (body != null) {
            rq = rq.body(body.toString());
        }
        return rq.post("/api/auth/login");
    }

    @Step
    public Response tryUserLogout(String refreshToken) {
        return given().contentType(ContentType.JSON)
                .body(new JSONObject().put("token", refreshToken).toString())
                .post("/api/auth/logout");
    }

    @Step
    public Response tryUserPasswordResetRequest(JSONObject body) {
        var rq = given().contentType(ContentType.JSON);
        if (body != null) {
            rq = rq.body(body.toString());
        }
        return rq.post("/api/password-reset");
    }

    @Step
    public Response tryUserPasswordReset(JSONObject body) {
        var rq = given().contentType(ContentType.JSON);
        if (body != null) {
            rq = rq.body(body.toString());
        }
        return rq.post("/api/password-reset/reset");
    }

    @Step
    public Response tryUserGetInfo(String token) {
        return given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, token)
                .get("/api/auth/user");
    }

    @Step
    public Response tryUserUpdate(JSONObject body, String token) {
        var rq = given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, token);
        if (body != null) {
            rq = rq.body(body.toString());
        }
        return rq.patch("/api/auth/user");
    }

    @Step
    public Response tryUserDelete(String token) {
        return given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, token)
                .delete("/api/auth/user");
    }

    @Step
    public void assertResponseWithCodeAndMessage(Response response, Matcher<Integer> code, String message, boolean success) {
        response.then().assertThat().statusCode(code)
                .body("message", equalTo(message),
                        "success", equalTo(success));
    }

    @Step
    public void assertResponse(Response response, Matcher<Integer> code, String field, Matcher<?> matcher, Object... other) {
        response.then().assertThat().statusCode(code).body(field, matcher, other);
    }

    @Step
    public void assertResponse(Response response, Matcher<Integer> code) {
        response.then().assertThat().statusCode(code);
    }

    @Step
    public Response tryOrdersGetAll() {
        return given().contentType(ContentType.JSON)
                .get("/api/orders/all");
    }

    @Step
    public Response tryOrdersGet(String token) {
        return given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, token)
                .get("/api/orders");
    }

    @Step
    public Response tryGetIngredients() {
        return given().contentType(ContentType.JSON)
                .get("/api/ingredients");
    }

    @Step
    public Response tryOrderCreate(JSONObject body, String token) {
        var rq = given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, token);
        if (body != null) {
            rq = rq.body(body.toString());
        }
        return rq.post("/api/orders");
    }
}
