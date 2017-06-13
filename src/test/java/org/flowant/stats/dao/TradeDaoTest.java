package org.flowant.stats.dao;

import static org.flowant.stats.dao.Doc.EXP_DLR;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.flowant.stats.BaseTest;
import org.flowant.util.DateTimeUtil;
import org.junit.Assert;
import org.junit.Test;

public class TradeDaoTest extends BaseTest {
    int TEST_CNT = 100;
    @Test
    public void makeSpacerDocumentList() {
        String startYearMonth = DateTimeUtil.makeYearMonthString(2000, 1);
        String endYearMonth = DateTimeUtil.plusMonths(startYearMonth, TEST_CNT - 1);

        String yearMonth = startYearMonth;
        List<Doc> docList = new ArrayList<>();
        for (int i = 0; i < TEST_CNT; i++) {
            Doc doc = TradeDAO.makeSpacerDoc(Optional.empty(), yearMonth);
            docList.add(doc);
            yearMonth = DateTimeUtil.plusOneMonth(yearMonth);
        }

        // make hole
        for (int i = 0; i < docList.size(); i++) {
            if (i % 3 == 0)
                docList.remove(i);
        }
        docList.remove(docList.size() - 1);

        docList = TradeDAO.makeSpacerDocList(docList.iterator(), startYearMonth, endYearMonth);

        logger.log(l, () -> "startYearMonth:" + startYearMonth);
        if (logger.isLoggable(l)) {
            for (int i = 0; i < docList.size(); i++) {
                logger.log(l, docList.get(i).toString());
            }
        }
        logger.log(l, () -> "endYearMonth:" + endYearMonth);

        yearMonth = startYearMonth;
        Iterator<Doc> iter = docList.iterator();
        while(iter.hasNext()) {
            Assert.assertEquals(yearMonth, iter.next().getYm());
            yearMonth = DateTimeUtil.plusOneMonth(yearMonth);
        }
        Assert.assertEquals(endYearMonth, docList.get(docList.size() - 1).getYm());
    }

    @Test
    public void findAllNationCode() throws IOException {
        List<String> sortedList = TradeDAO.findNationCode(DateTimeUtil.makeMonthsBefore(0), EXP_DLR, true);
        sortedList.forEach(s -> logger.log(l, () -> s.toString()));

        sortedList = TradeDAO.findNationCode(DateTimeUtil.makeMonthsBefore(1), EXP_DLR, false);
        sortedList.forEach(s -> logger.log(l, () -> s.toString()));
    }

    @Test
    public void findLatestYearMonth() throws IOException {
        Optional<String> yearMonth = TradeDAO.findLatestYm(true);
        logger.log(l, () -> "latestYearMonth:" + yearMonth.orElse("collection is empty"));
    }

}
