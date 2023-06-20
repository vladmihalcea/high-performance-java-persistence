package com.vladmihalcea.hpjp.spring.transaction.jpa.service;

import com.vladmihalcea.hpjp.hibernate.forum.dto.PostDTO;
import com.vladmihalcea.hpjp.hibernate.transaction.forum.Post;
import org.springframework.stereotype.Service;

/**
 * @author Vlad Mihalcea
 */
@Service
public interface ReleaseAfterStatementForumService {

    Post newPost(String title);

    PostDTO savePostTitle(Long id, String title);
}
