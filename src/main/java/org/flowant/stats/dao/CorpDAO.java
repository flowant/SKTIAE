package org.flowant.stats.dao;

import static java.util.Optional.empty;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BinaryOperator;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.flowant.util.StringUtil;

public class CorpDAO extends MongoDAO {
    static final String DATABASE_NAME = "corporation";
    static final String COL_CORP_HSCODES = "corporation_hscodes";
    static final String HS_CD = "Hscode";

    private static Map<String, List<String>> cacheCorpNameHsCodes;

    private static CorpDAO single = new CorpDAO();

    private CorpDAO() {
        super(DATABASE_NAME);
    }

    public static Stream<Doc> findAll() throws IOException {
        return single.find(COL_CORP_HSCODES, empty(), empty(), empty(), empty());
    }

    public static Map<String, List<String>> findCorpNameHsCodes(boolean useCache) throws IOException {
        if (cacheCorpNameHsCodes == null || useCache == false) {
            cacheCorpNameHsCodes = findCorpNameHsCodes();
        }
        return cacheCorpNameHsCodes;
    }

    public static Map<String, List<String>> findCorpNameHsCodes() throws IOException {
        BinaryOperator<List<String>> listCombiner = (list1, list2) -> {
            list1.addAll(list2);
            return list1;
        };

        Map<String, Optional<List<String>>> map = findAll().collect(groupingBy(Doc::getCorpName,
                mapping(doc -> parse(doc.get(HS_CD)), reducing(listCombiner))));

        return map.entrySet().stream().filter(entry -> entry.getValue().get().size() > 0)
                .collect(toMap(Entry::getKey, entry -> entry.getValue().get().stream().distinct().collect(toList())));
    }

    /**
     * DB에 컴마로 분리되어 여러개의 hsCode가 저장되어 있으며 부정환 값을 제거하여 List 로 반환. hsCode는 4자리부터
     * 10자리까지 있으며, 5자리 혹은 9자리의 잘못된 hsCode 제거. 4자리 혹은 6자리는 유효한 코드지만, 상품의 범위가 너무
     * 클수 있기때문에 제거. DB에 hsCode가 Long 혹은 String 두 가지 형태로 저장 되어있음.
     *
     * @param hsCodes
     * @return
     *
     * @see CorpDaoTest
     */
    public static List<String> parse(Object hsCodes) {
        return Arrays.stream(String.valueOf(hsCodes).split(",")).map(StringUtil::removeNonDigit)
                .filter(str -> str.length() == 10).collect(Collectors.toList());
    }

}
