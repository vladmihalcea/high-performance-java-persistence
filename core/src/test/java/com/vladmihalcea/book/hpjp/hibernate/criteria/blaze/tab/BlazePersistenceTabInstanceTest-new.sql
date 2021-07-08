SELECT tabinstanc0_.tab_key                    AS tab_key1_0_,
       tabinstanc0_.tab_ver                    AS tab_ver2_0_,
       tabinstanc0_.tab_acronym                AS tab_acronym3_0_,
       tabinstanc0_.tab_additional_data        AS tab_additional_dat4_0_,
       tabinstanc0_.tab_additional_data_number AS tab_additional_dat5_0_
FROM tab_instance tabinstanc0_
INNER JOIN tab_object tabobject1_ ON tabinstanc0_.tab_key = tabobject1_.tab_key
INNER JOIN (
    SELECT
       NULL tabKey,
       NULL tabVer
    FROM dual
    WHERE 1 = 0
    UNION ALL (
        SELECT
            tabobject0_.tab_key AS col_0_0_,
            nvl(tabinstanc1_.tab_ver, tabkeyver4_.tabVer) AS col_1_0_
        FROM tab_object tabobject0_
        LEFT OUTER JOIN tab_instance tabinstanc1_ ON tabobject0_.tab_key = tabinstanc1_.tab_key
        INNER JOIN tab_version tabversion2_ ON tabinstanc1_.tab_ver = tabversion2_.tab_key
        INNER JOIN tab_source tabsource3_
            ON (tabversion2_.tab_source = tabsource3_.tab_key
                AND tabsource3_.tab_acronym = ?
                AND (tabinstanc1_.tab_ver in (?, ?, ?))
            )
        LEFT OUTER JOIN (
            SELECT
                NULL tabKey,
                NULL tabVer
            FROM dual
            WHERE 1 = 0
            UNION ALL (
            SELECT
                tabinstanc0_.tab_key      AS col_0_0_,
                max(tabinstanc0_.tab_ver) AS col_1_0_
            FROM tab_instance tabinstanc0_
            INNER JOIN tab_version tabversion1_ ON tabinstanc0_.tab_ver = tabversion1_.tab_key
            INNER JOIN tab_source tabsource2_ ON tabversion1_.tab_source = tabsource2_.tab_key
            WHERE tabsource2_.tab_acronym <> ?
                AND (tabinstanc0_.tab_ver in (?, ?, ?))
            GROUP BY tabinstanc0_.tab_key)
        ) tabkeyver4_
            ON ((NULL IS NULL)
            AND tabkeyver4_.tabKey = tabobject0_.tab_key)
        WHERE tabinstanc1_.tab_ver IS NOT NULL
          OR tabkeyver4_.tabVer IS NOT NULL
    )
) tabkeyver2_ ON ((NULL IS NULL)
     AND tabinstanc0_.tab_ver = tabkeyver2_.tabVer
     AND tabinstanc0_.tab_key = tabkeyver2_.tabKey)
WHERE tabinstanc0_.tab_ver in (?, ?, ?)
order by tabobject1_.tab_acronym ASC