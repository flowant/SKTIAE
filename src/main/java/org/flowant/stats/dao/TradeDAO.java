package org.flowant.stats.dao;

import static java.util.Collections.reverseOrder;
import static java.util.Comparator.naturalOrder;
import static java.util.Map.Entry.comparingByValue;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summingLong;
import static java.util.stream.Collectors.toList;
import static org.flowant.stats.Config.getConfig;
import static org.flowant.stats.Config.Keys.AnalysisStartYearMonth;
import static org.flowant.stats.StatsChart.SEP_ROW_DESC;
import static org.flowant.stats.dao.Doc.BAL_PAY;
import static org.flowant.stats.dao.Doc.DESC_KOR;
import static org.flowant.stats.dao.Doc.DESC_NATION;
import static org.flowant.stats.dao.Doc.EXP_DLR;
import static org.flowant.stats.dao.Doc.EXP_WGT;
import static org.flowant.stats.dao.Doc.HS_CD;
import static org.flowant.stats.dao.Doc.HS_CODE;
import static org.flowant.stats.dao.Doc.ID;
import static org.flowant.stats.dao.Doc.IMP_DLR;
import static org.flowant.stats.dao.Doc.IMP_WGT;
import static org.flowant.stats.dao.Doc.STATE_CD;
import static org.flowant.stats.dao.Doc.YEAR_MONTH;
import static org.flowant.util.DateTimeUtil.makeLastMonth;
import static org.flowant.util.DateTimeUtil.makeMonthsBefore;
import static org.flowant.util.DateTimeUtil.periodByMonth;
import static org.flowant.util.DateTimeUtil.plusMonths;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Spliterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.flowant.function.FunctionT;
import org.flowant.function.PredicateT;
import org.flowant.stats.StatsException;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;

public class TradeDAO extends MongoDAO {

    static final String DATABASE_NAME = "trade";

    static final String COL_NATION_ITEM = "nation_item";
    static final String COL_NATION = "국가";
    static final String COL_HSCODE_DESC = "신성질";

    private static Map<String, List<Doc>> cacheFindNiGroupByHsCodeYm = new HashMap<>();
    private static Optional<String> cacheLatestYm = empty();

    private static TradeDAO single = new TradeDAO();

    public TradeDAO() {
        super(DATABASE_NAME);
    }

    public static void upsertManyNi(Optional<List<Doc>> docList, boolean upsert)
            throws IOException, StatsException {

        if (docList.isPresent() == false)
            return;

        MongoCollection<Doc> collection = single.getCollection(COL_NATION_ITEM);

        Iterator<Doc> iter = docList.get().iterator();
        while (iter.hasNext()) {
            Doc doc = iter.next();
            single.logger.info("upsert doc:" + doc.toJson());

            Doc filter = new Doc();
            filter.put(HS_CD, doc.get(HS_CD));
            filter.put(YEAR_MONTH, doc.get(YEAR_MONTH));
            filter.put(STATE_CD, doc.get(STATE_CD));

            UpdateOptions option = new UpdateOptions();
            option.upsert(upsert);

            collection.updateOne(filter, new Doc("$set", doc), option);
        }
    }

    public static List<String> findHsCode() throws IOException {
        MongoCollection<Doc> collection = single.getCollection(COL_NATION_ITEM);
        Spliterator<String> spliterator = collection.distinct(HS_CD, String.class).spliterator();
        return StreamSupport.stream(spliterator, true).collect(toList());
    }

    public static List<Doc> findNi(String hsCode, String nationCode)
            throws IOException {

        return findNi(hsCode, nationCode, getConfig(AnalysisStartYearMonth));
    }

    public static List<Doc> findNi(String hsCode, String stateCd, String startYm)
            throws IOException {

        Doc filter = new Doc();
        filter.put(HS_CD, hsCode);
        filter.put(STATE_CD, stateCd);
        filter.put(YEAR_MONTH, new Doc("$gte", startYm));

        Doc sort = new Doc();
        sort.put(YEAR_MONTH, 1);

        Stream<Doc> stream = single.find(COL_NATION_ITEM, of(filter), of(sort), empty(), empty());
        return makeSpacerDocList(stream.iterator(), startYm);
    }

