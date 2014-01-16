


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
public class IntradayJoiningFiles {
    public static void main(String [] args) {
        double timeStart = System.nanoTime();
        String folder = "D:\\_paper2 Nyse Event\\";
        TreeMap<Integer, String> joinedData = new TreeMap<Integer, String>();   // line number, array of values

        long nmQuotes = 0;                                                      // number of quotes
        long nmTrades = 0;                                                      // number of trades
        long nmQuotesNYSE = 0;                                                  // number of quotes
        long nmTradesNYSE = 0;                                                  // number of trades
        double shareVolume = 0.0;                                               // number of shares traded
        double shareVolumeNYSE = 0.0;                                           // number of shares traded at NYSE
        String[] companies = null;                                              // holder for the names or tickers of the companies
        String joinedLineData = null;
        String header = "date,";

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
            String FileName = company + "_burst.csv";
            try {
                File file = new File(folder + "\\quotesTrades\\" + FileName);
                FileReader fileReader = new FileReader(file);
                BufferedReader br = new BufferedReader(fileReader);
                String line;
                String companyEntry;
                int i = 1;
                while ((line = br.readLine()) != null) {            // read until end
                    if (joinedData.containsKey(i)){
                        joinedLineData = joinedData.get(i);
                        companyEntry = line.substring(9);
                        joinedLineData += "," + companyEntry;
                    } else {
                        joinedLineData= new String();
                        companyEntry = line;
                        joinedLineData += companyEntry;
                    }
                    joinedData.put(i, joinedLineData);
                    i++;
                }
                br.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
            header = header + company + "qAall," + company + "qAnyse," +  company + "qBall,"
                    + company + "qBnyse," + company + "qAll," + company + "qNYSE,"
                    + company + "nAll," + company + "nNYSE,";
        }

        // priting quotes and trades
        try{
            String outputFileName = folder + "quotesTrades\\IntradayJoinedData.csv";
            FileWriter writer = new FileWriter(outputFileName, true);
            header = header + "\r";
            writer.write(header);

            int sz = joinedData.keySet().size();
            for (int i = 1; i < (sz + 1); i++){
                joinedLineData = joinedData.get(i);
                writer.write(joinedLineData + "\r");
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
