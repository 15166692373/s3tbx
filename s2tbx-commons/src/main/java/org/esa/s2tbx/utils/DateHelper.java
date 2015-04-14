package org.esa.s2tbx.utils;

import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.util.logging.BeamLogManager;

import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Utility class for date manipulation
 *
 * @author Cosmin Cara
 */
public class DateHelper {

    /**
     * Computes the median (average) of two dates.
     * This method handles the possible overflow that can occur.
     *
     * @param startDate The first date
     * @param endDate   The second date
     * @return  The date between the two input dates
     */
    public static Date average(Date startDate, Date endDate) {
        Date averageDate = null;
        if (startDate != null && endDate != null) {
            BigInteger averageMillis = BigInteger.valueOf(startDate.getTime())
                                                 .add(BigInteger.valueOf(endDate.getTime()))
                                                 .divide(BigInteger.valueOf(2L));
            averageDate = new Date(averageMillis.longValue());
        }
        return averageDate;
    }

    /**
     * Computes the median (average) of two <code>ProductData.UTC</code> data structures.
     *
     * @param startDate The first date
     * @param endDate   The second date
     * @return  The date between the two input dates
     */
    public static ProductData.UTC average(ProductData.UTC startDate, ProductData.UTC endDate) {
        ProductData.UTC average = null;
        if (startDate != null && endDate != null) {
            BigInteger averageMillis = BigInteger.valueOf(startDate.getAsDate().getTime()).add(BigInteger.valueOf(endDate.getAsDate().getTime())).divide(BigInteger.valueOf(2L));
            Date averageDate = new Date(averageMillis.longValue());
            average = ProductData.UTC.create(averageDate, 0L);
        }
        return average;
    }

    /**
     * Utility method for returning a <code>ProductData.UTC</code> date from a string
     * using the given date format.
     * Why not using <code>ProductData.UTC.parse(text, pattern)</code> method?
     * Because it errors in the case of a format like dd-MM-yyyy'T'HH:mm:ss.SSSSSS (which should be
     * perfectly fine).
     * @param stringData    The string to be converted into a date
     * @param dateFormat    The format of the string date
     * @return  The UTC date representation.
     */
    public static ProductData.UTC parseDate(String stringData, String dateFormat) {
        ProductData.UTC parsedDate = null;
        if (stringData != null) {
            try {
                if (stringData.endsWith("Z"))
                    stringData = stringData.substring(0,stringData.length() - 1);
                if (!stringData.contains("."))
                    stringData = stringData + ".000000";
                String microseconds = stringData.substring(stringData.indexOf(".") + 1);
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
                simpleDateFormat.setCalendar(Calendar.getInstance(TimeZone.getTimeZone("UTC")));
                Date date = simpleDateFormat.parse(stringData);
                parsedDate = ProductData.UTC.create(date, Long.parseLong(microseconds));
            } catch (ParseException e) {
                BeamLogManager.getSystemLogger().warning(String.format("Date not in expected format. Found %s, expected %s",
                                                                       stringData,
                                                                       dateFormat));
            }
        }
        return parsedDate;
    }
}
