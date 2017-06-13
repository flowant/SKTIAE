package org.flowant.util;

import static java.time.temporal.ChronoField.*;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

public class DateTimeUtil {

    protected static final DateTimeFormatter formatterYm;
    static {
        formatterYm = new DateTimeFormatterBuilder()
                .appendValue(YEAR, 4)
                .appendValue(MONTH_OF_YEAR, 2)
                .toFormatter();
    }

    public static YearMonth parse(String yearMonth) {
        return YearMonth.parse(yearMonth, formatterYm);
    }

    public static String makeYearMonthString(int year, int month) {
        return YearMonth.of(year, month).format(formatterYm);
    }

    public static String plusMonths(String yearMonth, long months) {
        YearMonth ym = YearMonth.parse(yearMonth, formatterYm);
        return ym.plusMonths(months).format(formatterYm);
    }

    public static String plusOneMonth(String yearMonth) {
        return plusMonths(yearMonth, 1);
    }

    public static String minusMonths(String yearMonth, long months) {
        YearMonth ym = YearMonth.parse(yearMonth, formatterYm);
        return ym.minusMonths(months).format(formatterYm);
    }

    public static String minusOneMonth(String yearMonth) {
        return minusMonths(yearMonth, 1);
    }

    public static String makeThisMonth() {
        return YearMonth.now().format(formatterYm);
    }

    public static String makeLastMonth() {
        return YearMonth.now().minusMonths(1).format(formatterYm);
    }

    public static String makeMonthsBefore(long i) {
        return YearMonth.now().minusMonths(i).format(formatterYm);
    }

    public static boolean isThisMonth(String yearMonth) {
        YearMonth ym = YearMonth.parse(yearMonth, formatterYm);
        return ym.equals(YearMonth.now());
    }

    public static int periodByMonth(String startYearMonth, String endYearMonth) {
        YearMonth endYm = parse(endYearMonth);
        YearMonth startYm = parse(startYearMonth);
        YearMonth period = endYm.minusYears(startYm.getYear()).minusMonths(startYm.getMonthValue());
        return period.getMonthValue() + period.getYear() * 12;
    }
}
