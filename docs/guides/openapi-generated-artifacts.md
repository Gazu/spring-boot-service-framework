# Generate OpenAPI Contract Artifacts

Use this guide when a service team has an OpenAPI document and needs generated
models, server API contracts, and REST client interfaces as versioned Maven
artifacts.

## 1. Add The Spec

Place the OpenAPI file under one of the scanned locations:

```text
docs/openapi/
src/main/openapi/
openapi/
swagger/
```

The spec must define `info.title` and `info.version`:

```yaml
openapi: 3.0.3
info:
  title: retail-loyalty-rewards
  version: '1.0.0'
```

`info.title` becomes the artifact base name and `info.version` becomes the
artifact version.

## 2. Register The Version

After adding or intentionally changing a spec version, update the committed
catalog:

```bash
./gradlew generateOpenApiSpecVersionCatalog
```

This updates:

```text
docs/openapi/spec-versions.properties
```

If the spec content changes without a new `info.version`, validation fails.

## 3. Generate Artifacts

Generate all OpenAPI JARs:

```bash
./gradlew openApiModelsJar openApiServerApiJar openApiClientJar
```

For `retail-loyalty-rewards:1.0.0`, the generated coordinates are:

```text
com.smbtech.openapi:retail-loyalty-rewards-models:1.0.0
com.smbtech.openapi:retail-loyalty-rewards-api:1.0.0
com.smbtech.openapi:retail-loyalty-rewards-client:1.0.0
```

Generated JARs are written under:

```text
build/libs/openapi/models/
build/libs/openapi/api/
build/libs/openapi/client/
```

## 4. Validate

Run the full OpenAPI validation set:

```bash
./gradlew openApiCompatibilityCheck
```

This validates:

- public OpenAPI Gradle task names;
- spec naming and the committed spec version catalog;
- generated metadata;
- advanced model generation for refs, enums, arrays, maps, and validation
  annotations;
- generated `models`, `api`, and `client` JAR contents;
- artifact separation across `models`, `api`, and `client`;
- reproducible source and JAR generation;
- consumer-style compilation against the generated artifacts;
- local Maven publication layout and POM dependencies;
- reusable generator module compatibility;
- OpenAPI Gradle build-logic compatibility.

For local development, this is the main command to run before publishing or
committing OpenAPI generator changes.

## 5. Publish Locally

Publish generated OpenAPI artifacts to the root local build repository:

```bash
./gradlew publishOpenApiArtifactsToLocalBuildRepository
```

The repository path is:

```text
build/repository/openapi
```

Consumer builds can use it during local development:

```groovy
repositories {
    maven {
        url = uri('../spring-boot-service-framework/build/repository/openapi')
    }
    mavenCentral()
}

dependencies {
    implementation 'com.smbtech.openapi:retail-loyalty-rewards-client:1.0.0'
}
```

## 6. Implement The Server API

A service exposing the generated API implements the delegate from the API JAR:

```java
import com.smbtech.openapi.retailloyaltyrewards.api.RetailLoyaltyRewardsApiDelegate;
import com.smbtech.openapi.retailloyaltyrewards.model.RewardsSummaryResponse;
import com.smbtech.openapi.retailloyaltyrewards.model.VoucherResponse;
import org.springframework.stereotype.Component;

@Component
class RetailLoyaltyRewardsHandler implements RetailLoyaltyRewardsApiDelegate {

    @Override
    public RewardsSummaryResponse getMemberRewardsSummary(String memberId) {
        return new RewardsSummaryResponse();
    }

    @Override
    public VoucherResponse getVoucher(String memberId, String voucherId) {
        return new VoucherResponse();
    }
}
```

The generated controller delegates to this bean.

## 7. Consume The Client

A client service can inject or create the generated interface through the REST
client starter API:

```java
import com.smbtech.openapi.retailloyaltyrewards.client.RetailLoyaltyRewardsClient;
import com.smbtech.openapi.retailloyaltyrewards.model.RewardsSummaryResponse;
import org.springframework.stereotype.Service;

@Service
class LoyaltyRewardsService {

    private final RetailLoyaltyRewardsClient client;

    LoyaltyRewardsService(RetailLoyaltyRewardsClient client) {
        this.client = client;
    }

    RewardsSummaryResponse summary(String memberId) {
        return client.getMemberRewardsSummary(memberId);
    }
}
```

The generated client uses `@HttpApiClient("retail-loyalty-rewards")`, so the
consumer application must configure a matching REST client name.

Minimal client configuration:

```yaml
smbtech:
  rest-clients:
    clients:
      retail-loyalty-rewards:
        base-url: https://loyalty.example
        authentication-type: NONE
```

## References

- [OpenAPI Code Generation](../openapi-codegen.md)
- [REST Client Starter Guide](../rest-client.md)
- [Dependency and Local Publication](../rest-client/setup.md)
- [Troubleshooting](../troubleshooting.md)
