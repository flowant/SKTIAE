package org.flowant.stats.trade;

import static org.flowant.stats.Config.getConfig;
import static org.flowant.stats.Config.Keys.ServiceKeyNationItemTrade;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.flowant.util.FileUtil;

public class OpenApiClient {

    public static void downloadTradeDataAndSaveToFile(String ym, String stateCd, String filePath)
            throws IOException {

        String addr = "http://openapi.customs.go.kr/openapi/service/newTradestatistics/getNitemtradeList"
                + "?serviceKey=" + getConfig(ServiceKeyNationItemTrade) + "&searchBgnDe=" + ym
                + "&searchEndDe=" + ym + "&searchStatCd=" + stateCd;
        URL url = new URL(addr);
        FileUtil.rmdirs(filePath);
        try (InputStream in = url.openStream()) {
            Files.copy(in, Paths.get(filePath));
        }
        TradeFiles.cacheFileNames(filePath);
    }

}
