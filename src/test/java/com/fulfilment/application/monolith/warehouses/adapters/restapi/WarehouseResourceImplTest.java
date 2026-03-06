package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
class WarehouseResourceImplTest {

    @InjectMock
    WarehouseRepository warehouseRepository;

    @InjectMock
    CreateWarehouseOperation createWarehouseOperation;

    @InjectMock
    ArchiveWarehouseOperation archiveWarehouseOperation;

    @InjectMock
    ReplaceWarehouseOperation replaceWarehouseOperation;

    private Warehouse sampleWarehouse;

    @BeforeEach
    void setUp() {
        sampleWarehouse = new Warehouse();
        sampleWarehouse.businessUnitCode = "WH-001";
        sampleWarehouse.location = "Amsterdam";
        sampleWarehouse.capacity = 100;
        sampleWarehouse.stock = 50;
    }

    // ── GET /warehouse ────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /warehouse returns all warehouses")
    void listAll_returnsAllWarehouses() {
        Warehouse second = new Warehouse();
        second.businessUnitCode = "WH-002";
        second.location = "Rotterdam";
        second.capacity = 200;
        second.stock = 80;

        when(warehouseRepository.getAll())
                .thenReturn(List.of(sampleWarehouse, second));

        given()
                .when().get("/warehouse")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$.size()", is(2))
                .body("[0].businessUnitCode", is("WH-001"))
                .body("[0].location", is("Amsterdam"))
                .body("[1].businessUnitCode", is("WH-002"));
    }

    @Test
    @DisplayName("GET /warehouse returns empty list when no warehouses exist")
    void listAll_returnsEmptyList_whenNoneExist() {
        when(warehouseRepository.getAll()).thenReturn(List.of());

        given()
                .when().get("/warehouse")
                .then()
                .statusCode(200)
                .body("$.size()", is(0));
    }

    // ── GET /warehouse/{id} ───────────────────────────────────────────────────

    @Test
    @DisplayName("GET /warehouse/{id} returns warehouse when found")
    void getById_returnsWarehouse_whenFound() {
        when(warehouseRepository.findByBusinessUnitCode("WH-001"))
                .thenReturn(sampleWarehouse);

        given()
                .when().get("/warehouse/WH-001")
                .then()
                .statusCode(200)
                .body("businessUnitCode", is("WH-001"))
                .body("location", is("Amsterdam"))
                .body("capacity", is(100))
                .body("stock", is(50));
    }

    @Test
    @DisplayName("GET /warehouse/{id} returns 404 when not found")
    void getById_returns404_whenNotFound() {
        when(warehouseRepository.findByBusinessUnitCode("WH-999"))
                .thenReturn(null);

        given()
                .when().get("/warehouse/WH-999")
                .then()
                .statusCode(404)
                .body("error", containsString("WH-999"));
    }

