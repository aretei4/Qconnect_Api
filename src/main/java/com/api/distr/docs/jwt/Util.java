package com.api.distr.docs.jwt;

import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class Util {
	
	/**
     * Converts a date string to java.sql.Date
     *
     * @param dateStr  the date string to convert
     * @param format   the expected date format (e.g. "yyyy-MM-dd" or "dd-MM-yyyy")
     * @return         java.sql.Date or null if parsing fails
     */
    public static Date toSqlDate(String dateStr, String format) {
     
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            sdf.setLenient(false); // strict parsing
            java.util.Date utilDate = sdf.parse(dateStr);
            return new Date(utilDate.getTime());
        } catch (ParseException e) {
            System.err.println("Invalid date format for value: " + dateStr + " (expected: " + format + ")");
            return new Date(System.currentTimeMillis());
        }
    }

}
