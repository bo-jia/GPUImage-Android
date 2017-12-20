package com.gpuimage.appdemo.fragment.home;

import android.content.Context;

import com.gpuimage.appdemo.QDDataManager;

/**
 * @author cginechen
 * @date 2016-10-20
 */

public class HomeLabController extends HomeController {

    public HomeLabController(Context context) {
        super(context);
    }

    @Override
    protected String getTitle() {
        return "Lab";
    }

    @Override
    protected ItemAdapter getItemAdapter() {
        return new ItemAdapter(getContext(), QDDataManager.getInstance().getLabDescriptions());
    }
}
