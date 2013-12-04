import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.zip.ZipFile;
import java.util.Enumeration;
import java.util.zip.ZipEntry;


/**
 * Created with IntelliJ IDEA.
 * User: rojcek
 * Date: 04.12.13
 * Time: 15:57
 * To change this template use File | Settings | File Templates.
 */
public class tradesQuotes {
    public static void main(String [] args) {
        double timeStart = System.nanoTime();
        String folder = "D:\\_paper2 Nyse Event\\";
        HashMap<String, Long> quotes = new HashMap<String, Long>();     // HashMap holding number of quotes per day
        HashMap<String, Long> trades = new HashMap<String, Long>();     // HashMap holding number of trades per day
        long nmQuotes = 0;                                              // number of quotes
        long nmTrades = 0;                                              // number of quotes
        String companyFile = null;                                      // name of the company and of the file analyzed

        // reading quotes
        try {
            ZipFile zipFile = new ZipFile(folder + "\\quotes\\BAC_1.zip");
            companyFile = "BAC";
            Enumeration entries = zipFile.entries();
            BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

            ZipEntry ze = (ZipEntry) entries.nextElement();
            long size = ze.getSize();
            if (size > 0) {
                System.out.println("Length is " + size);
                BufferedReader br = new BufferedReader(new InputStreamReader(zipFile.getInputStream(ze)));   // TODO: test inputStream vs FileReader
                String line;
                String[] lineData = null;
                String date = null;             // is string in data at position [1]
                line = br.readLine();
                while ((line = br.readLine()) != null) {
                    lineData = line.split(",");
                    if (!lineData[1].equals(date)){
                        quotes.put(date, nmQuotes);
                        date = lineData[1];
                        nmQuotes = 1;
                        System.out.println(date);
                    } else /*if(lineData[8].equals("N"))*/ {
                        nmQuotes++;
                    }
                }
                br.close();
            }

        }  catch (IOException e) {
            e.printStackTrace();
        }

        try {
            ZipFile zipFile = new ZipFile(folder + "\\quotes\\BAC_2.zip");
            companyFile = "BAC";
            Enumeration entries = zipFile.entries();
            BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

            ZipEntry ze = (ZipEntry) entries.nextElement();
            long size = ze.getSize();
            if (size > 0) {
                System.out.println("Length is " + size);
                BufferedReader br = new BufferedReader(new InputStreamReader(zipFile.getInputStream(ze)));   // TODO: test inputStream vs FileReader
                String line;
                String[] lineData = null;
                String date = null;             // is string in data at position [1]
                line = br.readLine();
                while ((line = br.readLine()) != null) {
                    lineData = line.split(",");
                    if (!lineData[1].equals(date)){
                        quotes.put(date, nmQuotes);
                        date = lineData[1];
                        nmQuotes = 1;
                        System.out.println(date);
                    } else /*if(lineData[8].equals("N"))*/ {
                        nmQuotes++;
                    }
                }
                br.close();
            }

        }  catch (IOException e) {
            e.printStackTrace();
        }


        // priting quotes
        try{
            String outputFileName = folder + companyFile + ".csv";
            FileWriter writer = new FileWriter(outputFileName, true);

            Iterator dates = quotes.keySet().iterator();
            String date = null;
            while (dates.hasNext()){
                date = (String) dates.next();
                writer.write(date + "," + quotes.get(date) + "\r");
            }
            writer.close();
        }
        catch (Exception e){
            e.printStackTrace();
            System.exit(1);
        }


        // reading trades
        try {
            ZipFile zipFile = new ZipFile(folder + "\\trades\\BAC.zip");
            companyFile = "BAC";
            Enumeration entries = zipFile.entries();
            BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

            ZipEntry ze = (ZipEntry) entries.nextElement();
            long size = ze.getSize();
            if (size > 0) {
                System.out.println("Length is " + size);
                BufferedReader br = new BufferedReader(new InputStreamReader(zipFile.getInputStream(ze)));   // TODO: test inputStream vs FileReader
                String line;
                String[] lineData = null;
                String date = null;             // is string in data at position [1]
                while ((line = br.readLine()) != null) {
                    lineData = line.split(",");
                    if (!lineData[1].equals(date)){
                        trades.put(date, nmTrades);
                        date = lineData[1];
                        nmTrades = 1;
                        System.out.println(date);
                    } else /*if(lineData[8].equals("N"))*/ {
                        nmTrades++;
                    }
                }
                br.close();
            }

        }  catch (IOException e) {
            e.printStackTrace();
        }


        // printing trades
        try{
            String outputFileName = folder + companyFile + ".csv";
            FileWriter writer = new FileWriter(outputFileName, true);

            Iterator dates = trades.keySet().iterator();
            String date = null;
            while (dates.hasNext()){
                date = (String) dates.next();
                writer.write(date + "," + trades.get(date) + "\r");
            }
            writer.close();
        }
        catch (Exception e){
            e.printStackTrace();
            System.exit(1);
        }

        double timeEnd = System.nanoTime();
        System.out.println("Time elapsed: " + (timeEnd - timeStart));
    }
}
