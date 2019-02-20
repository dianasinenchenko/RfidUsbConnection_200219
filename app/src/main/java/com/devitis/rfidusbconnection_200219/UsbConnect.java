package com.devitis.rfidusbconnection_200219;

import android.app.Application;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.LinkedList;

import static java.lang.Thread.sleep;

/**
 * Created by Diana on 20.02.2019.
 */

public class UsbConnect extends Application {

    public static final String TAG = "UsbControl";
    private static final boolean DEBUG = false;

    private static UsbConnect usbConnect;
    private static UsbDevice usbDevice;
    private static UsbInterface usbInterface;
    private static UsbDeviceConnection usbDeviceConnection;
    private static UsbEndpoint usbEndpointOut;
    private static UsbEndpoint usbEndpointIn;

//    0:Gen2, 1:Gen2+RSSI, 2:ISO6B
    public static int tagMode;
    public static boolean stop;
    private static Thread thread;
    private static final int DATA_LENGTH = 64;
    private static ByteBuffer usbDataBuffer = ByteBuffer.allocate(DATA_LENGTH);
    private final LinkedList<UsbRequest> usbInRequestPool = new LinkedList<UsbRequest>();

    public static UsbConnect newInstance() {
        usbConnect = new UsbConnect();
        return usbConnect;
    }

    public static UsbConnect getInstance() {
        return usbConnect;
    }

    public UsbDevice getUsbDevice() {
        return usbDevice;
    }

    public int sendCmd(byte[] message, int length) {
        int sendBytes = 0;
        synchronized (this) {
            if (usbDeviceConnection != null) {
//                Performs a control transaction on endpoint zero for this device.
                sendBytes = usbDeviceConnection.controlTransfer(
                        0x21, 0x09, 0x00, 0x00, message, length, 2000
                );
            }
        }
        return sendBytes;
    }

    private UsbRequest getInRequest() {
        synchronized (usbInRequestPool) {
            if (usbInRequestPool.isEmpty()) {
                if (DEBUG) Log.d(TAG, "pool is empty");
                UsbRequest request = new UsbRequest();
                request.initialize(usbDeviceConnection, usbEndpointIn);
                return request;
            } else {
                return usbInRequestPool.removeFirst();
            }
        }
    }

    public byte[] getResponse() {
        UsbRequest request = getInRequest();
        request.queue(usbDataBuffer, DATA_LENGTH);
        if (usbDataBuffer.hasArray()) {
            if (DEBUG) Log.d(TAG, "buffer have data");
            return usbDataBuffer.array();
        }
        return null;
    }

    public void setTagMode(int tagmode) {
        tagMode = tagmode;
    }

    public int getTagMode() {
        return tagMode;
    }

    private void sleep(int millisecond) {
        try {
            Thread.sleep(millisecond);
        } catch (InterruptedException e) {
        }
    }


    private Thread startThread() {
        if (DEBUG) Log.d(TAG, "start Thread");
        stop = false;
        thread = new Thread(new NewRunnable());
        thread.start();
        return thread;
    }

    private void stopThread() {
        stop = true;
        try {
            thread.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void clearBuffer() {
        sleep(1500);
        getResources();
        synchronized (usbInRequestPool) {
            usbInRequestPool.clear();
        }
        getResources();
    }

    public boolean setUsbInstance(UsbManager manager, UsbDevice device) throws IllegalAccessException {
        if (device != null) {
            if (usbDeviceConnection != null) {
                if (usbInterface != null) {
                    usbDeviceConnection.releaseInterface(usbInterface);
                    usbInterface = null;
                }
                usbDeviceConnection.close();
                usbDeviceConnection = null;
                usbDevice = null;
            }
            UsbInterface usbInterface1 = device.getInterface(0);
            UsbDeviceConnection connection = manager.openDevice(device);
            if (connection != null) {
                if (DEBUG) Log.d(TAG, "open succeeded");
//                 force -> true to disconnect kernel driver if necessary
//                 must be done before sending or receiving data on any UsbEndpoints belonging to the interface
                if (connection.claimInterface(usbInterface1, true)) {
                    if (DEBUG) Log.d(TAG, "claim interface succeeded");
                    usbDevice = device;
                    usbInterface = usbInterface1;
                    usbDeviceConnection = connection;


//                  Endpoints are the channels for sending and receiving data over USB
                    UsbEndpoint endpointOut = null;
                    UsbEndpoint endpointIn = null;

                    for (int i = 0; i < usbInterface1.getEndpointCount(); i++) {
                        UsbEndpoint endpoint = usbInterface1.getEndpoint(i);
//                    USB_DIR_OUT/USB_DIR_IN    is OUT/IN (host to device)
                        if (endpoint.getDirection() == UsbConstants.USB_DIR_OUT)
                            endpointOut = endpoint;
                        else if (endpoint.getDirection() == UsbConstants.USB_DIR_IN)
                            endpointIn = endpoint;
                    }
                    if (endpointOut == null || endpointIn == null) {
                        Log.e(TAG, "not all endpoints found ");
                        try {
                            throw new IllegalAccessException("not all endpoints found");
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }

                    }
                    usbEndpointOut = endpointOut;
                    usbEndpointIn = endpointIn;

                    startThread();
                    clearBuffer();

                    return true;

                } else {
                    Log.e(TAG, "claim interface falied");
                    connection.close();
                }
            } else {
                Log.e(TAG, "open falied");
            }
        } else {
            clearBuffer();
            stopThread();
            usbDeviceConnection = null;
            usbInterface = null;
            usbDevice = null;
        }
        return false;
    }


    private class NewRunnable implements Runnable {
        @Override
        public void run() {
            while (true) {
                if (stop) {
                    Log.d(TAG, "Stop Thread");
                    return;
                }

                UsbRequest request = usbDeviceConnection.requestWait();

                if (request == null)
                    break;

                request.setClientData(null);

                synchronized (usbInRequestPool) {
                    usbInRequestPool.add(request);
                }
            }
        }
    }

}
