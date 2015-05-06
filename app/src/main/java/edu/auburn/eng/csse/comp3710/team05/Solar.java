
package edu.auburn.eng.csse.comp3710.team05;

/**
 * //Created by davis on 3/12/15.
 */

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class Solar {
    public static double equationOfTime(int daysOfYear){
        double D = (double) 360*(daysOfYear-81)/365;
        //System.out.println(D);
        double out = (9.87*Math.sin(Math.toRadians(2*D)))
                -(7.53*Math.cos(Math.toRadians(D)))-(1.5*Math.sin(Math.toRadians(D)));
        //System.out.println(out);
        return out;

    }
    public static double declinationAngle(int daysOfYear){
        double tmp = (double) (daysOfYear+234)/365*360;
        double out = 23.45*Math.sin(Math.toRadians(tmp));
        //System.out.println(tmp);
        //System.out.println(out);
        return out;
    }
    public static Calendar apparentSolarTime(double lng, Calendar time){
        if(time.getTimeZone().inDaylightTime(time.getTime())){
            long ms = time.getTimeInMillis();
            ms -= time.get(Calendar.DST_OFFSET);
            time.setTimeInMillis(ms);
            //System.out.println(time.getTime());
        }
        double lstm = 15 * Math.round(lng/15);
        //System.out.println(lstm);
        //System.out.println(equationOfTime((time.get(Calendar.DAY_OF_YEAR))));
        double offset = 4*(lstm-lng)+ equationOfTime((time.get(Calendar.DAY_OF_YEAR)));
        //System.out.println(offset);
        time.add(Calendar.MINUTE, (int) offset);

        return time;
    }
    public static double hourAngle(Calendar ast){
        double minPastMidnight = 60.0*ast.get(Calendar.HOUR_OF_DAY) + ast.get(Calendar.MINUTE)+ ast.get(Calendar.SECOND)/60.0;
        //System.out.println(minPastMidnight);
        return (minPastMidnight - 720) / 4;
    }
    //Given a 
    public static double altAngle(double lat, double lng, Calendar time){
        double out = Math.cos(Math.toRadians(lat))
                *Math.cos(Math.toRadians(declinationAngle(time.get(Calendar.DAY_OF_YEAR))))
                *Math.cos(Math.toRadians(hourAngle(apparentSolarTime(lng,time))))
                +Math.sin(Math.toRadians(lat))*Math.sin(Math.toRadians(declinationAngle(time.get(Calendar.DAY_OF_YEAR))));
        System.out.println(Math.toDegrees(Math.asin(out)));
        return out;
    }
    public static void main(String args[]){
        double val = Solar.declinationAngle(202);
        Calendar now = Calendar.getInstance();

        Solar.altAngle(32.6059,85.4866,now);
        System.out.println(Solar.hourAngle(now));
        System.out.println(Solar.declinationAngle(now.get(Calendar.DAY_OF_YEAR)));

    }

}

