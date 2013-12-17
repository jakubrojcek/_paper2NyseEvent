import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.zip.ZipFile;
import java.util.Enumeration;
import java.util.zip.ZipEntry;

/**
 * Created by rojcek on 17.12.13.
 */
public class IntradayTradesQuotes {
    public static void main(String [] args) {
        double timeStart = System.nanoTime();
        String folder = "D:\\_paper2 Nyse Event\\";
        String ZipFileName = "AA_2.zip";
        ArrayList qA = new ArrayList(4680);     // quotes at Ask per 5 second intervals
        ArrayList qB = new ArrayList(4680);     // quotes at Bid per 5 second intervals
        ArrayList n = new ArrayList(4680);      // number of trades per 5 second intervals
        try {
            ZipFile zipFile = new ZipFile(folder + "\\trades\\" + ZipFileName);
            Enumeration entries = zipFile.entries();
            ZipEntry ze = (ZipEntry) entries.nextElement();
            long size = ze.getSize();

            if (size > 0) {
                System.out.println("Length is " + size);
                BufferedReader br = new BufferedReader(new InputStreamReader(zipFile.getInputStream(ze)));
                String line;
                String[] lineData = null;
                String date = null;             // is string in data at position [1]
                br.readLine();                  // this is just header

                while ((line = br.readLine()) != null) {
                    lineData = line.split(",");
                    if (!lineData[1].equals(date)){             // new date, collect data
                        date = lineData[1];                     // new date
                        System.out.println(date);
                    } else  {
                        if(lineData[8].equals("N") || lineData[8].equals("P")){     // NYSE and ARCA

                        } else {

                        }
                    }
                }
                br.close();
            }

        }  catch (IOException e) {
            e.printStackTrace();
        }
    }
}
