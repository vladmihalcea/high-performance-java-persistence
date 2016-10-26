package com.vladmihalcea.book.hpjp.hibernate.inheritance;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Vlad Mihalcea
 */
public class SortIndexCollectionInheritance extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Child1.class,
            Child2.class,
            CommonObject.class,
        };
    }

    @Test
    public void testScopeIdentity() {
        doInJPA(entityManager -> {
            Child1 child1 = new Child1();
            child1.id = 1L;
            child1.getCommonObjects().add(new CommonObject());
            child1.getCommonObjects().add(new CommonObject());
            child1.getCommonObjects().add(new CommonObject());

            Child2 child2 = new Child2();
            child2.id = 1L;
            child2.getCommonObjects().add(new CommonObject());
            child2.getCommonObjects().add(new CommonObject());

            entityManager.persist(child1);
            entityManager.persist(child2);
        });
    }

    @MappedSuperclass
    public static abstract class AbstractParent {
        public abstract List<CommonObject> getCommonObjects();
    }

    @Entity(name = "Child1")
    //@Table
    public static class Child1 extends AbstractParent {

        @Id
        private Long id;

        @OneToMany(targetEntity = CommonObject.class, cascade = CascadeType.ALL, orphanRemoval = true)
        @JoinTable(name = "child_1_common_objects", joinColumns = @JoinColumn(name="child1_id", nullable = false))
        @OrderColumn(name = "sort_index")
        private List<CommonObject> commonObjects = new ArrayList<>();

        public List<CommonObject> getCommonObjects() {
            return this.commonObjects;
        }
    }

    @Entity(name = "Child2")
    //@Table
    public static class Child2 extends AbstractParent {

        @Id
        private Long id;

        @OneToMany(targetEntity = CommonObject.class, cascade = CascadeType.ALL, orphanRemoval = true)
        @JoinTable(name = "child_2_common_objects", joinColumns = @JoinColumn(name="child2_id", nullable = false))
        @OrderColumn(name = "sort_index")
        private List<CommonObject> commonObjects = new ArrayList<>();

        public List<CommonObject> getCommonObjects() {
            return this.commonObjects;
        }
    }

    @Entity(name = "CommonObject")
    public static class CommonObject {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;
    }
}
