package com.vladmihalcea.hpjp.spring.data.query.method.repository;

import com.vladmihalcea.hpjp.spring.data.query.method.domain.Post;
import com.vladmihalcea.hpjp.spring.data.query.method.domain.PostCommentDTO;
import io.hypersistence.utils.hibernate.query.DistinctListTransformer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.query.TupleTransformer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Vlad Mihalcea
 */
public class CustomPostCommentRepositoryImpl implements CustomPostCommentRepository {
    
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<PostCommentDTO> findCommentHierarchy(Post post) {
        return entityManager.createQuery("""
            select 
                pc.id as id,
                pc.post.id as postId,
                pc.parent.id as parentId,
                pc.review as review,
                pc.createdOn as createdOn,
                pc.votes as votes               
            from PostComment pc
            where
                pc.post = :post
            order by createdOn
            """)
        .setParameter("post", post)
        .unwrap(org.hibernate.query.Query.class)
        .setTupleTransformer(new PostCommentTupleTransformer())
        .setResultListTransformer(DistinctListTransformer.INSTANCE)
        .getResultList();
    }

    public static class PostCommentTupleTransformer implements TupleTransformer {

        private Map<Long, PostCommentDTO> commentDTOMap = new LinkedHashMap<>();

        @Override
        public PostCommentDTO transformTuple(Object[] tuple, String[] aliases) {
            Map<String, Integer> aliasToIndexMap = aliasToIndexMap(aliases);
            PostCommentDTO commentDTO = new PostCommentDTO(tuple, aliasToIndexMap);
            commentDTOMap.put(commentDTO.getId(), commentDTO);

            PostCommentDTO parent = commentDTOMap.get(commentDTO.getParentId());
            if (parent != null) {
                parent.addReply(commentDTO);
            }

            return commentDTO.root();
        }

        private Map<String, Integer> aliasToIndexMap(String[] aliases) {
            Map<String, Integer> aliasToIndexMap = new LinkedHashMap<>();
            for (int i = 0; i < aliases.length; i++) {
                aliasToIndexMap.put(aliases[i], i);
            }
            return aliasToIndexMap;
        }
    }
}
