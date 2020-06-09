package com.vladmihalcea.book.hpjp.hibernate.association;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.NaturalId;
import org.junit.Test;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Set;

/**
 * @author Kyriakos Mandalas
 */
public class OneToManyJoinColumnNonPKTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Customer.class,
            Order.class,
        };
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
        	Customer customer = new Customer();
        	customer.setCode("978-9730228236");
        	customer.setMsisdn("+306972333666");
        	customer.setStatus("ACTIVE");
        	entityManager.persist(customer);

        	Order pendingOrder = new Order();
			pendingOrder.setOrderNumber(73647367);
			pendingOrder.setStatus("PENDING");
			pendingOrder.setCustomer(customer);
        	entityManager.persist(pendingOrder);

			Order completedOrder = new Order();
			completedOrder.setOrderNumber(84758478);
			completedOrder.setStatus("COMPLETED");
			completedOrder.setCustomer(customer);
			entityManager.persist(completedOrder);
        });
        doInJPA(entityManager -> {
            Customer customer = entityManager.createQuery(
                "select c " +
                "from Customer c " +
                "inner join fetch c.orders o " +
                "where c.msisdn = :msisdn", Customer.class)
            .setParameter( "msisdn", "+306972333666" )
            .getSingleResult();

            //assertEquals(
            //    "amazon.co.uk",
            //    publication.getPublisher()
            //);
			//
            //assertEquals(
            //    "High-Performance Java Persistence",
            //    publication.getBook().getTitle()
            //);
        });
    }

	@Entity(name = "Customer")
	@Table(name = "customer")
	public static class Customer implements Serializable {

		@Id
		@GeneratedValue
		private Long id;

		@Column(unique = true)
		private String msisdn;

		@Column(name = "status")
		private String status;

		@Column(name = "code", unique = true, nullable = false)
		@NaturalId
		private String code;

		@OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		private Set<Order> orders;

		public String getMsisdn() {
			return msisdn;
		}

		public void setMsisdn(String msisdn) {
			this.msisdn = msisdn;
		}

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}

		public Set<Order> getOrders() {
			return orders;
		}

		public void setOrders(Set<Order> orders) {
			this.orders = orders;
		}
	}

	@Entity(name = "Order")
	@Table(name = "orders")
	public static class Order {

		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "order_no", unique = true)
		private Integer orderNumber;

		@Column(name = "status")
		private String status;

		@ManyToOne(fetch = FetchType.LAZY, optional = false)
		@JoinColumn(name = "customer_code", referencedColumnName = "code", nullable = false)
		private Customer customer;

		public Integer getOrderNumber() {
			return orderNumber;
		}

		public void setOrderNumber(Integer orderNumber) {
			this.orderNumber = orderNumber;
		}

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}

		public Customer getCustomer() {
			return customer;
		}

		public void setCustomer(Customer customer) {
			this.customer = customer;
		}
	}

}
