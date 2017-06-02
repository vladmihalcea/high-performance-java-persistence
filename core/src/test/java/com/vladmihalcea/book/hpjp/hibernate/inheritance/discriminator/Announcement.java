package com.vladmihalcea.book.hpjp.hibernate.inheritance.discriminator;

import java.util.Date;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

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
