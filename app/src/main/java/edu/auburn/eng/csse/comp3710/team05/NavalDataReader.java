/*
This class establishes a connection with the naval observatory's public
website and parses their data for the sun and moon's altitude and
azimuth. Of most importance is the data structure (HashMap) it provides
upon creation, which hashes an array containing the the altitude and 
azimuth of the celestial body (respectively) to the time of day. 
The member variables firstTime and lastTime correspond to the first and 
last time of the day that the celestial body is within visible range.
*/

//import android.util.Log;
package edu.auburn.eng.csse.comp3710.team05;

import java.util.*;
import java.net.*;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class NavalDataReader implements Runnable, Serializable {
    private String url = "http://aa.usno.navy.mil/cgi-bin/aa_altazw.pl?form=2&body=";
    private boolean isEast = false;
    private boolean isSouth = false;
    private String lattitude = " ";
    private String longitude = " ";
    private URL myUrl;
    private String body;
    private String tz;
    private String mid1;
    private String mid2;
    private String firstTime;
    private String lastTime;
    private String year, month, day, tz_sign, lat_sign, lon_sign;
    int[] lonData, latData;
    private HashMap<String, Double[]> values;
    public HashMap<String, Double[]> myvls;
    private transient Thread myThread;
    boolean flag4 = false;

    public NavalDataReader(String bodyIn, String lat, String lon) {
        double tempLat = Double.parseDouble(lat);
        double tempLon = Double.parseDouble(lon);
        body = bodyIn;
        tz_sign = "";
        if (tempLat < 0) isSouth = true;
        if (tempLon > 0) isEast = true;
        lon_sign = "";
        lat_sign = "";
        if (isSouth) lat_sign = "-1";
        else lat_sign = "1";
        if (isEast) lon_sign = "1";
        else lon_sign = "-1";
        lonData = convertDecToDeg(tempLon);
        latData = convertDecToDeg(tempLat);
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

        Date date = new Date();
        TimeZone tze = TimeZone.getDefault();
        String myDay = dateFormat.format(date);
        int offSet = tze.getOffset(date.getTime())/3600000;
        if (offSet >= 0) tz_sign = "1";
        if (offSet < 0) tz_sign = "-1";
        tz = Integer.toString(Math.abs(offSet));
        year = myDay.substring(0, 4);
        month = myDay.substring(5, 7);
        String time = myDay.substring(11, 16);
        int monthInt = Integer.parseInt(month);
        monthInt = monthInt / 1;
        month = String.valueOf(monthInt);
        day = myDay.substring(8, 10);
        int dayInt = Integer.parseInt(day);
        dayInt = dayInt / 1;
        day = String.valueOf(dayInt);
        if (body.equals("Moon")) body = "11";
        if (body.equals("Sun")) body = "10";
        url += body + "&year=" + year + "&month=" + month + "&day=" + day +
                "&intv_mag=1&place=%28no+name+given%29&lon_sign=" + lon_sign +
                "&lon_deg=" + lonData[0] + "&lon_min=" + lonData[1] +
                "&lat_sign=" + lat_sign + "&lat_deg=" + latData[0] + "&lat_min=" +
                latData[1] + "&tz=" + tz + "&tz_sign=" + tz_sign;

        myThread = new Thread(this);
        myThread.start();
    }



    public int[] convertDecToDeg(double numIn) {

        int degrees = (int) numIn;
        double m = (numIn % 1) * 60;
        int min = (int) m;
        int[] x = new int[2];
        x[0] = Math.abs(degrees);
        x[1] = Math.abs(min);
        return x;

    }

    public void run() {
        try {
            myUrl = new URL(url);
            HttpURLConnection connect = (HttpURLConnection) myUrl.openConnection();
            this.getInfo(connect);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            //Log.e("wtf", "Malformed URL");
        } catch (IOException e) {
            e.printStackTrace();
            //Log.e("wtf", "IOException URL");
        }

    }

    private void getInfo(HttpURLConnection con) {
        String page = " ";
        if (con == null) {
            System.out.println("Connection Problem");
            //Log.e("TAG", "Connection Problems");
        }
        if (con != null) {

            try {

                BufferedReader br =
                        new BufferedReader(
                                new InputStreamReader(con.getInputStream()));

                String input;

                while ((input = br.readLine()) != null) {
                    page += input + "\n";
                }
                br.close();

            } catch (IOException e) {
                //Log.e("TAG", "Connection Problems");
                e.printStackTrace();
            }

        }
        if (values == null)
            values = new HashMap<String, Double[]>();
        Scanner scan = new Scanner(page);
        for (int x = 0; x < 24; x++) {
            if (scan.hasNextLine()) {
                String dump = scan.nextLine();
            }
        }
        boolean flag = true;
        boolean flag1 = false;
        boolean flag3 = false;
        String anTime = " ";
        String time = " ";


        while (scan.hasNextLine()) {
            String line = scan.nextLine();
            if (line.contains("</")) break;
            if(line.length() < 2){
                flag3 = true;
                anTime = time;

            }
            if (line.contains(":")) {

                Scanner linescan = new Scanner(line);
                time = linescan.next();
                if (values.size() < 1) {
                    firstTime = time;
                    flag = false;
                }
                Double altitude = Double.parseDouble(linescan.next());
                Double azimuth = Double.parseDouble(linescan.next());
                Double[] myDubs = new Double[2];

                myDubs[0] = altitude;
                myDubs[1] = azimuth;
                values.put(time, myDubs);
                lastTime = time;
                if(flag3){
                    if(flag4) { flag4 = false; lastTime = anTime; break;}
                    values = new HashMap<String, Double[]>();
                    flag3 = false;
                    flag4 = true;
                    /*values.put("skip", new Double[0]);
                    mid2 = time;*/
                }
            }
        }
        if (flag4){
            Calendar c = Calendar.getInstance();
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date date = new Date();
            c.setTime(date);
            c.add(Calendar.DATE, 1);
            date = c.getTime();
            String myDay = dateFormat.format(date);
            day = myDay.substring(8, 10);

            url = "http://aa.usno.navy.mil/cgi-bin/aa_altazw.pl?form=2&body=";
            url += body + "&year=" + year + "&month=" + month + "&day=" + day +
                    "&intv_mag=1&place=%28no+name+given%29&lon_sign=" + lon_sign +
                    "&lon_deg=" + lonData[0] + "&lon_min=" + lonData[1] +
                    "&lat_sign=" + lat_sign + "&lat_deg=" + latData[0] + "&lat_min=" +
                    latData[1] + "&tz=" + tz + "&tz_sign=" + tz_sign;

            try {
                myUrl = new URL(url);
                HttpURLConnection connect = (HttpURLConnection) myUrl.openConnection();
                this.getInfo(connect);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                //Log.e("wtf", "Malformed URL");
            } catch (IOException e) {
                e.printStackTrace();
                //Log.e("wtf", "IOException URL");
            }


        }


        try {
            myThread.sleep(200);
        } catch(Exception e){

        }
        myvls = values;

    }

    public void getNextDay(){
        if(values.containsKey("skip")){

        }
    }

    public HashMap<String, Double[]> getValues() {
        return values;
    }

    public double[][] getOrderedValues() {
        int k = 0;
        double[][] mDo = new double[values.size() - 1][2];
        String aTime = firstTime.substring(0, 2) + firstTime.substring(3, 5);
        String aTime2 = lastTime.substring(0, 2) + lastTime.substring(3, 5);
        int firstNum = Integer.parseInt(firstTime.substring(0, 2));
        int last = Integer.parseInt(firstTime.substring(3, 5));
        String x = " ";
        while (!aTime.equals(aTime2)) {
            String zx = Integer.toString(firstNum);
            String zy = Integer.toString(last);
            if (zx.length() == 1) zx = "0" + zx;
            if (zy.length() == 1) zy = "0" + zy;
            x = zx + ":" + zy;
            Double[] t = values.get(x);
            mDo[k][0] = t[0];
            mDo[k][1] = t[1];
            if (x.equals(mid1)) {
                x = mid2;
                last = Integer.parseInt(x.substring(3, 5));
                firstNum = Integer.parseInt(x.substring(0, 2));
            }

            last = (last + 1) % 60;
            if (last == 0) {
                firstNum = (firstNum + 1) % 24;
            }
            String aNum = Integer.toString(firstNum);
            String aNum2 = Integer.toString(last);
            if (aNum.length() == 1) aNum = "0" + aNum;
            if (aNum2.length() == 1) aNum2 = "0" + aNum2;

            aTime = aNum + aNum2;
            k++;
        }


        return mDo;



    }
/*
    public static void main(String[] args){
        NavalDataReader myReader = new NavalDataReader("Moon", "32.617834", "-85.505733");
        while (myReader.myvls == null){
            try {
                Thread.sleep(9200);
            } catch(Exception e){

            }
        }
        double[][] vals = myReader.getOrderedValues();
        for(int x = 0; x< vals.length; x++){
            System.out.println(vals[x][0]);
            System.out.println(vals[x][1]);
        }
    }
}*/
}