    public static List<Doc> findNi(String ym, String sortField, String stateCd, int skip, int limit)
            throws IOException {

        Doc filter = new Doc();
        filter.put(YEAR_MONTH, ym);
        filter.put(STATE_CD, stateCd);

        Doc sort = new Doc();
        sort.put(sortField, -1);

        return single.find(COL_NATION_ITEM, of(filter), of(sort), of(skip), of(limit)).collect(toList());
    }

    public static List<Doc> findNiGroupByYm(String hsCode) throws IOException {
        return findNiGroupByYm(hsCode, getConfig(AnalysisStartYearMonth));
    }

    public static List<Doc> findNiGroupByYm(String hsCode, String startYm) throws IOException {
        /*
         db.nation_item.aggregate( [
        { $match : { hsCd : "8542321010", year:{$gte:"201507"}}},
        { $group: { _id: "$year",
                    hsCd: { $first: "$hsCd" },
                    year: { $first: "$year" },
                    statKor: { $first: "$statKor" },
                    expDlr: { $sum: "$expDlr" } } } ,
        { $sort: { year: 1 } }
        ] )
         */

        Doc match = new Doc("$match",
                new Doc(HS_CD, hsCode).append(YEAR_MONTH, new Doc("$gte", startYm)));

        Doc group = new Doc("$group", new Doc(ID, "$year")
                .append(HS_CD, new Doc("$first", "$hsCd"))
                .append(YEAR_MONTH, new Doc("$first", "$year"))
                .append(DESC_KOR, new Doc("$first", "$statKor"))
                .append(EXP_DLR, new Doc("$sum", "$expDlr"))
                .append(IMP_DLR, new Doc("$sum", "$impDlr"))
                .append(EXP_WGT, new Doc("$sum", "$expWgt"))
                .append(IMP_WGT, new Doc("$sum", "$impWgt"))
                .append(BAL_PAY, new Doc("$sum", "$balPayments")));

        Doc sort = new Doc("$sort", new Doc(YEAR_MONTH, 1));

        Stream<Doc> stream = single.match(COL_NATION_ITEM, empty(), empty(), match, group, sort);
        return makeSpacerDocList(stream.iterator(), startYm);
    }

    public static List<Doc> findNiGroupByHsCode(String ym, String sortField, int skip, int limit)
            throws IOException {

        /*
            db.nation_item.aggregate( [
                { $match : { year : "201506"} },
                { $group: { _id: "$hsCd",
                            hsCd: { $first: "$hsCd" },
                            year: { $first: "$year" },
                            statKor: { $first: "$statKor" },
                            expDlr: { $sum: "$expDlr" } } },
                { $sort: { expDlr: -1 } },
                { $skip: 10 },
                { $limit : 10 }
            ] )
         */

        Doc match = new Doc("$match", new Doc(YEAR_MONTH, ym));

        Doc group = new Doc("$group", new Doc(ID, "$hsCd")
                .append(HS_CD, new Doc("$first", "$hsCd"))
                .append(YEAR_MONTH, new Doc("$first", "$year"))
                .append(DESC_KOR, new Doc("$first", "$statKor"))
                .append(EXP_DLR, new Doc("$sum", "$expDlr"))
                .append(IMP_DLR, new Doc("$sum", "$impDlr"))
                .append(EXP_WGT, new Doc("$sum", "$expWgt"))
                .append(IMP_WGT, new Doc("$sum", "$impWgt"))
                .append(BAL_PAY, new Doc("$sum", "$balPayments")));

        Doc sort = new Doc("$sort", new Doc(sortField, -1));

        return single.match(COL_NATION_ITEM, of(skip), of(limit), match, group, sort).collect(toList());
    }

    synchronized public static List<Doc> findNiGroupByHsCodeYm(String startYm, boolean useCache)
            throws IOException {

        if (cacheFindNiGroupByHsCodeYm.containsKey(startYm) == false || useCache == false) {
            cacheFindNiGroupByHsCodeYm.put(startYm, findNiGroupByHsCodeYm(startYm));
        }
        return cacheFindNiGroupByHsCodeYm.get(startYm);
    }

