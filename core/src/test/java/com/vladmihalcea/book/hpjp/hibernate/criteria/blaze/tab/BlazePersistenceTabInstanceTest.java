package com.vladmihalcea.book.hpjp.hibernate.criteria.blaze.tab;

import com.blazebit.persistence.Criteria;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.spi.CriteriaBuilderConfiguration;
import com.vladmihalcea.book.hpjp.hibernate.criteria.blaze.tab.cte.TabKeyVer;
import com.vladmihalcea.book.hpjp.util.AbstractOracleIntegrationTest;
import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.EntityManagerFactory;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class BlazePersistenceTabInstanceTest extends AbstractOracleIntegrationTest {

    private CriteriaBuilderFactory cbf;

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            TabInstance.class,
            TabObject.class,
            TabSource.class,
            TabVersion.class,
            TabKeyVer.class
        };
    }

    @Override
    protected EntityManagerFactory newEntityManagerFactory() {
        EntityManagerFactory entityManagerFactory = super.newEntityManagerFactory();
        CriteriaBuilderConfiguration config = Criteria.getDefault();
        cbf = config.createCriteriaBuilderFactory(entityManagerFactory);
        return entityManagerFactory;
    }

    @Before
    public void init() {
        super.init();
        executeStatement("DROP table tab_instance");
        executeStatement("DROP table tab_object");
        executeStatement("DROP table tab_version");
        executeStatement("DROP table tab_source");

        executeStatement("""
            CREATE TABLE tab_source (
                 tab_key INTEGER,
                 tab_acronym VARCHAR(10),
                 PRIMARY KEY (tab_key)
             )
            """);
        executeStatement("""                         
            CREATE TABLE tab_object (
                 tab_key INTEGER,
                 tab_acronym VARCHAR(10),
                 PRIMARY KEY (tab_key)
             )
            """);
        executeStatement("""
            CREATE TABLE tab_version (
                tab_key INTEGER,
                tab_source INTEGER,
                tab_time_stamp INTEGER,
                tab_acronym VARCHAR(10),
                PRIMARY KEY (tab_key),
                FOREIGN KEY (tab_source) REFERENCES tab_source(tab_key)
            )
            """);
        executeStatement("""                             
            CREATE TABLE tab_instance (
                 tab_key INTEGER,
                 tab_ver INTEGER,
                 tab_acronym VARCHAR(50),
                 tab_additional_data VARCHAR(50),
                 tab_additional_data_number NUMBER(10),
                 PRIMARY KEY (tab_key, tab_ver),
                 FOREIGN KEY (tab_key) REFERENCES tab_object(tab_key),
                 FOREIGN KEY (tab_ver) REFERENCES tab_version(tab_key)
             )
            """);

        insertData();
    }

    private void insertData() {
        executeStatement("INSERT INTO tab_source VALUES (983, 'Region 1')");
        executeStatement("INSERT INTO tab_source VALUES (984, 'Central')");
        executeStatement("INSERT INTO tab_source VALUES (985, 'Region 2')");

        executeStatement("INSERT INTO tab_version VALUES (1, 983, 20, 'R1.1')");
        executeStatement("INSERT INTO tab_version VALUES (2, 983, 21, 'R1.2')");
        executeStatement("INSERT INTO tab_version VALUES (3, 985, 22, 'R2.1')");
        executeStatement("INSERT INTO tab_version VALUES (4, 983, 23, 'R1.3')");
        executeStatement("INSERT INTO tab_version VALUES (5, 984, 24, 'Central')");
        executeStatement("INSERT INTO tab_version VALUES (6, 983, 25, 'R1.4')");
        executeStatement("INSERT INTO tab_version VALUES (7, 985, 26, 'R2.2')");
        executeStatement("INSERT INTO tab_version VALUES (8, 984, 27, 'Central')");
        executeStatement("INSERT INTO tab_version VALUES (9, 985, 28, 'R2.3')");

        executeStatement("INSERT INTO tab_object VALUES (100, 'Object 1')");
        executeStatement("INSERT INTO tab_object VALUES (200, 'Object 2')");
        executeStatement("INSERT INTO tab_object VALUES (300, 'Object 3')");

        executeStatement("INSERT INTO tab_instance VALUES (100, 3, 'O1 [R2.1] - R1/R2/C --> C', 'AD1', 1050)");
        executeStatement("INSERT INTO tab_instance VALUES (100, 4, 'O1 [R1.3] - R1/R2/C --> C', 'AD1', 1051)");
        executeStatement("INSERT INTO tab_instance VALUES (100, 5, 'O1 [C] - R1/R2/C --> C', 'AD2', 1050)");
        executeStatement("INSERT INTO tab_instance VALUES (200, 5, 'O2 [C] - R1/C --> C', 'AD4', 1250)");
        executeStatement("INSERT INTO tab_instance VALUES (200, 3, 'O2 [R2.1] - R1/C --> C', 'AD4', 1250)");
        executeStatement("INSERT INTO tab_instance VALUES (300, 4, 'O3 [R1.3] - R1/R2 --> R1', 'AD8', 1300)");
        executeStatement("INSERT INTO tab_instance VALUES (300, 3, 'O3 [R2.1] - R1/R2 --> R1', 'AD9', 1301)");
    }

    @Test
    public void testTwoLevelLeftJoinSubqueryWithGroupByQuery() {
        doInJPA(entityManager -> {
            /*
            SELECT
                tabinstanc0_.tab_key AS tab_key1_0_,
                tabinstanc0_.tab_ver AS tab_ver2_0_,
                tabinstanc0_.tab_acronym AS tab_acronym3_0_,
                tabinstanc0_.tab_additional_data AS tab_additional_dat4_0_,
                tabinstanc0_.tab_additional_data_number AS tab_additional_dat5_0_
            FROM tab_instance tabinstanc0_
            INNER JOIN tab_object tabobject1_ ON tabinstanc0_.tab_key = tabobject1_.tab_key
            JOIN (
                SELECT
                    o.tab_key AS tab_key,
                    nvl(bf.tab_ver, a.tab_ver) AS tab_ver
                FROM tab_object o
                LEFT OUTER JOIN tab_instance bf ON bf.tab_key = o.tab_key
                JOIN tab_version vf ON bf.tab_ver = vf.tab_key
                JOIN tab_source df ON vf.tab_source = df.tab_key
                    AND df.tab_acronym = 'Central'
                    AND bf.tab_ver IN (3, 4, 5)
                LEFT OUTER JOIN (
                    SELECT
                        ba.tab_key AS tab_key,
                        max(ba.tab_ver) AS tab_ver
                    FROM tab_instance ba
                    JOIN tab_version va ON ba.tab_ver = va.tab_key
                    JOIN tab_source da ON va.tab_source = da.tab_key
                    WHERE da.tab_acronym != 'Central'
                          AND ba.tab_ver IN (3, 4, 5)
                    GROUP BY ba.tab_key
                ) a ON a.tab_key = o.tab_key
                WHERE bf.tab_ver IS NOT NULL
                      OR a.tab_ver IS NOT NULL
            ) o2 ON tabinstanc0_.tab_ver = o2.tab_ver
                    AND tabinstanc0_.tab_key = o2.tab_key
            WHERE tabinstanc0_.tab_ver IN (3, 4, 5)
            ORDER BY tabobject1_.tab_acronym ASC
             */

            List<Long> tabVer = List.of(3L, 4L, 5L);

            List<TabInstance> tabInstances = cbf.create(entityManager, TabInstance.class)
                .from(TabInstance.class, "tabinstanc0_")
                .innerJoin("tabinstanc0_.tabObject", "tabobject1_")
                .innerJoinOnSubquery(TabKeyVer.class, "o2")
                    .from(TabObject.class, "o")
                    .bind("tabKey").select("o.tabKey")
                    .bind("tabVer").select("nvl(bf.tabVersion.tabKey, a.tabVer)")
                    .leftJoin("o.tabInstances", "bf")
                    .innerJoin("bf.tabVersion", "vf")
                    .innerJoinOn(TabSource.class, "df")
                        .onExpression("vf.tabSource = df")
                        .on("df.tabAcronym").eqExpression(":tabAcronym")
                        .on("bf.id.tabVer").in(tabVer)
                    .end()
                    .leftJoinOnSubquery(TabKeyVer.class, "a")
                        .from(TabInstance.class, "ba")
                        .bind("tabKey").select("ba.tabObject.tabKey")
                        .bind("tabVer").select("max(ba.id.tabVer)")
                        .innerJoin("ba.tabVersion", "va")
                        .innerJoin("va.tabSource", "da")
                        .where("da.tabAcronym").notEqExpression(":tabAcronym")
                        .where("ba.id.tabVer").in(tabVer)
                        .groupBy("ba.tabObject.tabKey")
                        .end()
                        .onExpression("a.tabKey = o.tabKey")
                    .end()
                    .whereOr()
                    .where("bf.id.tabVer").isNotNull()
                    .where("a.tabVer").isNotNull()
                    .endOr()
                .end()
                .onExpression("tabinstanc0_.id.tabVer = o2.tabVer")
                .onExpression("tabinstanc0_.id.tabKey = o2.tabKey")
                .end()
                .where("tabinstanc0_.id.tabVer").in(tabVer)
                .orderByAsc("tabobject1_.tabAcronym")
                .setParameter("tabAcronym", "Central")
                .getResultList();

            assertEquals(2, tabInstances.size());
            assertEquals("O1 [C] - R1/R2/C --> C", tabInstances.get(0).getTabAcronym());
            assertEquals("O2 [C] - R1/C --> C", tabInstances.get(1).getTabAcronym());
            /*assertThat(tabInstances).extracting(TabInstance::getTabAcronym)
                .containsExactlyInAnyOrder("O1 [C] - R1/R2/C --> C", "O2 [C] - R1/C --> C", "O3 [R1.3] - R1/R2 --> R1");
             */
        });
    }

    @Test
    public void testLeftJoinWithGroupByQuery() {
        doInJPA(entityManager -> {
            /*
             * SELECT
             *     o.tab_key AS tab_key,
             *     nvl(bf.tab_ver, a.tab_ver) AS tab_ver
             * FROM tab_object o
             * LEFT OUTER JOIN tab_instance bf ON bf.tab_key = o.tab_key
             * JOIN tab_version vf ON bf.tab_ver = vf.tab_key
             * JOIN tab_source df ON vf.tab_source = df.tab_key
             *     AND df.tab_acronym = 'Central'
             *     AND bf.tab_ver IN (3, 4, 5)
             * LEFT OUTER JOIN (
             *     SELECT
             *         ba.tab_key AS tab_key,
             *         max(ba.tab_ver) AS tab_ver
             *     FROM tab_instance ba
             *     JOIN tab_version va ON ba.tab_ver = va.tab_key
             *     JOIN tab_source da ON va.tab_source = da.tab_key
             *     WHERE da.tab_acronym != 'Central'
             *           AND ba.tab_ver IN (3, 4, 5)
             *     GROUP BY ba.tab_key
             * ) a ON a.tab_key = o.tab_key
             * WHERE bf.tab_ver IS NOT NULL
             *       OR a.tab_ver IS NOT NULL
             */

            List tabVer = List.of(3L, 4L, 5L);

            List<TabObject> tabObjects = cbf.create(entityManager, TabObject.class)
                .from(TabObject.class, "o")
                .leftJoin("o.tabInstances", "bf")
                .innerJoin("bf.tabVersion", "vf")
                .innerJoinOn(TabSource.class, "df")
                    .onExpression("vf.tabSource = df")
                    .on("df.tabAcronym").eqExpression(":tabAcronym")
                    .on("bf.id.tabVer").in(tabVer)
                .end()
                .leftJoinOnSubquery(TabKeyVer.class, "a")
                    .from(TabInstance.class, "ba")
                    .bind("tabKey").select("ba.tabObject.tabKey")
                    .bind("tabVer").select("max(ba.id.tabVer)")
                    .innerJoin("ba.tabVersion", "va")
                    .innerJoin("va.tabSource", "da")
                    .where("da.tabAcronym").notEqExpression(":tabAcronym")
                    .where("ba.id.tabVer").in(tabVer)
                    .groupBy("ba.tabObject.tabKey")
                    .end()
                .onExpression("a.tabKey = o.tabKey")
                .end()
                .whereOr()
                    .where("bf.id.tabVer").isNotNull()
                    .where("a.tabVer").isNotNull()
                .endOr()
                .select("o.tabKey", "tabKey")
                .select("nvl(bf.tabVersion.tabKey, a.tabVer)", "tabVer")
                .setParameter("tabAcronym", "Central")
                .getResultList();

            assertTrue(tabObjects.size() > 0);
        });
    }

    @Test
    public void testGroupByQuery() {
        doInJPA(entityManager -> {
            /*
             * SELECT
             *     ba.tab_key AS tab_key,
             *     max(ba.tab_ver) AS tab_ver
             * FROM tab_instance ba
             * JOIN tab_version va ON ba.tab_ver = va.tab_key
             * JOIN tab_source da ON va.tab_source = da.tab_key
             * WHERE da.tab_acronym != 'Central'
             *       AND ba.tab_ver IN (3, 4, 5)
             * GROUP BY ba.tab_key
             */

            List tabVer = List.of(3L, 4L, 5L);

            List<TabInstance> tabInstances = cbf.create(entityManager, TabInstance.class)
                .from(TabInstance.class, "ba")
                .innerJoin("ba.tabVersion", "va")
                .innerJoin("va.tabSource", "da")
                .where("da.tabAcronym").notEqExpression(":tabAcronym")
                .where("ba.id.tabVer").in(tabVer)
                .groupBy("ba.tabObject.tabKey")
                .select("ba.tabObject.tabKey", "tab_key")
                .select("max(ba.id.tabVer)", "tab_ver")
                .setParameter("tabAcronym", "Central")
                .getResultList();

            assertTrue(tabInstances.size() > 0);
        });
    }
}
