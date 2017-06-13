package org.flowant.stats.util;

import org.flowant.stats.BaseTest;
import org.flowant.util.StringUtil;
import org.junit.Assert;
import org.junit.Test;

public class StringUtilTest extends BaseTest {

    @Test
    public void removeNonDigit() {
        String str = StringUtil.removeNonDigit("201705");
        Assert.assertEquals(str, "201705");

        str = StringUtil.removeNonDigit("2017.05");
        Assert.assertEquals(str, "201705");

        str = StringUtil.removeNonDigit("2017-05");
        Assert.assertEquals(str, "201705");

        str = StringUtil.removeNonDigit("2017y05m");
        Assert.assertEquals(str, "201705");
    }

}
