package com.vladmihalcea.book.hpjp.hibernate.transaction.spring.jpa.service;

import com.vladmihalcea.book.hpjp.hibernate.forum.dto.PostDTO;
import com.vladmihalcea.book.hpjp.hibernate.transaction.forum.Post;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@Service
public interface ReleaseAfterStatementForumService {

    Post newPost(String title, String... tags);

    PostDTO savePostTitle(Long id, String title);
}
