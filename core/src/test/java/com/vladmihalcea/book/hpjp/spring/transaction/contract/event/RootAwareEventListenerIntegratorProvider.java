package com.vladmihalcea.book.hpjp.spring.transaction.contract.event;

import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.boot.spi.IntegratorProvider;

import java.util.Collections;
import java.util.List;

public class RootAwareEventListenerIntegratorProvider implements IntegratorProvider {

    @Override
    public List<Integrator> getIntegrators() {
        return Collections.singletonList(RootAwareEventListenerIntegrator.INSTANCE);
    }
}

