import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by rojcek on 10.01.14.
 */
public class variousTests {
    public static void main(String [] args) {
        /*try {
            String now = "16:08:23";
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            Date date = sdf.parse(now);
            System.out.println(sdf.parse(now).getTime());
            now = "01:00:00";
            date = sdf.parse(now);
            System.out.println(date.getTime());
        }  catch (ParseException e) {
            e.printStackTrace();
        }*/

        ArrayList<String[]> sizes = new ArrayList<String[]>();
        NBBOfilter nbbOfilter = new NBBOfilter(5, 10);
        TreeMap<Double, ArrayList<String[]>> bids = new TreeMap<Double, ArrayList<String[]>>();
        ArrayList bids2minimize = new ArrayList();
        bids2minimize.add(1.9);bids2minimize.add(2.9);bids2minimize.add(0.9);bids2minimize.add(3.9);bids2minimize.add(3.2);
        bids2minimize.add(4.9);bids2minimize.add(3.9);bids2minimize.add(1.9);bids2minimize.add(1.3);bids2minimize.add(3.1);
        System.out.println(nbbOfilter.bestAsk(bids2minimize));


        String[] line1 = new String[9]; String[] line2 = new String[9];
        for (int i = 0; i < 9; i++){
            line1[i] = ""; line2[i] = "";
        }
        line1[6] = "10"; line2[6] = "11";line1[8] = "N";line2[8] = "P";
        sizes.add(line1); sizes.add((line2));
        bids.put(10.0, sizes);

        sizes = new ArrayList<String[]>(); line1[6] = "9";sizes.add(line1);
        bids.put(10.5, sizes);

        sizes = new ArrayList<String[]>(); line1[6] = "10";sizes.add(line1);
        bids.put(9.5, sizes);

        sizes = new ArrayList<String[]>(); line1[6] = "10";sizes.add(line1);
        bids.put(11.5, sizes);

        sizes = new ArrayList<String[]>(); line1[6] = "10"; line2[6] = "20";sizes.add(line1);sizes.add(line2);
        bids.put(12.5, sizes);

        System.out.println(nbbOfilter.bestAsk(bids)[0]);
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
