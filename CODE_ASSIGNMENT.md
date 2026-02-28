# Code Assignment - Senior Java Hackathon

**Time Expectation**: ~6 hours

**Before starting:**
- Read [BRIEFING.md](BRIEFING.md) for domain context
- Read [README.md](README.md) to understand the reference implementations
- Study the existing code patterns and tests

---

## Overview

This assignment focuses on **transaction management, concurrency handling, and optimistic locking** — critical skills for senior backend engineers.

The codebase contains implementations for Archive and Replace operations, along with a test suite. Your job is to understand the existing code, ensure all tests pass, and answer discussion questions.

> **Important**: The codebase may contain bugs and the test suite may not pass out of the box. Investigating failures, identifying root causes, and fixing the underlying code is part of the assignment.

---

## What's Already Implemented (Study These)

The codebase contains complete reference implementations for Archive and Replace operations:

### Archive Warehouse Operation
- `ArchiveWarehouseUseCase.java` - Complete implementation with validations
- `ArchiveWarehouseUseCaseTest.java` - Full test suite
- `WarehouseResourceImpl.archiveAWarehouseUnitByID()` - REST endpoint
- `WarehouseRepository.update()` - Database operations

**Implemented Business Rules**:
1. Only existing warehouses can be archived
2. Already-archived warehouses cannot be archived again
3. Archiving sets the `archivedAt` timestamp to current time
4. Proper error responses for validation failures

### Replace Warehouse Operation
- `ReplaceWarehouseUseCase.java` - Complete implementation with validations
- `ReplaceWarehouseUseCaseTest.java` - Full test suite
- `WarehouseResourceImpl.replaceTheCurrentActiveWarehouse()` - REST endpoint
- `WarehouseRepository.update()` - Database operations

**Implemented Business Rules**:
1. Only existing warehouses can be replaced
2. Archived warehouses cannot be replaced
3. New location must be valid (exists in the system)
4. New capacity cannot exceed location's max capacity
5. New stock cannot exceed new capacity

---

## Your Tasks

### Task 1: Study the Reference Implementation

**Goal**: Understand the existing code and architecture before attempting anything else.

**What to Study**:
1. **Archive Use Case** (`ArchiveWarehouseUseCase.java`) - validations, fields updated, repository interaction
2. **Replace Use Case** (`ReplaceWarehouseUseCase.java`) - validations, LocationResolver interaction, field handling
3. **Repository Layer** (`WarehouseRepository.java`) - how `create()` and `update()` are implemented and whether they behave consistently
4. **REST Endpoints** (`WarehouseResourceImpl.java`) - how endpoints wire use cases, exception handling, transaction boundaries
5. **Test Patterns** - study `ArchiveWarehouseUseCaseTest.java` and `ReplaceWarehouseUseCaseTest.java`, understand the full test coverage

---

### Task 2: Make All Tests Pass

**Goal**: Ensure the entire test suite passes — investigate root causes of any failures and fix the underlying code.

**Instructions**:
1. Run the full test suite: `./mvnw clean test`
2. Also run integration tests that aren't included by default (e.g., classes with `IT` suffix): `./mvnw test -Dtest=WarehouseConcurrencyIT,WarehouseTestcontainersIT`
3. Identify any failing tests, investigate their root causes, and fix the underlying code
4. Do whatever is needed — the goal is a fully working codebase where all tests pass consistently

**Success Criteria**:
- All tests pass when running `./mvnw clean test`
- All explicitly targeted integration tests also pass
- No flaky tests — results are consistent across multiple runs

---

### Task 3: Answer Discussion Questions

Answer both questions in [QUESTIONS.md](QUESTIONS.md):

**Question 1: API Specification Approaches**

The Warehouse API is defined in an OpenAPI YAML file from which code is generated. The `Product` and `Store` endpoints are hand-coded directly.

What are the pros and cons of each approach? Which would you choose and why?

    I see Store and Product are implemented in StoreResource and ProductResource respectively.
	While WarehouseResourceImpl implements WarehouseResource which is generated from yaml.
	StoreResource and ProductResource is the traditional approach that I had followed in many projects so, looks more familiar.
	It is easy while doing prototying and during times when the details are not flushed out and the requirements are changing.

	Some disadvantages which I observed are listed here
	- We cannot clearly separate the concerns
	  Example: If UI team wants to work parallelly on the UI features, they have to depend on backend.
	  With no spec for API's they have to rely on running a devserver to test UI features.
	  Mocking is hard and if the backend API changes without notice all the mocks becomes irrelevant.
	- Difficulty collobrating with other teams
		Example: If another team depends on our code, they can't be sure if any spec is broken.
		So, they end up testing their code as well after our code deployment.
		This creates many implicit dependency between team instead of just relying on the interface that is the API spec.
	- If Swagger API is not generated then they have to look at the code to see the endpoints that are exposed/changed and rely on our 
        documentation in ticketing system or Jira to get the details.
	- The method names are not uniform and they have different coding styles after sometime and they becomes hard to maintain.


	OpenAPI first has lots of advantages like
		- Less code to maintain since code is generated
		- It is easy to communicate between different teams and different sections within same teams like backend vs UI etc
		- Design has to be thought beforehand so, it could be more refined
		- It forces everyone to align and stablize on a requirement before proceeding so, changed down the line can be minimized
	There are few disadvantages I see as well
		- Not sure if the yaml file is more readable for many and making change is difficult in huge yaml file.
		- It is slow for rapid development and in the initial phase of projects when requirements are changing.
		- Can be difficult for people coming from traditional way.
    
    I will choose OpenAPI first approach for new projects where requirements are sufficiently clear and when there are multiple teams or 
    stakeholders are invloved.

