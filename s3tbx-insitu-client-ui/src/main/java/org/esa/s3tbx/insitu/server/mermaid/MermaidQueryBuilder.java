package org.esa.s3tbx.insitu.server.mermaid;

import org.esa.s3tbx.insitu.server.QueryBuilder;
import org.esa.snap.core.util.StringUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Marco Peters
 */
class MermaidQueryBuilder extends QueryBuilder {

    private static final String PARAM_LON_MIN = "lonMin";
    private static final String PARAM_LAT_MIN = "latMin";
    private static final String PARAM_LON_MAX = "lonMax=";
    private static final String PARAM_LAT_MAX = "latMax";
    private static final String PARAM_START_DATE = "startDate";
    private static final String PARAM_STOP_DATE = "stopDate";
    private static final String PARAM_PARAM = "param";
    private static final String PARAM_CAMPAIGN = "campaign";
    private static final String PARAM_SHIFT = "shift";
    private static final String PARAM_LIMIT = "limit";
    private static final String PARAM_COUNT_ONLY = "count_only";
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    public String createQuery() {
        List<String> queryParams= new ArrayList<>();

        if(lonMin != null) {
            queryParams.add(PARAM_LON_MIN + "=" + lonMin);
        }
        if(latMin != null) {
            queryParams.add(PARAM_LAT_MIN + "=" + latMin);
        }
        if(lonMax != null) {
            queryParams.add(PARAM_LON_MAX + lonMax);
        }
        if(latMax != null) {
            queryParams.add(PARAM_LAT_MAX + "=" + latMax);
        }
        if(startDate != null) {
            queryParams.add(PARAM_START_DATE + "=" + DATE_FORMAT.format(startDate.getAsDate()));
        }
        if(stopDate != null) {
            queryParams.add(PARAM_STOP_DATE + "=" + DATE_FORMAT.format(stopDate.getAsDate()));
        }
        if(param != null) {
            queryParams.add(PARAM_PARAM + "=" + StringUtils.arrayToCsv(param));
        }
        if(campaign != null) {
            queryParams.add(PARAM_CAMPAIGN + "=" + campaign);
        }
        if(shift > 0) {
            queryParams.add(PARAM_SHIFT + "=" + shift);
        }
        if(limit > 0) {
            queryParams.add(PARAM_LIMIT + "=" + limit);
        }
        if(countOnly) {
            queryParams.add(PARAM_COUNT_ONLY);
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("/").append(subject).append("?");

        for (int i = 0; i < queryParams.size(); i++) {
            String queryParam = queryParams.get(i);
            sb.append(queryParam);
            if(i < queryParams.size()-1) {
                sb.append("&");
            }
        }

        return sb.toString();
    }
}
