package com.vladmihalcea.book.hpjp.hibernate.inheritance.spring.dao;

import com.vladmihalcea.book.hpjp.hibernate.inheritance.spring.model.Notification;
import org.springframework.stereotype.Repository;

/**
 * @author Vlad Mihalcea
 */
@Repository
public class NotificationDAOImpl extends GenericDAOImpl<Notification, Long> implements NotificationDAO {

	protected NotificationDAOImpl() {
		super( Notification.class );
	}
}
