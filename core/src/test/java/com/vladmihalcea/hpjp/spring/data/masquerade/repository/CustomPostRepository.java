package com.vladmihalcea.hpjp.spring.data.masquerade.repository;

import com.blazebit.persistence.PagedList;
import com.vladmihalcea.hpjp.spring.data.masquerade.dto.PostDTO;
import org.springframework.data.domain.Sort;

/**
 * @author Vlad Mihalcea
 */
public interface CustomPostRepository {

    PagedList<PostDTO> findTopN(Sort sortBy, int pageSize);

    PagedList<PostDTO> findNextN(Sort sortBy, PagedList<PostDTO> previousPage);
}
