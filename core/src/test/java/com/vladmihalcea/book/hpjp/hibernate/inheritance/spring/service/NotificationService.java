package com.vladmihalcea.book.hpjp.hibernate.inheritance.spring.service;

/**
 * @author Vlad Mihalcea
 */
public interface NotificationService {

	void sendCampaign(String name, String message);
}
