package org.flowant.stats.trade;

import java.io.IOException;

import org.flowant.stats.BaseTest;
import org.flowant.util.DateTimeUtil;
import org.flowant.util.FileUtil;
import org.junit.Assert;
import org.junit.Test;

public class OpenApiClientTest extends BaseTest {
    @Test
    public void downloadTradeDataAndSaveToFile() throws IOException {
        String downloadTestFilePath = "downloadTestFile";
        FileUtil.rmdirs(downloadTestFilePath);
        OpenApiClient.downloadTradeDataAndSaveToFile(DateTimeUtil.makeMonthsBefore(6), "JP", downloadTestFilePath);
        String contents = FileUtil.readStringFromLittleFile(downloadTestFilePath);
        Assert.assertTrue(contents.length() > 0);
        logger.log(l, () -> "downloaded contents:" + contents);
        FileUtil.rmdirs(downloadTestFilePath);
    }
}
