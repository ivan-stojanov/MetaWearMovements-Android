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
    public float Beta;
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








/* od vs solusion 1
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
*/
/* od vs solusion 2
        SamplePeriod = 1f / 256f;
        Beta = 0.1f;

        float q1 = Quaternion[0], q2 = Quaternion[1], q3 = Quaternion[2], q4 = Quaternion[3];   // short name local variable for readability
        float norm;
        float s1, s2, s3, s4;
        float qDot1, qDot2, qDot3, qDot4;

        // Auxiliary variables to avoid repeated arithmetic
        float _2q1 = 2f * q1;
        float _2q2 = 2f * q2;
        float _2q3 = 2f * q3;
        float _2q4 = 2f * q4;
        float _4q1 = 4f * q1;
        float _4q2 = 4f * q2;
        float _4q3 = 4f * q3;
        float _8q2 = 8f * q2;
        float _8q3 = 8f * q3;
        float q1q1 = q1 * q1;
        float q2q2 = q2 * q2;
        float q3q3 = q3 * q3;
        float q4q4 = q4 * q4;

        // Normalise accelerometer measurement
        norm = (float)Math.sqrt(accX * accX + accY * accY + accZ * accZ);
        if (norm == 0f) return; // handle NaN
        norm = 1 / norm;        // use reciprocal for division
        accX *= norm;
        accY *= norm;
        accZ *= norm;

        // Gradient decent algorithm corrective step
        s1 = _4q1 * q3q3 + _2q3 * accX + _4q1 * q2q2 - _2q2 * accY;
        s2 = _4q2 * q4q4 - _2q4 * accX + 4f * q1q1 * q2 - _2q1 * accY - _4q2 + _8q2 * q2q2 + _8q2 * q3q3 + _4q2 * accZ;
        s3 = 4f * q1q1 * q3 + _2q1 * accX + _4q3 * q4q4 - _2q4 * accY - _4q3 + _8q3 * q2q2 + _8q3 * q3q3 + _4q3 * accZ;
        s4 = 4f * q2q2 * q4 - _2q2 * accX + 4f * q3q3 * q4 - _2q3 * accY;
        norm = 1f / (float)Math.sqrt(s1 * s1 + s2 * s2 + s3 * s3 + s4 * s4);    // normalise step magnitude
        s1 *= norm;
        s2 *= norm;
        s3 *= norm;
        s4 *= norm;

        // Compute rate of change of quaternion
        qDot1 = 0.5f * (-q2 * gyroX - q3 * gyroY - q4 * gyroZ) - Beta * s1;
        qDot2 = 0.5f * (q1 * gyroX + q3 * gyroZ - q4 * gyroY) - Beta * s2;
        qDot3 = 0.5f * (q1 * gyroY - q2 * gyroZ + q4 * gyroX) - Beta * s3;
        qDot4 = 0.5f * (q1 * gyroZ + q2 * gyroY - q3 * gyroX) - Beta * s4;

        // Integrate to yield quaternion
        q1 += qDot1 * SamplePeriod;
        q2 += qDot2 * SamplePeriod;
        q3 += qDot3 * SamplePeriod;
        q4 += qDot4 * SamplePeriod;
        norm = 1f / (float)Math.sqrt(q1 * q1 + q2 * q2 + q3 * q3 + q4 * q4);    // normalise quaternion
        Quaternion[0] = q1 * norm;
        Quaternion[1] = q2 * norm;
        Quaternion[2] = q3 * norm;
        Quaternion[3] = q4 * norm;
*/

//od https://bitbucket.org/cinqlair/mpu9250/src/0b38d94e630291eeff31fb0c1897425f64cb0c31/mpu9250_OpenGL/src/MadgwickAHRS.cpp?at=master&fileviewer=file-view-default

        float sampleFreq = 100.0f;		// sample frequency in Hz
        float betaDef = 0.02f;		// 2 * proportional gain
        float q0 = Quaternion[0], q1 = Quaternion[1], q2 = Quaternion[2], q3 = Quaternion[3];
        float recipNorm;
        float s0, s1, s2, s3;
        float qDot1, qDot2, qDot3, qDot4;
        float _2q0, _2q1, _2q2, _2q3, _4q0, _4q1, _4q2 ,_8q1, _8q2, q0q0, q1q1, q2q2, q3q3;

