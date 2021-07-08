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
    JOIN tab_source df
        ON vf.tab_source = df.tab_key
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