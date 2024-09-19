package com.vladmihalcea.hpjp.spring.data.recursive.service;

import com.vladmihalcea.hpjp.spring.data.recursive.domain.PostCommentDTO;
import com.vladmihalcea.hpjp.spring.data.recursive.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Vlad Mihalcea
 */
@Service
@Transactional(readOnly = true)
public class ForumService {

    @Autowired
    private PostRepository postRepository;

    public List<PostCommentDTO> findTopCommentHierarchiesByPostUsingJava(Long postId, int ranking) {
        List<PostCommentDTO> postComments = postRepository.findAllCommentDTOsByPost(postId);

        Map<Long, PostCommentDTO> postCommentMap = postComments
            .stream()
            .collect(Collectors.toMap(PostCommentDTO::getId, Function.identity()));

        List<PostCommentDTO> postCommentRoots = postComments
            .stream()
            .filter(pcs -> {
                boolean isRoot = pcs.getParentId() == null;
                if(!isRoot) {
                    postCommentMap.get(pcs.getParentId()).addChild(pcs);
                }
                return isRoot;
            })
            .sorted(
                Comparator.comparing(PostCommentDTO::getTotalScore).reversed()
            )
            .limit(ranking)
            .toList();

        return postCommentRoots;
    }

    public List<PostCommentDTO> findTopCommentHierarchiesByPostUsingSQL(Long postId, int ranking) {
        return postRepository.findTopCommentDTOsByPost(postId, ranking);
    }
}
