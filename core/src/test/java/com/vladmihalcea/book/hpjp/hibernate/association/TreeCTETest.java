package com.vladmihalcea.book.hpjp.hibernate.association;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.SQLQuery;
import org.hibernate.transform.ResultTransformer;
import org.junit.Test;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
public class TreeCTETest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Node.class
        };
    }

    @Override
    public void init() {
        super.init();
        doInJPA(entityManager -> {
            Node root = new Node("x");
            Node child1 = new Node("x");
            Node child2 = new Node("x");

            Node child11 = new Node("x");
            Node child12 = new Node("x");

            Node child21 = new Node("x");
            Node child22 = new Node("y");

            root.addChild(child1);
            root.addChild(child2);

            child1.addChild(child11);
            child1.addChild(child12);

            child2.addChild(child21);
            child2.addChild(child22);

            entityManager.persist(root);
            entityManager.persist(child1);
            entityManager.persist(child2);
            entityManager.persist(child11);
            entityManager.persist(child12);
            entityManager.persist(child21);
            entityManager.persist(child22);
        });
    }

    @Test
    public void test() {

        Node root = (Node) doInJPA(entityManager -> {
            return entityManager.createNativeQuery(
                    "WITH RECURSIVE node_tree(id, parent_id, val) AS ( " +
                    "    select n.id, n.parent_id, n.val " +
                    "    from node n " +
                    "    where n.val = :val and parent_id is null     " +
                    "    UNION ALL " +
                    "    select n.id, n.parent_id, n.val " +
                    "    from node n " +
                    "    inner join node_tree nt on nt.id = n.parent_id " +
                    "    where n.val = :val " +
                    ") " +
                    "SELECT id, parent_id, val " +
                    "FROM node_tree ")
                .setParameter("val", "x")
                .unwrap(SQLQuery.class)
                .addEntity(Node.class)
                .setResultTransformer(new ResultTransformer() {
                    @Override
                    public Object transformTuple(Object[] tuple, String[] aliases) {
                        Node node = (Node) tuple[0];
                        if(node.parent != null) {
                            node.parent.addChild(node);
                        }
                        return node;
                    }

                    @Override
                    public List transformList(List collection) {
                        return Collections.singletonList(collection.get(0));
                    }
                })
                .uniqueResult();
        });
        assertNotNull(root);
    }

    @Test
    public void testRecursion() {
        Node node = doInJPA(entityManager -> {
            Node root = entityManager.createQuery("select n from Node n where n.parent is null", Node.class).getSingleResult();
            fetchChildren(root);
            return root;
        });
        fetchChildren(node);
    }

    public void fetchChildren(Node node) {
        for (Node _node : node.getChildren()) {
            fetchChildren(_node);
        }
    }

    @Entity(name = "Node")
    public static class Node  {

        @Id
        @GeneratedValue
        private Long id;

        @ManyToOne
        @JoinColumn(name = "parent_id")
        private Node parent;

        private String val;

        @Transient
        private List<Node> children = new ArrayList<>();

        public Node() {}

        public Node(String value) {
            this.val = value;
        }

        public Node getParent() {
            return parent;
        }

        public List<Node> getChildren() {
            return children;
        }

        public void addChild(Node child) {
            children.add(child);
            child.parent = this;
        }
    }
}
