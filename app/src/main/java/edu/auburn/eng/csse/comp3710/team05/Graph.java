package edu.auburn.eng.csse.comp3710.team05;

import android.util.Log;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by davis on 4/25/15.
 */
public class Graph implements Serializable{
    private int scr_h;
    private int scr_w;
    private double cam_w;
    private double cam_h;
    private double pdH;
    private double pdW;
    private String time;
    int centerW;
    int centerH;
    private HashMap<String, Double[]> myMap;
    private double curAz;
    private double curAlt;
    boolean ready = false;
    boolean containsT = false;
    private double[][] spCoor;
    public Graph(double degW, double degH,
                 int screenHor, int screenVert){
        cam_w = Math.toRadians(degW);
        cam_h = Math.toRadians(degH);
        scr_h = screenVert;
        scr_w = screenHor;
        centerH = scr_h/2;
        centerW = scr_w/2;

        pdH = (double) scr_h/cam_h;
        pdW = (double) scr_w/cam_w;
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

        Date date = new Date();
        String myDay = dateFormat.format(date);
        String year = myDay.substring(0, 4);
        String month = myDay.substring(5, 7);
        time = myDay.substring(11, 16);


    }
    public void setMap(HashMap<String, Double[]> mapIn){
        myMap = mapIn;
        if (!(myMap.containsKey(time))){
            containsT = false;
        }
        else {
            containsT = true;
        }
    }
    public void updateCoordinates(double[][] sphereCoordinates){
        spCoor = sphereCoordinates;
        ready = true;
    }
    public static double normalize(double num)
    {
        return num - Math.floor(num/360.0)*360.0;
    }
    public double[] plotSun(double pitch, double azimuth, double roll){
       double[] output = {0,0};
        curAlt = myMap.get(time)[0].doubleValue();
        curAz = myMap.get(time)[1].doubleValue();
        azimuth = Math.toRadians(normalize(Math.toDegrees(azimuth-Math.PI)));
        //Log.i("azimuth", Double.toString(Math.toDegrees(azimuth-Math.PI)));
        //if (azimuth < 0) {  azimuth += Math.PI*2;}
        //pitch += Math.PI/2;
        output[0] = Math.toRadians(curAlt) - pitch;
        output[1] = Math.toRadians(curAz) - azimuth;
        output[0] = output[0]* pdH;
        output[1] = output[1]* pdW;
        output[0] = centerH - output[0];
        output[1] += centerW;
        if(output[0] < 0) output[0]=0;
        else if(output[0] > scr_h) output[0] = scr_h;
        if(output[1] < 0) output[1]=0;
        else if(output[1] > scr_w) output[1] = scr_w;
        return output;


    }
    private double[] roll(double x, double y, double roll){
        //if (roll < 0) roll = -roll;
        roll = normalize(roll);
        double tmp[] = {x,y};
        tmp[0] = Math.sin(roll) * tmp[1] + Math.cos(roll) * tmp[0]; //vertical component
        tmp[1] = Math.cos(roll) * tmp[1] - Math.sin(roll) * tmp[0]; //horizontal component
        return tmp;
    }
    public float[] horizon(double angle, int width, double pitch, double azimuth, double roll){
        float output[] = {0,0,0,0};
        double center = Math.toRadians(angle) - pitch;
        center = center * pdH;
        center = centerH - center;
        output[0] = centerW - width/2;
        output[1] = (float) center;
        output[3] = (float) center;
        output[2] = centerW + width/2;

        /*output[0] = (float) roll( output[0], output[1], roll)[1];
        output[1] = (float) roll( output[0], output[1], roll)[0];
        output[2] = (float) roll( output[2], output[3], roll)[1];
        output[2] = (float) roll( output[2], output[3], roll)[0];*/
        return output;
    }
    public boolean containsTime(){
        return containsT;
    }
    public boolean ready(){
        return ready;
    }
    public float[] points(double pitch, double azimuth, double roll){
        //init output array. each hashmap key will need two coordinates in the final output
        float[] output = {};
        if(ready) {
            azimuth = Math.toRadians(normalize(Math.toDegrees(azimuth-Math.PI)));
            output = new float[(spCoor.length-1) * 4];
            double tmp[][] = new double[spCoor.length][2];
            int i = 0;
            for (double[] pair : spCoor) {
                //degrees from phone pointing vector
                tmp[i][0] = Math.toRadians(pair[0]) - pitch;
                tmp[i][1] = Math.toRadians(pair[1]) - azimuth;
                //pixels per degree from pointing vector
                tmp[i][0] = tmp[i][0] * pdH;
                tmp[i][1] = tmp[i][1] * pdW;
                //roll correction using expanded rotation matrix
                //roll +=
                //tmp[i][0] = Math.sin(roll) * tmp[i][1] + Math.cos(roll) * tmp[i][0]; //vertical component
                //tmp[i][1] = Math.cos(roll) * tmp[i][1] - Math.sin(roll) * tmp[i][0]; //horixontal component
                //correct coordinates to screen coordinates (0,0) top left and Y axis is inverted
                tmp[i][0] = centerH - tmp[i][0];
                tmp[i][1] += centerW;

                i++;
            }
            for (i = 0; i < tmp.length-1; i++) {
                //if (i != tmp.length-1) {
                    output[i * 4] = Math.round(tmp[i][1]);
                    output[i * 4 + 1] = Math.round(tmp[i][0]);
                    output[i * 4 + 2] = Math.round(tmp[i+1][1]);
                    output[i * 4 + 3] = Math.round(tmp[i+1][0]);
                    /*Log.i("Points" + Integer.toString(i), "{"+Double.toString(output[i * 2])+","
                            +Double.toString(output[i * 2+1])+","
                            +Double.toString(output[i * 2+2])+","
                            +Double.toString(output[i * 2+3])+"}");*/
                /*} else {
                    output[i * 4] = Math.round(tmp[i][1]);
                    output[i * 4 + 1] = Math.round(tmp[i][0]);
                }*/
            }
        }
        return output;
    }
    public HashMap<String, Double[]> getMap(){
        return myMap;
    }
}
