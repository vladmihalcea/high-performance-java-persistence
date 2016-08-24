package com.vladmihalcea.book.hpjp.jooq.pgsql.crud;

import com.vladmihalcea.book.hpjp.jooq.pgsql.schema.crud.tables.records.PostCommentDetailsRecord;
import org.hibernate.Session;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.stream.Stream;

import static com.vladmihalcea.book.hpjp.jooq.pgsql.schema.crud.Tables.POST;
import static com.vladmihalcea.book.hpjp.jooq.pgsql.schema.crud.Tables.POST_COMMENT_DETAILS;

/**
 * @author Vlad Mihalcea
 */
public class StreamTest extends AbstractJOOQPostgreSQLIntegrationTest {

    @Override
    protected String ddlScript() {
        return "initial_schema.sql";
    }

    @Before
    public void init() {
        super.init();

        doInJOOQ(sql -> {
            sql
            .deleteFrom(POST)
            .execute();

            long id = 0L;

            sql
            .insertInto(
                POST_COMMENT_DETAILS).columns(
                POST_COMMENT_DETAILS.ID,
                POST_COMMENT_DETAILS.POST_ID,
                POST_COMMENT_DETAILS.USER_ID,
                POST_COMMENT_DETAILS.IP,
                POST_COMMENT_DETAILS.FINGERPRINT
            )
            .values(++id, 1L, 1L, "192.168.0.2", "ABC123")
            .values(++id, 1L, 2L, "192.168.0.3", "ABC456")
            .values(++id, 1L, 3L, "192.168.0.4", "ABC789")
            .values(++id, 2L, 1L, "192.168.0.2", "ABC123")
            .values(++id, 2L, 2L, "192.168.0.3", "ABC456")
            .values(++id, 2L, 4L, "192.168.0.3", "ABC456")
            .values(++id, 2L, 5L, "192.168.0.3", "ABC456")
            .execute();
        });
    }

    @Test
    public void testStream() {
        doInJOOQ(sql -> {

            Long lastProcessedId = 1L;

            try (Stream<PostCommentDetailsRecord> stream = sql
                .selectFrom(POST_COMMENT_DETAILS)
                .where(POST_COMMENT_DETAILS.ID.gt(lastProcessedId))
                .stream()) {
                processStream(stream);
            }
        });
    }

    private void processStream(Stream<PostCommentDetailsRecord> stream) {
        Map<Long, Map<IpFingerprint, List<Long>>> registryMap = new MaxSizeHashMap<>(25);

        stream.forEach(postCommentDetails -> {
            Long postId = postCommentDetails.get(POST_COMMENT_DETAILS.POST_ID);
            String ip = postCommentDetails.get(POST_COMMENT_DETAILS.IP);
            String fingerprint = postCommentDetails.get(POST_COMMENT_DETAILS.FINGERPRINT);
            Long userId = postCommentDetails.get(POST_COMMENT_DETAILS.USER_ID);

            Map<IpFingerprint, List<Long>> fingerprintsToPostMap = registryMap.get(postId);
            if(fingerprintsToPostMap == null) {
                fingerprintsToPostMap = new HashMap<>();
                registryMap.put(postId, fingerprintsToPostMap);
            }

            IpFingerprint ipFingerprint = new IpFingerprint(ip, fingerprint);

            List<Long> userIds = fingerprintsToPostMap.get(ipFingerprint);
            if(userIds == null) {
                userIds = new ArrayList<>();
                fingerprintsToPostMap.put(ipFingerprint, userIds);
            }

            if(!userIds.contains(userId)) {
                userIds.add(userId);
                if(userIds.size() > 1) {
                    notifyPossibleMultipleAccountFraud(postId, userIds);
                }
            }
        });
    }

    @Test
    public void testHibernateStream() {
        doInJPA(entityManager -> {
            Map<Long, Map<IpFingerprint, List<Long>>> registryMap = new MaxSizeHashMap<>(1000);
            Long lastProcessedId = 1L;

            Stream<Object[]> stream = entityManager.unwrap(Session.class).createNativeQuery(
                "select post_id, user_id, ip, fingerprint " +
                "from post_comment_details " +
                "where id > :id")
            .setParameter("id", lastProcessedId)
            .stream();

            stream.forEach(pcd -> {
                Long postId = ((Number) pcd[0]).longValue();
                Long userId = ((Number) pcd[1]).longValue();
                String ip = (String) pcd[2];
                String fingerprint = (String) pcd[3];

                Map<IpFingerprint, List<Long>> fingerprintsToIpMap = registryMap.get(postId);
                if(fingerprintsToIpMap == null) {
                    fingerprintsToIpMap = new HashMap<>();
                    registryMap.put(postId, fingerprintsToIpMap);
                }

                IpFingerprint ipFingerprint = new IpFingerprint(ip, fingerprint);

                List<Long> userIds = fingerprintsToIpMap.get(ipFingerprint);
                if(userIds == null) {
                    userIds = new ArrayList<>();
                    fingerprintsToIpMap.put(ipFingerprint, userIds);
                }
                if(!userIds.contains(userId)) {
                    userIds.add(userId);
                }
                if(userIds.size() > 1) {
                    notifyPossibleMultipleAccountFraud(postId, userIds);
                }
            });
        });
    }

    private void notifyPossibleMultipleAccountFraud(Long postId, List<Long> userIds) {
        LOGGER.info("Post id {} possible fraud with user ids {}", postId, userIds);
    }

    public static class IpFingerprint {
        private final String ip;
        private final String fingerprint;

        public IpFingerprint(String ip, String fingerprint) {
            this.ip = ip;
            this.fingerprint = fingerprint;
        }

        public String getIp() {
            return ip;
        }

        public String getFingerprint() {
            return fingerprint;
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            IpFingerprint that = (IpFingerprint) o;
            return Objects.equals(ip, that.ip) &&
                    Objects.equals(fingerprint, that.fingerprint);
        }

        @Override public int hashCode() {
            return Objects.hash(ip, fingerprint);
        }
    }

    public class MaxSizeHashMap<K, V> extends LinkedHashMap<K, V> {
        private final int maxSize;

        public MaxSizeHashMap(int maxSize) {
            this.maxSize = maxSize;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > maxSize;
        }
    }
}
