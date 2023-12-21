package com.vladmihalcea.hpjp.spring.blaze;

import com.blazebit.persistence.PagedList;
import com.vladmihalcea.hpjp.spring.blaze.domain.Post;
import com.vladmihalcea.hpjp.spring.blaze.repository.PostRepository;
import com.vladmihalcea.hpjp.spring.blaze.service.ForumService;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;

import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author Vlad Mihalcea
 */
public class SpringBlazePersistenceMockTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Test
    public void testWithMocks() {
        final int PAGE_SIZE = 25;

        PostRepository postRepository = Mockito.mock(PostRepository.class);
        ForumService forumService = new ForumService(postRepository);

        PagedList<Post> pagedList = Mockito.mock(PagedList.class);
        when(postRepository.findTopN(any(Sort.class), eq(PAGE_SIZE))).thenReturn(pagedList);

        PagedList<Post> topPage = forumService.firstLatestPosts(PAGE_SIZE);

        assertSame(pagedList, topPage);
    }
}

