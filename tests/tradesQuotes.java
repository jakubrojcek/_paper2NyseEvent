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
 * This class sifts through many big zipped files given their names and produces per date cumulants of quotes, trades and volumes
 */
public class tradesQuotes {
    public static void main(String [] args) {
        double timeStart = System.nanoTime();
        String folder = "C:\\Users\\rojcek\\Documents\\School\\SFI\\_paper2b NYSE event\\DATA\\";
        HashMap<String, Long[]> quotesTrades = new HashMap<String, Long[]>();           // HashMap holding number of quotes and trades per day
        HashMap<String, Double[]> volumesShares = new HashMap<String, Double[]>();        // HashMap holding number of dollar and share volume per day
        HashMap<String, String[]> companysFiles = new HashMap<String, String[]>();  // HashMap holding company's name and a string of quotesFiles and a string of tradesFiles

        long nmQuotes = 0;                                                      // number of quotes
        long nmTrades = 0;                                                      // number of trades
        long nmQuotesNYSE = 0;                                                  // number of quotes
        long nmTradesNYSE = 0;                                                  // number of trades
        double shareVolume = 0.0;                                               // number of shares traded
        double shareVolumeNYSE = 0.0;                                           // number of shares traded at NYSE
        String[] companies = null;                                              // holder for the names or tickers of the companies

        // reading companies TODO: you can pass this later
        try {
            File myFile = new File(folder + "companies.csv");
            FileReader fileReader = new FileReader(myFile);
            BufferedReader reader = new BufferedReader(fileReader);
            String line = reader.readLine();
            companies = line.split(",");
            reader.close();
            fileReader.close();
        } catch (Exception ex){
            ex.printStackTrace();
        }
        // reading files TODO: you can pass this later
        try {
            File myFile = new File(folder + "quotesFiles.csv");
            FileReader fileReader = new FileReader(myFile);
            BufferedReader reader = new BufferedReader(fileReader);
            String line;
                while ((line = reader.readLine()) != null) {
                    String[] quotesFiles = line.split(",");
                    String quotesFilesTemp = new String();
                    for (int i = 1; i < quotesFiles.length; i++){
                        quotesFilesTemp += quotesFiles[i] + ",";
                    }
                    String[] entry = new String[2];
                    entry[0] = quotesFilesTemp;
                    companysFiles.put(quotesFiles[0], entry);
                }
            // process tradesFiles now
            myFile = new File(folder + "tradesFiles.csv");
            fileReader = new FileReader(myFile);
            reader = new BufferedReader(fileReader);
            while ((line = reader.readLine()) != null) {
                String[] tradesFiles = line.split(",");
                String tradesFilesTemp = new String();
                for (int i = 1; i < tradesFiles.length; i++){
                    tradesFilesTemp += tradesFiles[i] + ",";
                }
                String[] entry = companysFiles.get(tradesFiles[0]);
                entry[1] = tradesFilesTemp;
                companysFiles.put(tradesFiles[0], entry);
            }
            reader.close();
            fileReader.close();
        } catch (Exception ex){
            ex.printStackTrace();
        }

        for (String company : companies) {
            Long[] numbers = new Long[4];                   // holds numbers of quotes and trades
            Double[] volumes = new Double[2];               // holds volumes
            String[] quotesFiles = companysFiles.get(company)[0].split(",");

            for (String ZipFileName : quotesFiles){         // processing quotes
                try {
                    ZipFile zipFile = new ZipFile(folder + "\\quotes\\" + ZipFileName);
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

                        while ((line = br.readLine()) != null) {            // read until end
                            lineData = line.split(",");
                            if (!lineData[1].equals(date)){                 // new date, collect numbers
                                numbers[0] = nmQuotes;
                                numbers[1] = nmQuotesNYSE;
                                if (!quotesTrades.containsKey(date) && date != null){ // prevent duplicities
                                    quotesTrades.put(date, numbers);
                                }
                                numbers = new Long[4];                      // restart numbers
                                date = lineData[1];
                                nmQuotes = 1;
                                nmQuotesNYSE = 1;
                                System.out.println(date);
                            } else {
                                if(lineData[8].equals("N") || lineData[8].equals("P")){     // NYSE and Arca
                                    nmQuotesNYSE++;
                                } else {
                                    nmQuotes++;
                                }
                            }
                        }
                        if (!quotesTrades.containsKey(date) && date != null){ // prevent duplicities
                            quotesTrades.put(date, numbers);
                        }
                        br.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            String[] tradesFiles = companysFiles.get(company)[1].split(",");
            for (String ZipFileName : tradesFiles){        // processing trades
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
                                numbers[2] = nmTrades;
                                numbers[3] = nmTradesNYSE;
                                volumes[0] = shareVolume;
                                volumes[1] = shareVolumeNYSE;
                                if (quotesTrades.containsKey(date) && date != null){    // already collected date
                                    if (!volumesShares.containsKey(date)){
                                        volumesShares.put(date, volumes);
                                    }
                                }
                                date = lineData[1];                     // new date
                                if (quotesTrades.containsKey(date) && !volumesShares.containsKey(date)){    // asking for new date to get the numbers and not duplicities
                                    numbers = quotesTrades.get(date);
                                } else {
                                    numbers = new Long[4];
                                }
                                volumes = new Double[2];
                                nmTrades = 1;
                                nmTradesNYSE = 1;
                                shareVolume = 0.0;
                                shareVolumeNYSE = 0.0;
                                System.out.println(date);
                            } else  {
                                if(lineData[8].equals("N") || lineData[8].equals("P")){     // NYSE and ARCA
                                    nmTradesNYSE++;
                                    shareVolumeNYSE += Double.parseDouble(lineData[4]);
                                } else {
                                    nmTrades++;
                                    shareVolume += Double.parseDouble(lineData[4]);
                                }
                            }
                        }
                        if (quotesTrades.containsKey(date) && date != null){    // already collected date
                            if (!volumesShares.containsKey(date)){
                                volumesShares.put(date, volumes);
                            }
                        }
                        br.close();
                    }

                }  catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // priting quotes and trades
            try{
                String outputFileName = folder + "quotesTrades\\" + company + ".csv";
                FileWriter writer = new FileWriter(outputFileName, true);

                Iterator dates = quotesTrades.keySet().iterator();
                String date = null;
                while (dates.hasNext()){
                    date = (String) dates.next();
                    numbers = quotesTrades.get(date);
                    volumes = volumesShares.get(date);
                    writer.write(date + "," + numbers[0] + "," + numbers[1] + ","
                            + numbers[2] +"," + numbers[3] + ","
                            + volumes[0] + "," + volumes[1] + "," + "\r");
                }
                writer.close();
            }
            catch (Exception e){
                e.printStackTrace();
                System.exit(1);
            }
            quotesTrades = new HashMap<String, Long[]>();
            volumesShares = new HashMap<String, Double[]>();
        }




        // reading trades



        // printing trades
        /*try{
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
        }*/

        double timeEnd = System.nanoTime();
        System.out.println("Time elapsed: " + (timeEnd - timeStart));
    }
}
