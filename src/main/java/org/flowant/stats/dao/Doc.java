package org.flowant.stats.dao;

import java.util.Map;

import org.bson.Document;

public class Doc extends Document {
    private static final long serialVersionUID = -6490108906467480756L;

    public static final String HS_CD = "hsCd";
    public static final String HS_CODE = "HSCode";
    public static final String YEAR_MONTH = "year";
    public static final String STATE_CD = "statCd";

    public static final String ID = "_id";
    public static final String EXP_DLR = "expDlr";
    public static final String IMP_DLR = "impDlr";
    public static final String EXP_WGT = "expWgt";
    public static final String IMP_WGT = "impWgt";
    public static final String BAL_PAY = "balPayments";
    public static final String DESC_NATION = "statCdCntnKor1";
    public static final String DESC_KOR = "statKor";

    public static final String DESC_6_UNIT = "세번6단위품명";
    public static final String DESC_10_UNIT = "세번10단위품명";

    public static final String CORP_NAME = "기업명";

    public static final String CAGR = "cagr";

    public Doc() {
        super();
    }

    public Doc(final String key, final Object value) {
        super(key, value);
    }

    public Doc(final Map<String, Object> map) {
        super(map);
    }

    public Doc append(final String key, final Object value) {
        super.append(key, value);
        return this;
    }

    public String getHsCd() {
        return getString(HS_CD);
    }

    public String getYm() {
        return getString(YEAR_MONTH);
    }

    public Long getExpDlr() {
        return getLong(EXP_DLR);
    }

    public String getNationCd() {
        return getString("국가코드");
    }

    public String getStateCd() {
        return getString(STATE_CD);
    }

    public String getDesc6Unit() {
        return getString(DESC_6_UNIT);
    }

    public String getDesc10Unit() {
        return getString(DESC_10_UNIT);
    }

    public String getCorpName() {
        return getString(CORP_NAME);
    }

    public Double getCagr() {
        return getDouble(CAGR);
    }

    public boolean isFiniteCagr() {
        return Double.isFinite(getCagr());
    }

}
