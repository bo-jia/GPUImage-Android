package com.gpuimage.appdemo.model;

import com.gpuimage.appdemo.base.BaseFragment;

/**
 * @author cginechen
 * @date 2016-10-21
 */
public class ItemDescription {
    private Class<? extends BaseFragment> mKitDemoClass;
    private String mKitName;
    private String mKitDetailDescription;
    private int mIconRes;

    public ItemDescription(Class<? extends BaseFragment> kitDemoClass, String kitName){
        this(kitDemoClass, kitName, 0);
    }

    public ItemDescription(Class<? extends BaseFragment> kitDemoClass, String kitName, int iconRes) {
        mKitDemoClass = kitDemoClass;
        mKitName = kitName;
        mIconRes = iconRes;
    }

    public ItemDescription(Class<? extends BaseFragment> kitDemoClass, String kitName,
                           String kitDetailDescription, int iconRes) {
        mKitDemoClass = kitDemoClass;
        mKitName = kitName;
        mKitDetailDescription = kitDetailDescription;
        mIconRes = iconRes;
    }

    public void setItemDetailDescription(String kitDetailDescription) {
        mKitDetailDescription = kitDetailDescription;
    }

    public Class<? extends BaseFragment> getDemoClass() {
        return mKitDemoClass;
    }

    public String getName() {
        return mKitName;
    }

    public String getItemDetailDescription() {
        return mKitDetailDescription;
    }

    public int getIconRes() {
        return mIconRes;
    }
}
