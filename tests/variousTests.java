import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by rojcek on 10.01.14.
 */
public class variousTests {
    public static void main(String [] args) {
        try {
            String now = "16:08:23";
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            Date date = sdf.parse(now);
            System.out.println(sdf.parse(now).getTime());
            now = "01:00:00";
            date = sdf.parse(now);
            System.out.println(date.getTime());
        }  catch (ParseException e) {
            e.printStackTrace();
        }

        /*SimpleDateFormat sdf1 = new SimpleDateFormat("HH:mm:ss");

        Calendar cal = Calendar.getInstance();
        Date d = cal.getTime();
        Date d2 = now;
        System.out.println("Current Time is:"+d);
        System.out.println("Time value using kk:"+sdf1.format(now));
        //date.parse(now); */
        System.out.println("end");
    }
}
