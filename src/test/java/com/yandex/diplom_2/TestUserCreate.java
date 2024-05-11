package com.yandex.diplom_2;

import java.util.Arrays;
import java.util.Collection;
import org.hamcrest.Matcher;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;

@RunWith(Parameterized.class)
public class TestUserCreate extends BaseTest {
    @Parameterized.Parameters
    public static Collection<Object> data() {
        return Arrays.asList(new Object[][] {
                {USER_NAME, USER_EMAIL, USER_PASSWORD, true, OK_200, null},
                {null, USER_EMAIL, USER_PASSWORD, false, BAD_REQUEST_403, "Email, password and name are required fields"},
                {USER_NAME, null, USER_PASSWORD, false, BAD_REQUEST_403, "Email, password and name are required fields"},
                {USER_NAME, USER_EMAIL, null, false, BAD_REQUEST_403, "Email, password and name are required fields"},
                {USER_NAME, "not_email", USER_PASSWORD, false, ERR_500, null},
        });
    }

    private final String name;
    private final String email;
    private final String password;
    private final Boolean success;
    private final Matcher<Integer> code;
    private final String message;

    public TestUserCreate(String name, String email, String password, Boolean success, Matcher<Integer> code, String message) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.success = success;
        this.code = code;
        this.message = message;
    }

    @Test
    public void testCreate() {
        var response = tryUserCreate(new JSONObject()
                .put("email", email)
                .put("name", name)
                .put("password", password));
        if (success) {
            try {
                response.then().assertThat().statusCode(code)
                        .body("accessToken", notNullValue(),
                                "refreshToken", notNullValue(),
                                "user", notNullValue(),
                                "success", equalTo(success));
            }
            finally {
                var deleteResponse = tryUserDelete(response.then().extract().path("accessToken"));
                assertResponseWithCodeAndMessage(deleteResponse, ACCEPTED_202, "User successfully removed", true);
            }
        }
        else {
            if (message != null)
                assertResponseWithCodeAndMessage(response, code, message, success);
            else
                response.then().assertThat().statusCode(code);
        }

    }
}
