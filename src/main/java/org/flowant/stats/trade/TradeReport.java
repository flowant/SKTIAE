package org.flowant.stats.trade;

import static java.util.stream.Collectors.averagingDouble;
import static java.util.stream.Collectors.toList;
import static org.flowant.stats.Config.getConfig;
import static org.flowant.stats.Config.Keys.AnalysisStartYearMonth;
import static org.flowant.stats.StatsChart.WayToOutput.SAVE_TO_IMAGE;
import static org.flowant.stats.StatsChart.WayToOutput.SHOW_ON_SCREEN;
import static org.flowant.stats.dao.CorpDAO.findCorpNameHsCodes;
import static org.flowant.stats.dao.Doc.IMP_DLR;
import static org.flowant.stats.dao.Doc.EXP_DLR;
import static org.flowant.stats.dao.Doc.HS_CD;
import static org.flowant.stats.dao.Doc.YEAR_MONTH;
import static org.flowant.stats.dao.TradeDAO.findLatestYm;
import static org.flowant.stats.dao.TradeDAO.findNi;
import static org.flowant.stats.dao.TradeDAO.findNiGroupByHsCode;
import static org.flowant.stats.dao.TradeDAO.findNiGroupByYm;
import static org.flowant.stats.trade.TradeAnalysis.cagr;
import static org.flowant.stats.trade.TradeAnalysis.findHsCdSortedByCAGR;
import static org.flowant.util.DateTimeUtil.makeLastMonth;
import static org.flowant.util.DateTimeUtil.makeMonthsBefore;
import static org.flowant.util.DateTimeUtil.makeThisMonth;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.flowant.function.BiConsumerT;
import org.flowant.function.BiFunctionT;
import org.flowant.function.ConsumerT;
import org.flowant.function.FunctionT;
import org.flowant.function.SupplierT;
import org.flowant.stats.Initializer;
import org.flowant.stats.StatsChart;
import org.flowant.stats.dao.Doc;
import org.flowant.stats.dao.TradeDAO;



public class TradeReport {
    static Logger logger = Logger.getLogger(TradeReport.class.getSimpleName());

    static String analysisStartYm;
    static String analysisEndYm;

    public static <T> List<List<Doc>> findDocs(SupplierT<List<T>> keyList,
            FunctionT<T, List<Doc>> finder) {

        return keyList.get().stream().map(finder).filter(list -> !list.isEmpty()).collect(toList());
    }

    public static void showChart(String sortYm, String valueKey, String stateCd, int skip, int limit)
            throws IOException {

        List<List<Doc>> list = findDocs(() -> findNi(sortYm, valueKey, stateCd, skip, limit),
                doc -> findNi(doc.getHsCd(), stateCd));

        String title = stateCd + ", " + valueKey + ", skip:" + skip + ",limit:" + limit + ", " +
                analysisStartYm + " ~ " + analysisEndYm;

        showAndSaveChart(title, valueKey, list);
    }

    public static void showChart(String sortYm, String valueKey, int skip, int limit) throws IOException {
        List<List<Doc>> list = findDocs(() -> findNiGroupByHsCode(sortYm, valueKey, skip, limit),
                doc -> findNiGroupByYm(doc.getHsCd()));

        String title = "EARTH, " + valueKey + ", skip:" + skip + ", limit:" + limit + ", " +
                analysisStartYm + " ~ " + analysisEndYm;

        showAndSaveChart(title, valueKey, list);
    }

    public static void showChart(String hsCode, String valueKey) throws IOException {
        List<List<Doc>> list = new ArrayList<List<Doc>>();
        list.add(findNiGroupByYm(hsCode));
        String title = "EARTH, " + valueKey + ", " + analysisStartYm + " ~ " + analysisEndYm;
        showAndSaveChart(title, valueKey, list);
    }

