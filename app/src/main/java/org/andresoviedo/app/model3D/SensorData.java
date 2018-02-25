package org.andresoviedo.app.model3D;

import android.util.Log;

import org.andresoviedo.app.model3D.model.Object3DBuilder;
import org.andresoviedo.app.model3D.view.ModelActivity;

/**
 * Created by Ivan on 2018-02-24.
 */

public class SensorData
{
    public float accX;
    public float accY;
    public float accZ;
    public float gyroX;
    public float gyroY;
    public float gyroZ;
    public boolean hasAcc = false;
    public boolean hasGyro = false;
    public Long timestampAcc;
    public Long timestampGyro;
    public float[] Quaternion = new float[] { 1f, 0f, 0f, 0f };
    public float[] eInt = new float[] { 0f, 0f, 0f };
    public float Kp = 1;
    public float Ki = 0;
    public float SamplePeriod;
    public String LOG_TAG = "SensorDataTag";
    public ModelActivity modelActivity;

    public SensorData(String logtagVal, ModelActivity modelActivityVal){
        hasAcc = false;
        hasGyro = false;
        LOG_TAG = logtagVal;
        modelActivity = modelActivityVal;
    }

    public void setAccParams(float x, float y, float z, float sPeriod){
        accX = x;
        accY = y;
        accZ = z;
        hasAcc = true;
        timestampAcc = System.currentTimeMillis();
        SamplePeriod = sPeriod;
    }

    public void setGyroParams(float x, float y, float z){
        gyroX = x;
        gyroY = y;
        gyroZ = z;
        hasGyro = true;
        timestampGyro = System.currentTimeMillis();

        if(hasAcc == true)
            printAllParams();
    }

    public void printAllParams(){
        /*if(scene != null){
            scene.replaceObject(0,
                    Object3DBuilder.buildLine(
                            new float[] {
                                    0.0f, 1.5f, 0.5f, 0.1f, 1.15f, 0.5f,
                                    0.1f, 1.15f, 0.5f, 0.1f, 0.75f, accX + accY//new Random().nextFloat()
                            }
                    ).setColor(new float[] { 1.0f, 1.0f, 1.0f, 1.0f })
            );

            String printResult = "accX = " + Float.toString(accX) + " ; accY = " + Float.toString(accY) + " ; accZ = " + Float.toString(accZ) + " ; ";
            printResult += "gyroX = " + Float.toString(gyroX) + " ; gyroY = " + Float.toString(gyroY) + " ; gyroZ = " + Float.toString(gyroZ) + " ; ";
            //printResult += "timestampAcc = " + Long.toString(timestampAcc) + " ; timestampGyro = " + Long.toString(timestampGyro) + " ; \n";
            Log.i(LOG_TAG, printResult);
        //}*/

        //calculate Quaternion
        float q1 = Quaternion[0], q2 = Quaternion[1], q3 = Quaternion[2], q4 = Quaternion[3];   // short name local variable for readability
        float norm;
        float vx, vy, vz;
        float ex, ey, ez;
        float pa, pb, pc;

        // Normalise accelerometer measurement
        norm = (float)Math.sqrt(accX * accX + accY * accY + accZ * accZ);
        if (norm == 0f) return; // handle NaN
        norm = 1 / norm;        // use reciprocal for division
        accX *= norm;
        accY *= norm;
        accZ *= norm;

        // Estimated direction of gravity
        vx = 2.0f * (q2 * q4 - q1 * q3);
        vy = 2.0f * (q1 * q2 + q3 * q4);
        vz = q1 * q1 - q2 * q2 - q3 * q3 + q4 * q4;

        // Error is cross product between estimated direction and measured direction of gravity
        ex = (accY * vz - accZ * vy);
        ey = (accZ * vx - accX * vz);
        ez = (accX * vy - accY * vx);
        if (Ki > 0f)
        {
            eInt[0] += ex;      // accumulate integral error
            eInt[1] += ey;
            eInt[2] += ez;
        }
        else
        {
            eInt[0] = 0.0f;     // prevent integral wind up
            eInt[1] = 0.0f;
            eInt[2] = 0.0f;
        }

        // Apply feedback terms
        gyroX = gyroX + Kp * ex + Ki * eInt[0];
        gyroY = gyroY + Kp * ey + Ki * eInt[1];
        gyroZ = gyroZ + Kp * ez + Ki * eInt[2];

        // Integrate rate of change of quaternion
        pa = q2;
        pb = q3;
        pc = q4;
        q1 = q1 + (-q2 * gyroX - q3 * gyroY - q4 * gyroZ) * (0.5f * SamplePeriod);
        q2 = pa + (q1 * gyroX + pb * gyroZ - pc * gyroY) * (0.5f * SamplePeriod);
        q3 = pb + (q1 * gyroY - pa * gyroZ + pc * gyroX) * (0.5f * SamplePeriod);
        q4 = pc + (q1 * gyroZ + pa * gyroY - pb * gyroX) * (0.5f * SamplePeriod);

        // Normalise quaternion
        norm = (float)Math.sqrt(q1 * q1 + q2 * q2 + q3 * q3 + q4 * q4);
        norm = 1.0f / norm;
        Quaternion[0] = q1 * norm;
        Quaternion[1] = q2 * norm;
        Quaternion[2] = q3 * norm;
        Quaternion[3] = q4 * norm;

        modelActivity.collectData(LOG_TAG, Quaternion, System.currentTimeMillis());
        //Log.i(LOG_TAG, "Quaternion:   " + Float.toString(Quaternion[0]) + " , " + Float.toString(Quaternion[1]) + " , " + Float.toString(Quaternion[2]) + " , " + Float.toString(Quaternion[3]));
    }
}
