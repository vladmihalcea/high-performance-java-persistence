package com.vladmihalcea.book.hpjp.hibernate.car;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Objects;

@Entity
@Table(name = "owner_car")
public class OwnerCar {

    @EmbeddedId
    private OwnerCarId id = new OwnerCarId();

    @ManyToOne()
    @MapsId("ownerId")
    @JoinColumn(name = "owner_id",
        referencedColumnName = "id"
//        insertable = false,
//        updatable = false
    )
    private Owner owner;

    @NotNull
    @ManyToOne @MapsId("carId")
    @JoinColumn(name = "car_id",
        referencedColumnName = "id"
//        insertable = false,
//        updatable = false
    )
    private Car car;

    public OwnerCar() {
    }

    public OwnerCar(Long carId) {
        this.id.setCarId(carId);
    }

    public OwnerCarId getId() {
        return id;
    }

    public void setId(OwnerCarId id) {
        this.id = id;
    }

    public Owner getOwner() {
        return owner;
    }

    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    public Car getCar() {
        return car;
    }

    public void setCar(Car car) {
        this.car = car;
    }

    @Override
    public boolean equals(Object o) {
        boolean equal = false;

        if (this == o) {
            equal = true;
        } else if(o instanceof OwnerCar) {
            OwnerCar other = (OwnerCar) o;
            equal = Objects.equals(getId(), other.getId());
        }

        return equal;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
