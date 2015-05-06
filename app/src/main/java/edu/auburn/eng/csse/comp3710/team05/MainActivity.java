package edu.auburn.eng.csse.comp3710.team05;

import android.app.Activity;
import android.view.ViewGroup.LayoutParams;
import android.hardware.GeomagneticField;
import android.net.NetworkInfo;
import android.widget.Button;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import 	android.view.*;
import android.hardware.Camera;
import android.widget.FrameLayout;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import android.net.ConnectivityManager;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;


import edu.auburn.eng.csse.comp3710.team5.R;

import static edu.auburn.eng.csse.comp3710.team5.R.layout.fragment_view;


public class MainActivity extends Activity implements SensorEventListener{

    private Camera mCamera;
    private CameraPreview mPreview;
    private GPSLocation gps;
    private SensorManager mSensorManager;
    //private HashMap<String, Double[]> values;
    private Sensor mMagnet;
    private Sensor mSensor;
    private float azimuth_angle;
    private float pitch_angle;
    private float roll_angle;
    public static PreFragment frag;
    private int counter = 0; //handles time since refresh
    private static MyView mView;
    public static boolean isConnected;
    private static Context context;
    public static Graph g;
    static final float ALPHA = 0.15f;
    FrameLayout preview;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FragmentTransaction ft = null;
        MainActivity.context = getApplicationContext();

