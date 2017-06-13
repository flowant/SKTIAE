package org.flowant.stats.dao;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.flowant.stats.BaseTest;
import org.flowant.stats.dao.CorpDAO;
import org.junit.Assert;
import org.junit.Test;

public class CorpDaoTest extends BaseTest {
    int TEST_CNT = 100;

    @Test
    public void parse() throws IOException {
        List<String> testStrList = Arrays.asList(
                "95030033",
                "8516 79 9000",
                "3816.001",
                "8536.5",
                "7210.69",
                "2914.13-0000, 2907.23-1000",
                "5903.20-0000",
                "2914.13-0000, 2907.23-1000, 2914.11-0000",
                "8409993030",
                "8483.10-9090, 8409.99-3030, 8409993030");

        testStrList.forEach(strBefore -> {
            logger.log(l, () -> "before:" + strBefore);
            List<String> hsCodeList = CorpDAO.parse(strBefore);
            hsCodeList.forEach(str -> {
                Assert.assertFalse(str.matches("\\D"));
                Assert.assertEquals(10, str.length());
                logger.log(l, () -> str);
            });
        });
    }

    @Test
    public void findCorpHsCodes() throws IOException {
        Map<String, List<String>> corpHsCodesMap = CorpDAO.findCorpNameHsCodes();
        logger.log(l, () -> "size:" + corpHsCodesMap.size());
        corpHsCodesMap.forEach((name, list) -> {
            logger.log(l, () -> name + ":" + list);
        });
    }
}
