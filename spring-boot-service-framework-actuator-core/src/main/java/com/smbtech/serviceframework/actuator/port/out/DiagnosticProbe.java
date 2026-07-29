package com.smbtech.serviceframework.actuator.port.out;

import com.smbtech.serviceframework.actuator.domain.ComponentHealth;

/**
 * Defines a framework-neutral component diagnostic probe.
 *
 * <p>Implementations should be passive unless the consuming application explicitly owns and enables
 * an active check.
 */
public interface DiagnosticProbe {

    /**
     * Returns the stable component name used for ordering and aggregation.
     *
     * @return non-blank component name
     */
    String componentName();

    /**
     * Evaluates the component and returns a bounded, non-sensitive result.
     *
     * @return component health result
     */
    ComponentHealth check();
}
