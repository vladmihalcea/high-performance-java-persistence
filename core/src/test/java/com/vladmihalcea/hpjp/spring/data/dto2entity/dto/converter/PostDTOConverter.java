package com.vladmihalcea.hpjp.spring.data.dto2entity.dto.converter;

import com.vladmihalcea.hpjp.spring.data.dto2entity.domain.Post;
import com.vladmihalcea.hpjp.spring.data.dto2entity.domain.PostComment;
import com.vladmihalcea.hpjp.spring.data.dto2entity.domain.PostDetails;
import com.vladmihalcea.hpjp.spring.data.dto2entity.domain.Tag;
import com.vladmihalcea.hpjp.spring.data.dto2entity.dto.PostCommentDTO;
import com.vladmihalcea.hpjp.spring.data.dto2entity.dto.PostDTO;
import com.vladmihalcea.hpjp.spring.data.dto2entity.dto.PostDetailsDTO;
import com.vladmihalcea.hpjp.spring.data.dto2entity.dto.TagDTO;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * @author Vlad Mihalcea
 */
public class PostDTOConverter {

    public static PostDTO of(Post post) {
        return new PostDTO()
            .setId(post.getId())
            .setTitle(post.getTitle())
            .setDetails(
                new PostDetailsDTO()
                    .setId(post.getDetails().getId())
                    .setCreatedOn(post.getDetails().getCreatedOn())
                    .setCreatedBy(post.getDetails().getCreatedBy())
            )
            .setComments(
                post.getComments().stream().map(
                    pc -> new PostCommentDTO()
                        .setId(pc.getId())
                        .setReview(pc.getReview())
                ).collect(Collectors.toCollection((ArrayList::new)))
            )
            .setTags(
                post.getTags().stream().map(
                    t -> new TagDTO()
                        .setId(t.getId())
                        .setName(t.getName())
                ).collect(Collectors.toCollection((HashSet::new)))
            );
    }

    public static Post of(PostDTO postDTO) {
        Post post = new Post()
            .setId(postDTO.getId())
            .setTitle(postDTO.getTitle())
            .setDetails(
                new PostDetails()
                    .setId(postDTO.getDetails().getId())
                    .setCreatedOn(postDTO.getDetails().getCreatedOn())
                    .setCreatedBy(postDTO.getDetails().getCreatedBy())
            );

        for(PostCommentDTO commentDTO : postDTO.getComments()) {
            post.addComment(
                new PostComment()
                    .setId(commentDTO.getId())
                    .setReview(commentDTO.getReview())
            );
        }

        for(TagDTO tagDTO : postDTO.getTags()) {
            post.addTag(
                new Tag()
                    .setId(tagDTO.getId())
                    .setName(tagDTO.getName())
            );
        }

        return post;
    }
}
