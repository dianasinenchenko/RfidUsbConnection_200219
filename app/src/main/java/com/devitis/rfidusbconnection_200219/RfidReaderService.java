package com.devitis.rfidusbconnection_200219;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Diana on 20.02.2019.
 */

public class RfidReaderService {

    private String markerId;
    private String markerType;
    private Map<String, String> markerData = new HashMap<String, String>();


    public RfidReaderService(String markerId, String markerType, Map<String, String> markerData) {
        this.markerId = markerId;
        this.markerType = markerType;
        this.markerData = markerData;
    }

    public String getMarkerId() {
        return markerId;
    }

    public void setMarkerId(String markerId) {
        this.markerId = markerId;
    }

    public Map<String, String> getMarkerData() {
        return markerData;
    }

    public void setMarkerData(Map<String, String> markerData) {
        this.markerData = markerData;
    }

    public String getMarkerType() {
        return markerType;
    }

    public void setMarkerType(String markerType) {
        this.markerType = markerType;
    }

    public void foundMarkerEvent(String markerId, String markerType, Map<String, String> markerData) {

    }

}
