/*
 *  Copyright 1997-2011 teatrove.org
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.teatrove.tea.runtime;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.joda.time.DateTimeZone;
import org.joda.time.ReadableInstant;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.teatrove.trove.util.DecimalFormat;
import org.teatrove.trove.util.Pair;

/**
 * The default runtime context class that Tea templates get compiled to use.
 * All functions callable from a template are defined in the context. To add
 * more or override existing ones, do so when extending this class.
 *
 * @author Brian S O'Neill
 */
public abstract class DefaultContext extends Writer
    implements Context
{
    private static final String DEFAULT_NULL_FORMAT = "null";

    // Although the Integer.toString method keeps getting more optimized
    // with each release, it still isn't very fast at converting small values.
    private static final String[] INT_VALUES = {
         "0",  "1",  "2",  "3",  "4",  "5",  "6",  "7",  "8",  "9",
        "10", "11", "12", "13", "14", "15", "16", "17", "18", "19",
        "20", "21", "22", "23", "24", "25", "26", "27", "28", "29",
        "30", "31", "32", "33", "34", "35", "36", "37", "38", "39",
        "40", "41", "42", "43", "44", "45", "46", "47", "48", "49",
        "50", "51", "52", "53", "54", "55", "56", "57", "58", "59",
        "60", "61", "62", "63", "64", "65", "66", "67", "68", "69",
        "70", "71", "72", "73", "74", "75", "76", "77", "78", "79",
        "80", "81", "82", "83", "84", "85", "86", "87", "88", "89",
        "90", "91", "92", "93", "94", "95", "96", "97", "98", "99",
    };

    private static final int FIRST_INT_VALUE = 0;
    private static final int LAST_INT_VALUE = 99;

    private static Map<Locale, Locale> cLocaleCache;
    private static Map<Object, DecimalFormat> cDecimalFormatCache;

    static {
        cLocaleCache =
            Collections.synchronizedMap(new HashMap<Locale, Locale>(7));

        cDecimalFormatCache =
            Collections.synchronizedMap(new HashMap<Object, DecimalFormat>(47));
    }

    private Locale mLocale;
    private String mNullFormat = DEFAULT_NULL_FORMAT;
    private DecimalFormat mDecimalFormat;

    // Fields used with date formatting.
    private DateTimeFormatter mDateTimeFormatter;
    private DateTimeZone mDateTimeZone;
    private String mDateTimePattern;

    public DefaultContext() {
    }

    /**
     * @hidden
     */
    public void write(int c) throws IOException {
        try {
            print(String.valueOf((char)c));
        }
        catch (Exception e) {
            if (e instanceof IOException) {
                throw (IOException)e;
            }
            if (e instanceof RuntimeException) {
                throw (RuntimeException)e;
            }
            throw new IOException(e.toString());
        }
    }

    /**
     * @hidden
     */
    public void write(char[] cbuf, int off, int len) throws IOException {
        try {
            if (cbuf == null) {
                print(mNullFormat);
            }
            else {
                print(new String(cbuf, off, len));
            }
        }
        catch (Exception e) {
            if (e instanceof IOException) {
                throw (IOException)e;
            }
            if (e instanceof RuntimeException) {
                throw (RuntimeException)e;
            }
            throw new IOException(e.toString());
        }
    }

    /**
     * @hidden
     */
    public void flush() throws IOException {
    }

    /**
     * @hidden
     */
    public void close() throws IOException {
    }

    /**
     * Method that is the runtime receiver. Implementations should call one
     * of the toString methods when converting this object to a string.
     * <p>
     * NOTE:  This method should <b>not</b> be called directly within a
     * template.
     *
     * @see org.teatrove.tea.compiler.Compiler#getRuntimeReceiver
     * @hidden
     */
    public abstract void print(Object obj) throws Exception;

    /**
     * @hidden
     */
    public void print(Date date) throws Exception {
        if (date == null) {
            write(mNullFormat);
        }
        else {
            if (mDateTimeFormatter == null) {
                // Force formatter and time zone to be set.
                dateFormat(null);
            }
            // DateTimeZone already set when formatter was created :-)
            mDateTimeFormatter.printTo(this, date.getTime());
        }
    }

    /**
     * @hidden
     */
    public void print(ReadableInstant instant) throws Exception {
        if (instant == null) {
            write(mNullFormat);
        }
        else {
            if (mDateTimeFormatter == null) {
                // Force formatter and time zone to be set.
                dateFormat(null);
            }
            // DateTimeZone already set when formatter was created :-)
            mDateTimeFormatter.printTo(this, instant.getMillis());
        }
    }

    /**
     * @hidden
     */
    public void print(Number n) throws Exception {
        print(toString(n));
    }

    /**
     * @hidden
     */
    public void print(int n) throws Exception {
        print(toString(n));
    }

    /**
     * @hidden
     */
    public void print(float n) throws Exception {
        print(toString(n));
    }

    /**
     * @hidden
     */
    public void print(long n) throws Exception {
        print(toString(n));
    }

    /**
     * @hidden
     */
    public void print(double n) throws Exception {
        print(toString(n));
    }

    /**
     * @hidden
     */
    public String toString(Object obj) {
        if (obj == null) {
            return mNullFormat;
        }
        else if (obj instanceof String) {
            return (String)obj;
        }
        else if (obj instanceof Date) {
            return toString((Date)obj);
        }
        else if (obj instanceof Number) {
            return toString((Number)obj);
        }
        else if (obj instanceof ReadableInstant) {
            return toString((ReadableInstant)obj);
        }
        else {
            String str = obj.toString();
            return (str == null) ? mNullFormat : str;
        }
    }

    /**
     * @hidden
     */
    public String toString(String str) {
        return (str == null) ? mNullFormat : str;
    }

    /**
     * @hidden
     */
    public String toString(Date date) {
        if (date == null) {
            return mNullFormat;
        }

        if (mDateTimeFormatter == null) {
            // Force formatter and time zone to be set.
            dateFormat(null);
        }

        // DateTimeZone already set when formatter was created :-)
        //return mDateTimeFormatter.print(date.getTime(), mDateTimeZone);
        return mDateTimeFormatter.print(date.getTime());
    }

    /**
     * @hidden
     */
    public String toString(ReadableInstant instant) {
        if (instant == null) {
            return mNullFormat;
        }

        if (mDateTimeFormatter == null) {
            // Force formatter and time zone to be set.
            dateFormat(null);
        }

        // DateTimeZone already set when formatter was created :-)
        return mDateTimeFormatter.print(instant.getMillis());
    }

    /**
     * @hidden
     */
    public String toString(Number n) {
        if (n == null) {
            return mNullFormat;
        }
        else if (mDecimalFormat == null) {
            if (n instanceof Integer) {
                return toString(((Integer)n).intValue());
            }
            else if (n instanceof Long) {
                return toString(((Long)n).longValue());
            }
            else {
                return n.toString();
            }
        }
        else {
            if (n instanceof Integer ||
                n instanceof AtomicInteger ||
                n instanceof Short ||
                n instanceof Byte) {
                return mDecimalFormat.format(n.intValue());
            }
            if (n instanceof Double ||
                n instanceof BigDecimal) {
                return mDecimalFormat.format(n.doubleValue());
            }
            if (n instanceof Float) {
                return mDecimalFormat.format(n.floatValue());
            }
            if (n instanceof Long ||
                n instanceof BigInteger ||
                n instanceof AtomicLong) {
                return mDecimalFormat.format(n.longValue());
            }
            return mDecimalFormat.format(n.doubleValue());
        }
    }

    /**
     * @hidden
     */
    public String toString(int n) {
        if (mDecimalFormat == null) {
            if (n <= LAST_INT_VALUE && n >= FIRST_INT_VALUE) {
                return INT_VALUES[n];
            }
            else {
                return Integer.toString(n);
            }
        }
        else {
            return mDecimalFormat.format(n);
        }
    }

    /**
     * @hidden
     */
    public String toString(float n) {
        return (mDecimalFormat == null) ? Float.toString(n) :
            mDecimalFormat.format(n);
    }

    /**
     * @hidden
     */
    public String toString(long n) {
        if (mDecimalFormat == null) {
            if (n <= LAST_INT_VALUE && n >= FIRST_INT_VALUE) {
                return INT_VALUES[(int)n];
            }
            else {
                return Long.toString(n);
            }
        }
        else {
            return mDecimalFormat.format(n);
        }
    }

    /**
     * @hidden
     */
    public String toString(double n) {
        return (mDecimalFormat == null) ? Double.toString(n) :
            mDecimalFormat.format(n);
    }

    public void setLocale(Locale locale) {
        if (locale == null) {
            mLocale = null;
            mDateTimeFormatter = null;
            mDecimalFormat = null;
        }
        else {
            synchronized (cLocaleCache) {
                Locale cached = cLocaleCache.get(locale);
                if (cached == null) {
                    cLocaleCache.put(locale, locale);
                }
                else {
                    locale = cached;
                }
            }

            mLocale = locale;
            dateFormat(null);
            numberFormat(null);
        }
    }

    public void setLocale(String language, String country) {
        setLocale(new Locale(language, country));
    }

    public void setLocale(String language, String country, String variant) {
        setLocale(new Locale(language, country, variant));
    }

    public java.util.Locale getLocale() {
        return mLocale;
    }

    public Locale[] getAvailableLocales() {
        return Locale.getAvailableLocales();
    }

    public void nullFormat(String format) {
        mNullFormat = (format == null) ? DEFAULT_NULL_FORMAT : format;
    }

    public String getNullFormat() {
        return mNullFormat;
    }

    public void dateFormat(String format) {
        dateFormat(format, null);
    }

    public void dateFormat(String format, String timeZoneID) {
        DateTimeZone zone;
        if (timeZoneID != null) {
            zone = DateTimeZone.forID(timeZoneID);
        }
        else {
            zone = DateTimeZone.getDefault();
        }
        mDateTimeZone = zone;

        /* --Original before joda upgrade
        DateTimeFormat dtFormat;
        if (mLocale == null) {
            dtFormat = DateTimeFormat.getInstance(zone); --orig
        }
        else {
            dtFormat = DateTimeFormat.getInstance(zone, mLocale);
        }

        if (format == null) {
            format = dtFormat.getPatternForStyle("LL"); --orig
        }*/

        if (format == null) {
            format = DateTimeFormat.patternForStyle("LL", mLocale);
        }
        DateTimeFormatter formatter = DateTimeFormat.forPattern(format).withZone(zone);
        if (mLocale != null) {
            formatter = formatter.withLocale(mLocale);
        }

        mDateTimeFormatter = formatter;
        mDateTimePattern = format;
    }

    public String getDateFormat() {
        if (mDateTimeFormatter == null) {
            // Force formatter and time zone to be set.
            dateFormat(null);
        }
        return mDateTimePattern;
    }

    public String getDateFormatTimeZone() {
        if (mDateTimeFormatter == null) {
            // Force formatter and time zone to be set.
            dateFormat(null);
        }
        DateTimeZone zone = mDateTimeZone;
        return zone == null ? null : zone.getID();
    }

    public TimeZone[] getAvailableTimeZones() {
        String[] IDs = TimeZone.getAvailableIDs();
        TimeZone[] zones = new TimeZone[IDs.length];
        for (int i=zones.length; --i >= 0; ) {
            zones[i] = TimeZone.getTimeZone(IDs[i]);
        }
        return zones;
    }

    public void numberFormat(String format) {
        numberFormat(format, null, null);
    }

    public void numberFormat(String format, String infinity, String NaN) {
        if (format == null && infinity == null && NaN == null) {
            if (mLocale == null) {
                mDecimalFormat = null;
            }
            else {
                mDecimalFormat = DecimalFormat.getInstance(mLocale);
            }
            return;
        }

        Object key = format;
        if (mLocale != null) {
            key = new Pair(key, mLocale);
        }

        if (infinity != null || NaN != null) {
            key = new Pair(key, infinity);
            key = new Pair(key, NaN);
        }

        if ((mDecimalFormat = cDecimalFormatCache.get(key)) == null) {

            mDecimalFormat = DecimalFormat.getInstance(format, mLocale);

            if (infinity != null) {
                mDecimalFormat = mDecimalFormat.setInfinity(infinity);
            }
            if (NaN != null) {
                mDecimalFormat = mDecimalFormat.setNaN(NaN);
            }

            cDecimalFormatCache.put(key, mDecimalFormat);
        }
    }

    public String getNumberFormat() {
        return mDecimalFormat == null ? null : mDecimalFormat.getPattern();
    }

    public String getNumberFormatInfinity() {
        return mDecimalFormat == null ? null : mDecimalFormat.getInfinity();
    }

    public String getNumberFormatNaN() {
        return mDecimalFormat == null ? null : mDecimalFormat.getNaN();
    }
    
}
