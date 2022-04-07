package com.vladmihalcea.book.hpjp.hibernate.criteria;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.entity.BlogEntityProvider;
import org.junit.Test;

import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import java.util.List;

/**
 * EntityGraphMapperTest - Test mapping to entity
 *
 * @author Vlad Mihalcea
 */
public class TupleTest extends AbstractTest {

    private BlogEntityProvider entityProvider = new BlogEntityProvider();

    @Override
    protected Class<?>[] entities() {
        return entityProvider.entities();
    }

    @Override
    public void init() {
        super.init();
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
