package com.vladmihalcea.book.hpjp.hibernate.car;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "owner")
public class Owner {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "name")
    private String name;

    @OneToMany(mappedBy = "owner", cascade = {CascadeType.PERSIST})
    private List<OwnerCar> ownerCarList = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<OwnerCar> getOwnerCarList() {
        return ownerCarList;
    }

    public void setOwnerCarList(List<OwnerCar> ownerCarList) {
        this.ownerCarList = ownerCarList;
    }

    public void addOwnerCar(OwnerCar ownerCar) {
        getOwnerCarList().add(ownerCar);
        ownerCar.setOwner(this);
    }
}
