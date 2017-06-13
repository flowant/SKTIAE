package org.flowant.stats.trade;

import static org.flowant.stats.dao.Doc.BAL_PAY;
import static org.flowant.stats.dao.Doc.DESC_KOR;
import static org.flowant.stats.dao.Doc.DESC_NATION;
import static org.flowant.stats.dao.Doc.EXP_DLR;
import static org.flowant.stats.dao.Doc.EXP_WGT;
import static org.flowant.stats.dao.Doc.HS_CD;
import static org.flowant.stats.dao.Doc.IMP_DLR;
import static org.flowant.stats.dao.Doc.IMP_WGT;
import static org.flowant.stats.dao.Doc.STATE_CD;
import static org.flowant.stats.dao.Doc.YEAR_MONTH;
import static org.flowant.util.StringUtil.removeNonDigit;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.flowant.stats.StatsException;
import org.flowant.stats.dao.Doc;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
public class TradeParser {
    static Logger logger = Logger.getLogger(TradeParser.class.getSimpleName());

    public static final String RESULT_CODE = "resultCode";
    public static final String SUCCESS_00 = "00";
    public static final String ITEM = "item";
    public static final String BODY = "body";
    public static final String ITEMS = "items";

    public static final String TOTAL = "총계";

    public static Optional<List<Doc>> parseNationItemFile(String path) throws IOException, StatsException {

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            try (FileInputStream fis = new FileInputStream(path)) {
                org.w3c.dom.Document xmlDoc = builder.parse(fis);

                NodeList itemList = xmlDoc.getElementsByTagName(RESULT_CODE);
                if (itemList.getLength() == 0) {
                    throw new StatsException(RESULT_CODE + " is not contained, xmlFilePath:" + path);
                }

                Element resultCode = (Element) itemList.item(0);
                if (resultCode.getTextContent().equals(SUCCESS_00) == false) {
                    throw new StatsException(RESULT_CODE + " is not success, xmlFilePath:" + path);
                }

                itemList = xmlDoc.getElementsByTagName(ITEM);

                if (itemList.getLength() == 0) {
                    logger.warning(ITEM + " is not contained, skip upsert, xmlFilePath:" + path);
                    return Optional.empty();
                }

                List<Doc> docList = new ArrayList<Doc>();
                for (int i = 0; i < itemList.getLength(); i++) {
                    Element item = (Element) itemList.item(i);

                    if (TOTAL.equals(item.getElementsByTagName(YEAR_MONTH).item(0).getTextContent())) {
                        logger.info("skip " + TOTAL + ", xmlFilePath:" + path);
                        continue;
                    }

                    Doc doc = new Doc();
                    doc.append(BAL_PAY, Long.parseLong(item.getElementsByTagName(BAL_PAY).item(0).getTextContent()));
                    doc.append(EXP_DLR, Long.parseLong(item.getElementsByTagName(EXP_DLR).item(0).getTextContent()));
                    doc.append(EXP_WGT, Long.parseLong(item.getElementsByTagName(EXP_WGT).item(0).getTextContent()));
                    doc.append(IMP_DLR, Long.parseLong(item.getElementsByTagName(IMP_DLR).item(0).getTextContent()));
                    doc.append(IMP_WGT, Long.parseLong(item.getElementsByTagName(IMP_WGT).item(0).getTextContent()));
                    doc.append(HS_CD, item.getElementsByTagName(HS_CD).item(0).getTextContent());
                    doc.append(STATE_CD, item.getElementsByTagName(STATE_CD).item(0).getTextContent());
                    doc.append(DESC_NATION, item.getElementsByTagName(DESC_NATION).item(0).getTextContent());
                    doc.append(DESC_KOR, item.getElementsByTagName(DESC_KOR).item(0).getTextContent());
                    doc.append(YEAR_MONTH, removeNonDigit(item.getElementsByTagName(YEAR_MONTH).item(0).getTextContent()));

                    docList.add(doc);
                }
                return Optional.of(docList);
            }
        } catch (ParserConfigurationException | SAXException e) {
            logger.severe(e.getLocalizedMessage());
            throw new StatsException(e);
        }
    }
}
