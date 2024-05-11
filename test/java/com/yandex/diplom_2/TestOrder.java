package com.yandex.diplom_2;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.json.JSONObject;
import org.junit.Test;

import static com.yandex.diplom_2.TestUser.createUserBody;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.oneOf;

public class TestOrder extends BaseTest {

    @Test
    public void testOrdersAll() {
        var orders = tryOrdersGetAll();
        assertResponse(orders, OK_200,
                "success", equalTo(true),
                "orders.owner", everyItem(nullValue()),
                "orders.price", everyItem(nullValue()),
                "orders.ingredients", everyItem(not(empty())),
                "orders.number", everyItem(greaterThan(0)),
                "orders.status", everyItem(oneOf("done",
                        //todo: proper naming of following statuses is not provided in API documentation
                        "preparing", "cancelled")),
                "total", greaterThan(0),
        "totalToday", notNullValue());

        //check sort orders are sorted by updatedAt
        List<Map<String, String>> ordersList = orders.jsonPath().getList("orders");
        var formatter = new DateTimeFormatterBuilder()
                .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .optionalStart().appendOffset("+HH:MM", "+00:00").optionalEnd()
                .optionalStart().appendOffset("+HHMM", "+0000").optionalEnd()
                .optionalStart().appendOffset("+HH", "Z").optionalEnd()
                .toFormatter();
        var prev = LocalDateTime.MAX;
        for (var order: ordersList) {
            var current = LocalDateTime.parse(order.get("updatedAt"), formatter);
            assertThat(current.isBefore(prev), equalTo(true));
            prev = current;
        }
    }

    @Test
    public void testOrdersNewUserForNew() {
        var created = tryUserCreate(createUserBody);
        try {
            var orders = tryOrdersGet(created.then().extract().path("accessToken"));
            assertResponse(orders, OK_200, "orders", empty(),
                    //bug total and totalToday for new users must be eq 0
                    "total", equalTo(0),
                    "totalToday", equalTo(0));
        }
        finally {
            tryUserDelete(created.then().extract().path("accessToken"));
        }
    }

    @Test
    public void testOrdersGetForAnon() {
        var orders = tryOrdersGet("asdasdadad");
        assertResponse(orders, UNAUTHORIZED_401, "message", equalTo("You should be authorised"),
                "success", equalTo(false));

        orders = tryOrdersGet("");
        assertResponse(orders, UNAUTHORIZED_401, "message", equalTo("You should be authorised"),
                "success", equalTo(false));

        orders = tryOrdersGet("Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjY2MzdlMjQ1OWVkMjgwMDAxYjNmZjFmYSIsImlhdCI6MTcxNDkzODQzNywiZXhwIjoxNzE0OTM5NjM3fQ.5ooKgqz4r8baWrmy53GLwMIdlIO7X55RquWJerVhoNw");
        assertResponse(orders, BAD_REQUEST_403, "message", equalTo("jwt expired"),
                "success", equalTo(false));
    }

    @Test
    public void testOrderCreateWithInvalidData() {
        var created = tryUserCreate(createUserBody);
        try {
            var burger = tryOrderCreate(null, created.then().extract().path("accessToken"));
            assertResponse(burger, BAD_REQUEST_400, "success", equalTo(false),
                    "message", equalTo("Ingredient ids must be provided"));

            burger = tryOrderCreate(new JSONObject().put("ingredients", List.of()), created.then().extract().path("accessToken"));
            assertResponse(burger, BAD_REQUEST_400, "success", equalTo(false),
                    "message", equalTo("Ingredient ids must be provided"));

            burger = tryOrderCreate(new JSONObject().put("ingredients", List.of("i'm not a hash")), created.then().extract().path("accessToken"));
            assertResponse(burger, ERR_500);

            burger = tryOrderCreate(new JSONObject().put("ingredients", List.of()), "i'm not a token");
            assertResponse(burger, BAD_REQUEST_403, "message", equalTo("jwt malformed"), "success", equalTo(false));

            burger = tryOrderCreate(new JSONObject().put("ingredients", List.of()), "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjY2MzdlMjQ1OWVkMjgwMDAxYjNmZjFmYSIsImlhdCI6MTcxNDkzODQzNywiZXhwIjoxNzE0OTM5NjM3fQ.5ooKgqz4r8baWrmy53GLwMIdlIO7X55RquWJerVhoNw");
            assertResponse(burger, BAD_REQUEST_403, "message", equalTo("jwt expired"), "success", equalTo(false));
        }
        finally {
            tryUserDelete(created.then().extract().path("accessToken"));
        }
    }

    @Test
    public void testOrderIngredients() {
        var ingredients = tryGetIngredients();
        assertResponse(ingredients, OK_200, "success", equalTo(true),
                "data", notNullValue(),
                "data.proteins", everyItem(greaterThan(0)),
                "data.fat", everyItem(greaterThan(0)),
                "data.carbohydrates", everyItem(greaterThan(0)),
                "data.calories", everyItem(greaterThan(0)),
                "data.price", everyItem(greaterThan(0)),
                "data.image", everyItem(notNullValue()),
                "data.name", everyItem(notNullValue()),
                "data.type", everyItem(oneOf("bun", "main", "sauce"))
        );
    }

