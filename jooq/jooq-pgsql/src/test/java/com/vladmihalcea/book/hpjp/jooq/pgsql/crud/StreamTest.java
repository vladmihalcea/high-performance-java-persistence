package com.vladmihalcea.book.hpjp.jooq.pgsql.crud;

import com.vladmihalcea.book.hpjp.jooq.pgsql.schema.crud.tables.records.PostCommentDetailsRecord;
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

    @Test
    public void testStream() {
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

            Long lastProcessedId = 1L;

            Map<Long, Map<IpFingerprint, List<Long>>> registryMap = new MaxSizeHashMap<>(1000);

            try (Stream<PostCommentDetailsRecord> stream = sql
                .selectFrom(POST_COMMENT_DETAILS)
                .where(POST_COMMENT_DETAILS.ID.gt(lastProcessedId))
                .stream()) {
                stream.forEach(pcd -> {
                    Long postId = pcd.get(POST_COMMENT_DETAILS.POST_ID);
                    Map<IpFingerprint, List<Long>> fingerprintsToIpMap = registryMap.get(postId);
                    if(fingerprintsToIpMap == null) {
                        fingerprintsToIpMap = new HashMap<>();
                        registryMap.put(postId, fingerprintsToIpMap);
                    }

                    String ip = pcd.get(POST_COMMENT_DETAILS.IP);
                    String fingerprint = pcd.get(POST_COMMENT_DETAILS.FINGERPRINT);
                    IpFingerprint ipFingerprint = new IpFingerprint(ip, fingerprint);

                    List<Long> userIds = fingerprintsToIpMap.get(ipFingerprint);
                    if(userIds == null) {
                        userIds = new ArrayList<>();
                        fingerprintsToIpMap.put(ipFingerprint, userIds);
                    }
                    Long userId = pcd.get(POST_COMMENT_DETAILS.USER_ID);
                    if(!userIds.contains(userId)) {
                        userIds.add(userId);
                    }
                    if(userIds.size() > 1) {
                        notifyPossibleFraud(postId, userIds);
                    }
                });
            }
        });
    }

    private void notifyPossibleFraud(Long postId, List<Long> userIds) {
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            IpFingerprint that = (IpFingerprint) o;
            return Objects.equals(ip, that.ip) &&
                    Objects.equals(fingerprint, that.fingerprint);
        }

        @Override
        public int hashCode() {
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
