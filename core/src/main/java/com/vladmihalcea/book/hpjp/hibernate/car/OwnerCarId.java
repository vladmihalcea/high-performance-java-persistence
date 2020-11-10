package com.vladmihalcea.book.hpjp.hibernate.car;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class OwnerCarId implements Serializable {

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "car_id")
    private Long carId;

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public Long getCarId() {
        return carId;
    }

    public void setCarId(Long modelId) {
        this.carId = modelId;
    }

    @Override
    public boolean equals(Object o) {
        boolean equal = false;

        if (this == o) {
            equal = true;
        } else if(o instanceof OwnerCarId) {
            OwnerCarId other = (OwnerCarId) o;

            equal = Objects.equals(getOwnerId(), other.getOwnerId()) && Objects.equals(getCarId(), other.getCarId());
        }

        return equal;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getOwnerId(), getCarId());
    }
}
