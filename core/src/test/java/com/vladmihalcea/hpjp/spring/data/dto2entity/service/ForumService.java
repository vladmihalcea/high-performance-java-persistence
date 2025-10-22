package com.vladmihalcea.hpjp.spring.data.dto2entity.service;

import com.vladmihalcea.hpjp.spring.data.dto2entity.domain.Post;
import com.vladmihalcea.hpjp.spring.data.dto2entity.domain.PostComment;
import com.vladmihalcea.hpjp.spring.data.dto2entity.domain.Tag;
import com.vladmihalcea.hpjp.spring.data.dto2entity.dto.PostDTO;
import com.vladmihalcea.hpjp.spring.data.dto2entity.dto.converter.PostDTOConverter;
import com.vladmihalcea.hpjp.spring.data.dto2entity.repository.PostCommentRepository;
import com.vladmihalcea.hpjp.spring.data.dto2entity.repository.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Vlad Mihalcea
 */
@Service
@Transactional(readOnly = true)
public class ForumService {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostCommentRepository postCommentRepository;

    public Post findWithDetailsAndCommentsAndTagsById(Long id) {
        Post post = postRepository.findByIdWithDetailsAndComments(id);
        LOGGER.debug("Post [{}] fetched with details and comments", post.getId());
        return postRepository.findByIdWithTags(id);
    }

    @Transactional(readOnly = true)
    public Post convertDTOToPost(PostDTO postDTO) {
        Post postFromDatabase = postRepository.findByIdWithDetailsAndComments(postDTO.getId());
        Post postFromDTO = PostDTOConverter.of(postDTO);

        postFromDatabase.setTitle(postDTO.getTitle());

        if(postFromDatabase.getDetails() != null) {
            postFromDatabase.getDetails().setCreatedBy(postFromDTO.getDetails().getCreatedBy());
            postFromDatabase.getDetails().setCreatedOn(postFromDTO.getDetails().getCreatedOn());
        } else {
            postFromDatabase.setDetails(postFromDTO.getDetails());
        }

        mergeComments(postFromDatabase, postFromDTO.getComments());
        mergeTags(postFromDatabase, postFromDTO.getTags());

        return postFromDatabase;
    }

    @Transactional
    public void mergePost(Post post) {
        postRepository.merge(post);
    }

    private void mergeComments(Post postFromDatabase, List<PostComment> commentsFromDTO) {
        List<PostComment> commentsFromDatabase = postFromDatabase.getComments();

        List<PostComment> removedComments = new ArrayList<>(commentsFromDatabase);
        removedComments.removeAll(commentsFromDTO);

        for(PostComment removedComment : removedComments) {
            postFromDatabase.removeComment(removedComment);
        }

        List<PostComment> newComments = new ArrayList<>(commentsFromDTO);
        newComments.removeAll(commentsFromDatabase);
        commentsFromDTO.removeAll(newComments);

        Map<Long, PostComment> postCommentByIdMap = commentsFromDatabase
            .stream()
            .collect(Collectors.toMap(PostComment::getId, Function.identity()));

        for(PostComment updatingComment : commentsFromDTO) {
            PostComment existingComment = postCommentByIdMap.get(updatingComment.getId());
            if(existingComment != null) {
                existingComment.setReview(updatingComment.getReview());
            }
        }

        for(PostComment newComment : newComments) {
            postFromDatabase.addComment(newComment);
        }
    }

    private void mergeTags(Post postFromDatabase, Set<Tag> tagsFromDTO) {
        Set<Tag> tagsFromDatabase = postFromDatabase.getTags();

        Set<Tag> removedTags = new HashSet<>(tagsFromDatabase);
        removedTags.removeAll(tagsFromDTO);

        for(Tag removedTag : removedTags) {
            postFromDatabase.removeTag(removedTag);
        }

        Set<Tag> newTags = new HashSet<>(tagsFromDTO);
        newTags.removeAll(tagsFromDatabase);
        tagsFromDTO.removeAll(newTags);

        Map<Long, Tag> tagByIdMap = tagsFromDatabase
            .stream()
            .collect(Collectors.toMap(Tag::getId, Function.identity()));

        for(Tag updatingTag : tagsFromDTO) {
            Tag existingTag = tagByIdMap.get(updatingTag.getId());
            if(existingTag != null) {
                existingTag.setName(updatingTag.getName());
            }
        }

        for(Tag newTag : newTags) {
            postFromDatabase.addTag(newTag);
        }
    }
}
