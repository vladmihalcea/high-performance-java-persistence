package com.vladmihalcea.hpjp.hibernate.criteria;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.entity.BlogEntityProvider;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import java.util.List;

/**
 * @author Vlad Mihalcea
 */
public class TupleTest extends AbstractTest {

    private BlogEntityProvider entityProvider = new BlogEntityProvider();

    @Override
    protected Class<?>[] entities() {
        return entityProvider.entities();
    }

    @Override
    public void afterInit() {
        doInJPA(entityManager -> {
            entityManager.persist(new BlogEntityProvider.Post(1L));
            entityManager.persist(new BlogEntityProvider.Post(2L));
        });
    }

    @Test
    public void testTuple() {
        doInJPA(entityManager -> {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Tuple> cq = cb.createTupleQuery();

            Root<BlogEntityProvider.Post> postRoot = cq.from(BlogEntityProvider.Post.class);
            Path<Long> idPath = postRoot.get("id");
            Path<String> titlePath = postRoot.get("title");
            cq.multiselect(idPath, titlePath);

            List<Tuple> resultList = entityManager.createQuery(cq).getResultList();

            for (Tuple tuple : resultList) {
                Long id = tuple.get(idPath);
                String title = tuple.get(titlePath);
            }
        });
    }
}
