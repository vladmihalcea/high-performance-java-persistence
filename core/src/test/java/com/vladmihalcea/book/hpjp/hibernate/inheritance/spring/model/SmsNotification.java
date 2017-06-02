package com.vladmihalcea.book.hpjp.hibernate.inheritance.spring.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author Vlad Mihalcea
 */
@Entity
@Table(name = "sms_notification")
public class SmsNotification extends Notification {

	@Column(name = "phone_number", nullable = false)
	private String phoneNumber;

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}
}
