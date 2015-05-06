/*  This class provides static methods to compute the moon's ephemeris.
The equations and calculations are based on a paper titled
"Low-Precision Formulae for Planetary Positions" by T.C. Van Flandern
and K. F. Pulkkinen.
*/
package edu.auburn.eng.csse.comp3710.team05;


import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.sql.Timestamp;



public class MoonCalculator implements Serializable{
	private static int year,month,day,hours,minutes;
	private static double seconds;
	private static double PI = 3.1415926535897932;
    private static Date date;

	//This method takes no parameters and returns the time since
	//the new standard epoch (noon, January 1, 2000)
	public static double getTime() {
	//declare a date
	 date= new Date();
	 //Get the current time stamp and convert it to a string
	 String myStamp = new Timestamp(date.getTime()).toString();
	 //myStamp = "1991 05-19 15:00:00";
	 //Parse out the year, month, hour, minutes and seconds
	 year = Integer.parseInt(myStamp.substring(0,4));
	 month = Integer.parseInt(myStamp.substring(5,7));
	 day = Integer.parseInt(myStamp.substring(8,10));
	 hours = Integer.parseInt(myStamp.substring(11,13));
	 minutes = Integer.parseInt(myStamp.substring(14,16));
	 seconds = Double.parseDouble(myStamp.substring(17, 19));
	 //calculate julian date
	 double juliandate = 367*year - (7*(year + (month + 9)/12)/4) + (275*month/9)+day - 730530;
	 //calculate julian time
	 double juliantime = juliandate + (hours + (minutes + seconds/60)/60)/24;
	 //return the time, in days, since the new standard epoch (noon, January 1, 2000)
	 return juliantime;
	}

	//This method takes the time since the new standard epoch and returns
	//the julian centuries since epoch 1900.0
	public static double getCenturies(double time){
		return time/36525;
    }
	public static double normalize(double num)
    {
		return num - Math.floor(num/360.0)*360.0;
	}

	public static double[] getArguments(int adder, double latIn, double longIn){
		float addition = (float)adder;
        double t = getTime() + addition;
		//System.out.println(t);
		//longitude of ascending node
		double n = 125.1228 - 0.0529538083*t;
		if(n>360.0 || n<0.0) n=normalize(n);
		//inclination to the eliptic plane
		double i = 5.1454;
		//argument of perihelion
		double w = 318.0634 + .1643573223*t;
		if(w>360.0 || w<0.0) w=normalize(w);
		//mean distance from Sun
		double a = 60.2666;
		//eccentricity
		double e = 0.054900;
		//mean anomaly
		double m = 115.3654 + 13.0649929509*t;
		if(m>360.0 || m<0.0) m=normalize(m);

		double eccentricAn = m+e*(180/PI)*Math.sin(m)*(1.0 + e*Math.cos(m));
		double e0 = eccentricAn;
		double e1 = e0 - (e0 - (180/PI) * e * Math.sin(e0*PI/180) - m) / (1 - e * Math.cos(e0*PI/180));
		while(Math.abs(e0-e1)>0.006){
			e0=e1;
			e1=e0 - (e0 - (180/PI) * e * Math.sin(e0*PI/180) - m) / (1 - e * Math.cos(e0*PI/180));
		}
		eccentricAn = e1;

		//obliquity of ecliptic
		double ecl = 23.4393 - .0000003563*t;
		double xv = a*(Math.cos((eccentricAn)*PI/180) - e);
		double yv = a*(Math.sqrt(1.0 -e*e)*Math.sin(eccentricAn*PI/180));
		//System.out.println(xv);
		//System.out.println(yv);
		//true anomaly
		double v = normalize((180/PI)*Math.atan2(yv*PI/180,xv*PI/180));
		//distance
		double r = Math.sqrt(xv*xv + yv*yv);
		//compute the planets position in 3D space
		double xh = r*(Math.cos(n*PI/180)*Math.cos((v+w)*PI/180) - Math.sin(n*PI/180)*Math.sin((v+w)*PI/180)*Math.cos(i*PI/180));
		double yh = r*(Math.sin(n*PI/180)*Math.cos((v+w)*PI/180) + Math.cos(n*PI/180)*Math.sin((v+w)*PI/180)*Math.cos(i*PI/180));
		double zh = r*(Math.sin((v+w)*PI/180)*Math.sin(i*PI/180));

		//compute ecliptic longitude and latitude
		double ecllon = normalize((180/PI)*Math.atan2(yh,xh));
		double ecllat = (180/PI)*Math.atan2(zh, Math.sqrt(xh*xh+yh*yh));
		System.out.println(ecllon + "\n" + ecllat);
		//rotate ecliptic rectangular coordinates to equatorial rectangular coordinates
		double xequat = xh;
		double yequat = yh*Math.cos(ecl*PI/180)-zh*Math.sin(ecl*PI/180);
		double zequat = yh*Math.sin(ecl*PI/180)+zh*Math.cos(ecl*PI/180);
		//compute right ascension(ra) and declination
		double ra = normalize((180/PI)*Math.atan2(yequat,xequat));
		double requat = Math.sqrt(xequat*xequat+yequat*yequat+zequat*zequat);
		double declination = 180/PI*Math.atan2(zequat, Math.sqrt(xequat*xequat+yequat*yequat));
		///System.out.println(declination);

		double t1 = getCenturies(t);
		//delta in degrees
		double delta = declination;
		//compute sidereal time
		//adjust to local sidereal time

		double theta0 = calculateApparentSiderealTime(new JulianDay(new Date()), ecl);
		//theta0 = calculateApparentSiderealTime(new JulianDay(new Date(year, month, day, hours, 0, 0)), ecl);
		double theta1 = normalize(theta0);
		double theta = normalize(theta1-longIn);
		//local hour angle
		double tau = theta - ra;
		//convert to horizon coordinates
		double beta = latIn;
		double temp = Math.sin(beta*PI/180)*Math.sin(delta*PI/180)+Math.cos(beta*PI/180)*Math.cos(delta*PI/180)*Math.cos(tau*PI/180);
		double temp2 = -Math.sin(tau*PI/180);
		double temp3 = (Math.cos(beta*PI/180)*Math.atan(delta*PI/180)-Math.sin(beta*PI/180)*Math.cos(tau*PI/180));
		double h = 180/PI*Math.asin(temp);
		double az = 180/PI*Math.atan2(temp2,temp3);
		//compute the parallax
		double horParal = 8.794/(r/149597870.0);
		double temp4 = Math.cos(h*PI/180)*Math.sin(horParal/3600*PI/180);
		//paralax in altitude
		double par = Math.asin(temp4);
		//correct altitude for paralax
		h = h-par;
		//System.out.println(h + "\n" + az);
		double[] args = new double[2];
		args[0] = az;
		args[1] = h;
		return args;
	}

