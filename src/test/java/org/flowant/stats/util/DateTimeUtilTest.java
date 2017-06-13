package org.flowant.stats.util;

import org.flowant.stats.BaseTest;
import org.flowant.util.DateTimeUtil;
import org.junit.Assert;
import org.junit.Test;

public class DateTimeUtilTest extends BaseTest {

    @Test
    public void makeYearMonth() {
        String str = DateTimeUtil.makeYearMonthString(2015, 1);
        Assert.assertEquals(str, "201501");

        str = DateTimeUtil.makeYearMonthString(2015, 12);
        Assert.assertEquals(str, "201512");
    }

    @Test
    public void plusOneMonth() {
        String str = DateTimeUtil.plusOneMonth("201501");
        Assert.assertEquals(str, "201502");

        str = DateTimeUtil.plusOneMonth("201509");
        Assert.assertEquals(str, "201510");

        str = DateTimeUtil.plusOneMonth("201509");
        Assert.assertEquals(str, "201510");

        str = DateTimeUtil.plusOneMonth("201512");
        Assert.assertEquals(str, "201601");
    }

    @Test
    public void minusOneMonth() {
        String str = DateTimeUtil.minusOneMonth("201502");
        Assert.assertEquals(str, "201501");

        str = DateTimeUtil.minusOneMonth("201601");
        Assert.assertEquals(str, "201512");

        str = DateTimeUtil.minusOneMonth("201512");
        Assert.assertEquals(str, "201511");

        str = DateTimeUtil.minusOneMonth("201510");
        Assert.assertEquals(str, "201509");

        str = DateTimeUtil.minusOneMonth("201501");
        Assert.assertEquals(str, "201412");
    }

    @Test
    public void periodByMonth() {
        Assert.assertEquals(-13, DateTimeUtil.periodByMonth("201507", "201406"));
        Assert.assertEquals(-12, DateTimeUtil.periodByMonth("201507", "201407"));
        Assert.assertEquals(-11, DateTimeUtil.periodByMonth("201507", "201408"));
        Assert.assertEquals(-1, DateTimeUtil.periodByMonth("201507", "201506"));
        Assert.assertEquals(0, DateTimeUtil.periodByMonth("201506", "201506"));
        Assert.assertEquals(1, DateTimeUtil.periodByMonth("201506", "201507"));
        Assert.assertEquals(11, DateTimeUtil.periodByMonth("201408", "201507"));
        Assert.assertEquals(12, DateTimeUtil.periodByMonth("201407", "201507"));
        Assert.assertEquals(13, DateTimeUtil.periodByMonth("201406", "201507"));

    }

    @Test
    public void thisMonthLastMonth() {
        String lastMonth = DateTimeUtil.makeLastMonth();
        Assert.assertEquals(DateTimeUtil.plusOneMonth(lastMonth), DateTimeUtil.makeThisMonth());
    }

}
