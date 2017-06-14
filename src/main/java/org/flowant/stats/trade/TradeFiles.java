package org.flowant.stats.trade;

import static org.flowant.stats.Config.getConfig;
import static org.flowant.stats.Config.Keys.TradeFileRepositoryPath;
import static org.flowant.stats.trade.TradeParser.ITEM;
import static org.flowant.stats.trade.TradeParser.RESULT_CODE;
import static org.flowant.stats.trade.TradeParser.SUCCESS_00;
import static org.flowant.util.FileUtil.listFiles;
import static org.flowant.util.FileUtil.rmdirs;
import static org.flowant.util.FileUtil.mkdirs;
import static java.util.stream.Collectors.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.flowant.function.PredicateT;
import org.flowant.function.ConsumerT;
import org.flowant.stats.StatsException;
import org.flowant.util.DateTimeUtil;
import org.flowant.util.FileUtil;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * 수출입 정보 파일들에 대한 반복적인 IO 연산을 줄이고, 비정상 파일들을 삭제 하는 기능 제공
 *
 * @author "Kyengwhan Jee"
 *
 */
public class TradeFiles {
    static Logger logger = Logger.getLogger(TradeFiles.class.getSimpleName());
    static List<String> cacheFileNames = null;
    final static Path repositoryPath = Paths.get(getConfig(TradeFileRepositoryPath));

    static {
        try {
            mkdirs(repositoryPath);
            cacheFileNames();
        } catch (IOException e) {
            logger.severe(e.getMessage());
        }
    }

    public static String getRepositoryPath() {
        return repositoryPath.toString();
    }

    synchronized public static List<String> cacheFileNames() throws IOException {
        cacheFileNames = Collections.synchronizedList(listFiles(repositoryPath));
        return cacheFileNames;
    }

    synchronized public static List<String> cacheFileNames(String fileName) {
        cacheFileNames.add(fileName);
        return cacheFileNames;
    }

    /**
     * 저장소에 있는 파일중 mustContain 에 해당하는 문자열을 포함하지 않은 파일들을 모두 삭제
     *
     * @param mustContain
     * @throws IOException
     */
    synchronized public static void removeFilesInRepositoryIfNotContains(String mustContain) throws IOException {
        PredicateT<String> filter = path -> Files.lines(Paths.get(path)).noneMatch(line -> line.contains(mustContain));
        List<String> fileList = cacheFileNames.stream().parallel().filter(filter).collect(toList());
        fileList.forEach((ConsumerT<String>) FileUtil::rmdirs);
        cacheFileNames.removeAll(fileList);
    }

    synchronized public static boolean existInRepository(String... fileNameContain) throws IOException {
        Predicate<String> containsAll = strFilePath -> {
            for (String s : fileNameContain) {
                if (strFilePath.contains(s) == false) {
                    return false;
                }
            }
            return true;
        };
        return cacheFileNames.stream().parallel().anyMatch(containsAll);
    }

    /**
     * 파일이름에 포함된 국가 코드,년, 월들이 같은 파일들을 모두 삭제
     *
     * @throws IOException
     */
    synchronized public static void findOverlapping(boolean removeOverlapping) throws IOException {
        List<String> list = cacheFileNames;
        int size = list.size();

        for (int i = 0; i < size; i++) {
            String headFilename = list.get(i);
            if (headFilename == null) {
                continue;
            }

            // openApiData\NationItem_Z1_201607_2016-11-04.xml
            // 상단의 부분중 Z1_201607 이 부분만 비교하여 같은지 확인
            String headSubname = headFilename.substring(23, 32);
            ArrayList<String> overlappedFiles = new ArrayList<String>();

            for (int j = i + 1; j < size; j++) {
                String tailFilename = list.get(j);
                if (tailFilename == null) {
                    continue;
                }
                // 국가 코드 비교
                if (headSubname.equals(tailFilename.substring(23, 25)) == false) {
                    break;
                }

                if (headSubname.equals(tailFilename.substring(23, 32))) {
                    overlappedFiles.add(tailFilename);
                    list.set(j, null);
                }
            }

            if (overlappedFiles.size() > 0) {
                overlappedFiles.add(headFilename);
                Iterator<String> iter = overlappedFiles.iterator();
                while (iter.hasNext()) {
                    String fileName = iter.next();
                    if (removeOverlapping) {
                        rmdirs(fileName);
                    }
                    logger.severe("remove:" + removeOverlapping + ", overlapped file:" + fileName);
                }
            }
        }

        cacheFileNames();
    }

