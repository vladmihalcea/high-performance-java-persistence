package com.vladmihalcea.book.hpjp.hibernate.query;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.Session;
import org.junit.Test;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class UpdateSubQueryTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            NccFailure.class
        };
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            NccFailure failure1 = new NccFailure();
            failure1.summary = "summary1";
            failure1.fileLinesData = "data1";
            failure1.subnetIds.add("id1");
            failure1.subnetIds.add("id2");
            failure1.subnetIds.add("id3");
            failure1.subnetIds.add("id4");
            failure1.subnetIds.add("id5");

            NccFailure failure2 = new NccFailure();
            failure1.summary = "summary2";
            failure1.fileLinesData = "data2";
            failure1.subnetIds.add("id2");
            failure1.subnetIds.add("id3");
            failure1.subnetIds.add("id4");

            entityManager.persist(failure1);
            entityManager.persist(failure2);
        });

        doInJPA(entityManager -> {
            Session session = entityManager.unwrap(Session.class);
            int count = session.createQuery(
                "update NccFailure f set f.summary=:summary " +
                "where f.id in (" +
                "   select nf.id " +
                "   from NccFailure nf " +
                "where 'id1' in elements(nf.subnetIds) ) ")
                .setParameter("summary", "summary")
                .executeUpdate();
            assertEquals(1, count);

            NccFailure failure = (NccFailure) session.createQuery(
                "select o " +
                        "from NccFailure o " +
                        "where o.id = ( select max(id) from NccFailure where summary = :summary ) ")
                .setParameter("summary", "summary")
                .uniqueResult();
            assertEquals("data2", failure.fileLinesData);

        });
    }

    @Entity(name = "NccFailure")
    @Table(name = "ncc_failure")
    public static class NccFailure
    {
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        private long id;


        @ElementCollection( fetch = FetchType.EAGER)
        @JoinTable(
            name = "ncc_failure_subnet_link",
            joinColumns = @JoinColumn(name = "ncc_failure__id")
        )
        @Column( name="ip_subnet_id", nullable = false)
        private final Set<String> subnetIds = new HashSet<>();
        @Lob
        @Column(name = "summary")
        private String summary;

        @Lob
        @Column(name = "file_lines_data")
        private String  fileLinesData;
    }
}
