package com.gpuimage.appdemo;


import com.gpuimage.appdemo.base.BaseFragment;
import com.gpuimage.appdemo.fragment.TestMovieReaderFragment;
import com.gpuimage.appdemo.model.QDItemDescription;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author cginechen
 * @date 2016-10-21
 */
public class QDDataManager {
    private static QDDataManager _sInstance;
    private HashMap<Class<? extends BaseFragment>, QDItemDescription> mComponentsNames;
    private HashMap<Class<? extends BaseFragment>, QDItemDescription> mUtilNames;
    private HashMap<Class<? extends BaseFragment>, QDItemDescription> mLabNames;

    public QDDataManager() {
        initComponentsDesc();
        initUtilDesc();
        initLabDesc();
    }

    public static QDDataManager getInstance() {
        if (_sInstance == null) {
            _sInstance = new QDDataManager();
        }
        return _sInstance;
    }

    /**
     * Components
     */
    private void initComponentsDesc() {
        mComponentsNames = new HashMap<>();
        mComponentsNames.put(TestMovieReaderFragment.class, TestMovieReaderFragment.mQDItemDescription);
    }

    /**
     * Helper
     */
    private void initUtilDesc() {
        mUtilNames = new HashMap<>();
        mUtilNames.put(TestMovieReaderFragment.class, TestMovieReaderFragment.mQDItemDescription);
    }

    /**
     * Lab
     */
    private void initLabDesc() {
        mLabNames = new HashMap<>();
        mLabNames.put(TestMovieReaderFragment.class, TestMovieReaderFragment.mQDItemDescription);
    }

    public QDItemDescription getDescription(Class<? extends BaseFragment> cls) {
        QDItemDescription itemDescription = null;
        if ((itemDescription = mComponentsNames.get(cls)) != null) {
            return itemDescription;
        }
        if ((itemDescription = mLabNames.get(cls)) != null) {
            return itemDescription;
        }
        if ((itemDescription = mUtilNames.get(cls)) != null) {
            return itemDescription;
        }
        return null;
    }

    public String getName(Class<? extends BaseFragment> cls) {
        QDItemDescription itemDescription = getDescription(cls);
        if (itemDescription == null) {
            return null;
        }
        return itemDescription.getName();
    }

    public List<QDItemDescription> getComponentsDescriptions() {
        List<QDItemDescription> list = new ArrayList<>();
        for (Map.Entry entry : mComponentsNames.entrySet()) {
            list.add((QDItemDescription) entry.getValue());
        }
        return list;
    }

    public List<QDItemDescription> getUtilDescriptions() {
        List<QDItemDescription> list = new ArrayList<>();
        for (Map.Entry entry : mUtilNames.entrySet()) {
            list.add((QDItemDescription) entry.getValue());
        }
        return list;
    }

    public List<QDItemDescription> getLabDescriptions() {
        List<QDItemDescription> list = new ArrayList<>();
        for (Map.Entry entry : mLabNames.entrySet()) {
            list.add((QDItemDescription) entry.getValue());
        }
        return list;
    }
}
