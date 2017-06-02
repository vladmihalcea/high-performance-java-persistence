package com.vladmihalcea.book.hpjp.hibernate.inheritance.spring.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.transaction.Transactional;

import com.vladmihalcea.book.hpjp.hibernate.inheritance.spring.model.Notification;
import com.vladmihalcea.book.hpjp.hibernate.inheritance.spring.dao.NotificationDAO;
import com.vladmihalcea.book.hpjp.hibernate.inheritance.spring.service.sender.NotificationSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Vlad Mihalcea
 */
@Service
public class NotificationServiceImpl
	implements NotificationService {

	@Autowired
	private NotificationDAO notificationDAO;

	@Autowired
	private List<NotificationSender> notificationSenders;

	private Map<Class<? extends Notification>, NotificationSender>
		notificationSenderMap = new HashMap<>();

	@PostConstruct
	@SuppressWarnings( "unchecked" )
	public void init() {
		for ( NotificationSender notificationSender : notificationSenders ) {
			notificationSenderMap.put(
				notificationSender.appliesTo(),
				notificationSender
			);
		}
	}

	@Override
	@Transactional
	@SuppressWarnings( "unchecked" )
	public void sendCampaign(String name, String message) {
		List<Notification> notifications = notificationDAO.findAll();

		for ( Notification notification : notifications ) {
			notificationSenderMap
				.get( notification.getClass() )
				.send( notification );
		}
	}
}
