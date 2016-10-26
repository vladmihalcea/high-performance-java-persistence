package com.vladmihalcea.book.hpjp.hibernate.transaction.spring.jpa.dao;

import com.vladmihalcea.book.hpjp.hibernate.transaction.forum.Tag;
import org.hibernate.Session;
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
        Session session =  getEntityManager().unwrap(Session.class);

        return getEntityManager().createQuery(
                "select t " +
                        "from Tag t " +
                        "where t.name in :tags", Tag.class)
                .setParameter("tags", Arrays.asList(tags))
                .getResultList();
    }
}
