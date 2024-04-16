package com.vladmihalcea.hpjp.hibernate.association;

import com.vladmihalcea.hpjp.hibernate.logging.validator.sql.SQLStatementCountValidator;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RunWith(BytecodeEnhancerRunner.class)
@EnhancementOptions(
    lazyLoading = true
)
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
