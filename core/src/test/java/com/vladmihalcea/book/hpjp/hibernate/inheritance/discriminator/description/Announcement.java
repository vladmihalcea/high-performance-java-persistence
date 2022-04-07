package com.vladmihalcea.book.hpjp.hibernate.inheritance.discriminator.description;

import java.util.Date;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 * @author Vlad Mihalcea
 */
@Entity
@Table(name = "announcement")
@DiscriminatorValue("2")
public class Announcement extends Topic {

	@Temporal(TemporalType.TIMESTAMP)
	private Date validUntil;

	public Date getValidUntil() {
		return validUntil;
	}

	public void setValidUntil(Date validUntil) {
		this.validUntil = validUntil;
	}
}
