package com.vladmihalcea.hpjp.spring.data.record;

import com.vladmihalcea.hpjp.spring.data.record.config.SpringDataJPARecordConfiguration;
import com.vladmihalcea.hpjp.spring.data.record.domain.PostCommentRecord;
import com.vladmihalcea.hpjp.spring.data.record.domain.PostRecord;
import com.vladmihalcea.hpjp.spring.data.record.service.ForumService;
import io.hypersistence.utils.hibernate.type.json.internal.JacksonUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.stream.LongStream;

/**
 * @author Vlad Mihalcea
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringDataJPARecordConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SpringDataJPARecordTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private ForumService forumService;

    public static final int POST_COMMENT_COUNT = 5;

    @Before
    public void init() {
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