    public static List<Doc> findNiGroupByHsCodeYm(String startYm) throws IOException {

        /*
         * db.nation_item.aggregate( [
         * { $match : { year:{ $gte:"201507" }}},
         * { $group: { _id: { hsCd: "$hsCd", year: "$year" },
         *     hsCd: { $first: "$hsCd" },
         *     year: { $first: "$year" },
         *     statKor: { $first: "$statKor"},
         *     expDlr: { $sum: "$expDlr" },
         *     impDlr: { $sum: "$impDlr" },
         *     expWgt: { $sum: "$expWgt" },
         *     impWgt: { $sum: "$impWgt" },
         *     balPayments: { $sum: "$balPayments" }
         * }},],{allowDiskUse:true} )
         */

        Doc match = new Doc("$match", new Doc(YEAR_MONTH, new Doc("$gte", startYm)));

        Doc group = new Doc("$group", new Doc(ID, new Doc(HS_CD, "$hsCd").append(YEAR_MONTH, "$year"))
                .append(HS_CD, new Doc("$first", "$hsCd"))
                .append(YEAR_MONTH, new Doc("$first", "$year"))
                .append(DESC_KOR, new Doc("$first", "$statKor"))
                .append(EXP_DLR, new Doc("$sum", "$expDlr"))
                .append(IMP_DLR, new Doc("$sum", "$impDlr"))
                .append(EXP_WGT, new Doc("$sum", "$expWgt"))
                .append(IMP_WGT, new Doc("$sum", "$impWgt"))
                .append(BAL_PAY, new Doc("$sum", "$balPayments")));

        return single.match(COL_NATION_ITEM, empty(), empty(), match, group).collect(toList());
    }

    public static List<Doc> findNiGroupByHsCodeYmState(String startYm) throws IOException {

        /*
            db.nation_item.aggregate( [
                { $match : { year:{ $gte:"201507" }}},
                { $group: { _id: { hsCd: "$hsCd", year: "$year", statCd: "$statCd" },
                            hsCd: { $first: "$hsCd" },
                            year: { $first: "$year" },
                            statCd: { $first: "$statCd" },
                            statCdCntnKor1: { $first: "$statCdCntnKor1" },
                            statKor: { $first: "$statKor" },
                            expDlr: { $sum: "$expDlr" },
                            impDlr: { $sum: "$impDlr" },
                            expWgt: { $sum: "$expWgt" },
                            impWgt: { $sum: "$impWgt" },
                            balPayments: { $sum: "$balPayments" } } },],
                {allowDiskUse:true} )
         */

        Doc match = new Doc("$match", new Doc(YEAR_MONTH, new Doc("$gte", startYm)));

        Doc group = new Doc("$group", new Doc(ID,
                new Doc(HS_CD, "$hsCd").append(YEAR_MONTH, "$year").append(STATE_CD, "$statCd"))
                        .append(HS_CD, new Doc("$first", "$hsCd"))
                        .append(YEAR_MONTH, new Doc("$first", "$year"))
                        .append(STATE_CD, new Doc("$first", "$statCd"))
                        .append(DESC_NATION, new Doc("$first", "$statCdCntnKor1"))
                        .append(DESC_KOR, new Doc("$first", "$statKor"))
                        .append(EXP_DLR, new Doc("$sum", "$expDlr"))
                        .append(IMP_DLR, new Doc("$sum", "$impDlr"))
                        .append(EXP_WGT, new Doc("$sum", "$expWgt"))
                        .append(IMP_WGT, new Doc("$sum", "$impWgt"))
                        .append(BAL_PAY, new Doc("$sum", "$balPayments")));

        return single.match(COL_NATION_ITEM, empty(), empty(), match, group)
                .collect(toList());
    }

    public static List<String> findNationCode() throws IOException {
        return single.find(COL_NATION, empty(), empty(), empty(), empty()).map(Doc::getNationCd)
                .collect(toList());
    }

