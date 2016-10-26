package com.vladmihalcea.book.hpjp.hibernate.transaction.spring.jpa.service;

import com.vladmihalcea.book.hpjp.hibernate.transaction.forum.Post;
import org.springframework.stereotype.Service;

/**
 * @author Vlad Mihalcea
 */
@Service
public interface ForumService {

    Post newPost(String title, String... tags);
}
