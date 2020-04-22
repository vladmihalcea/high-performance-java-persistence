package com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.jpa.compact;

import com.vladmihalcea.book.hpjp.hibernate.forum.dto.PostDTO;
import com.vladmihalcea.hibernate.type.util.ClassImportIntegrator;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.boot.spi.IntegratorProvider;

import java.util.Collections;
import java.util.List;

/**
 * @author Vlad Mihalcea
 */
public class ClassImportIntegratorIntegratorProvider implements IntegratorProvider {

    @Override
    public List<Integrator> getIntegrators() {
        return Collections.singletonList(
            new ClassImportIntegrator(Collections.singletonList(PostDTO.class))
        );
    }
}
