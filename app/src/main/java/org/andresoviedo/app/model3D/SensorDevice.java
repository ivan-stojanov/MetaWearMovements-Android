package org.andresoviedo.app.model3D;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.util.Log;

import com.mbientlab.metawear.AsyncDataProducer;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.data.Acceleration;
import com.mbientlab.metawear.data.AngularVelocity;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.AccelerometerBosch;
import com.mbientlab.metawear.module.AccelerometerMma8452q;
import com.mbientlab.metawear.module.GyroBmi160;

import bolts.Continuation;
import bolts.Task;

/**
 * Created by Ivan on 2018-02-24.
 */

public class SensorDevice
{
    String mwMacAddress;
    BluetoothManager btManager;
    BluetoothDevice btDevice;
    MetaWearBoard mwBoard;
    BtleService.LocalBinder serviceBinder;

    public Accelerometer accelerometer;
    public GyroBmi160 gyroBmi160;
    public static final float[] MMA845Q_RANGES= {2.f, 4.f, 8.f}, BOSCH_RANGES = {2.f, 4.f, 8.f, 16.f};
    //public static final float INITIAL_RANGE= 2.f;
    public static final float ACC_FREQ= 50.f;
    public int rangeIndex= 0;
    public float samplePeriod;
    public Route streamRouteAcc = null;
    public Route streamRouteGYRO = null;
    public static final float[] AVAILABLE_RANGES= {125.f, 250.f, 500.f, 1000.f, 2000.f};
    //public static final float INITIAL_RANGE= 125.f;
    public static final float GYR_ODR= 25.f;
    public String LOG_TAG;

    public SensorDevice(String mwMacAddressVal, BluetoothManager btManagerVal, BtleService.LocalBinder serviceBinderVal, String logtagVal)
    {
        mwMacAddress = mwMacAddressVal;
        btManager = btManagerVal;
        serviceBinder = serviceBinderVal;
        LOG_TAG = logtagVal;

        btDevice = btManager.getAdapter().getRemoteDevice(mwMacAddress);
        mwBoard = serviceBinder.getMetaWearBoard(btDevice);

        mwBoard.connectAsync().onSuccessTask(new Continuation<Void, Task<Route>>() {
            @Override
            public Task<Route> then(Task<Void> task) throws Exception {
                SensorData SensorDataObject = new SensorData();
                SensorDataObject.LOG_TAG = LOG_TAG;
                //accelerometer
                accelerometer = mwBoard.getModule(Accelerometer.class);
                accelerometer.configure()
                        .odr(50f)
                        .commit();

                Accelerometer.ConfigEditor<?> editor = accelerometer.configure();
                editor.odr(ACC_FREQ);
                if (accelerometer instanceof AccelerometerBosch) {
                    editor.range(BOSCH_RANGES[rangeIndex]);
                } else if (accelerometer instanceof AccelerometerMma8452q) {
                    editor.range(MMA845Q_RANGES[rangeIndex]);
                }
                editor.commit();
                samplePeriod = 1 / accelerometer.getOdr();

                final AsyncDataProducer producerAcc = accelerometer.packedAcceleration() == null ?
                        accelerometer.packedAcceleration() :
                        accelerometer.acceleration();

                producerAcc.addRouteAsync(source -> source.stream((data, env) -> {
                    //Log.i(LOG_TAG_ACC, data.value(Acceleration.class).toString());
                    final Acceleration value = data.value(Acceleration.class);
                    SensorDataObject.setAccParams(value.x(), value.y(), value.z(), samplePeriod);
                })).continueWith(taskAcc -> {
                    streamRouteAcc = taskAcc.getResult();
                    producerAcc.start();
                    accelerometer.start();

                    return null;
                });

                //gyro
                gyroBmi160 = mwBoard.getModule(GyroBmi160.class);
                gyroBmi160.configure()
                        .odr(GyroBmi160.OutputDataRate.ODR_50_HZ)
                        .range(GyroBmi160.Range.FSR_2000)
                        .commit();

                final float period = 1 / GYR_ODR;
                final AsyncDataProducer producerGYRO = gyroBmi160.packedAngularVelocity() == null ?
                        gyroBmi160.packedAngularVelocity() :
                        gyroBmi160.angularVelocity();
                producerGYRO.addRouteAsync(source -> source.stream((data, env) -> {
                    //Log.i(LOG_TAG_GYRO, data.value(AngularVelocity.class).toString());
                    final AngularVelocity value = data.value(AngularVelocity.class);
                    SensorDataObject.setGyroParams(value.x(), value.y(), value.z());
                })).continueWith(taskGYRO -> {
                    streamRouteGYRO = taskGYRO.getResult();
                    gyroBmi160.angularVelocity().start();
                    gyroBmi160.start();

                    return null;
                });

                return null;
            }
        }).continueWith(new Continuation<Route, Void>() {

            @Override
            public Void then(Task<Route> task) throws Exception {
                accelerometer.acceleration().start();
                accelerometer.start();

                return null;
            }
        });
    }

    public void stopStreams() {
        if (streamRouteAcc != null) {
            streamRouteAcc.remove();
            streamRouteAcc = null;
        }
        if (streamRouteGYRO != null) {
            streamRouteGYRO.remove();
            streamRouteGYRO = null;
        }
        if(accelerometer != null){
            accelerometer.stop();

            (accelerometer.packedAcceleration() == null ?
                    accelerometer.packedAcceleration() :
                    accelerometer.acceleration()
            ).stop();
            //accelerometer.stop();
            //accelerometer.acceleration().stop();
            Log.i(LOG_TAG, "accelerometer stopped");
        }

        if(gyroBmi160 != null){
            gyroBmi160.stop();
            (gyroBmi160.packedAngularVelocity() == null ?
                    gyroBmi160.packedAngularVelocity() :
                    gyroBmi160.angularVelocity()
            ).stop();
            Log.i(LOG_TAG, "gyroBmi160 stopped");
        }
    }
}
