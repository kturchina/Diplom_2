package com.yandex.diplom_2;

import java.time.Instant;
import org.json.JSONObject;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;

public class TestUser extends BaseTest {

    public static final JSONObject createUserBody = new JSONObject()
            .put("email", USER_EMAIL)
            .put("name", USER_NAME)
            .put("password", USER_PASSWORD);

    @Test
    public void testCreateUserWithSameEmail() {
        var created = tryUserCreate(createUserBody);
        try {
            var failed = tryUserCreate(createUserBody);
            assertResponseWithCodeAndMessage(failed, BAD_REQUEST_403, "User already exists", false);
        }
        finally {
            tryUserDelete(created.then().extract().path("accessToken"));
        }
    }

    @Test
    public void testNotExistingUserLogin() {
         var notExists = tryUserLogin(new JSONObject().put("email", USER_EMAIL).put("password", USER_PASSWORD));
         assertResponse(notExists, UNAUTHORIZED_401, "message", equalTo("email or password are incorrect"), "success", equalTo(false));
    }

    @Test
    public void testUserLoginLogout() {
        var created = tryUserCreate(createUserBody);
        try {
            var failed1 = tryUserLogin(null);
            assertResponseWithCodeAndMessage(failed1, UNAUTHORIZED_401, "email or password are incorrect", false);

            var failed2 = tryUserLogin(new JSONObject().put("email", USER_EMAIL));
            assertResponseWithCodeAndMessage(failed2, UNAUTHORIZED_401, "email or password are incorrect", false);

            var failed3 = tryUserLogin(new JSONObject().put("password", USER_PASSWORD));
            assertResponseWithCodeAndMessage(failed3, UNAUTHORIZED_401, "email or password are incorrect", false);

            var ok = tryUserLogin(new JSONObject().put("email", USER_EMAIL).put("password", USER_PASSWORD));
            assertResponse(ok, OK_200, "accessToken", notNullValue(),
                    "refreshToken", notNullValue(),
                    "user", notNullValue(),
                    "success", equalTo(true));

            var failedLogout1 = tryUserLogout(null);
            assertResponseWithCodeAndMessage(failedLogout1, NOT_FOUND_404, "Token required", false);

            var failedLogout2 = tryUserLogout(ok.then().extract().path("accessToken"));
            assertResponseWithCodeAndMessage(failedLogout2, NOT_FOUND_404, "Token required", false);

            var logout = tryUserLogout(ok.then().extract().path("refreshToken"));
            assertResponseWithCodeAndMessage(logout, OK_200, "Successful logout", true);
        }
        finally {
            tryUserDelete(created.then().extract().path("accessToken"));
        }
    }

    @Test
    public void testUserInfo() {
        var created = tryUserCreate(createUserBody);
        try {
            var info = tryUserGetInfo(created.then().extract().path("accessToken"));
            assertResponse(info, OK_200, "user.name", equalTo(USER_NAME),
                    "user.email", equalTo(USER_EMAIL),
                    "success", equalTo(true));
        }
        finally {
            tryUserDelete(created.then().extract().path("accessToken"));
        }
    }

    @Test
    public void testUserInfoAnon() {
        var info = tryUserGetInfo("Bearer not_real_token");
        assertResponseWithCodeAndMessage(info, BAD_REQUEST_403, "jwt malformed", false);

        info = tryUserGetInfo("Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjY2MzdlMjQ1OWVkMjgwMDAxYjNmZjFmYSIsImlhdCI6MTcxNDkzODQzNywiZXhwIjoxNzE0OTM5NjM3fQ.5ooKgqz4r8baWrmy53GLwMIdlIO7X55RquWJerVhoNw");
        assertResponseWithCodeAndMessage(info, BAD_REQUEST_403, "jwt expired", false);
    }


    @Test
    public void testUserInfoUpdate() {
        var created = tryUserCreate(createUserBody);
        try {
            var newName = "test-name";
            var newEmail = "testemail" + Instant.now().getEpochSecond() + "@ya.ru";
            var info = tryUserUpdate(new JSONObject()
                    .put("name", newName)
                    .put("email", newEmail), created.then().extract().path("accessToken"));
            assertResponse(info, OK_200, "user.name", equalTo(newName),
                    "user.email", equalTo(newEmail),
                    "success", equalTo(true));
        }
        finally {
            tryUserDelete(created.then().extract().path("accessToken"));
        }
    }

    @Test
    public void testUserInfoUpdateAnon() {
        var newName = "test-name";
        var newEmail = "testemail" + Instant.now().getEpochSecond() + "@ya.ru";
        var info = tryUserUpdate(new JSONObject()
                .put("name", newName)
                .put("email", newEmail), "asdadads");
        assertResponse(info, UNAUTHORIZED_401, "message", equalTo("You should be authorised"),
                "success", equalTo(false));
    }

    @Test
    public void testUserInfoUpdateExistingEmail() {
        var created1 = tryUserCreate(createUserBody);
        var created2 = tryUserCreate(new JSONObject(createUserBody.toString()).put("email", "testemail" + Instant.now().getEpochSecond() + "@ya.ru"));
        try {
            var newName = "test-name";
            var info = tryUserUpdate(new JSONObject()
                    .put("name", newName)
                    .put("email", USER_EMAIL), created2.then().extract().path("accessToken"));
            assertResponse(info, BAD_REQUEST_403,
                    "message", equalTo("User with such email already exists"),
                    "success", equalTo(false));
        }
        finally {
            tryUserDelete(created1.then().extract().path("accessToken"));
            tryUserDelete(created2.then().extract().path("accessToken"));
        }
    }



}
