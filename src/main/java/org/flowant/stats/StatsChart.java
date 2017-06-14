package org.flowant.stats;

import static java.util.stream.Collectors.toList;
import static org.flowant.stats.Config.getConfig;
import static org.flowant.stats.Config.getConfigInt;
import static org.flowant.stats.Config.Keys.ChartFont;
import static org.flowant.stats.Config.Keys.ChartImageHeight;
import static org.flowant.stats.Config.Keys.ChartImageOutputPath;
import static org.flowant.stats.Config.Keys.ChartImageWidth;
import static org.flowant.stats.Config.Keys.ChartLineWidth;
import static org.flowant.stats.Config.Keys.ChartScreenHeight;
import static org.flowant.stats.Config.Keys.ChartScreenWidth;
import static org.flowant.stats.Config.Keys.SearchUrlPrefix;
import static org.flowant.stats.Config.Keys.SearchUrlSuffix;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import org.flowant.stats.dao.Doc;
import org.flowant.function.FunctionT;
import org.flowant.util.FileUtil;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.LegendItemEntity;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

public class StatsChart extends ApplicationFrame {
    private static final long serialVersionUID = 1L;

    static Logger logger = Logger.getLogger(StatsChart.class.getSimpleName());

    static final int screenWidth = getConfigInt(ChartScreenWidth);
    static final int screenHeight = getConfigInt(ChartScreenHeight);
    static final int imageWidth = getConfigInt(ChartImageWidth);
    static final int imageHeight = getConfigInt(ChartImageHeight);
    static final float lineWidth = getConfig(ChartLineWidth, Float.class);

    static final String font = getConfig(ChartFont);
    static final String imageOutputPath = getConfig(ChartImageOutputPath);
    static final String searchUrlPrefix = getConfig(SearchUrlPrefix);
    static final String searchUrlSuffix = getConfig(SearchUrlSuffix);

    public static final String SEP_ROW_DESC = " ";

    Font fontTickLabel = new Font(font, Font.BOLD, 8);
    Font fontLabel = new Font(font, Font.BOLD, 16);
    Font fontLegend = new Font(font, Font.BOLD, 12);
    Font fontTitle = new Font(font, Font.BOLD, 20);

    String columnLabel;
    String rowLabel;

    String columnKey;
    String rowKey;
    String valueKey;
    FunctionT<List<String>, Map<String, String>> makeRowDescMap;

    CategoryDataset dataset;
    JFreeChart chart;
    ChartPanel chartPanel;

    Map<String, String> mapRowDesc = Collections.synchronizedMap(new HashMap<String, String>());