    public static HashMap<String, Double[]> makeMap(double[][] valsIn, int max){
        HashMap<String, Double[]> aMap = new HashMap<String, Double[]>();
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");


        String myDay = dateFormat.format(date);
        String time = myDay.substring(11, 16);
        int first = Integer.parseInt(time.substring(0,2));
        int sec = Integer.parseInt(time.substring(3,5));

        for (int x = 0; x < max; x++){
            Double[] myDub = new Double[2];
            myDub[0] = new Double(valsIn[x][0]);
            myDub[1] = new Double(valsIn[x][1]);
            aMap.put(time, myDub);
            sec = (sec + 1) % 60;
            if (sec == 0)  first = (first + 1) % 24;
            String zx = Integer.toString(first);
            String zy = Integer.toString(sec);
            if (zx.length() == 1) zx = "0" + zx;
            if (zy.length() == 1) zy = "0" + zy;
            time = zx + ":" + zy;

        }

        return aMap;
    }

/**
   * Calculates the mean sidereal time for the given julian day. The correction
   * for nutation is not taken into account.
   *
   * @param jd
   *          the julian day
   * @return the mean sidereal time in degrees
   */

   public static double calculateMeanSiderealTime(JulianDay jd) {
    double t = jd.getTimeFromJ2000();
    double t2 = t * t;
    // calculate the mean sidereal time at Greenwich for that instant
    return (280.46061837 + 360.98564736629 * (jd.getJD() - 2451545.0)
        + 0.000387933 * t2 - (t * t2) / 38710000);
  }


/**
   * Calculates the mean sidereal time for the given julian day. The correction
   * for nutation is taken into account.
   *
   * @param jd
   *          the julian day
   * @return the apparent sidereal time in degrees
   */

  public static double calculateApparentSiderealTime(JulianDay jd, double ecl) {
    //log.debug("Into SiderealTime.calculateApparentSiderealTime");
    double deltaPsi = new Nutation(jd).getDeltaLongitude() / 3600; // degrees
    //log.debug("Nutation in longitude = " + deltaPsi + " degrees" );
    //log.debug("Ecliptic obliquity = " + eps + " degrees" );
    return calculateMeanSiderealTime(jd) + (Math.cos(Math.toRadians(ecl))
        * deltaPsi);
  }

	public static void main(String[] args){
		double[] x = getArguments(0,0,0);
	}
}


