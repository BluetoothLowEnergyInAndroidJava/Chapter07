package tonyg.example.com.examplebleperipheral.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import tonyg.example.com.examplebleperipheral.ble.callbacks.BlePeripheralCallback;
import tonyg.example.com.examplebleperipheral.utilities.DataConverter;

/**
 * This class creates a local Bluetooth Peripheral
 *
 * @author Tony Gaitatzis backupbrain@gmail.com
 * @date 2016-03-06
 */
public class MyBlePeripheral {
    /** Constants **/
    private static final String TAG = MyBlePeripheral.class.getSimpleName();


    /** Peripheral and GATT Profile **/
    public static final String ADVERTISING_NAME =  "MyDevice";

    public static final UUID SERVICE_UUID = UUID.fromString("0000180c-0000-1000-8000-00805f9b34fb");
    public static final UUID CHARACTERISTIC_UUID = UUID.fromString("00002a56-0000-1000-8000-00805f9b34fb");

    private static final int CHARACTERISTIC_LENGTH = 20;


    /** Advertising settings **/

    // advertising mode can be one of:
    // - ADVERTISE_MODE_BALANCED,
    // - ADVERTISE_MODE_LOW_LATENCY,
    // - ADVERTISE_MODE_LOW_POWER
    int mAdvertisingMode = AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY;

    // transmission power mode can be one of:
    // - ADVERTISE_TX_POWER_HIGH
    // - ADVERTISE_TX_POWER_MEDIUM
    // - ADVERTISE_TX_POWER_LOW
    // - ADVERTISE_TX_POWER_ULTRA_LOW
    int mTransmissionPower = AdvertiseSettings.ADVERTISE_TX_POWER_HIGH;



    /** Callback Handlers **/
    public BlePeripheralCallback mBlePeripheralCallback;

    /** Bluetooth Stuff **/
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mBluetoothAdvertiser;

    private BluetoothGattServer mGattServer;
    private BluetoothGattService mService;
    private BluetoothGattCharacteristic mCharacteristic;


    /**
     * Construct a new Peripheral
     *
     * @param context The Application Context
     * @param blePeripheralCallback The callback handler that interfaces with this Peripheral
     * @throws Exception Exception thrown if Bluetooth is not supported
     */
    public MyBlePeripheral(final Context context, BlePeripheralCallback blePeripheralCallback) throws Exception {
        mBlePeripheralCallback = blePeripheralCallback;

        // make sure Android device supports Bluetooth Low Energy
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            throw new Exception("Bluetooth Not Supported");
        }

        // get a reference to the Bluetooth Manager class, which allows us to talk to talk to the BLE radio
        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);

        mGattServer = bluetoothManager.openGattServer(context, mGattServerCallback);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Beware: this function doesn't work on some systems
        if(!mBluetoothAdapter.isMultipleAdvertisementSupported()) {
            throw new Exception ("Peripheral mode not supported");
        }

        mBluetoothAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();

        // Use this method instead for better support
        if (mBluetoothAdvertiser == null) {
            throw new Exception ("Peripheral mode not supported");
        }

        setupDevice();
    }

    /**
     * Get the system Bluetooth Adapter
     *
     * @return BluetoothAdapter
     */
    public BluetoothAdapter getBluetoothAdapter() {
        return mBluetoothAdapter;
    }

    /**
     * Set up the Advertising name and GATT profile
     */
    private void setupDevice() {
        mService = new BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        mCharacteristic = new BluetoothGattCharacteristic(
                CHARACTERISTIC_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ);


        mService.addCharacteristic(mCharacteristic);

        mGattServer.addService(mService);


        // write random characters to the read Characteristic every timerInterval_ms
        int timerInterval_ms = 1000;
        TimerTask updateReadCharacteristicTask = new TimerTask() {
            @Override
            public void run() {
                int stringLength = (int) (Math.random() % CHARACTERISTIC_LENGTH);
                String randomString = DataConverter.getRandomString(stringLength);
                try {
                    mCharacteristic.setValue(randomString);

                } catch (Exception e) {
                    Log.e(TAG, "Error converting String to byte array");
                }

            }
        };
        Timer randomStringTimer = new Timer();
        randomStringTimer.schedule(updateReadCharacteristicTask, 0, timerInterval_ms);

    }



    /**
     * Start Advertising
     *
     * @throws Exception Exception thrown if Bluetooth Peripheral mode is not supported
     */
    public void startAdvertising() {
        // set the device name
        mBluetoothAdapter.setName(ADVERTISING_NAME);

        // Build Advertise settings with transmission power and advertise speed
        AdvertiseSettings advertiseSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(mAdvertisingMode)
                .setTxPowerLevel(mTransmissionPower)
                .setConnectable(true)
                .build();


        AdvertiseData.Builder advertiseBuilder = new AdvertiseData.Builder();
        // set advertising name
        advertiseBuilder.setIncludeDeviceName(true);

        // add Services
        advertiseBuilder.addServiceUuid(new ParcelUuid(SERVICE_UUID));

        AdvertiseData advertiseData = advertiseBuilder.build();

        // begin advertising
        try {
            mBluetoothAdvertiser.startAdvertising( advertiseSettings, advertiseData, mAdvertiseCallback );
        } catch (Exception e) {
            Log.e(TAG, "could not start advertising");
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }

    }


    /**
     * Stop advertising
     */
    public void stopAdvertising() {
        if (mBluetoothAdvertiser != null) {
            mBluetoothAdvertiser.stopAdvertising(mAdvertiseCallback);
            mBlePeripheralCallback.onAdvertisingStopped();
        }
    }


    /**
     * Check if a Characetristic supports read permissions
     * @return Returns <b>true</b> if property is writable
     */
    public static boolean isCharacteristicReadable(BluetoothGattCharacteristic characteristic) {
        return (characteristic.getProperties() & (BluetoothGattCharacteristic.PROPERTY_READ)) != 0;
    }



    private final BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, final int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            Log.v(TAG, "Connected");

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    mBlePeripheralCallback.onCentralConnected(device);
                    stopAdvertising();


                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    mBlePeripheralCallback.onCentralDisconnected(device);
                    try {
                        startAdvertising();
                    } catch (Exception e) {
                        Log.e(TAG, "error starting advertising");
                    }
                }
            }

        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            Log.d(TAG, "Device tried to read characteristic: " + characteristic.getUuid());
            Log.d(TAG, "Value: " + Arrays.toString(characteristic.getValue()));

            if (characteristic.getUuid() == mCharacteristic.getUuid()) {
                if (offset < CHARACTERISTIC_LENGTH) {
                    if (offset != 0) {
                        mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, offset, characteristic.getValue());
                    } else {
                        mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
                    }
                } else {
                    Log.d(TAG, "invalid offset when trying to read Characteristic");

                }
            }
        }

    };


    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);

            mBlePeripheralCallback.onAdvertisingStarted();
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            mBlePeripheralCallback.onAdvertisingFailed(errorCode);
        }
    };


}
