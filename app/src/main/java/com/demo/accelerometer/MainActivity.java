package com.demo.accelerometer;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Projet GMM - INSA 2017
 *
 * @author : fcamps@laas.fr, mathieu.zmudz@gmail.com, greault@etud.insa-toulouse.fr
 *
 */
public class MainActivity extends Activity implements SensorEventListener {
    private SensorManager sensorManager;

    //accelero GUI
    TextView X;
    TextView Y;
    TextView Z;
    TextView G;

    //gyr GUI
    TextView gyrX;
    TextView gyrY;
    TextView gyrZ;

    TextView compute;

    // button
    Button buttonYes;
    Button buttonNo;

    //data gyro
    float xAccelero = 0.0f;
    float yAccelero = 0.0f;
    float zAccelero = 0.0f;

    //display
    boolean isDisplay;


    // outpuFile
    FileOutputStream fosData = null;
    String dataToRecord = null;

    // var algo
    final float thetaMax = 55.0f;
    final float gMax = 2.5f;
    final float g = 9.81f;
    float[] origin = new float[3];
    float[] Acc = new float[3];
    float lastTimeAlgo = 0.0f;
    float actualTime = 0.0f;
    float alpha = 0.50f;
    Compute myCompute = null;
    boolean setOrigin=true;

    Object flag=new Object();

    boolean ANSWER_NO=false;
    boolean ANSWER_YES=false;
    int NO_ANSWER=2;

    final int NO=0;
    final int YES=1;

    Context context;


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.content_main);

        X = (TextView) findViewById(R.id.textViewX);
        Y = (TextView) findViewById(R.id.textViewY);
        Z = (TextView) findViewById(R.id.textViewZ);
        G = (TextView) findViewById(R.id.textViewG);

        compute = (TextView) findViewById(R.id.textViewCompute);

        Switch swDisplay = (Switch) findViewById(R.id.switchDisplay);

        swDisplay.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isDisplay)
                    isDisplay = false;
                else
                    isDisplay = true;
            }
        });

        gyrX = (TextView) findViewById(R.id.textViewGyrX);
        gyrY = (TextView) findViewById(R.id.textViewGyrY);
        gyrZ = (TextView) findViewById(R.id.textViewGyrZ);

        buttonYes = (Button) findViewById(R.id.buttonYES);

        buttonYes.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ANSWER_YES=true;

                Log.v("APP", "************ YES");

            }
        });

        buttonNo = (Button) findViewById(R.id.buttonNO);
        buttonNo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ANSWER_NO=true;

                Log.v("APP", "************ NO");
            }
        });

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor sensorAcc = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, sensorAcc, SensorManager.SENSOR_DELAY_NORMAL);

        if (myCompute == null)
            myCompute = new Compute(alpha);

        context = this.getApplicationContext();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            getAccelerometer(event);

            lastTimeAlgo = actualTime;

            Acc[0] = xAccelero;
            Acc[1] = yAccelero;
            Acc[2] = zAccelero;

            synchronized (flag) {
                // Abnormal acceleration
                if (Compute.norme(Acc) / g > gMax) {
                    sensorManager.unregisterListener(this);
                    // ask question "falling ?"
                    ANSWER_NO = false;
                    ANSWER_YES = false;

                    new Algo().execute(" ");
                }
                // Compute angle between an "origin" and a position
                else if(myCompute.getAngle(Acc, origin) > thetaMax)
                {
                    //TODO
                    // wait a time to delete false detection

                    sensorManager.unregisterListener(this);
                    // ask question "falling ?"
                    ANSWER_NO = false;
                    ANSWER_YES = false;

                    new Algo().execute(" ");
                }
            }
        }
    }


    /**
     * Ask if someone is falling ?
     *
     *
     */
    int fallDetected()
    {
        int count=0;

        while(count<3) {

            Log.v("APP", "************ ! PLAY ALERT ! ***************   " + Float.toString(Compute.norme(Acc)));

            if(ANSWER_NO) {

                return NO;
            }
            else if(ANSWER_YES) {
                return YES;
            }
                MediaPlayer mp = android.media.MediaPlayer.create(this.getApplicationContext(), R.raw.enregistrement_0002);
                mp.start();

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            count++;
        }
        return NO_ANSWER;
    }


    /**
     *
     */
    private class Algo extends AsyncTask<String, Void, Void>
    {
        /** The system calls this to perform work in a worker thread and
         * delivers it the parameters given to AsyncTask.execute() */
        protected Void doInBackground(String... dummy)
        {
            int count=0;

            while(count<3 && !ANSWER_NO && !ANSWER_YES)
            {
                // question fall ?
                MediaPlayer mp = android.media.MediaPlayer.create(context, R.raw.enregistrement_0002);
                mp.start();

                try
                {
                    if( !ANSWER_NO && !ANSWER_YES) {
                        Thread.sleep(3000);
                    }
                } catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
                count++;
            }

            // Answer if YES --> true fall
            if(ANSWER_YES) {

                MediaPlayer mp = android.media.MediaPlayer.create(context, R.raw.enregistrement_called_rescue);
                mp.start();
            }
            // Answer is NO --> false detection
            else if(ANSWER_NO)
            {
                //new origin
                setOrigin =true;
                restart_ACC();
            }
            // No answer --> start a timer then call rescues
            else if(!ANSWER_YES && !ANSWER_NO)
            {
                new myTime(context);
            }

            return null;
        }

        /** The system calls this to perform work in the UI thread and delivers
         * the result from doInBackground() */
        protected void onPostExecute(Integer result)
        {
        }
    }


    /**
     *
     */
    void restart_ACC()
{
    Log.v("APP", "************ RESTART ACC");
    sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
}


    /**
     *  Wait for 30s then call rescue
     */
    class myTime {

    myTime(final Context context) {
        new CountDownTimer(30000, 1000) {

            // Do you want to call rescue
            public void onTick(long millisUntilFinished) {
                Log.v("APP", "************ seconds remaining: " + millisUntilFinished / 1000);

                if (ANSWER_NO) {
                    this.cancel();
                    restart_ACC();
                }
            }

            public void onFinish() {
                Log.v("APP", "************ ! CALL ALERT ! ***********");

                MediaPlayer mp = MediaPlayer.create(context, R.raw.enregistrement_called_rescue);
                mp.start();
            }
        }.start();
    }
}

    /**
     * @param event
     */
    private void getAccelerometer(SensorEvent event) {

        float[] values = event.values;

        xAccelero = values[0];
        yAccelero = values[1];
        zAccelero = values[2];

        if (setOrigin) {
            // Set the origin of position after 100 samples
            origin[0] = xAccelero;
            origin[1] = yAccelero;
            origin[2] = zAccelero;

            setOrigin = false;
        }

        if (isDisplay) {
            X.setText(Float.toString(xAccelero));
            Y.setText(Float.toString(yAccelero));
            Z.setText(Float.toString(zAccelero));
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /**
     *
     */
    @Override
    protected void onResume() {
        super.onResume();
        // register this class as a listener for the orientation and
        // accelerometer sensors
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);

    }

    /**
     *
     */
    @Override
    protected void onPause() {
        // unregister listener
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    /**
     *
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    /**
     * @param body
     */
    public void writeData(String body) {
        try {
            fosData.write(body.getBytes());

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    /**
     *
     */
    public void createFile() {
        try {
            final File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/datas/");

            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    Log.e("ALERT", "could not create the directories");
                }
            }

            final File dataFile = new File(dir, "GyroAccelero.txt");

            if (!dataFile.exists()) {
                dataFile.createNewFile();
            }

            fosData = new FileOutputStream(dataFile, false);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}


