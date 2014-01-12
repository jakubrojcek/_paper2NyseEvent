import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
        String ZipFileName = "AA_3.zip";
        HashMap<String, ArrayList[]> tradesBursts = new HashMap<String, ArrayList[]>(); // date, trades per 5 secs
        ArrayList qAll = new ArrayList(4681);   // all quotes per 5 second intervals
        ArrayList qAall = new ArrayList(4681);  // quotes at Ask per 5 second intervals
        ArrayList qBall = new ArrayList(4681);  // quotes at Bid per 5 second intervals
        ArrayList qNYSE = new ArrayList(4681);  // all quotes at NYSE per 5 second intervals
        ArrayList qAnyse = new ArrayList(4681); // quotes at Ask at NYSE per 5 second intervals
        ArrayList qBnyse = new ArrayList(4681); // quotes at Bid at NYSE per 5 second intervals
        ArrayList nAll = new ArrayList(4681);   // number of trades per 5 second intervals
        ArrayList nNYSE = new ArrayList(4681);  // number of trades at NYSE per 5 second intervals
        long SUMqALL = 0;                       // interval sum of trades
        long SUMqAall = 0;                      // interval sum of trades
        long SUMqBall = 0;                      // interval sum of trades
        long SUMqNYSE = 0;                      // interval sum of trades
        long SUMqAnyse = 0;                     // interval sum of trades
        long SUMqBnyse = 0;                     // interval sum of trades
        long SUMnALL = 0;                       // interval sum of trades
        long SUMnNYSE = 0;                      // interval sum of trades

        int interval = 5000;                    // the length of the interval is 5 sec or 5000 millisec
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");    // format of the date to use
        String tradingStart = "9:30:00";
        String tradingEnd = "16:00:00";

        int Ask;
        int Bid;
        int AskSize;
        int BidSize;

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
                long time1 = sdf.parse(tradingStart).getTime();                  // past 5 sec threshold
                long timeEnd = sdf.parse(tradingEnd).getTime();
                long time2 = time1;                  // current time
                br.readLine();                  // this is just header

                while ((line = br.readLine()) != null) {
                    lineData = line.split(",");
                    if (!lineData[1].equals(date)){             // new unique date, collect data
                        ArrayList[] bursts = {nAll, nNYSE};
                        if (date != null){
                            tradesBursts.put(date, bursts);
                            SUMnALL = 0;
                            SUMnNYSE = 0;
                        }
                        date = lineData[1];                     // new date
                        time1 = sdf.parse(tradingStart).getTime();
                        time2 = sdf.parse(lineData[2]).getTime();
                        long nextTime = (time2 - time1) / interval;
                        while (nextTime > 0){
                            nAll.add(SUMnALL);
                            nNYSE.add(SUMnNYSE);
                            time1 += 5000;                          // TODO: increase by 5 until 55, then 0, alternatively put zeros until ratio less than 2
                            SUMnALL = 0;
                            SUMnNYSE = 0;
                            nextTime = (time2 - time1) / interval;
                        }
                        if(lineData[8].equals("N") || lineData[8].equals("P")){     // NYSE and ARCA
                            SUMnNYSE++;
                        } else {
                            SUMnALL++;
                        }
                        System.out.println(date);
                    } else {
                        time2 = sdf.parse(lineData[2]).getTime();
                        if (time2 > timeEnd){
                            break;
                        }
                        long nextTime = (time2 - time1) / interval;
                        while (nextTime > 0){
                            nAll.add(SUMnALL);
                            nNYSE.add(SUMnNYSE);
                            time1 += 5000;                          // TODO: increase by 5 until 55, then 0, alternatively put zeros until ratio less than 2
                            SUMnALL = 0;
                            SUMnNYSE = 0;
                            nextTime = (time2 - time1) / interval;
                        }
                        if(lineData[8].equals("N") || lineData[8].equals("P")){     // NYSE and ARCA
                            SUMnNYSE++;
                        } else {
                            SUMnALL++;
                        }
                    }
                }

                if (!tradesBursts.containsKey(date) && date != null){
                    nAll.add(SUMnALL);
                    nNYSE.add(SUMnNYSE);
                    ArrayList[] bursts = {nAll, nNYSE};
                    if (date != null){
                        tradesBursts.put(date, bursts);
                        SUMnALL = 0;
                        SUMnNYSE = 0;
                    }
                }
                br.close();
            }

        }  catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException p){
            p.printStackTrace();
        }

        try{
            String outputFileName = folder + "quotesTrades\\" + "AA_burst" + ".csv";
            FileWriter writer = new FileWriter(outputFileName, true);

            Iterator dates = tradesBursts.keySet().iterator();
            String date = null;
            int sz = 0;
            while (dates.hasNext()){
                date = (String) dates.next();
                ArrayList[] bursts = tradesBursts.get(date);
                sz = bursts[0].size();
                for (int i = 0; i < sz; i++){
                    writer.write(bursts[0].get(i) + "," + bursts[1].get(i) + "\r");
                }
            }
            writer.close();
        }
        catch (Exception e){
            e.printStackTrace();
            System.exit(1);
        }

    }
}
