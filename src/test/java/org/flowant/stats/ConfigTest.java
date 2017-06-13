package org.flowant.stats;

import static org.flowant.stats.Config.Keys.ChartImageHeight;
import static org.flowant.stats.Config.Keys.ChartLineWidth;
import static org.flowant.stats.Config.Keys.SearchUrlSuffix;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

public class ConfigTest extends BaseTest {
    @Test
    public void getConfig() throws IOException {
        int height = Config.getConfig(ChartImageHeight, Integer.class);
        logger.log(l, () -> "ChartImageHeight:" + height);
        Assert.assertEquals(height, Config.getConfigInt(ChartImageHeight));

        float lineWidth = Config.getConfig(ChartLineWidth, Float.class);
        logger.log(l, () -> "ChartLineWidth:" + lineWidth);
        Assert.assertTrue(lineWidth > 0);

        double lineWidthDouble = Config.getConfig(ChartLineWidth, Double.class);
        logger.log(l, () -> "ChartLineWidthDouble:" + lineWidthDouble);
        Assert.assertTrue(lineWidthDouble > 0);
    }

    @Test
    public void getConfigUnicode() throws IOException {
        String unicode = Config.getConfig(SearchUrlSuffix);
        logger.log(l, () -> "SearchUrlSuffix:" + unicode);
    }
}