    // ── POST /warehouse ───────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /warehouse creates warehouse and returns 200")
    void create_returnsCreatedWarehouse_whenValid() {
        doNothing().when(createWarehouseOperation).create(any(Warehouse.class));

        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                    "businessUnitCode": "WH-001",
                    "location": "Amsterdam",
                    "capacity": 100,
                    "stock": 50
                }
                """)
                .when().post("/warehouse")
                .then()
                .statusCode(200)
                .body("businessUnitCode", is("WH-001"))
                .body("location", is("Amsterdam"))
                .body("capacity", is(100))
                .body("stock", is(50));
    }

    @Test
    @DisplayName("POST /warehouse returns 400 when create operation throws IllegalArgumentException")
    void create_returns400_whenValidationFails() {
        doThrow(new IllegalArgumentException("Capacity must be greater than stock"))
                .when(createWarehouseOperation).create(any(Warehouse.class));

        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                    "businessUnitCode": "WH-001",
                    "location": "Amsterdam",
                    "capacity": 10,
                    "stock": 50
                }
                """)
                .when().post("/warehouse")
                .then()
                .statusCode(400)
                .body("error", containsString("Capacity must be greater than stock"));
    }

    @Test
    @DisplayName("POST /warehouse defaults stock to 0 when not provided")
    void create_defaultsStockToZero_whenStockIsNull() {
        doNothing().when(createWarehouseOperation).create(any(Warehouse.class));

        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                    "businessUnitCode": "WH-001",
                    "location": "Amsterdam",
                    "capacity": 100
                }
                """)
                .when().post("/warehouse")
                .then()
                .statusCode(200)
                .body("stock", is(0)); // defaulted to 0
    }

    // ── DELETE /warehouse/{id} ────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /warehouse/{id} archives warehouse successfully")
    void archive_returns204_whenWarehouseExists() {
        when(warehouseRepository.findByBusinessUnitCode("WH-001"))
                .thenReturn(sampleWarehouse);
        doNothing().when(archiveWarehouseOperation).archive(any(Warehouse.class));

        given()
                .when().delete("/warehouse/WH-001")
                .then()
                .statusCode(204);

        // verify archive was called with the correct warehouse
        verify(archiveWarehouseOperation).archive(argThat(w ->
                w.businessUnitCode.equals("WH-001")));
    }

    @Test
    @DisplayName("DELETE /warehouse/{id} returns 404 when warehouse not found")
    void archive_returns404_whenWarehouseNotFound() {
        when(warehouseRepository.findByBusinessUnitCode("WH-999"))
                .thenReturn(null);

        given()
                .when().delete("/warehouse/WH-999")
                .then()
                .statusCode(404)
                .body("error", containsString("WH-999"));

        // verify archive operation was never called
        verify(archiveWarehouseOperation, never()).archive(any());
    }

    @Test
    @DisplayName("DELETE /warehouse/{id} returns 400 when archive operation fails validation")
    void archive_returns400_whenValidationFails() {
        when(warehouseRepository.findByBusinessUnitCode("WH-001"))
                .thenReturn(sampleWarehouse);
        doThrow(new IllegalArgumentException("Warehouse already archived"))
                .when(archiveWarehouseOperation).archive(any(Warehouse.class));

        given()
                .when().delete("/warehouse/WH-001")
                .then()
                .statusCode(400)
                .body("error", containsString("Warehouse already archived"));
    }

    // ── Replace /warehouse/{businessUnitCode} ─────────────────────────────────────

    @Test
    @DisplayName("Replace warehouse successfully returns updated warehouse")
    void replace_returnsUpdatedWarehouse_whenValid() {
        Warehouse updated = new Warehouse();
        updated.businessUnitCode = "WH-001";
        updated.location = "Utrecht";
        updated.capacity = 150;
        updated.stock = 60;

        doNothing().when(replaceWarehouseOperation).replace(any(Warehouse.class));
        when(warehouseRepository.findByBusinessUnitCode("WH-001")).thenReturn(updated);

        given()
                .contentType(ContentType.JSON)
                .body("""
            {
                "location": "Utrecht",
                "capacity": 150,
                "stock": 60
            }
            """)
                .when().post("/warehouse/WH-001/replacement")
                .then()
                .statusCode(200)
                .body("businessUnitCode", is("WH-001"))
                .body("location", is("Utrecht"))
                .body("capacity", is(150))
                .body("stock", is(60));
    }

    @Test
    @DisplayName("Replace uses businessUnitCode from path, not from request body")
    void replace_usesPathBusinessUnitCode_notBodyCode() {
        doNothing().when(replaceWarehouseOperation).replace(any(Warehouse.class));
        when(warehouseRepository.findByBusinessUnitCode("WH-001")).thenReturn(sampleWarehouse);

        given()
                .contentType(ContentType.JSON)
                .body("""
            {
                "businessUnitCode": "WH-SHOULD-BE-IGNORED",
                "location": "Utrecht",
                "capacity": 150
            }
            """)
                .when().post("/warehouse/WH-001/replacement")
                .then()
                .statusCode(200);

        // key assertion — path code used, body code ignored
        verify(replaceWarehouseOperation).replace(argThat(w ->
                w.businessUnitCode.equals("WH-001")));
    }

    @Test
    @DisplayName("Replace defaults stock to 0 when stock is not provided in request")
    void replace_defaultsStockToZero_whenStockIsNull() {
        doNothing().when(replaceWarehouseOperation).replace(any(Warehouse.class));
        when(warehouseRepository.findByBusinessUnitCode("WH-001")).thenReturn(sampleWarehouse);

        given()
                .contentType(ContentType.JSON)
                .body("""
            {
                "location": "Amsterdam",
                "capacity": 100
            }
            """)
                .when().post("/warehouse/WH-001/replacement")
                .then()
                .statusCode(200);

        // verify stock was defaulted to 0
        verify(replaceWarehouseOperation).replace(argThat(w -> w.stock == 0));
    }

    @Test
    @DisplayName("Replace maps all fields from request body to domain model correctly")
    void replace_mapsAllFieldsCorrectly() {
        doNothing().when(replaceWarehouseOperation).replace(any(Warehouse.class));
        when(warehouseRepository.findByBusinessUnitCode("WH-001")).thenReturn(sampleWarehouse);

        given()
                .contentType(ContentType.JSON)
                .body("""
            {
                "location": "Rotterdam",
                "capacity": 200,
                "stock": 75
            }
            """)
                .when().post("/warehouse/WH-001/replacement")
                .then()
                .statusCode(200);

        // verify every field was mapped correctly to domain model
        verify(replaceWarehouseOperation).replace(argThat(w ->
                w.businessUnitCode.equals("WH-001") &&
                        w.location.equals("Rotterdam") &&
                        w.capacity == 200 &&
                        w.stock == 75));
    }

    @Test
    @DisplayName("Replace returns 400 when replace operation throws IllegalArgumentException")
    void replace_returns400_whenValidationFails() {
        doThrow(new IllegalArgumentException("Location not supported"))
                .when(replaceWarehouseOperation).replace(any(Warehouse.class));

        given()
                .contentType(ContentType.JSON)
                .body("""
            {
                "location": "InvalidLocation",
                "capacity": 100,
                "stock": 10
            }
            """)
                .when().post("/warehouse/WH-001/replacement")
                .then()
                .statusCode(400)
                .body("error", containsString("Location not supported"));
    }

    @Test
    @DisplayName("Replace returns updated state from repository after replace operation")
    void replace_returnsRepositoryState_afterReplaceOperation() {
        // The method calls replaceWarehouseOperation.replace() then fetches
        // fresh state from repository — we should return what the repo says,
        // not what was in the request
        Warehouse repoState = new Warehouse();
        repoState.businessUnitCode = "WH-001";
        repoState.location = "Amsterdam";
        repoState.capacity = 150;
        repoState.stock = 99; // repo may have different stock than request

        doNothing().when(replaceWarehouseOperation).replace(any(Warehouse.class));
        when(warehouseRepository.findByBusinessUnitCode("WH-001")).thenReturn(repoState);

        given()
                .contentType(ContentType.JSON)
                .body("""
            {
                "location": "Amsterdam",
                "capacity": 150,
                "stock": 10
            }
            """)
                .when().post("/warehouse/WH-001/replacement")
                .then()
                .statusCode(200)
                .body("stock", is(99)); // repo state returned, not request state
    }

    @Test
    @DisplayName("Replace calls replace operation before fetching updated state")
    void replace_callsReplaceOperation_thenFetchesUpdatedState() {
        doNothing().when(replaceWarehouseOperation).replace(any(Warehouse.class));
        when(warehouseRepository.findByBusinessUnitCode("WH-001")).thenReturn(sampleWarehouse);

        given()
                .contentType(ContentType.JSON)
                .body("""
            {
                "location": "Amsterdam",
                "capacity": 100,
                "stock": 50
            }
            """)
                .when().post("/warehouse/WH-001/replacement")
                .then()
                .statusCode(200);

        // verify call order — replace must happen before findByBusinessUnitCode
        InOrder inOrder = inOrder(replaceWarehouseOperation, warehouseRepository);
        inOrder.verify(replaceWarehouseOperation).replace(any(Warehouse.class));
        inOrder.verify(warehouseRepository).findByBusinessUnitCode("WH-001");
    }
}