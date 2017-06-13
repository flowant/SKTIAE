package org.flowant.stats.trade;

import static org.flowant.stats.Config.SEP_DIR;
import static org.flowant.stats.Config.getConfig;
import static org.flowant.stats.Config.Keys.CollectStartYearMonth;
import static org.flowant.stats.dao.Doc.EXP_DLR;
import static org.flowant.stats.dao.TradeDAO.findNationCode;
import static org.flowant.stats.dao.TradeDAO.upsertManyNi;
import static org.flowant.util.DateTimeUtil.makeLastMonth;
import static org.flowant.util.DateTimeUtil.makeMonthsBefore;
import static org.flowant.util.DateTimeUtil.periodByMonth;
import static org.flowant.util.DateTimeUtil.plusMonths;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.flowant.function.ConsumerT;
import org.flowant.stats.Initializer;
import org.flowant.stats.StatsException;
import org.flowant.stats.dao.Doc;
import org.flowant.util.FileUtil;

/**
 * 수출입 정보를 다운로드하여 파일로 저장한 후 데이터베이스에 저장 함
 *
 * @author "Kyengwhan Jee"
 *
 */
public class TradeDataCollector {

    static Logger logger = Logger.getLogger(TradeDataCollector.class.getSimpleName());


    public static void downloadAndSaveAllNationItem(String startYm, String endYm)
            throws IOException, StatsException {

        List<String> nationList = findNationCode(makeMonthsBefore(3), EXP_DLR, false);
        if (nationList.isEmpty()) {
            nationList = findNationCode();
        }
        ConsumerT<String> consumer = stateCd -> downloadAndSaveNationItem(startYm, endYm, stateCd);
        nationList.stream().parallel().forEach(consumer);
    }

    public static void downloadAndSaveNationItem(String startYm, String endYm, String stateCd)
            throws IOException, StatsException {

        int period = periodByMonth(startYm, endYm);
        for (int i = 0; i <= period; i++) {
            String ym = plusMonths(startYm, i);
            String niStateCdYm = "NationItem_" + stateCd + "_" + ym;
            String path = TradeFiles.getRepositoryPath() + SEP_DIR + niStateCdYm + "_"
                    + LocalDate.now().toString() + ".xml";
            logger.fine(() -> "OpenApi NationItem filePath:" + path);

            if (TradeFiles.existInRepository(niStateCdYm)) {
                logger.fine(() -> niStateCdYm + " already exist, skip download");
                continue;
            }

            logger.info("download " + path);
            OpenApiClient.downloadTradeDataAndSaveToFile(ym, stateCd, path);

            try {
                Optional<List<Doc>> docList = TradeParser.parseNationItemFile(path);
                upsertManyNi(docList, true);
            } catch (StatsException e) {
                logger.severe(e.getLocalizedMessage() + ", remove NationItem filePath:" + path);
                FileUtil.logContents(Level.SEVERE, path);
                FileUtil.rmdirs(path);
                throw e;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Initializer.Initialize();

        TradeFiles.findOverlapping(false);
        TradeFiles.deleteLastMonthFilesIfItemIsEmpty();
        TradeFiles.deleteOldFilesIfItemIsEmpty();

        downloadAndSaveAllNationItem(getConfig(CollectStartYearMonth), makeLastMonth());
    }

}
