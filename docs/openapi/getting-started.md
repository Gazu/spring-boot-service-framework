# OpenAPI Getting Started

This guide takes one OpenAPI contract from source to locally published models,
server API, and dual HTTP client artifacts. It uses only plugin defaults and the
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
```

Because the contract is under `src/main/openapi`, the plugin discovers it
without a `specs` block. The defaults generate all three artifact kinds under
group `com.smbtech.contracts`.

Explicit registration is only needed for a custom path or overrides:

```groovy
smbtechOpenApi {
    specs {
        register('warehouseInventoryCatalog') {
            input.set(file('src/main/openapi/warehouse-inventory-catalog.yaml'))
            groupId.set('com.example.contracts')
        }
    }
}
```

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
com.smbtech.contracts:warehouse-inventory-catalog-jdk21-model:1.0.0
com.smbtech.contracts:warehouse-inventory-catalog-jdk21-api:1.0.0
com.smbtech.contracts:warehouse-inventory-catalog-jdk21-client:1.0.0
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
    implementation 'com.smbtech.contracts:warehouse-inventory-catalog-jdk21-api:1.0.0'
}
```

Implement its delegate as a Spring bean:

```java
package com.example.inventory;

import com.smbtech.contracts.warehouseinventorycatalog.v1.api.DefaultApiDelegate;
import com.smbtech.contracts.warehouseinventorycatalog.v1.model.InventoryItemResponse;
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

## 7. Consume The Client Artifact

Add the generated client contract to a consumer application:

```groovy
dependencies {
    implementation 'com.smbtech.contracts:warehouse-inventory-catalog-jdk21-client:1.0.0'
}
```

The artifact contains matching interfaces for Spring HTTP Interface and Spring
Cloud OpenFeign. Choose the package that matches the consuming application's
HTTP stack.

### Spring HTTP Interface

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

import com.smbtech.contracts.warehouseinventorycatalog.v1.client.httpinterface.DefaultApi;
import com.smbtech.contracts.warehouseinventorycatalog.v1.model.InventoryItemResponse;
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

### Spring Cloud OpenFeign

The client artifact deliberately does not expose OpenFeign transitively. Add
the compatible Spring Cloud train and starter in the consuming application:

```groovy
dependencies {
    implementation platform('org.springframework.cloud:spring-cloud-dependencies:2025.1.2')
    implementation 'org.springframework.cloud:spring-cloud-starter-openfeign'
    implementation 'com.smbtech.contracts:warehouse-inventory-catalog-jdk21-client:1.0.0'
}
```

Enable scanning for the generated OpenFeign package:

```java
package com.example.inventory;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(
        basePackages =
                "com.smbtech.contracts.warehouseinventorycatalog.v1.client.openfeign")
class InventoryApplication {}
```

The OpenFeign `DefaultApi` has the same operations and model types as the HTTP
Interface variant and carries a direct `@FeignClient` annotation. Configure its
URL under the generated client name:

```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          warehouse-inventory-catalog:
            url: http://localhost:8080
```

Application code can then inject
`com.smbtech.contracts.warehouseinventorycatalog.v1.client.openfeign.DefaultApi`.
No generated endpoint URL or OpenFeign runtime is stored in the client JAR.

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
boundary for the provider, matching HTTP Interface and OpenFeign contracts for
consumers, and a repeatable compatibility gate.

Continue with the [OpenAPI Portal](index.md) for publication, versioning,
contract testing, mocks, scaffolding, and troubleshooting. The complete current
artifact contract is documented in
[OpenAPI Artifact Generation](generation.md); the complete current DSL and task
contract is defined in the
[OpenAPI Gradle Plugin Reference](plugin-reference.md).
