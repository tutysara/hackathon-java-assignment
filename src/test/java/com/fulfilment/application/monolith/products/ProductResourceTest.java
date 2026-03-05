package com.fulfilment.application.monolith.products;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.panache.common.Sort;
import io.restassured.http.ContentType;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@QuarkusTest
class ProductResourceTest {

    @InjectMock
    ProductRepository productRepository;

    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        sampleProduct = new Product();
        sampleProduct.id = 1L;
        sampleProduct.name = "Test Product";
        sampleProduct.description = "A test product";
        sampleProduct.price = new BigDecimal("9.99");
        sampleProduct.stock = 100;
    }

    // ── GET /product ──────────────────────────────────────────────────────────

    /*
    @Test
    @DisplayName("GET /product returns all products sorted by name")
    void getAll_returnsAllProducts() {
        Product second = new Product("Another Product");
        second.id = 2L;

        Sort nameSort = Sort.by("name");

        when(productRepository.listAll(eq(nameSort)))
                .thenReturn(List.of(second, sampleProduct));

        given()
                .when().get("/product")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("[0].name", is("Another Product"))
                .body("[1].name", is("Test Product"));
    }
    */
    @Test
    @DisplayName("GET /product returns empty list when no products exist")
    void getAll_returnsEmptyList_whenNoProducts() {
        when(productRepository.listAll(Sort.by("name")))
                .thenReturn(List.of());

        given()
                .when().get("/product")
                .then()
                .statusCode(200)
                .body("$.size()", is(0));
    }

    // ── GET /product/{id} ─────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /product/{id} returns product when found")
    void getSingle_returnsProduct_whenFound() {
        when(productRepository.findById(1L)).thenReturn(sampleProduct);

        given()
                .when().get("/product/1")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", is(1))
                .body("name", is("Test Product"))
                .body("description", is("A test product"));
    }

    @Test
    @DisplayName("GET /product/{id} returns 404 when product not found")
    void getSingle_returns404_whenNotFound() {
        when(productRepository.findById(99L)).thenReturn(null);

        given()
                .when().get("/product/99")
                .then()
                .statusCode(404)
                .body("code", is(404))
                .body("error", containsString("99"));
    }

    // ── POST /product ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /product creates product and returns 201")
    void create_returnsCreated_whenValid() {
        Product newProduct = new Product();
        newProduct.name = "New Product";
        newProduct.description = "Brand new";
        newProduct.price = new BigDecimal("5.99");
        newProduct.stock = 50;
        // id is null — valid for creation

        doNothing().when(productRepository).persist(any(Product.class));

        given()
                .contentType(ContentType.JSON)
                .body(newProduct)
                .when().post("/product")
                .then()
                .statusCode(201)
                .body("name", is("New Product"))
                .body("description", is("Brand new"));
    }

    @Test
    @DisplayName("POST /product returns 422 when id is set on request")
    void create_returns422_whenIdIsSet() {
        // id is already set — invalid for creation
        given()
                .contentType(ContentType.JSON)
                .body(sampleProduct) // sampleProduct has id = 1L
                .when().post("/product")
                .then()
                .statusCode(422)
                .body("code", is(422))
                .body("error", containsString("Id was invalidly set"));
    }

    // ── PUT /product/{id} ─────────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /product/{id} updates and returns product when valid")
    void update_returnsUpdatedProduct_whenValid() {
        Product updateRequest = new Product();
        updateRequest.name = "Updated Name";
        updateRequest.description = "Updated description";
        updateRequest.price = new BigDecimal("19.99");
        updateRequest.stock = 200;

        when(productRepository.findById(1L)).thenReturn(sampleProduct);
        doNothing().when(productRepository).persist(any(Product.class));

        given()
                .contentType(ContentType.JSON)
                .body(updateRequest)
                .when().put("/product/1")
                .then()
                .statusCode(200)
                .body("name", is("Updated Name"))
                .body("description", is("Updated description"));
    }

    @Test
    @DisplayName("PUT /product/{id} returns 422 when name is missing")
    void update_returns422_whenNameIsNull() {
        Product updateRequest = new Product();
        updateRequest.name = null; // missing name
        updateRequest.description = "Some description";

        given()
                .contentType(ContentType.JSON)
                .body(updateRequest)
                .when().put("/product/1")
                .then()
                .statusCode(422)
                .body("code", is(422))
                .body("error", containsString("Product Name was not set"));
    }

    @Test
    @DisplayName("PUT /product/{id} returns 404 when product not found")
    void update_returns404_whenProductNotFound() {
        Product updateRequest = new Product();
        updateRequest.name = "Updated Name";

        when(productRepository.findById(99L)).thenReturn(null);

        given()
                .contentType(ContentType.JSON)
                .body(updateRequest)
                .when().put("/product/99")
                .then()
                .statusCode(404)
                .body("code", is(404))
                .body("error", containsString("99"));
    }

    // ── DELETE /product/{id} ──────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /product/{id} returns 204 when product deleted")
    void delete_returns204_whenProductExists() {
        when(productRepository.findById(1L)).thenReturn(sampleProduct);
        doNothing().when(productRepository).delete(any(Product.class));

        given()
                .when().delete("/product/1")
                .then()
                .statusCode(204);

        verify(productRepository).delete(sampleProduct);
    }

    @Test
    @DisplayName("DELETE /product/{id} returns 404 when product not found")
    void delete_returns404_whenProductNotFound() {
        when(productRepository.findById(99L)).thenReturn(null);

        given()
                .when().delete("/product/99")
                .then()
                .statusCode(404)
                .body("code", is(404))
                .body("error", containsString("99"));

        verify(productRepository, never()).delete(any());
    }

    // ── ErrorMapper ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("ErrorMapper returns correct JSON structure on 500")
    void errorMapper_returns500_onUnexpectedException() {
        when(productRepository.listAll(any(Sort.class)))
                .thenThrow(new RuntimeException("Database connection lost"));

        given()
                .when().get("/product")
                .then()
                .statusCode(500)
                .body("code", is(500))
                .body("exceptionType", containsString("RuntimeException"))
                .body("error", is("Database connection lost"));
    }

    @Test
    @DisplayName("ErrorMapper returns correct code for WebApplicationException")
    void errorMapper_returnsCorrectCode_forWebApplicationException() {
        when(productRepository.findById(1L))
                .thenThrow(new WebApplicationException("Forbidden", 403));

        given()
                .when().get("/product/1")
                .then()
                .statusCode(403)
                .body("code", is(403));
    }
}