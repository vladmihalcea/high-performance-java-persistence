package com.vladmihalcea.hpjp.hibernate.association;

import com.vladmihalcea.hpjp.hibernate.logging.validator.sql.SQLStatementCountValidator;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@EnhancementOptions(
    lazyLoading = true
)
@BytecodeEnhanced
public class BidirectionalOneToOneNPlusOneBytecodeEnhancementTest extends BidirectionalOneToOneNPlusOneTest {

    @Test
    public void testNoNPlusOne() {
        SQLStatementCountValidator.reset();

        List<Post> posts = doInJPA(entityManager -> {
            return entityManager.createQuery("""
                select p
                from Post p
                where p.title like 'Post nr.%'
                """, Post.class)
            .getResultList();
        });

        assertEquals(100, posts.size());
        SQLStatementCountValidator.assertSelectCount(1);
    }
}
