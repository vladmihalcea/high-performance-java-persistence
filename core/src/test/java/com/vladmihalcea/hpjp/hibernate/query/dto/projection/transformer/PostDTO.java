package com.vladmihalcea.hpjp.hibernate.query.dto.projection.transformer;

import com.vladmihalcea.hpjp.util.AbstractTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Vlad Mihalcea
 */
public class PostDTO {

    public static final String ID_ALIAS = "p_id";
    public static final String TITLE_ALIAS = "p_title";

    private Long id;

    private String title;

    private List<PostCommentDTO> comments = new ArrayList<>();

    public PostDTO() {
    }

    public PostDTO(Long id, String title) {
        this.id = id;
        this.title = title;
    }

    public PostDTO(Object[] tuples, Map<String, Integer> aliasToIndexMap) {
        this.id = AbstractTest.longValue(tuples[aliasToIndexMap.get(ID_ALIAS)]);
        this.title = AbstractTest.stringValue(tuples[aliasToIndexMap.get(TITLE_ALIAS)]);
    }

    public Long getId() {
        return id;
    }

    public void setId(Number id) {
        this.id = id.longValue();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<PostCommentDTO> getComments() {
        return comments;
    }
}
