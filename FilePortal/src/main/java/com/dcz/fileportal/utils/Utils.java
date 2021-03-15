package com.dcz.fileportal.utils;


import com.dcz.fileportal.exceptions.InvalidTimeException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Utils {

    /**
     * Get the MD5 digest.
     *
     * @param source The source string.
     * @return The MD5 digest.
     */
    public static String md5(String source) {
        if (source == null) return null;
        byte[] sourceBytes = source.getBytes();
        String result = null;
        try {
            MessageDigest md = MessageDigest
                    .getInstance("MD5");
            md.update(sourceBytes);
            byte[] tmp = md.digest();
            result = byteArrayToHexString(tmp);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Create md5 hash fro the file.
     *
     * @param file File to upload.
     * @return MD5 hash.
     */
    public static String md5(File file) {
        if (file == null) return null;
        try {
            InputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            MessageDigest complete = MessageDigest.getInstance("MD5");
            int numRead;
            do {
                numRead = fis.read(buffer);
                if (numRead > 0) {
                    complete.update(buffer, 0, numRead);
                }
            } while (numRead != -1);
            fis.close();
            return byteArrayToHexString(complete.digest());
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Convert a byte array to a hex string.
     *
     * @param bytes The byte array to be converted.
     * @return The hex string.
     */
    private static String byteArrayToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (int aByte : bytes) {
            int byteVal = aByte;
            if (byteVal < 0) {
                byteVal += 256;
            }
            String hexCode = Integer.toHexString(byteVal);
            if (hexCode.length() % 2 == 1) {
                hexCode = "0" + hexCode;
            }
            sb.append(hexCode);
        }
        return sb.toString();
    }

    /**
     * Convert date string to timestamp.
     *
     * @param date The source date string.
     * @return Converted timestamp(milliseconds).
     */
    public static long dateStr2Timestamp(String date) throws InvalidTimeException {
        SimpleDateFormat sdf = new SimpleDateFormat(
                "yyyy-MM-dd hh:mm:ss", Locale.getDefault());
        Date formattedDate;
        try {
            formattedDate = sdf.parse(date);
        } catch (ParseException e) {
            throw new InvalidTimeException();
        }
        if (formattedDate == null) {
            throw new InvalidTimeException();
        }
        return formattedDate.getTime();
    }

    /**
     * Calculate the percent of file transferring.
     *
     * @param current The transferred size.
     * @param sum     Total size.
     * @return Transferred percent.
     */
    public static int getTransferPercent(long current, long sum) {
        float rate = current * 1.0f / sum;
        String decimalBit = "#.##";
        DecimalFormat format = new DecimalFormat(decimalBit);
        format.setRoundingMode(RoundingMode.DOWN);
        return (int) (Float.parseFloat(format.format(rate)) * 100);
    }

   /* public static <T> T fromJson(Gson gson, String json) {
        Type type = getClass().getGenericSuperclass();
        Type[] t = ((ParameterizedType) type).getActualTypeArguments();
        Type ty = new ParameterizedTypeImpl(t, JsonModel.class);
    }*/

}
