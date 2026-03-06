package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.TestTransaction;
import io.restassured.http.ContentType;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class StoreResourceTest {

    @BeforeEach
    @Transactional
    public void cleanDb() {
        Store.deleteAll();
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    void createStore(Store store) {
        store.persist();
    }

    // ---------------------------
    // CREATE
    // ---------------------------

    @Test
    public void testCreateStore() {
        String body = """
        {
            "name": "Amsterdam Store",
            "quantityProductsInStock": 50
        }
        """;

        given()
                .contentType("application/json")
                .body(body)
                .when()
                .post("/store")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", is("Amsterdam Store"))
                .body("quantityProductsInStock", is(50));
    }

    @Test
    public void testCreateStoreWithIdFails() {
        String body = """
        {
            "id": 1,
            "name": "Invalid Store",
            "quantityProductsInStock": 10
        }
        """;

        given()
                .contentType("application/json")
                .body(body)
                .when()
                .post("/store")
                .then()
                .statusCode(422)
                .body("code", is(422))
                .body("exceptionType", containsString("WebApplicationException"));
    }

    // ---------------------------
    // GET
    // ---------------------------

    @Test
    public void testGetAllStores() {
        Store s1 = new Store("A");
        s1.quantityProductsInStock = 10;
        createStore(s1);

        Store s2 = new Store("B");
        s2.quantityProductsInStock = 20;
        createStore(s2);

        given()
                .when()
                .get("/store")
                .then()
                .statusCode(200)
                .body("size()", is(2));
    }

    @Test
    @TestTransaction
    public void testGetSingleStore() {
        Store s = new Store();
        s.name = "Single";
        s.quantityProductsInStock = 5;
        createStore(s);

        given()
                .when()
                .get("/store/" + s.id)
                .then()
                .statusCode(200)
                .body("name", is("Single"));
    }

    @Test
    public void testGetNonExistingStore() {
        given()
                .when()
                .get("/store/99999")
                .then()
                .statusCode(404)
                .body("code", is(404));
    }

    // ---------------------------
    // UPDATE (PUT)
    // ---------------------------

    @Test
    @TestTransaction
    public void testUpdateStore() {
        Store s = new Store();
        s.name = "Old";
        s.quantityProductsInStock = 5;
        createStore(s);


        String body = """
        {
            "name": "Updated",
            "quantityProductsInStock": 100
        }
        """;

        given()
                .contentType("application/json")
                .body(body)
                .when()
                .put("/store/" + s.id)
                .then()
                .statusCode(200)
                .body("name", is("Updated"))
                .body("quantityProductsInStock", is(100));
    }

    @Test
    public void testUpdateNonExistingStore() {
        String body = """
        {
            "name": "Updated",
            "quantityProductsInStock": 100
        }
        """;

        given()
                .contentType("application/json")
                .body(body)
                .when()
                .put("/store/9999")
                .then()
                .statusCode(404);
    }

    // ---------------------------
    // PATCH
    // ---------------------------

    @Test
    @TestTransaction
    public void testPatchStore() {
        Store s = new Store();
        s.name = "PatchOld";
        s.quantityProductsInStock = 10;
        createStore(s);

        String body = """
        {
            "name": "PatchNew",
            "quantityProductsInStock": 20
        }
        """;

        given()
                .contentType("application/json")
                .body(body)
                .when()
                .patch("/store/" + s.id)
                .then()
                .statusCode(200)
                .body("name", is("PatchNew"));
    }
    // ---------------------------
    // DELETE
    // ---------------------------

    @Test
    @TestTransaction
    public void testDeleteStore() {
        Store s = new Store();
        s.name = "DeleteMe";
        s.quantityProductsInStock = 1;
        createStore(s);

        given()
                .when()
                .delete("/store/" + s.id)
                .then()
                .statusCode(204);

        assertNull(Store.findById(s.id));
    }

    @Test
    public void testDeleteNonExistingStore() {
        given()
                .when()
                .delete("/store/9999")
                .then()
                .statusCode(404);
    }

    // ── ErrorMapper — 404 scenarios ───────────────────────────────────────────

    @Test
    @DisplayName("ErrorMapper: GET /store/{id} returns correct error structure on 404")
    void errorMapper_get_404_hasCorrectStructure() {
        given()
                .when().get("/store/99999")
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("code", is(404))
                .body("exceptionType", is("jakarta.ws.rs.WebApplicationException"))
                .body("error", containsString("99999"));
    }

    @Test
    @DisplayName("ErrorMapper: PUT /store/{id} returns correct error structure on 404")
    void errorMapper_put_404_hasCorrectStructure() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                { "name": "Updated Store" }
                """)
                .when().put("/store/99999")
                .then()
                .statusCode(404)
                .body("code", is(404))
                .body("exceptionType", is("jakarta.ws.rs.WebApplicationException"))
                .body("error", containsString("99999"));
    }

    @Test
    @DisplayName("ErrorMapper: PUT /store/{id} returns error when store name is null")
    void errorMapper_put_422_hasCorrectStructure() {
        // 422 — same assertion for different code
              given()
                .contentType(ContentType.JSON)
                .body("""
                { "id": 1 }
                """)
                .when().put("/store/9999")
                .then()
                .statusCode(422)
                .body("code", is(422));
    }

    @Test
    @DisplayName("ErrorMapper: DELETE /store/{id} returns correct error structure on 404")
    void errorMapper_delete_404_hasCorrectStructure() {
        given()
                .when().delete("/store/99999")
                .then()
                .statusCode(404)
                .body("code", is(404))
                .body("exceptionType", is("jakarta.ws.rs.WebApplicationException"))
                .body("error", containsString("99999"));
    }

    @Test
    @DisplayName("ErrorMapper: PATCH /store/{id} returns correct error structure on 404")
    void errorMapper_patch_404_hasCorrectStructure() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                { "name": "Updated Store" }
                """)
                .when().patch("/store/99999")
                .then()
                .statusCode(404)
                .body("code", is(404))
                .body("exceptionType", is("jakarta.ws.rs.WebApplicationException"))
                .body("error", containsString("99999"));
    }

    @Test
    @DisplayName("ErrorMapper: PATCH /store/{id} returns correct error structure on 404")
    void errorMapper_patch_404_hasCorrectStructure_quantity() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                { 
                  "name": "Updated",
                  "quantityProductsInStock": 100
                }
                """)
                .when().patch("/store/99999")
                .then()
                .statusCode(404)
                .body("code", is(404))
                .body("exceptionType", is("jakarta.ws.rs.WebApplicationException"))
                .body("error", containsString("99999"));
    }

    @Test
    @DisplayName("ErrorMapper: PATCH /store/{id} returns error when store name is null")
    void errorMapper_patch_422_hasCorrectStructure() {
        // 422 — same assertion for different code
        given()
                .contentType(ContentType.JSON)
                .body("""
                { "id": 1 }
                """)
                .when().patch("/store/9999")
                .then()
                .statusCode(422)
                .body("code", is(422));
    }

}