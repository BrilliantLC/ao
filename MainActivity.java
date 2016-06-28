package lab3_206_03.uwaterloo.ca.lab3_206_03;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;
import java.io.PrintWriter;
import java.util.Arrays;
import ca.uwaterloo.sensortoy.LineGraphView;
import java.io.File;
import mapper.MapView;

public class MainActivity extends AppCompatActivity {
    public LineGraphView graph;
    public TextView accel,direction;

    public Button reset;
    public Button resetstp;
    public int step;
    public SensorEventListeners al, both;
    public MapView mapView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LinearLayout l = (LinearLayout) findViewById(R.id.linearLayout);
        l.setOrientation(LinearLayout.VERTICAL);
        //pedometer output display
        accel = new TextView(getApplicationContext());
        accel.setTextColor(Color.BLACK);
        l.addView(accel);
        direction = new TextView(getApplicationContext());
        direction.setTextColor(Color.BLACK);
        l.addView(direction);
        //graph display
        graph = new LineGraphView(getApplicationContext(), 100, Arrays.asList("x", "y", "z"));
        l.addView(graph);
        graph.setVisibility(View.VISIBLE);
        //reset map button
        reset = new Button(getApplicationContext());
        reset.setText("RESET GRAPH");
        reset.setGravity(Gravity.CENTER_HORIZONTAL);
        l.addView(
                reset,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT)
        );
        //reset step count button
        resetstp = new Button(getApplicationContext());
        resetstp.setText("RESET STEPS");
        resetstp.setGravity(Gravity.CENTER_HORIZONTAL);
        l.addView(
                resetstp,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT)
        );

        //request the sensor manager and get the accelerometer
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        Sensor accelerometerO = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor magnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        Sensor rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        //instantiate its sensor listener

        al = new SensorEventListeners(accel,direction, graph, step);
        sensorManager.registerListener(al, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);

        sensorManager.registerListener(al, magnetic, SensorManager.SENSOR_DELAY_FASTEST);

        sensorManager.registerListener(al, accelerometerO, SensorManager.SENSOR_DELAY_FASTEST);

        sensorManager.registerListener(al, rotation, SensorManager.SENSOR_DELAY_NORMAL);

        //on-click listeners for both buttons
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                graph.purge();
            }
        });
        resetstp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                al.resetit();
            }
        });
//        registerForContextMenu(mapView);
    }

//    @Override
//    public  void  onCreateContextMenu(ContextMenu menu , View v, ContextMenu.ContextMenuInfo menuInfo) {
//        super.onCreateContextMenu(menu , v, menuInfo);
//        mapView.onCreateContextMenu(menu , v, menuInfo);
//    }
//
//    @Override
//    public  boolean  onContextItemSelected(MenuItem item) {
//        return  super.onContextItemSelected(item) ||  mapView.onContextItemSelected(item);
//    }
}

class SensorEventListeners implements SensorEventListener {
    TextView output,direction;
    LineGraphView Graph;
    float z;
    int counter = 0;
    int prev = 0;
    int state= 0;
    float[] mag, acc, rot;
    double orientation;
    double prevor = 0;
    int nscounter, wecounter = 0;
    //three constructor parameters
    public SensorEventListeners(TextView outputView, TextView dirView, LineGraphView grp, int stp) {
        output = outputView;
        direction = dirView;
        Graph = grp;
        counter = stp;
    }

    //method for resetting the counter for the "resetstp" button
    public void resetit() {
        counter = 0;
        nscounter = 0;
        wecounter = 0;
    }

    public void onAccuracyChanged(Sensor s, int i) {
    }

    public void onSensorChanged(SensorEvent se) {

        if (se.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR){
            rot = se.values;
        }
        if (se.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            acc = se.values;
        }

        if (se.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            mag = se.values;
        }
        if (acc != null && mag != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, acc, mag);
                if (success) {
                    float[] orient = new float[3];
                    SensorManager.getOrientation(R, orient);
                    orientation = orient[0];
                    orientation = Math.toDegrees(orientation);
                    if (orientation < 0){
                        orientation = Math.abs(orientation);
                    }else if (orientation>0){
                        orientation = Math.abs(orientation-360);
                    }
                    if (orientation<=45 || orientation>315){
                        direction.setText("NORTH");
                    }else if(orientation>45 && orientation <= 135){
                        direction.setText("WEST");
                    }else if(orientation>135 && orientation <= 225){
                            direction.setText("SOUTH");
                    }else if(orientation>225 && orientation <= 315){
                            direction.setText("EAST");
                    }

//                    output.setText("" + orientation);
                }
        }
        double diff = orientation - prevor;
        double avg = orientation + prevor/2;

        if (se.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            //plot the graph
            Graph.addPoint(se.values);
            //we only care about the z component as our algorithm depends on the vertical acceleration
            z = se.values[2];
            //threshold = 4 m/s^2
            if(z>=4){
                state = 1;  //state 1, where the acceleration pass 4
            }else if (z<=-4) {
                state = 0;  //state 0, where the acceleration gets below -4
            }else{
                state = 2;  //state 2, where the acceleration is in-between the boundaries
            }
            //if acceleration is increasing from -4 to 4
            if(prev==0 && state==1){
                counter++;  //register as 1 step
                prev = state;  //sets previous state to the current state
                if (avg< 45 || avg > 315  ){
                    nscounter++;


                }else if (avg < 202.5 && avg >157.5 && (Math.abs(diff) > 270) ){
                    nscounter++;
                }
                else if(avg> 135 && avg < 225){
                    nscounter--;
                }
                else if(avg> 225 && avg < 315){
                    wecounter++;
                }
                else if(avg>45 && avg < 135){
                    wecounter--;

                }
                //if acceleration is decreasing from 4 to -4
            }else if (prev == 1 && state == 0){
                prev = state;
                prevor = orientation;
            }
        }
        output.setText(""+"ori"+orientation+ "\n average"+avg+"\nstep"+counter+"\nNS"+nscounter+"\nWE"+wecounter);

    }
}