    synchronized public static void deleteFileIfItemIsEmpty(String xmlFilePath) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            try (FileInputStream fis = new FileInputStream(xmlFilePath)) {
                org.w3c.dom.Document xmlDoc = builder.parse(fis);

                NodeList itemList = xmlDoc.getElementsByTagName(RESULT_CODE);
                if (itemList.getLength() == 0) {
                    throw new StatsException(RESULT_CODE + " is not contained, xmlFilePath:" + xmlFilePath);
                }

                Element resultCode = (Element) itemList.item(0);
                if (resultCode.getTextContent().equals(SUCCESS_00) == false) {
                    throw new StatsException(RESULT_CODE + " is not success, xmlFilePath:" + xmlFilePath);
                }

                itemList = xmlDoc.getElementsByTagName(ITEM);

                if (itemList.getLength() == 0) {
                    throw new StatsException("item is not contained, xmlFilePath:" + xmlFilePath);
                }
            }
        } catch (StatsException | ParserConfigurationException | SAXException e) {
            rmdirs(xmlFilePath);
            cacheFileNames.remove(xmlFilePath);
            logger.severe("remove empty file, " + e.getLocalizedMessage());
        }
    }

    /**
     * 최신 데이터에 해당하는 지난달 데이터중 아이템 항목이 비어있는 파일들을 삭제. 파일이 삭제된 이후 다시 다운로드 받는 기능이 수행
     * 됨.
     *
     * @throws IOException
     */
    synchronized public static void deleteLastMonthFilesIfItemIsEmpty() throws IOException {
        String lastYm = DateTimeUtil.makeLastMonth();
        Iterator<String> iter = cacheFileNames.stream().parallel()
                .filter(name -> name.contains(lastYm)).collect(toList()).iterator();
        while(iter.hasNext()) {
            deleteFileIfItemIsEmpty(iter.next());
        }
    }

    /**
     * 수출입 데이터년월과 실제 다운로드 받은 날의 차이가 2개월 이내인 파일을 대상으로 파일이 포함한 아이템이 없다면 삭제. 마지막
     * 다운로드 받은 데이터의 아이템이 그당시에 준비되지 않아 비어 있을 수도 있음. 이번달에 한번도 다운로드 기능이 동작 하지 않은
     * 경우에만 수행.
     *
     * @throws IOException
     */
    synchronized public static void deleteOldFilesIfItemIsEmpty() throws IOException {

        String thisYm = DateTimeUtil.makeThisMonth();

        // 이번달에 데이터를 다운로드 받은 적이 있다면 수행 중지
        boolean isDownloaded = cacheFileNames.stream().parallel().anyMatch((path) -> {
            String downloadYm = path.substring(33, 37) + path.substring(38, 40);
            return DateTimeUtil.periodByMonth(downloadYm, thisYm) == 0;
        });

        if (isDownloaded) {
            logger.info("The file downloaded in this month is exist, skip");
            return;
        }

        logger.info("The file downloaded in this month is not exist, remove files containing no item");
        // 데이터 년도와 다운로드 받은 날의 차이가 2개월 이내인 파일을 대상으로 파일이 포함한 아이템이 없다면 삭제
        Iterator<String> iter = cacheFileNames.stream().parallel().filter(path -> {
            String downloadYm = path.substring(33, 37) + path.substring(38, 40);
            String itemYm = path.substring(26, 32);
            return DateTimeUtil.periodByMonth(itemYm, downloadYm) < 3;
        }).collect(toList()).iterator();

        while (iter.hasNext()) {
            String path = iter.next();
            deleteFileIfItemIsEmpty(path);
        }
    }
}