    public static List<String> findNationCode(String startYm, String sortKey,
            boolean isNaturalOrder) throws IOException {

        Map<String, Long> nationSummingValueMap = findNiGroupByHsCodeYmState(startYm).stream().parallel()
                .collect(groupingBy(Doc::getStateCd, summingLong(doc -> doc.getLong(sortKey))));

        List<String> sortedList = nationSummingValueMap.entrySet().stream()
                .sorted(comparingByValue(isNaturalOrder ? naturalOrder() : reverseOrder()))
                .map(entry -> entry.getKey())
                .collect(toList());

        return sortedList;
    }

    public static Map<String, String> findHsCodeDesc(List<String> hsCodeList) throws IOException {
        FunctionT<String, Entry<String, String>> findDesc = hsCode -> {
            Doc doc = new Doc().append(HS_CODE, Long.parseLong(hsCode));
            Optional<Doc> oDoc = single.find(COL_HSCODE_DESC, of(doc), empty(), empty(), of(1))
                    .findAny();

            String desc = hsCode.substring(0, 4) + "." + hsCode.substring(4, 6) + "-" +
                    hsCode.substring(6, 10) + SEP_ROW_DESC +
                    oDoc.map(d -> d.getDesc6Unit() + ":" + d.getDesc10Unit()).orElse("desc_empty");

            return new AbstractMap.SimpleEntry<String, String>(hsCode, desc);
        };
        return hsCodeList.stream().map(findDesc)
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    private static Optional<String> findLatestYm() throws IOException {
        if (single.count(COL_NATION_ITEM, empty()) == 0)
            return empty();

        PredicateT<String> notEmpty = yearMonth -> single.count(COL_NATION_ITEM,
                of(new Doc().append(YEAR_MONTH, yearMonth))) > 0;

        Optional<String> oYearMonth = Stream.iterate(0, i -> ++i)
                .map(i -> makeMonthsBefore(i))
                .filter(notEmpty).findFirst();

        return oYearMonth;
    }

    synchronized public static Optional<String> findLatestYm(boolean useCache)
            throws IOException {
        if (cacheLatestYm.isPresent() == false || useCache == false) {
            cacheLatestYm = findLatestYm();
        }
        return cacheLatestYm;
    }

    /**
     * 국세청에서 받은 정보 중, 수출입데이터가 없는 년월이 있음. 년월에 값이 0으로 초기화 된 데이터를 만들어서 리스트로 반환 DB에
     * 0인 데이터의 값을 삽입하게 되면 의미없는 데이터의 량이 너무 많아 질 것으로 판단됨 DB는 국세청에서 받은 정보를 그대로 사용하고
     * 회수시 가공하는 방법을 택함
     *
     * @param baseDoc
     * @param startYm
     * @param beforeEndYearMonth
     * @return
     */
    public static List<Doc> makeSpacerDocList(Iterator<Doc> iter, String startYm, String endYm) {

        if (iter.hasNext() == false) {
            return Collections.emptyList();
        }

        int period = periodByMonth(startYm, endYm);
        Doc[] docArray = new Doc[period + 1];

        Optional<Doc> oDoc = Optional.empty();

        while (iter.hasNext()) {
            oDoc = Optional.of(iter.next());
            int index = periodByMonth(startYm, oDoc.get().getYm());
            docArray[index] = oDoc.get();
        }

        for (int i = 0; i < docArray.length; i++) {
            if (docArray[i] == null) {
                String yearMonth = plusMonths(startYm, i);
                docArray[i] = makeSpacerDoc(oDoc, yearMonth);
            }
        }

        return Arrays.asList(docArray);
    }

    public static List<Doc> makeSpacerDocList(Iterator<Doc> iter, String startYm)
            throws IOException {
        return makeSpacerDocList(iter, startYm, findLatestYm(true).orElse(makeLastMonth()));
    }

    static Doc makeSpacerDoc(Optional<Doc> oBaseDoc, String ym) {
        Doc spacer = oBaseDoc.isPresent() ? new Doc(oBaseDoc.get()) : new Doc();
        spacer.put(EXP_DLR, 0L);
        spacer.put(IMP_DLR, 0L);
        spacer.put(EXP_WGT, 0L);
        spacer.put(IMP_WGT, 0L);
        spacer.put(BAL_PAY, 0L);
        spacer.put(YEAR_MONTH, ym);
        return spacer;
    }

}
