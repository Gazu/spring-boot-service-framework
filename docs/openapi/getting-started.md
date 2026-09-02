# OpenAPI Getting Started

This guide takes one OpenAPI contract from source to locally published models,
server API, and HTTP client artifacts. It uses only plugin defaults and the
`warehouse-inventory-catalog:1.0.0` example exercised by this repository.

## Before You Start

You need Java 21, a Gradle 9 wrapper, and a Maven repository containing
framework version `0.5.2` and its Gradle plugin marker. Configure that repository
through your normal organization-level Gradle credentials; do not place
credentials in the project.

Create `settings.gradle`:

```groovy
pluginManagement {
    repositories {
        maven {
            url = uri(providers.gradleProperty('frameworkRepositoryUrl').get())
        }
        gradlePluginPortal()
    }
}

rootProject.name = 'warehouse-inventory-contract'
```

Declare only the repository URL in `gradle.properties`:

```properties
frameworkRepositoryUrl=https://artifactory.example.com/maven-releases
```

For development against a framework checkout, the value may be a `file:` URL
pointing to its published build repository.

## 1. Create The Contract

Create `src/main/openapi/warehouse-inventory-catalog.yaml`:

```yaml
openapi: 3.0.3
info:
  title: warehouse-inventory-catalog
  version: '1.0.0'
paths:
  /warehouses/{warehouseId}/items/{sku}:
    get:
      operationId: getWarehouseInventoryItem
      parameters:
        - name: warehouseId
          in: path
          required: true
          schema:
            type: string
        - name: sku
          in: path
          required: true
          schema:
            type: string
      responses:
        '200':
          description: Inventory item found
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/InventoryItemResponse'
components:
  schemas:
    InventoryItemResponse:
      type: object
      required:
        - sku
        - quantity
      properties:
        sku:
          type: string
        quantity:
          type: integer
          format: int64
          minimum: 0
```

Every operation needs a unique `operationId`. The title and version become the
default Maven artifact base name and version.

## 2. Apply And Configure The Plugin

Create `build.gradle`:

```groovy
plugins {
    id 'base'
    id 'com.smbtech.service-framework.openapi-generator' version '0.5.2'
}

repositories {
    maven {
        url = uri(providers.gradleProperty('frameworkRepositoryUrl').get())
    }
    mavenCentral()
}

smbtechOpenApi {
    specs {
        register('warehouseInventoryCatalog') {
            input.set(file('src/main/openapi/warehouse-inventory-catalog.yaml'))
        }
    }
}
```

The defaults generate all three artifact kinds under group
`com.smbtech.contracts`.

## 3. Validate The Contract

Run configuration and document validation before generation:

```bash
./gradlew smbtechOpenApiBuildLogicCheck smbtechOpenApiValidateSpecs
```

A successful run confirms the OpenAPI version, required metadata, operations,
operation IDs, effective names, versions, and coordinates.
The complete rule set is defined in [OpenAPI Validation](validation.md).

## 4. Generate The Artifacts

Compile and package every enabled artifact:

```bash
./gradlew smbtechOpenApiAssemble
```

The result under `build/libs/smbtech-openapi` contains binary and source JARs
for these coordinates:

```text
com.smbtech.contracts:warehouse-inventory-catalog-models:1.0.0
com.smbtech.contracts:warehouse-inventory-catalog-server-api:1.0.0
com.smbtech.contracts:warehouse-inventory-catalog-client:1.0.0
```

Each binary JAR embeds the original contract and deterministic metadata under
`META-INF/smbtech/openapi`.

## 5. Publish Locally

Publish the generated Maven modules:

```bash
./gradlew smbtechOpenApiPublishToLocalRepository
```

The repository is available at `build/repository/openapi`. Add it to a local
consumer before the framework repository:

```groovy
repositories {
    maven {
        url = uri('/path/to/warehouse-inventory-contract/build/repository/openapi')
    }
    maven {
        url = uri(providers.gradleProperty('frameworkRepositoryUrl').get())
    }
    mavenCentral()
}
```

For repository overrides, Maven consumers, remote registries, credentials, and
CI sequencing, use [OpenAPI Artifact Publishing](publishing.md).

## 6. Implement The Server API

Add the generated server contract to a Spring Boot application:

```groovy
dependencies {
    implementation 'com.smbtech.contracts:warehouse-inventory-catalog-server-api:1.0.0'
}
```

Implement its delegate as a Spring bean:

```java
package com.example.inventory;

import com.smbtech.contracts.warehouseinventorycatalog.api.DefaultApiDelegate;
import com.smbtech.contracts.warehouseinventorycatalog.model.InventoryItemResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
final class WarehouseInventoryDelegate implements DefaultApiDelegate {

    @Override
    public ResponseEntity<InventoryItemResponse> getWarehouseInventoryItem(
            String warehouseId, String sku) {
        return ResponseEntity.notFound().build();
    }
}
```

The generated controller discovers this bean and exposes the mapping declared
by the contract. Business and domain logic remain in the consuming application.

## 7. Consume The HTTP Client

Add the generated client contract to a consumer application:

```groovy
dependencies {
    implementation 'com.smbtech.contracts:warehouse-inventory-catalog-client:1.0.0'
}
```

The generated `DefaultApi` carries
`@HttpApiClient("warehouse-inventory-catalog")`. Configure the matching client:

```yaml
smbtech:
  rest-clients:
    clients:
      warehouse-inventory-catalog:
        base-url: http://localhost:8080
        authentication-type: NONE
```

Inject the generated interface normally:

```java
package com.example.inventory;

import com.smbtech.contracts.warehouseinventorycatalog.client.DefaultApi;
import com.smbtech.contracts.warehouseinventorycatalog.model.InventoryItemResponse;
import org.springframework.stereotype.Service;

@Service
final class WarehouseInventoryGateway {

    private final DefaultApi inventoryApi;

    WarehouseInventoryGateway(DefaultApi inventoryApi) {
        this.inventoryApi = inventoryApi;
    }

    InventoryItemResponse find(String warehouseId, String sku) {
        return inventoryApi.getWarehouseInventoryItem(warehouseId, sku).getBody();
    }
}
```

## 8. Run The Compatibility Gate

Before publishing or changing the contract, run:

```bash
./gradlew smbtechOpenApiCompatibilityCheck
```

This checks structural compatibility, reproducibility, generated artifact
separation, embedded metadata, consumer compilation, and mock compatibility.
The first version may run without a baseline. Before enforcing compatibility in
CI, commit the exact contract at
`src/main/openapi-baselines/warehouse-inventory-catalog/1.0.0.yaml` and set
`smbtechOpenApi.requireBaseline` to `true`.
Use [OpenAPI Contract Versioning](versioning.md) before evolving this first
contract.

## Expected Result

You now have one source contract, three versioned Maven artifacts, a delegate
boundary for the provider, an injectable HTTP interface for consumers, and a
repeatable compatibility gate.

Continue with the [OpenAPI Portal](index.md) for publication, versioning,
contract testing, mocks, scaffolding, and troubleshooting. The complete current
artifact contract is documented in
[OpenAPI Artifact Generation](generation.md); the complete current DSL and task
contract is defined in the
[OpenAPI Gradle Plugin Reference](plugin-reference.md).