    public StatsChart(String title, String columnLabel, String rowLabel, String columnKey,
            String rowKey, String valueKey, FunctionT<List<String>, Map<String, String>> makeRowDescMap,
            List<List<Doc>> list) {

        super(title);
        this.columnLabel = columnLabel;
        this.rowLabel = rowLabel;
        this.columnKey = columnKey;
        this.rowKey = rowKey;
        this.valueKey = valueKey;
        this.makeRowDescMap = makeRowDescMap;

        dataset = createDataset(list);
        chart = createChart(dataset);
        chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(screenWidth, screenHeight));
        chartPanel.addChartMouseListener(mouseListener);
        setContentPane(chartPanel);
    }

    public String getRowWithDesc(String rowKey) {
        return mapRowDesc.get(rowKey);
    }

    protected CategoryDataset createDataset(List<List<Doc>> list) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        mapRowDesc.putAll(makeRowDescMap.apply(list.stream().parallel().filter(l -> l.size() > 0)
                .map(docList -> docList.get(0).getString(rowKey)).collect(toList())));

        list.forEach(subList -> subList.forEach(doc -> {
            dataset.addValue(doc.getLong(valueKey), getRowWithDesc(doc.getString(rowKey)), doc.getString(columnKey));
        }));

        return dataset;
    }

    protected JFreeChart createChart(CategoryDataset dataset) {

        JFreeChart chart = ChartFactory.createLineChart(this.getTitle(), columnLabel, rowLabel, dataset,
                PlotOrientation.VERTICAL, true/* legend */, true/* tooltips */, true /* urls */);

        chart.setBackgroundPaint(Color.white);
        chart.setTextAntiAlias(true);

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.lightGray);
        plot.setRangeGridlinePaint(Color.white);

        chart.getLegend().setItemFont(fontLegend);
        chart.getTitle().setFont(fontTitle);

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        rangeAxis.setAutoRangeIncludesZero(false);
        rangeAxis.setTickLabelFont(fontTickLabel);
        rangeAxis.setLabelFont(fontLabel);

        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setTickLabelFont(fontTickLabel);
        domainAxis.setLabelFont(fontLabel);

        CategoryItemRenderer renderer = plot.getRenderer();

        int seriesCount = dataset.getRowKeys().size();
        for (int i = 0; i < seriesCount; i++) {
            plot.getRenderer().setSeriesStroke(i, new BasicStroke(lineWidth));
        }

        renderer.setBaseItemLabelFont(fontTickLabel);
        renderer.setBaseItemLabelGenerator(new TradeLabelGenerator());
        renderer.setBaseItemLabelsVisible(true);

        return chart;
    }

    static ChartMouseListener mouseListener = new ChartMouseListener() {
        @Override
        public void chartMouseClicked(ChartMouseEvent event) {
            logger.fine(() -> "chartMouseClicked");
            ChartEntity entity = event.getEntity();
            if (entity instanceof LegendItemEntity) {
                LegendItemEntity legendEntity = (LegendItemEntity) event.getEntity();
                String seriesKey = legendEntity.getSeriesKey().toString();
                if (Desktop.isDesktopSupported()) {
                    try {
                        String hsCodeSearchUrl = searchUrlPrefix + seriesKey.split(SEP_ROW_DESC)[0]
                                + searchUrlSuffix;
                        logger.fine(() -> "open browser, url: " + hsCodeSearchUrl);
                        Desktop.getDesktop().browse(new URI(hsCodeSearchUrl));
                    } catch (IOException | URISyntaxException e) {
                        logger.severe(e.getLocalizedMessage());
                    }
                }
            }
        }

        @Override
        public void chartMouseMoved(ChartMouseEvent event) {
        }
    };

    static class TradeLabelGenerator extends StandardCategoryItemLabelGenerator {
        private static final long serialVersionUID = 1L;

        public String generateLabel(CategoryDataset dataset, int series, int category) {
            return category == dataset.getColumnCount() - 1 ? dataset.getRowKey(series).toString() : "";
        }
    }

    public static enum WayToOutput {
        SHOW_ON_SCREEN, SAVE_TO_IMAGE
    }

    public static void output(String title, String columnLabel, String rowLabel, String columnKey,
            String rowKey, String valueKey, FunctionT<List<String>, Map<String, String>> makeRowDescMap,
            List<List<Doc>> list, WayToOutput... ways) throws IOException {

        Objects.requireNonNull(ways);
        StatsChart chart = new StatsChart(title, columnLabel, rowLabel, columnKey, rowKey, valueKey,
                makeRowDescMap, list);

        for (WayToOutput way : ways) {
            switch (way) {
            case SAVE_TO_IMAGE:
                FileUtil.mkdirs(imageOutputPath);
                String imagePath = imageOutputPath + Config.SEP_DIR + FileUtil.toSafePath(title) + ".jpg";
                FileUtil.rmdirs(imagePath);
                ChartUtilities.saveChartAsJPEG(new File(imagePath), chart.chart, imageWidth, imageHeight);
                break;
            case SHOW_ON_SCREEN:
                chart.pack();
                RefineryUtilities.centerFrameOnScreen(chart);
                chart.setVisible(true);
                break;
            default:
                logger.warning("unknown way to output:" + way);
            }
        }
    }

}