package com.gpuimage.appdemo.fragment.home;

import android.content.Context;

import com.gpuimage.appdemo.HomeDataManager;


/** 主界面，关于 QMUI Util 部分的展示。
 * Created by Kayo on 2016/11/21.
 */

public class HomeUtilController extends HomeController {

    public HomeUtilController(Context context) {
        super(context);
    }

    @Override
    protected String getTitle() {
        return "Helper";
    }

    @Override
    protected ItemAdapter getItemAdapter() {
        return new ItemAdapter(getContext(), HomeDataManager.getInstance().getUtilDescriptions());
    }
}
