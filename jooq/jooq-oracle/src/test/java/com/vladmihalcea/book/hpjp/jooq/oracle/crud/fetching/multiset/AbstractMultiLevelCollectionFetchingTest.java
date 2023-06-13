package com.vladmihalcea.book.hpjp.jooq.oracle.crud.fetching.multiset;

import com.vladmihalcea.book.hpjp.jooq.oracle.crud.AbstractJOOQOracleSQLIntegrationTest;
import com.vladmihalcea.book.hpjp.jooq.oracle.crud.fetching.multiset.domain.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Vlad Mihalcea
 */
public class AbstractMultiLevelCollectionFetchingTest extends AbstractJOOQOracleSQLIntegrationTest {

    public static final int POST_COUNT = 50;
    public static final int POST_COMMENT_COUNT = 20;
    public static final int TAG_COUNT = 10;
    public static final int VOTE_COUNT = 5;

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostComment.class,
            Tag.class,
            User.class,
            UserVote.class
        };
    }

    @Override
    protected String ddlScript() {
        return "clean_schema.sql";
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put("hibernate.jdbc.batch_size", "50");
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.order_updates", "true");
    }

    @Override
    public void afterInit() {
        doInJPA(entityManager -> {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            VoteType[] voteTypes = VoteType.values();
            int voteCount = voteTypes.length;

            User alice = new User()
                .setId(1L)
                .setFirstName("Alice")
                .setLastName("Smith");

            User bob = new User()
                .setId(2L)
                .setFirstName("Bob")
                .setLastName("Johnson");

            entityManager.persist(alice);
            entityManager.persist(bob);

            List<Tag> tags = new ArrayList<>();

            for (long i = 1; i <= TAG_COUNT; i++) {
                Tag tag = new Tag()
                    .setId(i)
                    .setName(String.format("Tag nr. %d", i));

                entityManager.persist(tag);
                tags.add(tag);
            }

            long commentId = 0;
            long voteId = 0;

            for (long postId = 1; postId <= POST_COUNT; postId++) {
                Post post = new Post()
                    .setId(postId)
                    .setTitle(String.format("Post nr. %d", postId));


                for (long i = 0; i < POST_COMMENT_COUNT; i++) {
                    PostComment comment = new PostComment()
                        .setId(++commentId)
                        .setReview(String.format("Comment nr. %d", commentId));

                    for (int j = 0; j < VOTE_COUNT; j++) {
                        comment.addVote(
                            new UserVote()
                                .setId(++voteId)
                                .setVoteType(voteTypes[random.nextInt(voteCount)])
                                .setUser(Math.random() > 0.5 ? alice : bob)
                        );
                    }

                    post.addComment(comment);

                }

                for (int i = 0; i < TAG_COUNT; i++) {
                    post.getTags().add(tags.get(i));
                }

                entityManager.persist(post);
            }
        });
    }

}
