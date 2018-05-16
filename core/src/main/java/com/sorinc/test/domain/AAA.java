package com.sorinc.test.domain;


public abstract class AAA {

    private final Guid aaaIdentifier ;

    protected AAA(Guid aaaIdentifier) {
        this.aaaIdentifier = aaaIdentifier;
    }

    public Guid getIdentifier(){
        return aaaIdentifier ;
    }
}
