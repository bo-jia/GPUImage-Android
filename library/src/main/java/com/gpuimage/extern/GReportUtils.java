package com.gpuimage.extern;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by j on 25/4/2018.
 */

public class GReportUtils {

    public static Context applicationContext;

    public enum Type {
        UnhandledColorFormat,
        ExceptionOfCreateDecoder,
        ExceptionOfConfigureCodec,
        ExceptionOfAudioTrack,
    }

    public static void report(Exception e, String info, Type type) {
        switch (type) {
            case UnhandledColorFormat:
                reportUnhandledColorFormat(info);
                break;
            case ExceptionOfCreateDecoder:
            case ExceptionOfConfigureCodec:
            case ExceptionOfAudioTrack:
                reportException(e, info, type);
                break;

        }
    }

    private static void reportUnhandledColorFormat(String info) {}

    private static void reportException(Exception e, String info, Type type) {}

}
