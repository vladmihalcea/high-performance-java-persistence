package com.vladmihalcea.hpjp.hibernate.multitenancy.partition.model;

import com.vladmihalcea.hpjp.hibernate.multitenancy.partition.PartitionContext;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.PartitionKey;

/**
 * @author Vlad Mihalcea
 */
@MappedSuperclass
@FilterDef(
    name = "partitionKey",
    parameters = @ParamDef(
        name = "partitionKey",
        type = String.class
    )
)
@Filter(
    name = "partitionKey",
    condition = "partition_key = :partitionKey"
)
public abstract class PartitionAware<T extends PartitionAware> {

    @Column(name = "partition_key")
    @PartitionKey
    private String partitionKey = PartitionContext.get();

    public String getPartitionKey() {
        return partitionKey;
    }

    public T setPartitionKey(String partitionKey) {
        this.partitionKey = partitionKey;
        return (T) this;
    }
}
