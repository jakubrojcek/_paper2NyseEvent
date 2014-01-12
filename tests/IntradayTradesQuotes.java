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
        String[] companies = null;

        HashMap<String, ArrayList[]> tradesBursts = new HashMap<String, ArrayList[]>(); // date, trades per 5 secs
        HashMap<String, HashMap> allBursts = new HashMap<String, HashMap>();            // company and previous HashMap
        ArrayList qAll = new ArrayList(4681);   // all quotes per 5 second intervals
        ArrayList qAall = new ArrayList(4681);  // quotes at Ask per 5 second intervals
        ArrayList qBall = new ArrayList(4681);  // quotes at Bid per 5 second intervals
        ArrayList qNYSE = new ArrayList(4681);  // all quotes at NYSE per 5 second intervals
        ArrayList qAnyse = new ArrayList(4681); // quotes at Ask at NYSE per 5 second intervals
        ArrayList qBnyse = new ArrayList(4681); // quotes at Bid at NYSE per 5 second intervals
        ArrayList nAll = new ArrayList(4681);   // number of trades per 5 second intervals
        ArrayList nNYSE = new ArrayList(4681);  // number of trades at NYSE per 5 second intervals
        long SUMqALL = 0;   // interval sum of trades
        long SUMqAall = 0;  // interval sum of trades
        long SUMqBall = 0;  // interval sum of trades
        long SUMqNYSE = 0;  // interval sum of trades
        long SUMqAnyse = 0; // interval sum of trades
        long SUMqBnyse = 0; // interval sum of trades
        long SUMnALL = 0;   // interval sum of trades
        long SUMnNYSE = 0;  // interval sum of trades

        int interval = 5000; // the length of the interval is 5 sec or 5000 millisec
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss"); // format of the date to use
        String tradingStart = "9:30:00";
        String tradingEnd = "16:00:00";

        int Ask;
        int Bid;
        int AskSize;
        int BidSize;

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
            for (int i = 0; i < 4; i++){
                // reading quotes
                String ZipFileName = company + "_" + i + ".zip";
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
                        String date = null; // is string in data at position [1]
                        long time1 = sdf.parse(tradingStart).getTime(); // past 5 sec threshold
                        long timeEnd = sdf.parse(tradingEnd).getTime();
                        long time2 = time1; // current time
                        br.readLine(); // this is just header
                        Ask = 999; Bid = 999; AskSize = 999; BidSize = 999;

                        while ((line = br.readLine()) != null) {
                            lineData = line.split(",");
                            if (!lineData[1].equals(date)){         // new unique date, collect data
                                ArrayList[] bursts = {qAall, qAnyse, qBall, qBnyse, qAll, qNYSE, nAll, nNYSE};
                                if (!tradesBursts.containsKey(date) && date != null){   // if date still not in tradesBursts
                                    tradesBursts.put(date, bursts);
                                }
                                qAll = new ArrayList(4681);     qAall = new ArrayList(4681);    qBall = new ArrayList(4681);
                                qNYSE = new ArrayList(4681);    qAnyse = new ArrayList(4681);   qBnyse = new ArrayList(4681);
                                SUMqAall = 0;   SUMqAnyse = 0;  SUMqBall = 0;
                                SUMqBnyse = 0;  SUMqALL = 0;    SUMqNYSE = 0;
                                Ask = 999; Bid = 999; AskSize = 999; BidSize = 999;

                                date = lineData[1]; // new date
                                time1 = sdf.parse(tradingStart).getTime();
                                time2 = sdf.parse(lineData[2]).getTime();
                                long nextTime = (Math.min(time2, timeEnd) - time1) / interval;
                                while (nextTime > 0){   // setting empty time slots with 0's
                                    qAall.add(SUMqAall);    SUMqAall = 0;
                                    qAnyse.add(SUMqAnyse);  SUMqAnyse = 0;
                                    qBall.add(SUMqBall);    SUMqBall = 0;
                                    qBnyse.add(SUMqBnyse);  SUMqBnyse = 0;
                                    qAll.add(SUMqALL);      SUMqALL = 0;
                                    qNYSE.add(SUMqNYSE);    SUMqNYSE = 0;

                                    time1 += 5000;
                                    nextTime = (Math.min(time2, timeEnd) - time1) / interval;
                                }
                                if (time2 > timeEnd){
                                    continue;                      // TODO: can't break if I have more dates in 1 file
                                }
                                if(lineData[8].equals("N") || lineData[8].equals("P")){ // NYSE and ARCA
                                    SUMqNYSE++;
                                    if (Bid != Integer.parseInt(lineData[3]) || BidSize != Integer.parseInt(lineData[5])){
                                        SUMqBnyse++;
                                    }
                                    if (Ask != Integer.parseInt(lineData[4]) || AskSize != Integer.parseInt(lineData[6])){
                                        SUMqAnyse++;
                                    }
                                } else {
                                    SUMqALL++;
                                    if (Bid != Integer.parseInt(lineData[3]) || BidSize != Integer.parseInt(lineData[5])){
                                        SUMqBall++;
                                    }
                                    if (Ask != Integer.parseInt(lineData[4]) || AskSize != Integer.parseInt(lineData[6])){
                                        SUMqAall++;
                                    }
                                }
                                Ask = Integer.parseInt(lineData[4]); Bid = Integer.parseInt(lineData[3]);
                                AskSize = Integer.parseInt(lineData[6]); BidSize = Integer.parseInt(lineData[5]);
                                System.out.println(date);
                            } else {                        // still the same date, new line of quote
                                time2 = sdf.parse(lineData[2]).getTime();
                                if (time2 > timeEnd){
                                    continue;
                                }
                                long nextTime = (time2 - time1) / interval;
                                while (nextTime > 0){
                                    qAall.add(SUMqAall);    SUMqAall = 0;
                                    qAnyse.add(SUMqAnyse);  SUMqAnyse = 0;
                                    qBall.add(SUMqBall);    SUMqBall = 0;
                                    qBnyse.add(SUMqBnyse);  SUMqBnyse = 0;
                                    qAll.add(SUMqALL);      SUMqALL = 0;
                                    qNYSE.add(SUMqNYSE);    SUMqNYSE = 0;

                                    time1 += 5000; // TODO: increase by 5 until 55, then 0, alternatively put zeros until ratio less than 2
                                    nextTime = (time2 - time1) / interval;
                                }
                                if(lineData[8].equals("N") || lineData[8].equals("P")){ // NYSE and ARCA
                                    SUMqNYSE++;
                                    if (Bid != Integer.parseInt(lineData[3]) || BidSize != Integer.parseInt(lineData[5])){
                                        SUMqBnyse++;
                                    }
                                    if (Ask != Integer.parseInt(lineData[4]) || AskSize != Integer.parseInt(lineData[6])){
                                        SUMqAnyse++;
                                    }
                                } else {
                                    SUMqALL++;
                                    if (Bid != Integer.parseInt(lineData[3]) || BidSize != Integer.parseInt(lineData[5])){
                                        SUMqBall++;
                                    }
                                    if (Ask != Integer.parseInt(lineData[4]) || AskSize != Integer.parseInt(lineData[6])){
                                        SUMqAall++;
                                    }
                                }
                                Ask = Integer.parseInt(lineData[4]); Bid = Integer.parseInt(lineData[3]);
                                AskSize = Integer.parseInt(lineData[6]); BidSize = Integer.parseInt(lineData[5]);
                            }
                        }

                        if (!tradesBursts.containsKey(date) && date != null){   // for the last row of the last date
                            qAall.add(SUMqAall);
                            qAnyse.add(SUMqAnyse);
                            qBall.add(SUMqBall);
                            qBnyse.add(SUMqBnyse);
                            qAll.add(SUMqALL);
                            qNYSE.add(SUMqNYSE);
                            ArrayList[] bursts = {qAall, qAnyse, qBall, qBnyse, qAll, qNYSE, nAll, nNYSE};
                            tradesBursts.put(date, bursts);
                            SUMqAall = 0;   SUMqAnyse = 0;  SUMqBall = 0;
                            SUMqBnyse = 0;  SUMqALL = 0;    SUMqNYSE = 0;
                            qAll = new ArrayList(4681);     qAall = new ArrayList(4681);    qBall = new ArrayList(4681);
                            qNYSE = new ArrayList(4681);    qAnyse = new ArrayList(4681);   qBnyse = new ArrayList(4681);
                            Ask = 999; Bid = 999; AskSize = 999; BidSize = 999;
                        }
                        br.close();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ParseException p){
                    p.printStackTrace();
                }
            }

            for (int i = 0; i < 3; i++){
                String ZipFileName = company + "_" + i + ".zip";
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
                        String date = null; // is string in data at position [1]
                        long time1 = sdf.parse(tradingStart).getTime(); // past 5 sec threshold
                        long timeEnd = sdf.parse(tradingEnd).getTime();
                        long time2 = time1; // current time
                        br.readLine(); // this is just header

                        while ((line = br.readLine()) != null) {
                            lineData = line.split(",");
                            if (!lineData[1].equals(date)){         // new unique date, collect data
                                date = lineData[1]; // new date
                                if (tradesBursts.containsKey(date)){   // if date still not in tradesBursts
                                    ArrayList[] bursts = tradesBursts.get(date);
                                    bursts[6] = nAll;
                                    bursts[7] = nNYSE;
                                }
                                nAll = new ArrayList(4681);
                                nNYSE = new ArrayList(4681);
                                SUMnALL = 0;
                                SUMnNYSE = 0;

                                time1 = sdf.parse(tradingStart).getTime();
                                time2 = sdf.parse(lineData[2]).getTime();
                                long nextTime = (Math.min(time2, timeEnd) - time1) / interval;
                                while (nextTime > 0){   // setting empty time slots with 0's
                                    nAll.add(SUMnALL);      SUMnALL = 0;
                                    nNYSE.add(SUMnNYSE);    SUMnNYSE = 0;
                                    time1 += 5000;
                                    nextTime = (Math.min(time2, timeEnd) - time1) / interval;
                                }
                                if (time2 > timeEnd){
                                    continue;
                                }
                                if(lineData[8].equals("N") || lineData[8].equals("P")){ // NYSE and ARCA
                                    SUMnNYSE++;
                                } else {
                                    SUMnALL++;
                                }
                            } else {                        // still the same date, new line of quote
                                time2 = sdf.parse(lineData[2]).getTime();
                                if (time2 > timeEnd){
                                    continue;
                                }
                                long nextTime = (time2 - time1) / interval;
                                while (nextTime > 0){
                                    nAll.add(SUMnALL);      SUMnALL = 0;
                                    nNYSE.add(SUMnNYSE);    SUMnNYSE = 0;

                                    time1 += 5000;          // TODO: change this to interval
                                    nextTime = (time2 - time1) / interval;
                                }
                                if(lineData[8].equals("N") || lineData[8].equals("P")){ // NYSE and ARCA
                                    SUMnNYSE++;
                                } else {
                                    SUMnALL++;
                                }
                            }
                        }

                        if (tradesBursts.containsKey(date) && date != null){   // if date still not in tradesBursts
                            ArrayList[] bursts = tradesBursts.get(date);
                            bursts[6] = nAll;
                            bursts[7] = nNYSE;
                        }
                        nAll = new ArrayList(4681);
                        nNYSE = new ArrayList(4681);
                        SUMnALL = 0;
                        SUMnNYSE = 0;
                        br.close();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ParseException p){
                    p.printStackTrace();
                }
            }

            allBursts.put(company, tradesBursts);
            tradesBursts = new HashMap<String, ArrayList[]>();
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
