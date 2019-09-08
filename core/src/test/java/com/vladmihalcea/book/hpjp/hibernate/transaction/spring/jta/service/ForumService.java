package com.vladmihalcea.book.hpjp.hibernate.transaction.spring.jta.service;

import com.vladmihalcea.book.hpjp.hibernate.forum.dto.PostDTO;
import com.vladmihalcea.book.hpjp.hibernate.transaction.forum.Post;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@Service
public interface ForumService {

    Post newPost(String title, String... tags);

    List<Post> findAllByTitle(String title);

    Post findById(Long id);

    PostDTO getPostDTOById(Long id);}
