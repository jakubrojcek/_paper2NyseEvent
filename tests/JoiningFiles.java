import java.io.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created with IntelliJ IDEA.
 * User: rojcek
 * Date: 06.12.13
 * Time: 17:10
 * To change this template use File | Settings | File Templates.
 */
public class JoiningFiles {
    public static void main(String [] args) {
        double timeStart = System.nanoTime();
        String folder = "D:\\_paper2 Nyse Event\\";
        TreeMap<String, HashMap<String, String>> joinedData = new TreeMap<String, HashMap<String, String>>();   // date, company, values

        long nmQuotes = 0;                                                      // number of quotes
        long nmTrades = 0;                                                      // number of trades
        long nmQuotesNYSE = 0;                                                  // number of quotes
        long nmTradesNYSE = 0;                                                  // number of trades
        double shareVolume = 0.0;                                               // number of shares traded
        double shareVolumeNYSE = 0.0;                                           // number of shares traded at NYSE
        String[] companies = null;                                              // holder for the names or tickers of the companies
        HashMap<String, String> dateData;

        // reading companies
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

        for (String company : companies) {
                // reading quotes
                String FileName = company + ".csv";
                try {
                    File file = new File(folder + "\\quotesTrades\\" + FileName);
                    FileReader fileReader = new FileReader(file);
                    BufferedReader br = new BufferedReader(fileReader);
                    String line;
                    String companyEntry;
                    String[] lineData = null;
                    while ((line = br.readLine()) != null) {            // read until end
                        lineData = line.split(",");
                        if (!joinedData.containsKey(lineData[0])){      // new date, collect numbers
                            dateData = new HashMap<String, String>();
                            joinedData.put(lineData[0], dateData);
                        } else {
                            dateData = joinedData.get(lineData[0]);
                        }
                        companyEntry = line.substring(9, line.length());
                        dateData.put(company, companyEntry);
                    }
                    br.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

        // priting quotes and trades
        try{
            String outputFileName = folder + "quotesTrades\\joinedData.csv";
            FileWriter writer = new FileWriter(outputFileName, true);

            Iterator dates = joinedData.keySet().iterator();
            Iterator companyKeys;
            HashMap<String, String> companyLine;
            String date = null;
            String[] completeData = new String[joinedData.keySet().size()];
            String company;
            String header = "date,";
            int i = 0;
            while (dates.hasNext()){
                date = (String) dates.next();
                companyKeys = joinedData.get(date).keySet().iterator();
                companyLine = joinedData.get(date);
                completeData[i] = date + ",";
                while (companyKeys.hasNext()){
                    company = (String) companyKeys.next();
                        if (i == 0){
                            header = header + company + "nmQuotes," + company + "nmQuotesNYSE," +
                                    company + "nmTrades," + company + "nmTradesNYSE," +
                                    company + "shareVolume," + company + "shareVolumeNYSE,";
                        }
                    completeData[i] = completeData[i] + companyLine.get(company);
                }
                completeData[i] = completeData[i] + "\r";
                if (i == 0){header = header + "\r";}
                i++;
            }
            writer.write(header);
            for (String s : completeData){
                writer.write(s);
            }
            writer.close();
        }
        catch (Exception e){
            e.printStackTrace();
            System.exit(1);
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
