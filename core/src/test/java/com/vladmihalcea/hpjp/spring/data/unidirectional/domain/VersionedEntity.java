package com.vladmihalcea.hpjp.spring.data.unidirectional.domain;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

/**
 * @author Vlad Mihalcea
 */
@MappedSuperclass
public class VersionedEntity {

    @Version
    private Short version;

    public Short getVersion() {
        return version;
    }

    public void setVersion(Short version) {
        this.version = version;
    }
}
