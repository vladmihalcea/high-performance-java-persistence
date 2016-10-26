package com.vladmihalcea.book.hpjp.hibernate.transaction.spring.jta.dao;

import com.vladmihalcea.book.hpjp.hibernate.transaction.forum.Tag;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@Repository
public class TagDAOImpl extends GenericDAOImpl<Tag, Long> implements TagDAO {

    protected TagDAOImpl() {
        super(Tag.class);
    }

    @Override
    public List<Tag> findByName(String... tags) {
        if(tags.length == 0) {
            throw new IllegalArgumentException("There's no tag name to search for!");
        }
        return getEntityManager().createQuery(
                "select t " +
                        "from Tag t " +
                        "where t.name in :tags", Tag.class)
                .setParameter("tags", Arrays.asList(tags))
                .getResultList();
    }
}
