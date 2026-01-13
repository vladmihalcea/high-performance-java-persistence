package com.vladmihalcea.hpjp.spring.data.dto2entity;

import com.vladmihalcea.hpjp.spring.common.AbstractSpringTest;
import com.vladmihalcea.hpjp.spring.data.dto2entity.config.SpringDataJPADTO2EntityConfiguration;
import com.vladmihalcea.hpjp.spring.data.dto2entity.domain.Post;
import com.vladmihalcea.hpjp.spring.data.dto2entity.domain.PostComment;
import com.vladmihalcea.hpjp.spring.data.dto2entity.domain.PostDetails;
import com.vladmihalcea.hpjp.spring.data.dto2entity.domain.Tag;
import com.vladmihalcea.hpjp.spring.data.dto2entity.dto.PostCommentDTO;
import com.vladmihalcea.hpjp.spring.data.dto2entity.dto.PostDTO;
import com.vladmihalcea.hpjp.spring.data.dto2entity.dto.TagDTO;
import com.vladmihalcea.hpjp.spring.data.dto2entity.dto.converter.PostDTOConverter;
import com.vladmihalcea.hpjp.spring.data.dto2entity.repository.PostCommentRepository;
import com.vladmihalcea.hpjp.spring.data.dto2entity.repository.PostRepository;
import com.vladmihalcea.hpjp.spring.data.dto2entity.service.ForumFacadeService;
import com.vladmihalcea.hpjp.spring.data.dto2entity.service.ForumService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDateTime;

/**
 * @author Vlad Mihalcea
 */
@ContextConfiguration(classes = SpringDataJPADTO2EntityConfiguration.class)
public class SpringDataJPADTO2EntityTest extends AbstractSpringTest {

    @Autowired
    private ForumService forumService;

    @Autowired
    private ForumFacadeService forumFacadeService;

    @Autowired
    private PostRepository postRepository;

    @Override
    protected Class<?>[] entities() {
        return new Class[] {
            PostComment.class,
            PostDetails.class,
            Post.class,
            Tag.class
        };
    }

    @Override
    public void afterInit() {
        postRepository.persist(
            new Post()
                .setId(1L)
                .setTitle("High-Performance Java Persistence")
                .setDetails(new PostDetails().setCreatedBy("Vlad Mihalcea"))
                .addComment(
                    new PostComment()
                        .setReview("Best book on JPA and Hibernate!")
                )
                .addComment(
                    new PostComment()
                        .setReview("A must-read for every Java developer!")
                )
                .addTag(new Tag().setName("JDBC"))
                .addTag(new Tag().setName("JPA"))
                .addTag(new Tag().setName("Hibernate"))
                .addTag(new Tag().setName("jOOQ"))
        );
    }

    @Test
    public void testDtoToEntitySync() {
        LOGGER.info("Sync DTOs and JPA entity state");

        Post post = forumService.findWithDetailsAndCommentsAndTagsById(1L);

        PostDTO postDTO = PostDTOConverter.of(post);

        postDTO.setTitle("High-Performance Java Persistence");
        postDTO.getDetails().setCreatedOn(LocalDateTime.now());
        postDTO.getComments().remove(0);
        postDTO.getComments().add(
            new PostCommentDTO()
                .setReview("A great reference book")
        );
        postDTO.getTags().remove(postDTO.getTags().stream().filter(t -> t.getName().equals("JPA")).findFirst().orElseThrow());
        postDTO.getTags().add(new TagDTO().setName("Jakarta Persistence"));

        forumFacadeService.updatePost(postDTO);
    }
}

