package com.smbtech.serviceframework.actuator.port.out;

import com.smbtech.serviceframework.actuator.domain.FrameworkModuleInfo;

/** Defines a provider of non-sensitive framework module information. */
public interface FrameworkModuleInfoProvider {

    /**
     * Returns the stable module name used for ordering.
     *
     * @return non-blank module name
     */
    String moduleName();

    /**
     * Returns immutable information about the module.
     *
     * @return module information
     */
    FrameworkModuleInfo provide();
}
