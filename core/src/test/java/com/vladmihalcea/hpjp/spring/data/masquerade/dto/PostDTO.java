package com.vladmihalcea.hpjp.spring.data.masquerade.dto;

import com.vladmihalcea.hpjp.util.CryptoUtils;

/**
 * @author Vlad Mihalcea
 */
public class PostDTO {

    private final String id;

    private final String title;

    public PostDTO(Long id, String title) {
        this.id = CryptoUtils.encrypt(id);
        this.title = title;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }
}
