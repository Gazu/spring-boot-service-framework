package com.smbtech.serviceframework.actuator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.smbtech.serviceframework.actuator.domain.ComponentHealth;
import com.smbtech.serviceframework.actuator.domain.ComponentStatus;
import com.smbtech.serviceframework.actuator.domain.FrameworkDiagnosticsSnapshot;
import com.smbtech.serviceframework.actuator.domain.FrameworkModuleInfo;
import com.smbtech.serviceframework.actuator.port.in.FrameworkDiagnostics;
import com.smbtech.serviceframework.actuator.port.out.DiagnosticProbe;
import com.smbtech.serviceframework.actuator.port.out.FrameworkModuleInfoProvider;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ActuatorPublicApiCompatibilityTest {

    @Test
    void preservesNeutralStatusValues() {
        assertEquals(
                Set.of(
                        ComponentStatus.UP,
                        ComponentStatus.DOWN,
                        ComponentStatus.OUT_OF_SERVICE,
                        ComponentStatus.UNKNOWN),
                Set.copyOf(Arrays.asList(ComponentStatus.values())));
    }

    @Test
    void preservesRecordComponents() {
        assertEquals(
                List.of(
                        "name:java.lang.String",
                        "status:com.smbtech.serviceframework.actuator.domain.ComponentStatus",
                        "details:java.util.Map"),
                recordComponents(ComponentHealth.class));
        assertEquals(
                List.of("capturedAt:java.time.Instant", "components:java.util.Map"),
                recordComponents(FrameworkDiagnosticsSnapshot.class));
        assertEquals(
                List.of(
                        "name:java.lang.String",
                        "version:java.lang.String",
                        "attributes:java.util.Map"),
                recordComponents(FrameworkModuleInfo.class));
    }

    @Test
    void preservesExtensionPortMethods() {
        assertEquals(
                Set.of(
                        "from(java.util.Collection,java.util.Collection,java.time.Clock):com.smbtech.serviceframework.actuator.port.in.FrameworkDiagnostics",
                        "modules():java.util.List",
                        "snapshot():com.smbtech.serviceframework.actuator.domain.FrameworkDiagnosticsSnapshot"),
                Set.copyOf(declaredMethods(FrameworkDiagnostics.class)));
        assertEquals(
                Set.of(
                        "check():com.smbtech.serviceframework.actuator.domain.ComponentHealth",
                        "componentName():java.lang.String"),
                Set.copyOf(declaredMethods(DiagnosticProbe.class)));
        assertEquals(
                Set.of(
                        "moduleName():java.lang.String",
                        "provide():com.smbtech.serviceframework.actuator.domain.FrameworkModuleInfo"),
                Set.copyOf(declaredMethods(FrameworkModuleInfoProvider.class)));
    }

    @Test
    void preservesDomainConvenienceMethods() throws NoSuchMethodException {
        assertMethod(ComponentHealth.class, "up", ComponentHealth.class, String.class);
        assertMethod(ComponentHealth.class, "up", ComponentHealth.class, String.class, Map.class);
        assertMethod(ComponentHealth.class, "down", ComponentHealth.class, String.class);
        assertMethod(ComponentHealth.class, "down", ComponentHealth.class, String.class, Map.class);
        assertMethod(ComponentHealth.class, "outOfService", ComponentHealth.class, String.class);
        assertMethod(
                ComponentHealth.class,
                "outOfService",
                ComponentHealth.class,
                String.class,
                Map.class);
        assertMethod(ComponentHealth.class, "unknown", ComponentHealth.class, String.class);
        assertMethod(
                ComponentHealth.class, "unknown", ComponentHealth.class, String.class, Map.class);
        assertMethod(ComponentHealth.class, "isUp", boolean.class);
        assertMethod(
                ComponentStatus.class,
                "worst",
                ComponentStatus.class,
                ComponentStatus.class,
                ComponentStatus.class);
        assertMethod(FrameworkDiagnosticsSnapshot.class, "status", ComponentStatus.class);
        assertMethod(FrameworkDiagnosticsSnapshot.class, "component", Optional.class, String.class);
        assertMethod(FrameworkDiagnosticsSnapshot.class, "isEmpty", boolean.class);
        assertMethod(
                FrameworkModuleInfo.class,
                "of",
                FrameworkModuleInfo.class,
                String.class,
                String.class);

        new ComponentHealth("component", ComponentStatus.UP, Map.of());
        new FrameworkDiagnosticsSnapshot(Instant.EPOCH, Map.of());
        new FrameworkModuleInfo("module", "1.0.0", Map.of());
    }

    private static List<String> recordComponents(Class<?> type) {
        return Arrays.stream(type.getRecordComponents())
                .map(ActuatorPublicApiCompatibilityTest::recordComponent)
                .toList();
    }

    private static String recordComponent(RecordComponent component) {
        return component.getName() + ":" + component.getType().getName();
    }

    private static List<String> declaredMethods(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
                .filter(method -> !method.isSynthetic())
                .map(ActuatorPublicApiCompatibilityTest::methodSignature)
                .toList();
    }

    private static String methodSignature(Method method) {
        String parameters =
                Arrays.stream(method.getParameterTypes())
                        .map(Class::getName)
                        .reduce((left, right) -> left + "," + right)
                        .orElse("");
        return method.getName() + "(" + parameters + "):" + method.getReturnType().getName();
    }

    private static void assertMethod(
            Class<?> owner, String name, Class<?> returnType, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        assertEquals(returnType, owner.getDeclaredMethod(name, parameterTypes).getReturnType());
    }
}
