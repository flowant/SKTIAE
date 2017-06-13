package org.flowant.stats.trade;

import static java.util.Comparator.comparingDouble;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.flowant.stats.dao.Doc.CAGR;
import static org.flowant.stats.dao.Doc.HS_CD;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.flowant.function.FunctionT;
import org.flowant.stats.dao.Doc;
import org.flowant.stats.dao.TradeDAO;

public class TradeAnalysis {
    static Logger logger = Logger.getLogger(TradeAnalysis.class.getSimpleName());

    public static double cagr(double firstValue, double lastValue, double period) {
        return Math.pow(lastValue / firstValue, 1 / period) - 1;
    }

    public static double cagr(List<Doc> hsCdYmList, String field) {
        int size = hsCdYmList.size();
        double lastValue = hsCdYmList.stream().map(doc -> doc.getLong(field)).reduce(0L, Long::sum);
        double firstValue = hsCdYmList.get(0).getLong(field);
        return cagr(firstValue, lastValue, size - 1);
    }

    public static double cagr(List<Doc> hsCdYmList, String field, String startYm) throws IOException {
        List<Doc> spacerList = TradeDAO.makeSpacerDocList(hsCdYmList.iterator(), startYm);
        return cagr(spacerList, field);
    }

    public static List<Doc> findHsCdSortedByCAGR(String startYm, String valueKey, int bottomValue, int skip, int limit)
            throws IOException {
        logger.fine(() -> "sortedByCAGR started");

        List<Doc> niGroupByHsCdYmList = TradeDAO.findNiGroupByHsCodeYm(startYm, true);
        logger.fine(() -> "findAllDocsAllNations completed");

        Map<String, List<Doc>> hsCdYmMap = niGroupByHsCdYmList.parallelStream()
                .collect(groupingBy(Doc::getHsCd));

        FunctionT<Map.Entry<String, List<Doc>>, List<Doc>> makeSpacerDocList =
                entry -> TradeDAO.makeSpacerDocList(entry.getValue().iterator(), startYm);

        List<Doc> cagrList = hsCdYmMap.entrySet().stream().parallel()
                .map(makeSpacerDocList)
                .filter(list -> list.stream().noneMatch(doc -> doc.getExpDlr() < bottomValue))
                .map(list -> new Doc(HS_CD, list.get(0).getHsCd()).append(CAGR, cagr(list, valueKey)))
                .filter(Doc::isFiniteCagr)
                .sorted(comparingDouble(Doc::getCagr).reversed())
                .skip(skip)
                .limit(limit)
                .collect(toList());

        return cagrList;
    }
}
