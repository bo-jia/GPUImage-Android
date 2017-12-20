package com.gpuimage.appdemo.utils;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class StringUtils {
    private static final String TAG = StringUtils.class.getSimpleName();

    /*
     * 字符串是否为空。
     */
    public static boolean isEmpty(String str) {
        String string = str;
        if (string == null) {
            return true;
        }
        string = string.trim();
        return TextUtils.isEmpty(string) || str.equalsIgnoreCase("null");
    }

//	/*
//	 * 字符串是否为空。
//	 */
//	public static boolean isEmptyWithUnknown(String str) {
//		String string = str;
//		if (string == null) {
//			return true;
//		}
//		string = string.trim();
//		return TextUtils.isEmpty(string) || string.equalsIgnoreCase("null") || isUnknownString(string);
//	}

    /*
     * 字符串是否相等。
     */
    public static boolean equals(String str1, String str2) {
        if (str1 == null || str2 == null) {
            return (str1 == str2);
        }
        return str2.equals(str1);
    }

    public static long parseLong(String str) {
        return isEmpty(str) ? 0 : Long.parseLong(str);
    }

    public static String escapeSql(String str) {
        if (str == null) {
            return null;
        }
        return replace(str, "'", "''");
    }

    public static String escapeFileName(String str) {
        if (str == null) {
            return null;
        }

        /** 非法字符包括：/\:*?"<>| */
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '/' || c == '\\' || c == ':' || c == '*' || c == '?' || c == '"' || c == '<' || c == '>' || c == '|') {
                continue;
            } else {
                builder.append(c);
            }
        }
        String ret = builder.toString();
        // 替换可能出现的\t
        if (ret != null) {
            ret = ret.replaceAll("\t", "");
        }
        return ret;
    }

    public static String replace(String text, String searchString, String replacement) {
        return replace(text, searchString, replacement, -1);
    }

    public static String replace(String text, String searchString, String replacement, int max) {
        if (isEmpty(text) || isEmpty(searchString) || replacement == null || max == 0) {
            return text;
        }
        int start = 0;
        int end = text.indexOf(searchString, start);
        if (end == -1) {
            return text;
        }
        int replLength = searchString.length();
        int increase = replacement.length() - replLength;
        increase = (increase < 0 ? 0 : increase);
        increase *= (max < 0 ? 16 : (max > 64 ? 64 : max));
        StringBuffer buf = new StringBuffer(text.length() + increase);
        while (end != -1) {
            buf.append(text.substring(start, end)).append(replacement);
            start = end + replLength;
            if (--max == 0) {
                break;
            }
            end = text.indexOf(searchString, start);
        }
        buf.append(text.substring(start));
        return buf.toString();
    }

    public static int toInt(String str, int defaultValue) {
        if (str == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException nfe) {
            return defaultValue;
        }
    }

    /**
     * @param pathA
     * @param pathB
     * @return
     */
    public static final boolean isPathChange(String pathA, String pathB) {
        boolean isPathChange = false;
        if (StringUtils.isEmpty(pathA)) {
            isPathChange = !StringUtils.isEmpty(pathB);
        } else {
            isPathChange = !pathA.equals(pathB);
        }
        return isPathChange;
    }


    public static String getTimeString(long time) {
        if (time <= 0) {
            return "0:00";
        }

        int min = (int) (time / 60);
        int second = (int) (time % 60);
        if (second < 10) {
            return min + ":0" + second;
        }
        return min + ":" + second;
    }


    public static boolean isABC(char c) {
        if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
            return true;
        }

        return false;
    }

    /**
     * 将首字母转换为合法的排序字母
     *
     * @param c 输入字符
     * @return 大写字母或"|"
     */
    public static char formatFirstLetter(char c) {
        if (c >= 'A' && c <= 'Z') {
            return c;
        }

        if (c >= 'a' && c <= 'z') {
            return (char) (c + ('A' - 'a'));
        }

        return '|';
    }

    /**
     * 获取字符串的长度，中文占一个字符,英文数字占半个字符
     *
     * @param value 指定的字符串
     * @return 字符串的长度
     */
    public static double chineseLength(String value) {
        if (TextUtils.isEmpty(value))
            return 0;

        double valueLength = 0;
        String chinese = "[\u4e00-\u9fa5]";
        // 获取字段值的长度，如果含中文字符，则每个中文字符长度为2，否则为1
        for (int i = 0; i < value.length(); i++) {
            // 获取一个字符
            String temp = value.substring(i, i + 1);
            // 判断是否为中文字符
            if (temp.matches(chinese)) {
                // 中文字符长度为1
                valueLength += 1;
            } else {
                // 其他字符长度为0.5
                valueLength += 0.5;
            }
        }
        //进位取整
        return Math.ceil(valueLength);
    }

    /**
     * 获得最大长度的汉字字串
     *
     * @param source
     * @param maxLength
     * @return
     */
    public static String getChineseStringByMaxLength(String source, int maxLength) {
        String cutString = "";
        double tempLength = 0;
        for (int i = 0; i < source.length(); i++) {
            String temp = source.substring(i, i + 1);
            String chinese = "[\u4e00-\u9fa5]";
            // 判断是否为中文字符
            if (temp.matches(chinese)) {
                // 中文字符长度为1
                tempLength += 1;
            } else {
                // 其他字符长度为0.5
                tempLength += 0.5;
            }
            cutString += temp;
            if (Math.ceil(tempLength) == maxLength) {
                break;
            }
        }
        return cutString;
    }

    public static String trim(String str) {
        if (isEmpty(str)) {
            return null;
        }
        return str.trim();
    }

    /**
     * [A-z0-9]{2,} 任意字母和数字组合，并长度大于等于2，必选
     * <p>
     * [@] 必选
     * <p>
     * [a-z0-9]{2,} 任意小写字母和数字组合，并长度大于等于2，必选
     * <p>
     * [.] 必选
     * <p>
     * \p{Lower}{2,} 任意小写字母，并长度大于等于2，必选
     * <p>
     *
     * @param emailString
     * @return
     */
    public static boolean validateEmailAddress(String emailString) {
        String format = "[A-z0-9]{2,}[@][a-z0-9]{2,}[.]\\p{Lower}{2,}";
        if (emailString.matches(format)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 匹配格式： 11位手机号码 3-4位区号，7-8位直播号码，1－4位分机号 如：12345678901、1234-12345678-1234
     *
     * @param phoneString
     * @return
     */
    public static boolean validatePhoneNumber(String phoneString) {
        String format = "((\\d{11})|^((\\d{7,8})|(\\d{4}|\\d{3})-(\\d{7,8})|(\\d{4}|\\d{3})-(\\d{7,8})-(\\d{4}|\\d{3}|\\d{2}|\\d{1})|(\\d{7,8})-(\\d{4}|\\d{3}|\\d{2}|\\d{1}))$)";
        if (phoneString.matches(format)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 5-12位的数字
     *
     * @param qqString
     * @return
     */
    public static boolean validateQQNumber(String qqString) {
        String format = "^\\d{5,12}$";
        if (qqString.matches(format)) {
            return true;
        } else {
            return false;
        }
    }

    public static String getIdScopeString(List<Integer> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }

        StringBuffer sb = new StringBuffer();
        for (Integer id : list) {
            sb.append("'").append(id).append("',");
        }

        return sb.substring(0, sb.length() - 1);
    }

    public static void appendTwoDigits(String str, int n) {
        if (n < 10) {
            str = str + "0";
        }
        return;
    }

    public static float getFloatValue(String string) {
        float ret = 0;
        try {
            if (isEmpty(string)) {
                return 0;
            }
            ret = Float.parseFloat(string);
            return ret;
        } catch (NumberFormatException e1) {
            LogUtil.e("getFloatValue NumberFormatException", e1);
            return ret;
        } catch (Exception e) {
            LogUtil.e("getFloatValue", e);
        }
        return ret;
    }

    public static int getIntegerValue(String string) {
        int ret = 0;
        try {
            if (isEmpty(string)) {
                return 0;
            }
            ret = Integer.parseInt(string);
            return ret;
        } catch (NumberFormatException e1) {
            LogUtil.e("getIntegerValue NumberFormatException", e1);
            return ret;
        } catch (Exception e) {
            LogUtil.e("getIntegerValue", e);
        }
        return ret;
    }

    public static long getLongValue(String string) {
        long ret = 0;
        try {
            if (isEmpty(string)) {
                return 0;
            }

            ret = Long.parseLong(string);
            return ret;
        } catch (NumberFormatException e1) {
            LogUtil.e("getLongValue NumberFormatException", e1);
        } catch (Exception e) {
            LogUtil.e("getLongValue", e);
        }
        return ret;
    }

    public static String getListString(String[] values) {
        StringBuffer result = new StringBuffer("");

        if (values.length < 1) return "";
        result.append(values[0] + "");
        for (int i = 1; i < values.length; i++) {
            result.append("," + values[i]);
        }

        return result.toString();
    }

    public static String getListString(Iterator<?> iterator) {
        StringBuffer result = new StringBuffer("");

        if (iterator.hasNext()) result.append(iterator.next());
        while (iterator.hasNext()) {
            result.append("," + iterator.next());
        }

        return result.toString();
    }

    public static Set<Integer> getIntegerSetFromStringSplite(String value, String split) {
        Set<Integer> result = new HashSet<>();
        if (!isEmpty(value) && !isEmpty(split)) {
            String[] strs = value.split(split);
            if (strs != null && strs.length > 0)
                for (String item : strs) {
                    result.add(Integer.parseInt(item));
                }
        }
        return result;
    }

    public static Set<String> getStringSetFromStringSplite(String value, String split) {
        Set<String> result = new HashSet<>();
        if (!isEmpty(value) && !isEmpty(split)) {
            String[] strs = value.split(split);
            if (strs != null && strs.length > 0) {
                for (String item : strs) {
                    result.add(item);
                }
            }
        }
        return result;
    }

    public static ArrayList<String> getStringListFromSplit(String value, String split) {
        ArrayList<String> result = new ArrayList<>();
        if (!isEmpty(value) && !isEmpty(split)) {
            String[] strs = value.split(split);
            if (strs != null && strs.length > 0)
                for (String item : strs) {
                    result.add(item);
                }
        }
        return result;
    }

    public static ArrayList<Float> getFloatArrayFromSplit(String value, String split) {
        ArrayList<Float> result = new ArrayList<>();
        if (!isEmpty(value) && !isEmpty(split)) {
            String[] strs = value.split(split);
            if (strs != null && strs.length > 0)
                for (String item : strs) {
                    try {
                        result.add(Float.parseFloat(item));
                    } catch (Exception e) {
                        LogUtil.e(TAG, "getFloatArrayFromSplit error: value:" + value);
                    }
                }
        }

        return result;
    }

    public static ArrayList<Double> getDoubleArrayFromSplit(String value, String split) {
        ArrayList<Double> result = new ArrayList<>();
        if (!isEmpty(value) && !isEmpty(split)) {
            String[] strs = value.split(split);
            if (strs != null && strs.length > 0)
                for (String item : strs) {
                    try {
                        result.add(Double.parseDouble(item));
                    } catch (Exception e) {
                        LogUtil.e(TAG, "getFloatArrayFromSplit error: value:" + value);
                    }
                }
        }

        return result;
    }

    public static ArrayList<Integer> getIntegerArrayFromSplit(String value, String split) {
        ArrayList<Integer> result = new ArrayList<>();
        if (!isEmpty(value) && !isEmpty(split)) {
            String[] strs = value.split(split);
            if (strs != null && strs.length > 0)
                for (String item : strs) {
                    try {
                        result.add(Integer.parseInt(item));
                    } catch (Exception e) {
                        LogUtil.e(TAG, "getFloatArrayFromSplit error: value:" + value);
                    }
                }
        }

        return result;
    }

    public static String getStrPartOf(String value, String split, int index) {
        String result = "";
        if (!isEmpty(value)) {
            String[] splitStr = value.split(split);
            if (index >= 0 && index < splitStr.length) {
                return splitStr[index];
            }
        }

        return result;
    }

    public static long getLongPartOf(String value, String split, int index) {
        long result = -1;
        try {
            if (!isEmpty(value)) {
                String[] splitStr = value.split(split);
                if (index >= 0 && index < splitStr.length) {
                    return Long.parseLong(splitStr[index]);
                }
            }
        } catch (Exception e) {
            LogUtil.e("getLongPartOf", e);
        }
        return result;
    }

    public static int getIntPartOf(String value, String split, int index) {
        int result = -1;
        try {
            if (!isEmpty(value)) {
                String[] splitStr = value.split(split);
                if (index >= 0 && index < splitStr.length) {
                    return Integer.parseInt(splitStr[index]);
                }
            }
        } catch (Exception e) {
            LogUtil.e("getLongPartOf", e);
        }
        return result;
    }

    public static String joinArray(Collection<?> objects, String split) {
        if (objects != null && objects.size() > 0) {
            StringBuilder builder = new StringBuilder("");
            for (Object item : objects) {
                if (builder.toString().equals(""))
                    builder.append(item.toString());
                else
                    builder.append(split).append(item.toString());
            }
            return builder.toString();
        }

        return null;
    }

    public static String joinInteger2Array(Collection<Integer> objects, String split) {
        if (objects != null && objects.size() > 0) {
            StringBuilder builder = new StringBuilder("");
            for (Object item : objects) {
                if (builder.toString().equals("")) {
                    builder.append(item.toString());
                } else {
                    builder.append(split).append(item.toString());
                }
            }
            return builder.toString();
        }

        return null;
    }

    public static String getByteHexString(byte a) {
        //得到高四位
        int hi = ((a >> 4) & 0x0F);
        //得到低四位
        int lo = (a & 0x0F);
        char[] hex = new char[2];
        //如果高四位是大于9的，将其转换成字符a-z, 最后要将得到的转换成字符（char）类型，不然得到的是二进制数
        hex[0] = hi > 9 ? (char) (hi - 10 + 'a') : (char) (hi + '0');
        hex[1] = lo > 9 ? (char) (lo - 10 + 'a') : (char) (lo + '0');
        return new String(hex);
    }

    /**
     * 根据下载url获取资源名字,获取最后一个/后面的内容
     *
     * @param downloadUrl 资源下载的url
     * @return 资源名字
     */
    public static String getResNameFromUrl(String downloadUrl) {
        return downloadUrl.substring(downloadUrl.lastIndexOf("/") + 1, downloadUrl.length());
    }
}