    @Test
    public void testOrderCreate() {
        var created = tryUserCreate(createUserBody);
        try {
            var ingredients = tryGetIngredients();
            List<String> ingredientIds = ingredients.jsonPath().getList("data._id");

            var singleIngredient = List.of(ingredientIds.get(0));
            var burgerSingleIngredient = tryOrderCreate(new JSONObject()
                    .put("ingredients", singleIngredient), created.then().extract().path("accessToken"));

            assertResponse(burgerSingleIngredient, OK_200, "success", equalTo(true),
                    "name", notNullValue(),
                    "order.owner.email", equalTo(USER_EMAIL),
                    "order.price", greaterThan(0),
                    "order.ingredients._id", hasSize(singleIngredient.size()),
                    "order.ingredients._id", containsInAnyOrder(singleIngredient.toArray()));
        }
        finally {
            tryUserDelete(created.then().extract().path("accessToken"));
        }
    }

    @Test
    public void testOrderCreateOnlyBuns() {
        var created = tryUserCreate(createUserBody);
        try {
            var ingredients = tryGetIngredients();
            assertResponse(ingredients, OK_200, "success", equalTo(true), "data", notNullValue());
            List<Map<String, String>> ingredientIds = ingredients.jsonPath().getList("data");

            var onlyBuns = ingredientIds.stream()
                    .filter(h -> "bun".equals(h.get("type")))
                    .map(h -> h.get("_id"))
                    .collect(Collectors.toList());
            var burgerSingleIngredient = tryOrderCreate(new JSONObject()
                    .put("ingredients", onlyBuns), created.then().extract().path("accessToken"));

            assertResponse(burgerSingleIngredient, OK_200, "success", equalTo(true),
                    "name", notNullValue(),
                    "order.owner.email", equalTo(USER_EMAIL),
                    "order.price", greaterThan(0),
                    "order.ingredients._id", hasSize(onlyBuns.size()),
                    "order.ingredients._id", containsInAnyOrder(onlyBuns.toArray()));
        }
        finally {
            tryUserDelete(created.then().extract().path("accessToken"));
        }
    }

    @Test
    public void testOrderCreateOnlySalsas() {
        var created = tryUserCreate(createUserBody);
        try {
            var ingredients = tryGetIngredients();
            assertResponse(ingredients, OK_200, "success", equalTo(true), "data", notNullValue());
            List<Map<String, String>> ingredientIds = ingredients.jsonPath().getList("data");

            var onlySalsas = ingredientIds.stream()
                    .filter(h -> "sauce".equals(h.get("type")))
                    .map(h -> h.get("_id"))
                    .collect(Collectors.toList());
            var burgerSingleIngredient = tryOrderCreate(new JSONObject()
                    .put("ingredients", onlySalsas), created.then().extract().path("accessToken"));

            assertResponse(burgerSingleIngredient, OK_200, "success", equalTo(true),
                    "name", notNullValue(),
                    "order.owner.email", equalTo(USER_EMAIL),
                    "order.price", greaterThan(0),
                    "order.ingredients._id", hasSize(onlySalsas.size()),
                    "order.ingredients._id", containsInAnyOrder(onlySalsas.toArray()));
        }
        finally {
            tryUserDelete(created.then().extract().path("accessToken"));
        }
    }

    @Test
    public void testOrderCreateWithInvalidIngredients() {
        var created = tryUserCreate(createUserBody);
        try {
            var ingredients = tryGetIngredients();
            List<String> ingredientIds = ingredients.jsonPath().getList("data._id");

            /*
            try to send trash like
            "ingredients": [
                "61c0c5a71d1f82001bdaaa6d",
                "61c0c5a71d1f82001bdaaa6d",
                [
                    "61c0c5a71d1f82001bdaaa6d"
                ],
                [
                    "61c0c5a71d1f82001bdaaa6d"
                ]
            ]
             */
            var burgerSingleIngredient = tryOrderCreate(new JSONObject()
                    .put("ingredients", List.of(
                            ingredientIds.get(0),
                            ingredientIds.get(0),
                            List.of(ingredientIds.get(0)),
                            List.of(ingredientIds.get(0))
                    )), created.then().extract().path("accessToken"));

            assertResponse(burgerSingleIngredient, BAD_REQUEST_403);
        }
        finally {
            tryUserDelete(created.then().extract().path("accessToken"));
        }
    }

    @Test
    public void testOrderCreateIncCounter() {
        var created = tryUserCreate(createUserBody);
        try {
            var initialStats = tryOrdersGet(created.then().extract().path("accessToken"));
            var ingredients = tryGetIngredients();
            List<String> ingredientIds = ingredients.jsonPath().getList("data._id");
            var burger = tryOrderCreate(new JSONObject().put("ingredients", List.of(ingredientIds.get(0))), created.then().extract().path("accessToken"));
            assertResponse(burger, OK_200);

            var newStats = tryOrdersGet(created.then().extract().path("accessToken"));
            assertResponse(newStats, OK_200,
                    //ignoring case when test are running at midnight
                    "totalToday", greaterThan(initialStats.then().extract().path("totalToday")),
                    "total", greaterThan(initialStats.then().extract().path("total")));

        }
        finally {
            tryUserDelete(created.then().extract().path("accessToken"));
        }
    }
}
