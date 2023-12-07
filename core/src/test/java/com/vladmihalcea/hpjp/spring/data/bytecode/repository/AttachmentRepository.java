package com.vladmihalcea.hpjp.spring.data.bytecode.repository;

import com.vladmihalcea.hpjp.hibernate.forum.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
}