    public static void showCagrChart(String calculateStartYm, String valueKey, int bottomValue, int skip, int limit)
            throws IOException {

        List<List<Doc>> list = findDocs(
                () -> findHsCdSortedByCAGR(calculateStartYm, valueKey, bottomValue, skip, limit),
                doc -> findNiGroupByYm(doc.getHsCd()));

        String title = "EARTH, " + "CAGR, " + valueKey + ", skip:" + skip + ", limit:" + limit + ", " +
                analysisStartYm + " ~ " + analysisEndYm;
        showAndSaveChart(title, valueKey, list);
    }

    public static void showAndSaveChart(String title, String valueKey, List<List<Doc>> list) throws IOException {
        if (list.isEmpty()){
            logger.warning("list is empty, cannot show and save chart:" + title);
            return;
        }
        StatsChart.output(title, "YearMonth", valueKey, YEAR_MONTH, HS_CD, valueKey, TradeDAO::findHsCodeDesc, list,
                SAVE_TO_IMAGE, SHOW_ON_SCREEN);
    }

    public static BiFunctionT<String, List<String>, Double> averageCagr = (String startYm, List<String> hsCodes) -> {
        List<List<Doc>> list = findDocs(() -> hsCodes, hsCode -> findNiGroupByYm(hsCode, startYm));
        FunctionT<List<Doc>, Double> cagr = hsCdYmList -> cagr(hsCdYmList, EXP_DLR, startYm);
        return list.stream().map(cagr).collect(averagingDouble(Double::doubleValue));
    };

    public static void showCorpCagrCharts(String startYm, String valueKey, int limit) throws IOException {
        ConsumerT<SimpleEntry<Entry<String, List<String>>, Double>> showCorpChart = entry -> {
            String corpDesc = "Avr Cagr:" + String.format("%.4f", entry.getValue()) + ", " + entry.getKey().getKey();
            showCorpChart(valueKey, corpDesc, entry.getKey().getValue());
        };

        Map<String, List<String>> mapCorpHsCodes = findCorpNameHsCodes(true);
        mapCorpHsCodes.entrySet().stream().parallel()
                .map(entry -> new SimpleEntry<Entry<String, List<String>>, Double>(entry,
                        averageCagr.apply(startYm, entry.getValue())))
                .filter(entry -> Double.isFinite(entry.getValue()))
                .sorted(Entry.comparingByValue(Collections.reverseOrder()))
                .limit(limit)
                .forEachOrdered(showCorpChart);
    }

    public static void showCorpChart(String valueKey, String corpDesc, List<String> hsCodeList) throws IOException {
        List<List<Doc>> list = findDocs(() -> hsCodeList, hsCode -> findNiGroupByYm(hsCode));
        String title = corpDesc + ", " + valueKey + ", " + analysisStartYm + " ~ " + analysisEndYm;
        showAndSaveChart(title, valueKey, list);
    }

    public static void showCharts(int cntPerChart, int cntAll, BiConsumerT<Integer, Integer> showChart)
            throws IOException {

        for (int i = 0; i < cntAll; i += cntPerChart) {
            showChart.accept(i, cntPerChart);
        }
    }

    public static void main(String[] args) throws IOException {
        Initializer.Initialize();
        analysisStartYm = getConfig(AnalysisStartYearMonth);
        analysisEndYm = findLatestYm(true).orElse(makeThisMonth());

        int cntPerChart = 10;
        int cntAll = 50;
        showChart("9022130000", EXP_DLR);
        showCharts(cntPerChart, cntAll, (skip, limit) -> showChart(analysisEndYm, EXP_DLR, skip, limit));
        showCharts(cntPerChart, cntAll, (skip, limit) -> showChart(analysisEndYm, EXP_DLR, "US", skip, limit));
        showCharts(cntPerChart, cntAll,
                (skip, limit) -> showCagrChart(makeMonthsBefore(7), IMP_DLR, 500000, skip, limit));
        showCorpCagrCharts(makeMonthsBefore(12), EXP_DLR, 15);
    }

}