        if(savedInstanceState == null) {

            ConnectivityManager cm =
                    (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            isConnected = activeNetwork != null &&
                    activeNetwork.isConnectedOrConnecting();
            mCamera = getCameraInstance();
            mPreview = new CameraPreview(this, mCamera);

            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            mMagnet = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gps = new GPSLocation(mPreview.getContext());

            checkCameraHardware(getApplicationContext());
            FragmentManager fragmentManager = getFragmentManager();
            ft = fragmentManager.beginTransaction();
            frag = (PreFragment) fragmentManager.findFragmentByTag("frag");
            preview = (FrameLayout) findViewById(R.id.camera_preview);
            preview.addView(mPreview);

            // create the fragment and data the first time
            if (frag == null) {

                frag = new PreFragment();
                fragmentManager.beginTransaction().add(preview.getId(),frag, "frag").addToBackStack("wtf").commit();
                ft.show(frag);
                // load the data from the web
                frag.setAllData(isConnected, roll_angle, pitch_angle, azimuth_angle, gps, g, mSensorManager, mSensor
                        , mMagnet);
            }

            mView = new MyView(this);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

            addContentView(mView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT));



        }

    }
    public Graph getG(){
        return g;
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        // store the data in the fragment
        frag.setAllData(isConnected, roll_angle, pitch_angle, azimuth_angle, gps, g, mSensorManager, mSensor
                , mMagnet);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        frag = (PreFragment) getFragmentManager().getFragment(
                savedInstanceState, "frag");
       /* mCamera = getCameraInstance();
        mPreview = new CameraPreview(this, mCamera);
        mSensor = frag.getmSensor();
        mMagnet = frag.getmMagnet();
        gps = frag.getGPS();
        g = frag.getGr();
        mSensorManager = frag.getmSensorManager();
        isConnected = frag.getConnected();
        azimuth_angle = frag.getAzimuth_angle();
        pitch_angle = frag.getPitch_angle();
        roll_angle = frag.getRoll_angle();
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();
        if(frag.values != null)
        g.setMap(frag.values);
        double[][] val = frag.orderedValues;
        NavalDataReader r = null;
        if(frag.myReader != null) {
            g.updateCoordinates(frag.myReader.getOrderedValues());
            r = frag.myReader;
        }
        frag.myReader = r;
        // load the data from the web
        frag.setOrder(val);
        PreFragment myF = new PreFragment();
        myF.setAllData(isConnected, roll_angle, pitch_angle, azimuth_angle, gps, g, mSensorManager, mSensor
                , mMagnet);
        frag = myF;
        fragmentManager.beginTransaction().add(preview.getId(),frag, "frag").commit();
        ft.show(frag);
        mView = new MyView(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        addContentView(mView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT));
*/
    }

    public static Context getAppContext() {
        return MainActivity.context;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mMagnet, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        if (mCamera == null)
        mCamera = getCameraInstance();

    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        if (!(mCamera == null)) {
            mCamera.release();
        }
    }

    float[] mGravity;
    float[] mGeomagnetic;
    protected float[] lowPass( float[] input, float[] output ) {
        if ( output == null ) return input;

        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        GeomagneticField mag = new GeomagneticField((float) gps.getLatitude(),
                (float) gps.getLongitude(), (float) gps.getAltitude(), System.currentTimeMillis());
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = lowPass( event.values.clone(), mGravity );
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = lowPass( event.values.clone(), mGeomagnetic );
        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            if (success) {
                float R_out[] = new float[9];
                SensorManager.remapCoordinateSystem(R, SensorManager.AXIS_X, SensorManager.AXIS_MINUS_Z, R_out);
                float orientation[] = new float[3];
                SensorManager.getOrientation(R_out, orientation);
                azimuth_angle = orientation[0]; // orientation contains: azimuth, pitch, and roll
                azimuth_angle += Math.toRadians(mag.getDeclination()); // correct azimuth from magnetic north to true north
                //Log.i("Magnetic declination", Double.toString(mag.getDeclination()));
                pitch_angle = orientation[1];
                roll_angle = orientation[2];
                mView.postInvalidate();
            }
        }
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
        // You must implement this callback in your code.
    }
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        getFragmentManager().putFragment(outState, "frag", frag);



    }

    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance

        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    /** A basic Camera preview class */
    public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
        private SurfaceHolder mHolder;
        private Camera mCamera;

        public CameraPreview(Context context, Camera camera) {
            super(context);
            mCamera = camera;
            mHolder = getHolder();
            mHolder.addCallback(this);
            // deprecated setting, but required on Android versions prior to 3.0
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        public void setCamera(Camera camIn){
            mCamera = camIn;
        }

        public void onPauseMySurfaceView(){

        }
        public void onResumeMySurfaceView(){

        }


        public void surfaceCreated(SurfaceHolder holder) {

            // The Surface has been created, now tell the camera where to draw the preview.
            if (mCamera != null) {
                try {
                    mCamera.setPreviewDisplay(holder);
                    //cameraId should be 0?

                    setCameraDisplayOrientation(MainActivity.this, 0, mCamera);
                    // mCamera.startPreview();
                    g = new Graph( mCamera.getParameters().getHorizontalViewAngle(),
                            mCamera.getParameters().getVerticalViewAngle(),
                            this.getWidth(),this.getHeight());
                    Log.i("surfacecreated", "surfaceCreated");
                } catch (IOException e) {
                    Log.d("CameraPreview Error", "Error setting camera preview: " + e.getMessage());
                }
            }
            //make the surface drawable
            setWillNotDraw(false);
            //start thread

        }
        public void setCameraDisplayOrientation(Activity activity,
                                                int cameraId, android.hardware.Camera camera) {
            android.hardware.Camera.CameraInfo info =
                    new android.hardware.Camera.CameraInfo();
            android.hardware.Camera.getCameraInfo(cameraId, info);
            int rotation = activity.getWindowManager().getDefaultDisplay()
                    .getRotation();
            int degrees = 0;
            switch (rotation) {
                case Surface.ROTATION_0: degrees = 0; Log.d("Camera Degrees 0", "0 degrees"); break;
                case Surface.ROTATION_90: degrees = 90; Log.d("Camera Degrees 90", "90 degrees"); break;
                case Surface.ROTATION_180: degrees = 0; Log.d("Camera Degrees 180", "180 degrees"); break;
                case Surface.ROTATION_270: degrees = 270; Log.d("Camera Degrees 270", "270 degrees"); break;
            }

            int result;
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                result = (info.orientation + degrees) % 360;
                result = (360 - result) % 360;  // compensate the mirror
            } else {  // back-facing
                result = (info.orientation - degrees + 360) % 360;
            }
            camera.setDisplayOrientation(result);
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            if (mCamera != null) {
                //mCamera.stopPreview();
                mCamera.release();
                mCamera = null;}
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            // If your preview can change or rotate, take care of those events here.
            // Make sure to stop the preview before resizing or reformatting it.

            if (mHolder.getSurface() == null){
                // preview surface does not exist
                return;
            }

            // stop preview before making changes
            try {
                mCamera.stopPreview();
            } catch (Exception e){
                // ignore: tried to stop a non-existent preview
            }

            // set preview size and make any resize, rotate or
            // reformatting changes here

            // start preview with new settings
            try {
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();

            } catch (Exception e){
                Log.d("CameraPreview Error", "Error starting camera preview: " + e.getMessage());
            }
        }
    }
    class MyView extends View implements SeekBar.OnSeekBarChangeListener {
        protected final Paint rectanglePaint = new Paint();
        private SeekBar mySeek; // declare seekbar object variable
        // declare text label objects
        private TextView textProgress,textAction;
        public MyView(Context c) {
            super(c);
            setFocusable(true);
            //paint a rectangle
            rectanglePaint.setARGB(255, 200, 0, 0);
            rectanglePaint.setTextSize(30);
            rectanglePaint.setStyle(Paint.Style.FILL);
            rectanglePaint.setStrokeWidth(3);

        }

        public void doSetup(Context c){
            LayoutParams lparams1 = new LayoutParams(
                    100, 25);

            LayoutParams lparams = new LayoutParams(
                    100, 25);
            SeekBar mySeek = new SeekBar(c); // make seekbar object
            mySeek.setOnSeekBarChangeListener(this); // set seekbar listener.
            mySeek.setMax(6);
            mySeek.setLayoutParams(lparams1);
            // since we are using this class as the listener the class is "this"

            // make text label for progress value
            TextView textProgress = new TextView(c);
            textProgress.setLayoutParams(lparams);
            TextView textAction = new TextView(c);
            textAction.setLayoutParams(lparams);
            preview.addView(textAction);
            preview.addView(textProgress);
            preview.addView(mySeek);


        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // TODO Auto-generated method stub
            textAction.setText("starting to track touch");

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // TODO Auto-generated method stub
            seekBar.setSecondaryProgress(seekBar.getProgress()); // set the shade of the previous value.
            textAction.setText("ended tracking touch");
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {
            if(textProgress != null) {

                textProgress.setText("The value is: " + progress);
            }
            if (textAction != null) {
                // change action text label to changing
                textAction.setText("changing");
            }
        }

        @Override
        protected void onDraw(Canvas canvas){

            canvas.drawColor(Color.TRANSPARENT);

            if(frag.myReader != null) {
                if (frag.myReader.getOrderedValues() != null) {

                    g.updateCoordinates(frag.myReader.getOrderedValues());
                    if(g.getMap() == null){
                        g.setMap(frag.myReader.getValues());
                    }
                }
            }
            if(g.ready()) {
                canvas.drawLines(g.points(pitch_angle, azimuth_angle, roll_angle), rectanglePaint);
                canvas.drawLines(g.horizon(0.0, this.getWidth(), pitch_angle, azimuth_angle, roll_angle), rectanglePaint);
                if (g.containsTime()){
                    canvas.drawCircle((float) g.plotSun(pitch_angle, azimuth_angle, roll_angle)[1], (float) g.plotSun(pitch_angle, azimuth_angle, roll_angle)[0], 10, rectanglePaint);
                }
            }
            gps.getLocation();
            if(gps.canGetLocation()){

                // rotate canvas using the following:
                // canvas.save();
                // canvas.rotate((float) (90, 50, 50);
                // canvas.draw whatever;
                // canvas.restore();
                // see this: http://stackoverflow.com/questions/14294532/canvas-drawtext-direction
            }
            else{
                gps.showSettingsAlert();
            }

        }


    }

    public static class PreFragment extends Fragment {

        private Camera mCamera;
        private MyView mView;

        private GPSLocation gps;
        private SensorManager mSensorManager;
        public HashMap<String, Double[]> values;
        private Sensor mMagnet;
        private Sensor mSensor;
        private float azimuth_angle;
        private float pitch_angle;
        private float roll_angle;
        private float az_change;
        private float pi_change;
        private float ro_change;
        public PreFragment frag;
        public boolean isConnected;
        public Graph fragG;
        private NavalDataReader myReader;
        public double[][] orderedValues;
        LinearLayout layout;
        PreFragment myfrag;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            final View rootview = inflater.inflate(fragment_view, container, false);
            final Button button = (Button) rootview.findViewById(R.id.sunButton);
            gps = new GPSLocation(rootview.getContext());

            setRetainInstance(true);
            layout = (LinearLayout) rootview.findViewById(R.id.fragment_view);
            button.setOnClickListener(new View.OnClickListener() {

                public void onClick(View v) {
                    // Perform action on click
                    if (isConnected) {
                        myReader = new NavalDataReader("Sun", String.valueOf(gps.getLatitude()), String.valueOf(gps.getLongitude()));
                        while (myReader.myvls == null) {
                            try {
                                Thread.sleep(900);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        FragmentManager fragmentManager = getFragmentManager();
                        FragmentTransaction ft = fragmentManager.beginTransaction();
                        orderedValues = myReader.getOrderedValues();
                        g.setMap(myReader.getValues());
                        g.updateCoordinates(orderedValues);
                        ft.hide(myfrag);
                        layout.setVisibility(View.GONE);
                    }
                }
            });
            final Button button2 = (Button) rootview.findViewById(R.id.moonButton);

            button2.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // Perform action on click
                    if (isConnected) {
                        myReader = new NavalDataReader("Moon", String.valueOf(gps.getLatitude()), String.valueOf(gps.getLongitude()));

                        while (myReader.myvls == null) {
                            try {
                                Thread.sleep(900);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        FragmentManager fragmentManager = getFragmentManager();
                        FragmentTransaction ft = fragmentManager.beginTransaction();
                        orderedValues = myReader.getOrderedValues();
                        g.setMap(myReader.getValues());
                        g.updateCoordinates(orderedValues);
                        ft.hide(myfrag);
                        layout.setVisibility(View.GONE);
                    } else //can't download data; will have to calculate on device
                    {
                        int timeAdder = 0;

                        //array to store the args returned
                        int max_time = (60 * 12);
                        double[][] othP = new double[max_time][2];
                        double[][] positions = new double[max_time][3];

                        //neg 8 altitude to neg 8

                        //while altitude > -8 && while timeAdder < 12 hours
                        int i = 0;
                        double az = 0;
                        double h = 0;
                        while (timeAdder < max_time) {
                            az = MoonCalculator.getArguments(timeAdder, gps.getLatitude(), gps.getLongitude())[0];
                            h = MoonCalculator.getArguments(timeAdder, gps.getLatitude(), gps.getLongitude())[1];
                            if (h > -11.8) {
                                // moon calculator get arguments
                                positions[i][2] = az;
                                othP[i][1] = az;
                                othP[i][0] = h;
                                positions[i][1] = h;
                                positions[i][0] = (MoonCalculator.getTime() + (timeAdder / 60 / 60 / 24));
                                i++;
                            }
                            timeAdder++;
                        }
                        orderedValues = othP;
                        g.setMap(MoonCalculator.makeMap(othP, max_time));
                        g.updateCoordinates(orderedValues);



                    }

                }
            });
            if (savedInstanceState != null) {
                //Restore the fragment's state here

            }
            return rootview;

        }

        public void setAllData(boolean con, float rollIn, float pitch, float az, GPSLocation gpsIn, Graph gIn,
                               SensorManager manIn, Sensor senIn, Sensor magIn) {
            //mCamera = camIn;
            this.isConnected = con;
            this.roll_angle = rollIn;
            this.pitch_angle = pitch;
            this.azimuth_angle = az;
            this.gps = gpsIn;
            this.fragG = gIn;

            this.mSensorManager = manIn;
            this.mSensor = senIn;
            this.mMagnet = magIn;
            //this.values = MainActivity.values;
            this.mView = MainActivity.mView;
        }

        public GPSLocation getGPS() {
            //MainActivity.values = this.values;
            MainActivity.mView = this.mView;
            return this.gps;
        }

        public SensorManager getmSensorManager() {
            return this.mSensorManager;
        }

        public Sensor getmSensor() {
            return this.mSensor;
        }

        public Sensor getmMagnet(){
            return this.mMagnet;
        }
        public float getAzimuth_angle() {
            return this.azimuth_angle;
        }
        public float getPitch_angle() {
            return this.pitch_angle;
        }
        public float getRoll_angle() {
            return this.roll_angle;
        }
        public boolean getConnected() {
            return isConnected;
        }
        public Graph getGr() {
            return fragG;
        }
        public void setOrder(double [][] oIn){
            this.orderedValues = oIn;
        }

        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);





        }
    }


}