package com.vladmihalcea.hpjp.spring.transaction.routing;

import com.vladmihalcea.hpjp.hibernate.transaction.forum.Post;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@Service
public interface ForumService {

    Post newPost(String title, String... tags);

    List<Post> findAllPostsByTitle(String title);
}