/*
Log.i("testDebug q0 111, q0 = ", Float.toString(q0));
Log.i("testDebug q1 111, q1 = ", Float.toString(q1));
Log.i("testDebug q2 111, q2 = ", Float.toString(q2));
Log.i("testDebug q3 111, q3 = ", Float.toString(q3));
*/        // Rate of change of quaternion from gyroscope
        qDot1 = 0.5f * (-q1 * gyroX - q2 * gyroY - q3 * gyroZ);
        qDot2 = 0.5f * (q0 * gyroX + q2 * gyroZ - q3 * gyroY);
        qDot3 = 0.5f * (q0 * gyroY - q1 * gyroZ + q3 * gyroX);
        qDot4 = 0.5f * (q0 * gyroZ + q1 * gyroY - q2 * gyroX);

        // Compute feedback only if accelerometer measurement valid (avoids NaN in accelerometer normalisation)
        if(!((accX == 0.0f) && (accY == 0.0f) && (accZ == 0.0f))) {

            // Normalise accelerometer measurement
            recipNorm = invSqrt(accX * accX + accY * accY + accZ * accZ);
            accX *= recipNorm;
            accY *= recipNorm;
            accZ *= recipNorm;

            // Auxiliary variables to avoid repeated arithmetic
            _2q0 = 2.0f * q0;
            _2q1 = 2.0f * q1;
            _2q2 = 2.0f * q2;
            _2q3 = 2.0f * q3;
            _4q0 = 4.0f * q0;
            _4q1 = 4.0f * q1;
            _4q2 = 4.0f * q2;
            _8q1 = 8.0f * q1;
            _8q2 = 8.0f * q2;
            q0q0 = q0 * q0;
            q1q1 = q1 * q1;
            q2q2 = q2 * q2;
            q3q3 = q3 * q3;

            // Gradient decent algorithm corrective step
            s0 = _4q0 * q2q2 + _2q2 * accX + _4q0 * q1q1 - _2q1 * accY;
            s1 = _4q1 * q3q3 - _2q3 * accX + 4.0f * q0q0 * q1 - _2q0 * accY - _4q1 + _8q1 * q1q1 + _8q1 * q2q2 + _4q1 * accZ;
            s2 = 4.0f * q0q0 * q2 + _2q0 * accX + _4q2 * q3q3 - _2q3 * accY - _4q2 + _8q2 * q1q1 + _8q2 * q2q2 + _4q2 * accZ;
            s3 = 4.0f * q1q1 * q3 - _2q1 * accX + 4.0f * q2q2 * q3 - _2q2 * accY;
            recipNorm = invSqrt(s0 * s0 + s1 * s1 + s2 * s2 + s3 * s3); // normalise step magnitude
            s0 *= recipNorm;
            s1 *= recipNorm;
            s2 *= recipNorm;
            s3 *= recipNorm;

            // Apply feedback step
            qDot1 -= betaDef * s0;
            qDot2 -= betaDef * s1;
            qDot3 -= betaDef * s2;
            qDot4 -= betaDef * s3;
        }

        // Integrate rate of change of quaternion to yield quaternion
        q0 += qDot1 * (1.0f / sampleFreq);
        q1 += qDot2 * (1.0f / sampleFreq);
        q2 += qDot3 * (1.0f / sampleFreq);
        q3 += qDot4 * (1.0f / sampleFreq);/*
Log.i("testDebug q0 222, q0 = ", Float.toString(q0));
Log.i("testDebug q1 222, q1 = ", Float.toString(q1));
Log.i("testDebug q2 222, q2 = ", Float.toString(q2));
Log.i("testDebug q3 222, q3 = ", Float.toString(q3));
*/        // Normalise quaternion
        recipNorm = invSqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
        q0 *= recipNorm;
        q1 *= recipNorm;
        q2 *= recipNorm;
        q3 *= recipNorm;/*
Log.i("testDebug q0 333, q0 = ", Float.toString(q0));
Log.i("testDebug q1 333, q1 = ", Float.toString(q1));
Log.i("testDebug q2 333, q2 = ", Float.toString(q2));
Log.i("testDebug q3 333, q3 = ", Float.toString(q3));
*/

        float norm = (float)Math.sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
        //norm = 1.0f / norm;
        Quaternion[0] = q0;// * norm;
        Quaternion[1] = q1;// * norm;
        Quaternion[2] = q2;// * norm;
        Quaternion[3] = q3;// * norm;

        modelActivity.collectData(LOG_TAG, Quaternion, System.currentTimeMillis());
        //Log.i(LOG_TAG, "Quaternion:   " + Float.toString(Quaternion[0]) + " , " + Float.toString(Quaternion[1]) + " , " + Float.toString(Quaternion[2]) + " , " + Float.toString(Quaternion[3]));
    }

    public static float invSqrt(float x) {
        float xhalf = 0.5f * x;
        int i = Float.floatToIntBits(x);
        i = 0x5f3759df - (i >> 1);
        x = Float.intBitsToFloat(i);
        x *= (1.5f - xhalf * x * x);
        return x;
    }
}
