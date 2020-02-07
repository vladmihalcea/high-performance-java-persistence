package com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.jpa;

import com.vladmihalcea.book.hpjp.hibernate.forum.dto.PostDTO;
import com.vladmihalcea.hibernate.type.util.ClassImportIntegrator;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.boot.spi.IntegratorProvider;

import java.util.Arrays;
import java.util.List;

/**
 * @author Vlad Mihalcea
 */
public class ClassImportIntegratorIntegratorProvider implements IntegratorProvider {

    @Override
    public List<Integrator> getIntegrators() {
        return Arrays.asList(
            new ClassImportIntegrator(Arrays.asList(PostDTO.class))
        );
    }
}
