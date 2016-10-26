package com.vladmihalcea.book.hpjp.hibernate.identifier.access;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class EmbeddableCollectionAccessStrategyTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
                Patch.class,
                Change.class,
        };
    }

    @Test
    public void testSequenceIdentifierGenerator() {
        doInJPA(entityManager -> {
            Patch patch = new Patch();
            Change ch1 = new Change();
            ch1.setDiff("123");
            ch1.setPath("/a");
            Change ch2 = new Change();
            ch2.setDiff("456");
            ch2.setPath("/b");
            patch.changes.add(ch1);
            patch.changes.add(ch2);
            entityManager.persist(patch);
        });
        doInJPA(entityManager -> {
            Patch path = entityManager.find(Patch.class, 1L);
            assertEquals(2, path.changes.size());
        });
    }

    /**
     * Patch - Patch
     *
     * @author Vlad Mihalcea
     */
    @Entity(name = "Patch")
    public static class Patch {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;

        @ElementCollection
        @CollectionTable(
                name="patch_change",
                joinColumns=@JoinColumn(name="patch_id")
        )
        @OrderColumn(name = "index_id")
        private List<Change> changes = new ArrayList<>();

        public List<Change> getChanges() {
            return changes;
        }
    }

    @Embeddable
    @Access(AccessType.PROPERTY)
    public static class Change {

        private String path;

        private String diff;

        public Change() {}

        @Column(name = "path", nullable = false)
        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        @Column(name = "diff", nullable = false)
        public String getDiff() {
            return diff;
        }

        public void setDiff(String diff) {
            this.diff = diff;
        }
    }
}
