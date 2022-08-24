package com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.transformer;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.transform.ResultTransformer;

import java.util.*;

/**
 *
 * @author Vlad Mihalcea
 */
public class PostDTOResultTransformer implements ResultTransformer {

    private Map<Long, PostDTO> postDTOMap = new LinkedHashMap<>();

    @Override
    public PostDTO transformTuple(Object[] tuple, String[] aliases) {
        Map<String, Integer> aliasToIndexMap = aliasToIndexMap(aliases);
        Long postId = AbstractTest.longValue(tuple[aliasToIndexMap.get(PostDTO.ID_ALIAS)]);

        PostDTO postDTO = postDTOMap.computeIfAbsent(
            postId,
            id -> new PostDTO(tuple, aliasToIndexMap)
        );
        postDTO.getComments().add(new PostCommentDTO(tuple, aliasToIndexMap));

        return postDTO;
    }

    @Override
    public List<PostDTO> transformList(List collection) {
        return new ArrayList<>(postDTOMap.values());
    }

    private Map<String, Integer> aliasToIndexMap(String[] aliases) {
        Map<String, Integer> aliasToIndexMap = new LinkedHashMap<>();
        for (int i = 0; i < aliases.length; i++) {
            aliasToIndexMap.put(aliases[i].toLowerCase(Locale.ROOT), i);
        }
        return aliasToIndexMap;
    }
}
