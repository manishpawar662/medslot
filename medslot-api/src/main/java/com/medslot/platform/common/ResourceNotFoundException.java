// com/medslot/platform/common/ResourceNotFoundException.java
package com.medslot.platform.common;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}