**Question 2: Testing Strategy**

Given time and resource constraints, how would you prioritize tests for this project?

Which types of tests (unit, integration, parameterized, concurrency) would you focus on, and how would you ensure effective coverage over time?
With Testing We have to make sure our code performs correctly

    When we have resource constraints, at a minimum we have to make sure if are meeting all business use cases correctly
    To achieve this
        - We can write sufficient number of unit test cases to cover like 70 to 80% code
        - Concentrate on Integration tests since we have to test the system as a whole to find
            - Concurrency bugs where many entities are interacing with same data.
            - Bugs involving maintaining state invariants across different components.
                Example: Even in our testing there are was a failing integration test due to concurrency issue 
                with event propogation and db transaction which threw an exception and got rolledback.

        - To speed up unit test writing we can write a parameterized test and fill the table with 
          different values for data and can cover all edge cases quickly.
        - We can also use tools like OpenAPI and generate code instead of writing them manually 
          to avoid errors even before they occur.
        - Finally write tests based on scenarios in requirement and don't try to cover all implementation details.
---

### Bonus Task: Warehouse Search & Filter API

**If you complete the main tasks with time to spare**, implement a search and filter endpoint.

**Endpoint**:
```
GET /warehouse/search
```

**Query Parameters**:
| Parameter | Type | Description |
|---|---|---|
| `location` | `string` | Filter by location identifier (e.g. `AMSTERDAM-001`) |
| `minCapacity` | `integer` | Filter warehouses with capacity ≥ this value |
| `maxCapacity` | `integer` | Filter warehouses with capacity ≤ this value |
| `sortBy` | `string` | Sort field: `createdAt` (default) or `capacity` |
| `sortOrder` | `string` | `asc` or `desc` (default: `asc`) |
| `page` | `integer` | Page number, 0-indexed (default: `0`) |
| `pageSize` | `integer` | Page size (default: `10`, max: `100`) |

**Requirements**:
1. All parameters are optional
2. Archived warehouses must be excluded
3. Multiple filters use AND logic
4. Add integration test(s)

---

## Going Beyond

If you finish early or want to show more of what you can do — this is your space.

There are no fixed requirements here. Think about what a production-grade version of this system would look like and bring whatever you think adds value. Some prompts to get you thinking:

- Are there edge cases or failure modes not covered by the existing tests?
- Is there anything in the architecture, API design, or error handling you would do differently?
- What observability, resilience, or operational concerns would you address in a real system?
- Is there anything else in the codebase that looks off to you?

There are no wrong answers — we're interested in how you think and what you prioritise.

---

## Deliverables

1. **All tests passing**
   - Full test suite passes consistently
   - Any bugs found in the codebase are fixed
   - Integration tests (IT-suffix classes) also pass

2. **Answers to questions** in [QUESTIONS.md](QUESTIONS.md)
   - Thoughtful analysis of API specification approaches
   - Well-reasoned testing strategy

3. **(Bonus) Search endpoint** with tests
   - Working implementation
   - Proper pagination and filtering
   - Integration tests

---

## Available Locations

These are the predefined locations available in the system:

| Identifier | Max Warehouses | Max Capacity |
|---|---|---|
| ZWOLLE-001 | 1 | 40 |
| ZWOLLE-002 | 2 | 50 |
| AMSTERDAM-001 | 5 | 100 |
| AMSTERDAM-002 | 3 | 75 |
| TILBURG-001 | 1 | 40 |
| HELMOND-001 | 1 | 45 |
| EINDHOVEN-001 | 2 | 70 |
| VETSBY-001 | 1 | 90 |

---

## Running the Code

```bash
# Compile and run tests
./mvnw clean test

# Run specific test class
./mvnw test -Dtest=ArchiveWarehouseUseCaseTest

# Run specific test method
./mvnw test -Dtest=ArchiveWarehouseUseCaseTest#testConcurrentArchiveAndStockUpdateCausesOptimisticLockException

# Start development mode
./mvnw quarkus:dev

# Access Swagger UI
open http://localhost:8080/q/swagger-ui
```
