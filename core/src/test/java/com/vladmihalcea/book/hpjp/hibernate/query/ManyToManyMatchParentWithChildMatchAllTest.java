package com.vladmihalcea.book.hpjp.hibernate.query;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class ManyToManyMatchParentWithChildMatchAllTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
                Cluster.class,
                Tag.class,
                ClusterTag.class
        };
    }

    @Override
    protected void afterInit() {
        doInJPA(entityManager -> {
            Cluster cluster1 = new Cluster();
            cluster1.id = 1L;
            cluster1.name = "Cluster 1";

            entityManager.persist(cluster1);

            Cluster cluster2 = new Cluster();
            cluster2.id = 2L;
            cluster2.name = "Cluster 2";

            entityManager.persist(cluster2);

            Cluster cluster3 = new Cluster();
            cluster3.id = 3L;
            cluster3.name = "Cluster 3";

            entityManager.persist(cluster3);

            Tag tag1 = new Tag();
            tag1.id = 1L;
            tag1.name = "Spark";
            tag1.value = "2.2";

            entityManager.persist(tag1);

            Tag tag2 = new Tag();
            tag2.id = 2L;
            tag2.name = "Hadoop";
            tag2.value = "2.7";

            entityManager.persist(tag2);

            Tag tag3 = new Tag();
            tag3.id = 3L;
            tag3.name = "Spark";
            tag3.value = "2.3";

            entityManager.persist(tag3);

            Tag tag4 = new Tag();
            tag4.id = 4L;
            tag4.name = "Hadoop";
            tag4.value = "2.6";

            entityManager.persist(tag4);

            cluster1.addTag(tag1);
            cluster1.addTag(tag2);

            cluster2.addTag(tag1);
            cluster2.addTag(tag4);

            cluster3.addTag(tag3);
            cluster3.addTag(tag4);
        });
    }

    @Test
    public void testNativeQueryJoin() {
        doInJPA(entityManager -> {
            List<Cluster> clusters = entityManager.createNativeQuery(
                "select * " +
                "from cluster c " +
                "join (" +
                "   select ct.cluster_id as c_id, count(*)" +
                "   from cluster_tag ct " +
                "   join tag t on ct.tag_id = t.id " +
                "   where " +
                "       (t.tag_name = :tagName1 and t.tag_value = :tagValue1) or " +
                "       (t.tag_name = :tagName2 and t.tag_value = :tagValue2) " +
                "   group by ct.cluster_id " +
                "   having count(*) = 2" +
                ") ct1 on c.id = ct1.c_id ", Cluster.class)
            .setParameter("tagName1", "Spark")
            .setParameter("tagValue1", "2.2")
            .setParameter("tagName2", "Hadoop")
            .setParameter("tagValue2", "2.7")
            .getResultList();

            assertEquals(1, clusters.size());
        });
    }

    @Test
    public void testNativeQueryExists() {
        doInJPA(entityManager -> {
            List<Cluster> clusters = entityManager.createNativeQuery(
                "select * " +
                "from cluster c " +
                "where exists (" +
                "   select ct.cluster_id as c_id, count(*)" +
                "   from cluster_tag ct " +
                "   join tag t on ct.tag_id = t.id " +
                "   where " +
                "       c.id = ct.cluster_id and ( " +
                "           (t.tag_name = :tagName1 and t.tag_value = :tagValue1) or " +
                "           (t.tag_name = :tagName2 and t.tag_value = :tagValue2) " +
                "       )" +
                "   group by ct.cluster_id " +
                "   having count(*) = 2 " +
                ") ", Cluster.class)
            .setParameter("tagName1", "Spark")
            .setParameter("tagValue1", "2.2")
            .setParameter("tagName2", "Hadoop")
            .setParameter("tagValue2", "2.7")
            .getResultList();

            assertEquals(1, clusters.size());
        });
    }

    @Test
    public void testJPQLExists() {
        doInJPA(entityManager -> {
            List<Cluster> clusters = entityManager.createQuery(
                "select c " +
                "from Cluster c " +
                "where exists (" +
                "    select ctc.id " +
                "    from ClusterTag ct " +
                "    join ct.cluster ctc " +
                "    join ct.tag ctt " +
                "    where " +
                "        c.id = ctc.id and ( " +
                "            (ctt.name = :tagName1 and ctt.value = :tagValue1) or " +
                "            (ctt.name = :tagName2 and ctt.value = :tagValue2) " +
                "        )" +
                "    group by ctc.id " +
                "    having count(*) = 2" +
                ") ", Cluster.class)
            .setParameter("tagName1", "Spark")
            .setParameter("tagValue1", "2.2")
            .setParameter("tagName2", "Hadoop")
            .setParameter("tagValue2", "2.7")
            .getResultList();

            assertEquals(1, clusters.size());
        });
    }

    @Test
    public void testJPQLExistsImplicitJoin() {
        doInJPA(entityManager -> {
            List<Cluster> clusters = entityManager.createQuery(
                "select c " +
                "from Cluster c " +
                "where exists (" +
                "    select ct.cluster.id " +
                "    from ClusterTag ct " +
                "    join ct.tag ctt " +
                "    where " +
                "        c.id = ct.cluster.id and ( " +
                "            (ctt.name = :tagName1 and ctt.value = :tagValue1) or " +
                "            (ctt.name = :tagName2 and ctt.value = :tagValue2) " +
                "        )" +
                "    group by ct.cluster.id " +
                "    having count(*) = 2" +
                ") ", Cluster.class)
            .setParameter("tagName1", "Spark")
            .setParameter("tagValue1", "2.2")
            .setParameter("tagName2", "Hadoop")
            .setParameter("tagValue2", "2.7")
            .getResultList();

            assertEquals(1, clusters.size());
        });
    }

    @Entity(name = "Cluster")
    @Table(name = "cluster")
    public static class Cluster {

        @Id
        private Long id;

        private String name;

        @OneToMany(mappedBy = "cluster", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<ClusterTag> tags = new ArrayList<>();

        public Long getId() {
            return id;
        }

        public List<ClusterTag> getTags() {
            return tags;
        }

        public void addTag(Tag tag) {
            ClusterTag postTag = new ClusterTag(this, tag);
            tags.add(postTag);
        }

        public void removeTag(Tag tag) {
            for (Iterator<ClusterTag> iterator = tags.iterator(); iterator.hasNext(); ) {
                ClusterTag postTag = iterator.next();
                if (postTag.getCluster().equals(this) &&
                        postTag.getTag().equals(tag)) {
                    iterator.remove();
                    postTag.setCluster(null);
                    postTag.setTag(null);
                }
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Cluster post = (Cluster) o;
            return Objects.equals(name, post.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }

    @Embeddable
    public static class ClusterTagId implements Serializable {

        @Column(name = "cluster_id")
        private Long clusterId;

        @Column(name = "tag_id")
        private Long tagId;

        public ClusterTagId() {}

        public ClusterTagId(Long clusterId, Long tagId) {
            this.clusterId = clusterId;
            this.tagId = tagId;
        }

        public Long getClusterId() {
            return clusterId;
        }

        public Long getTagId() {
            return tagId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ClusterTagId that = (ClusterTagId) o;
            return Objects.equals(clusterId, that.clusterId) &&
                    Objects.equals(tagId, that.tagId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(clusterId, tagId);
        }
    }

    @Entity(name = "ClusterTag")
    @Table(name = "cluster_tag")
    public static class ClusterTag {

        @EmbeddedId
        private ClusterTagId id;

        @ManyToOne
        @MapsId("clusterId")
        private Cluster cluster;

        @ManyToOne
        @MapsId("tagId")
        private Tag tag;

        private ClusterTag() {}

        public ClusterTag(Cluster cluster, Tag tag) {
            this.cluster = cluster;
            this.tag = tag;
            this.id = new ClusterTagId(cluster.getId(), tag.getId());
        }

        public ClusterTagId getId() {
            return id;
        }

        public Cluster getCluster() {
            return cluster;
        }

        public void setCluster(Cluster cluster) {
            this.cluster = cluster;
        }

        public Tag getTag() {
            return tag;
        }

        public void setTag(Tag tag) {
            this.tag = tag;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ClusterTag that = (ClusterTag) o;
            return Objects.equals(cluster, that.cluster) &&
                    Objects.equals(tag, that.tag);
        }

        @Override
        public int hashCode() {
            return Objects.hash(cluster, tag);
        }
    }

    @Entity(name = "Tag")
    @Table(name = "tag")
    public static class Tag {

        @Id
        private Long id;

        @Column(name = "tag_name")
        private String name;

        @Column(name = "tag_value")
        private String value;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Tag tag = (Tag) o;
            return Objects.equals(name, tag.name) &&
                    Objects.equals(value, tag.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, value);
        }
    }
}
