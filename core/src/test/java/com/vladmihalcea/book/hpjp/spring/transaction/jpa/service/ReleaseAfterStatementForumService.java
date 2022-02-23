package com.vladmihalcea.book.hpjp.spring.transaction.jpa.service;

import com.vladmihalcea.book.hpjp.hibernate.forum.dto.PostDTO;
import com.vladmihalcea.book.hpjp.hibernate.transaction.forum.Post;
import org.springframework.stereotype.Service;

/**
 * @author Vlad Mihalcea
 */
@Service
public interface ReleaseAfterStatementForumService {

    Post newPost(String title);

    PostDTO savePostTitle(Long id, String title);
}
