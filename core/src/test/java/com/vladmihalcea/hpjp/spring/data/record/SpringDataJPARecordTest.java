package com.vladmihalcea.hpjp.spring.data.record;

import com.vladmihalcea.hpjp.spring.common.AbstractSpringTest;
import com.vladmihalcea.hpjp.spring.data.record.config.SpringDataJPARecordConfiguration;
import com.vladmihalcea.hpjp.spring.data.record.domain.Post;
import com.vladmihalcea.hpjp.spring.data.record.domain.PostComment;
import com.vladmihalcea.hpjp.spring.data.record.domain.PostCommentRecord;
import com.vladmihalcea.hpjp.spring.data.record.domain.PostRecord;
import com.vladmihalcea.hpjp.spring.data.record.service.ForumService;
import io.hypersistence.utils.hibernate.type.json.internal.JacksonUtil;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import java.util.stream.LongStream;

/**
 * @author Vlad Mihalcea
 */
@ContextConfiguration(classes = SpringDataJPARecordConfiguration.class)
public class SpringDataJPARecordTest extends AbstractSpringTest {

    public static final int POST_COMMENT_COUNT = 5;

    @Autowired
    private ForumService forumService;

    @Override
    protected Class<?>[] entities() {
        return new Class[]{
            PostComment.class,
            Post.class
        };
    }

    @Override
    public void afterInit() {
        PostRecord postRecord = new PostRecord(
            1L,
            "High-Performance Java Persistence",
            LongStream.rangeClosed(1, POST_COMMENT_COUNT).mapToObj(i ->
                new PostCommentRecord(
                    null,
                    String.format("Good review nr. %d", i)
                )
            ).toList()
        );

        forumService.insertPostRecord(postRecord);
    }

    @Test
    public void test() {
        PostRecord postRecord = forumService.findPostRecordById(1L);

        LOGGER.info("PostRecord to JSON: {}", JacksonUtil.toString(postRecord));

        String upatedPostRecordJSONSTring = """
            {
              "id": 1,
              "title": "High-Performance Java Persistence, 2nd edition",
              "comments": [
                {
                  "id": 1,
                  "review": "Best book on JPA and Hibernate!"
                },
                {
                  "id": 2,
                  "review": "A must-read for every Java developer!"
                }
              ]
            }
            """;

        forumService.mergePostRecord(
            JacksonUtil.fromString(upatedPostRecordJSONSTring, PostRecord.class)
        );
    }
}

