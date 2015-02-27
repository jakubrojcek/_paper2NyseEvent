import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by rojcek on 17.12.13.
 * outputs NBBO, midquote, NBBO filtered transactions, #trades, #quotes, #for NYSE, #bestQuotes, #bestNYSE, #filtered trades per exchange
 * input quotesFiles AA,AA_0.zip,AA_1.zip,AA_2.zip,AA_3.zip,AA_4.zip.. next line
 * and trades files in the same format.
 */
public class IntradayTradesQuotes {
    public static void main(String [] args) {
        double timeBeginAlgo = System.nanoTime();
        String folder = "C:\\Users\\rojcek\\Documents\\School\\SFI\\_paper2b NYSE event\\DATA\\";

        int interval = 5000;          // the length of the interval is 5 sec or 5000 millisec, 468000 = 7.8' or 50 intervals
        int intervalFilter = 90000;     // the length of interval looking back to NBBOs to discard transaction, 90s reporting obligation period
        int nWindow = (intervalFilter / interval) + 1;
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss"); // format of the date to use
        String tradingStart = "9:30:00";
        String tradingEnd = "16:00:00";
        int nInterval = 1;
        try {
            nInterval = 1 + (int)(sdf.parse(tradingEnd).getTime() - sdf.parse(tradingStart).getTime()) / interval;
        } catch (Exception ex){
            ex.printStackTrace();
        }

        NBBOfilter nbbOfilter = new NBBOfilter(nWindow, nInterval);       // filter used to determine the NBBO and whether to keep a transaction
        HashMap<String, String[]> companysFiles = new HashMap<String, String[]>();  // HashMap holding company's name and a string of quotesFiles and a string of tradesFiles
        TreeMap<String, ArrayList[]> quotesTrades = new TreeMap<String, ArrayList[]>();     // KEY: date, #quotes, #trades, bid, ask per interval, keys are sorted
        HashMap<String, TreeMap> allQuotesTrades = new HashMap<String, TreeMap>();            // company and previous HashMap
        TreeMap<Double, ArrayList<String[]>> bids = new TreeMap<Double, ArrayList<String[]>>(); // holder for transaction lines for bid as a key for an interval
        TreeMap<Double, ArrayList<String[]>> asks = new TreeMap<Double, ArrayList<String[]>>(); // holder for transaction lines for ask as a key for an interval
        ArrayList<String[]> transactions = new ArrayList<String[]>();          // holder for transactions for a day
        ArrayList<String[]> transactionsInterval = new ArrayList<String[]>();          // holder for transactions for a day
        ArrayList qAll = new ArrayList(nInterval);      // # all quotes per 5 second intervals
        ArrayList qAllNBBO = new ArrayList(nInterval);  // # all NBBO quotes per 5 second intervals
        ArrayList qAall = new ArrayList(nInterval);     // # quotes at Ask per 5 second intervals
        ArrayList qBall = new ArrayList(nInterval);     // # quotes at Bid per 5 second intervals
        ArrayList qAskNBOO = new ArrayList(nInterval);  // ask prices per 5 second intervals
        ArrayList qBidNBBO = new ArrayList(nInterval);  // bid prices per 5 second intervals
        ArrayList qNYSE = new ArrayList(nInterval);     // # all quotes at NYSE per 5 second intervals
        ArrayList qNyseNBBO = new ArrayList(nInterval); // # all remaining quotes at NYSE per 5 second intervals
        ArrayList qAnyse = new ArrayList(nInterval);    // # quotes at Ask at NYSE per 5 second intervals
        ArrayList qBnyse = new ArrayList(nInterval);    // # quotes at Bid at NYSE per 5 second intervals
        ArrayList nAll = new ArrayList(nInterval);      // # number of trades per 5 second intervals
        ArrayList nAllNBOO = new ArrayList(nInterval);  // # number of remaining trades per 5 second intervals
        ArrayList nNYSE = new ArrayList(nInterval);     // # number of trades at NYSE per 5 second intervals
        ArrayList nNyseNBBO = new ArrayList(nInterval); // # number of remaining trades at NYSE per 5 second intervals
        ArrayList nPrices = new ArrayList(nInterval);   // average transaction prices per 5 second intervals
        ArrayList nQuantities = new ArrayList(nInterval);// average transaction quantities per 5 second intervals
        long SUMqALL = 0;                               // interval sum of quotes all
        long SUMqAall = 0;                              // interval sum of ask quotes
        long SUMqBall = 0;                              // interval sum of bids quotes
        long SUMqNYSE = 0;                              // interval sum of quotes NYSE
        long SUMqAnyse = 0;                             // interval sum of ask quotes NYSE
        long SUMqBnyse = 0;                             // interval sum of bid quotes NYSE
        long SUMqFilteredAll = 0;                       // interval sum of remaining quotes all
        long SUMqFilteredNYSE = 0;                      // interval sum of remaining quotes NYSE
        long SUMnALL = 0;                               // interval sum of trades all
        long SUMnNYSE = 0;                              // interval sum of trades NYSE
        long SUMnFilteredAll = 0;                       // interval sum of remaining trades all
        long SUMnFilteredNYSE = 0;                      // interval sum of remaining trades NYSE
        double bestBid = 0.0;                           // best bid in the interval
        double bestAsk = 999.0;                         // best bid in the interval
        double vwapPrice = 0.0;                         // vwap price in the interval
        double vwapQuantity = 999.0;                    // vwap quantity in the interval
        double[] bids4Filtering = new double[nInterval];// to determine if to keep transaction or not
        double[] asks4Filtering = new double[nInterval];// to determine if to keep transaction or not

        double Ask;
        double Bid;
        int AskSize;
        int BidSize;
        ArrayList<String[]> newLine;

        // reading files
        try {
            File myFile = new File(folder + "quotesFiles3.csv");
            FileReader fileReader = new FileReader(myFile);
            BufferedReader reader = new BufferedReader(fileReader);
            String line;
            while ((line = reader.readLine()) != null) {    // for each company
                String[] quotesFiles = line.split(",");
                String quotesFilesTemp = new String();
                for (int i = 1; i < quotesFiles.length; i++){   // collect the quotes files, [0] entry is the company
                    quotesFilesTemp += quotesFiles[i] + ",";
                }
                String[] entry = new String[2];
                entry[0] = quotesFilesTemp;
                companysFiles.put(quotesFiles[0], entry);
            }
            // process tradesFiles now
            myFile = new File(folder + "tradesFiles3.csv");
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

        Iterator companies = companysFiles.keySet().iterator();
        while (companies.hasNext()){                        // for all companies, run through its files
            String company = (String) companies.next();
            String[] quotesFiles = companysFiles.get(company)[0].split(",");    // first through its quotes files
            transactions = new ArrayList<String[]>();

            for (String ZipFileName : quotesFiles){         // processing quotes
                // reading quotes
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
                        String date = "nullDate"; // is string in data at position [1]
                        long time1 = sdf.parse(tradingStart).getTime(); // past 5 sec threshold
                        long timeEnd = sdf.parse(tradingEnd).getTime();
                        long time2 = time1; // current time
                        br.readLine(); // this is just header
                        Ask = 999.0; Bid = 999.0; AskSize = 999; BidSize = 999;

                        while ((line = br.readLine()) != null) {
                            lineData = line.split(",");
                            if (!lineData[1].equals(date)){         // new unique date, collect data
                                double[] filteredBids = nbbOfilter.bestBid(bids);
                                double[] filteredAsks = nbbOfilter.bestAsk(asks);
                                SUMqFilteredAll = (long)(filteredBids[1] + filteredAsks[1]);
                                SUMqFilteredNYSE = (long)(filteredBids[2] + filteredAsks[2]);
                                if (filteredAsks[0] >= filteredBids[0]){
                                    bestAsk = filteredAsks[0];
                                    bestBid = filteredBids[0];
                                }
                                qAall.add(SUMqAall);            SUMqAall = 0;
                                qAnyse.add(SUMqAnyse);          SUMqAnyse = 0;
                                qBall.add(SUMqBall);            SUMqBall = 0;
                                qBnyse.add(SUMqBnyse);          SUMqBnyse = 0;
                                qAll.add(SUMqALL);              SUMqALL = 0;
                                qNYSE.add(SUMqNYSE);            SUMqNYSE = 0;
                                qAllNBBO.add(SUMqFilteredAll);  SUMqFilteredAll = 0;
                                qNyseNBBO.add(SUMqFilteredNYSE);SUMqFilteredNYSE = 0;
                                qAskNBOO.add(bestAsk);  // don't change best ask
                                qBidNBBO.add(bestBid);  // don't change best ask
                                bids = new TreeMap<Double, ArrayList<String[]>>(); asks = new TreeMap<Double, ArrayList<String[]>>();
                                if (!quotesTrades.containsKey(date) && !date.equals("nullDate")){   // if date still not in quotesTrades
                                    long nextTime = (timeEnd - time1) / interval;
                                    while (nextTime > 0){   // setting empty time slots with 0's
                                        filteredBids = nbbOfilter.bestBid(bids);
                                        filteredAsks = nbbOfilter.bestAsk(asks);
                                        SUMqFilteredAll = (long)(filteredBids[1] + filteredAsks[1]);
                                        SUMqFilteredNYSE = (long)(filteredBids[2] + filteredAsks[2]);
                                        if (filteredAsks[0] >= filteredBids[0]){
                                            bestAsk = filteredAsks[0];
                                            bestBid = filteredBids[0];
                                        }
                                        qAall.add(SUMqAall);            SUMqAall = 0;
                                        qAnyse.add(SUMqAnyse);          SUMqAnyse = 0;
                                        qBall.add(SUMqBall);            SUMqBall = 0;
                                        qBnyse.add(SUMqBnyse);          SUMqBnyse = 0;
                                        qAll.add(SUMqALL);              SUMqALL = 0;
                                        qNYSE.add(SUMqNYSE);            SUMqNYSE = 0;
                                        qAllNBBO.add(SUMqFilteredAll);  SUMqFilteredAll = 0;
                                        qNyseNBBO.add(SUMqFilteredNYSE);SUMqFilteredNYSE = 0;
                                        qAskNBOO.add(bestAsk);  // don't change best ask
                                        qBidNBBO.add(bestBid);  // don't change best ask
                                        bids = new TreeMap<Double, ArrayList<String[]>>(); asks = new TreeMap<Double, ArrayList<String[]>>();
                                        time1 += interval;
                                        nextTime = (timeEnd - time1) / interval;
                                    }
                                    ArrayList[] quotesTradesData = {qAall, qAnyse, qBall, qBnyse, qAll, qNYSE, nAll, nNYSE, qAskNBOO, qBidNBBO, qAllNBBO, qNyseNBBO, nAllNBOO, nNyseNBBO, nPrices, nQuantities};
                                    quotesTrades.put(date, quotesTradesData);
                                }
                                qAll = new ArrayList(nInterval);     qAall = new ArrayList(nInterval);    qBall = new ArrayList(nInterval);
                                qNYSE = new ArrayList(nInterval);    qAnyse = new ArrayList(nInterval);   qBnyse = new ArrayList(nInterval);
                                qAllNBBO = new ArrayList(nInterval); qNyseNBBO = new ArrayList(nInterval);qAskNBOO = new ArrayList(nInterval);  qBidNBBO = new ArrayList(nInterval);
                                bids = new TreeMap<Double, ArrayList<String[]>>(); asks = new TreeMap<Double, ArrayList<String[]>>();
                                Ask = 999; Bid = 999; AskSize = 999; BidSize = 999; bestAsk = 999.0; bestBid = 999.0;

                                date = lineData[1]; // new date
                                time1 = sdf.parse(tradingStart).getTime();
                                time2 = sdf.parse(lineData[2]).getTime();
                                long nextTime = (Math.min(time2, timeEnd) - time1) / interval;
                                while (nextTime > 0){   // setting empty time slots with 0's
                                    filteredBids = nbbOfilter.bestBid(bids);
                                    filteredAsks = nbbOfilter.bestAsk(asks);
                                    SUMqFilteredAll = (long)(filteredBids[1] + filteredAsks[1]);
                                    SUMqFilteredNYSE = (long)(filteredBids[2] + filteredAsks[2]);
                                    if (filteredAsks[0] >= filteredBids[0]){
                                        bestAsk = filteredAsks[0];
                                        bestBid = filteredBids[0];
                                    }
                                    qAall.add(SUMqAall);            SUMqAall = 0;
                                    qAnyse.add(SUMqAnyse);          SUMqAnyse = 0;
                                    qBall.add(SUMqBall);            SUMqBall = 0;
                                    qBnyse.add(SUMqBnyse);          SUMqBnyse = 0;
                                    qAll.add(SUMqALL);              SUMqALL = 0;
                                    qNYSE.add(SUMqNYSE);            SUMqNYSE = 0;
                                    qAllNBBO.add(SUMqFilteredAll);  SUMqFilteredAll = 0;
                                    qNyseNBBO.add(SUMqFilteredNYSE);SUMqFilteredNYSE = 0;
                                    qAskNBOO.add(bestAsk);  // don't change best ask
                                    qBidNBBO.add(bestBid);  // don't change best ask
                                    bids = new TreeMap<Double, ArrayList<String[]>>(); asks = new TreeMap<Double, ArrayList<String[]>>();
                                    time1 += interval;
                                    nextTime = (Math.min(time2, timeEnd) - time1) / interval;
                                }
                                if (time2 > timeEnd){
                                    continue;                      // TODO: can't break if I have more dates in 1 file
                                }

                                if (Bid != Double.parseDouble(lineData[3]) || BidSize != Integer.parseInt(lineData[5])){
                                    if (Double.parseDouble(lineData[3]) > 0.0 && Integer.parseInt(lineData[5]) > 0){
                                        Bid = Double.parseDouble(lineData[3]); BidSize = Integer.parseInt(lineData[5]);
                                        if (bids.containsKey(Bid)){
                                            bids.get(Bid).add(lineData);
                                        } else {
                                            newLine = new ArrayList<String[]>();
                                            newLine.add(lineData);
                                            bids.put(Bid, newLine);
                                        }
                                        if(lineData[8].equals("N") || lineData[8].equals("P")){ // NYSE and ARCA
                                            SUMqNYSE++;
                                            SUMqBnyse++;
                                        }
                                        SUMqALL++;
                                        SUMqBall++;
                                    }
                                }
                                if (Ask != Double.parseDouble(lineData[4]) || AskSize != Integer.parseInt(lineData[6])){
                                    if (Double.parseDouble(lineData[4]) > 0.0 && Integer.parseInt(lineData[6]) > 0){
                                        Ask = Double.parseDouble(lineData[4]); AskSize = Integer.parseInt(lineData[6]);
                                        if (asks.containsKey(Ask)){
                                            asks.get(Ask).add(lineData);
                                        } else {
                                            newLine = new ArrayList<String[]>();
                                            newLine.add(lineData);
                                            asks.put(Ask, newLine);
                                        }
                                        if(lineData[8].equals("N") || lineData[8].equals("P")){ // NYSE and ARCA
                                            SUMqNYSE++;
                                            SUMqAnyse++;
                                        }
                                        SUMqALL++;
                                        SUMqAall++;
                                    }
                                }
                                System.out.println(date);
                            } else {                        // still the same date, new line of quote
                                time2 = sdf.parse(lineData[2]).getTime();
                                if (time2 > timeEnd){
                                    continue;
                                }
                                long nextTime = (time2 - time1) / interval;
                                while (nextTime > 0){                   // putting zeros and past quotes, where no data
                                    double[] filteredBids = nbbOfilter.bestBid(bids);
                                    double[] filteredAsks = nbbOfilter.bestAsk(asks);
                                    SUMqFilteredAll = (long)(filteredBids[1] + filteredAsks[1]);
                                    SUMqFilteredNYSE = (long)(filteredBids[2] + filteredAsks[2]);
                                    if (filteredAsks[0] >= filteredBids[0]){
                                        bestAsk = filteredAsks[0];
                                        bestBid = filteredBids[0];
                                    }
                                    qAall.add(SUMqAall);            SUMqAall = 0;
                                    qAnyse.add(SUMqAnyse);          SUMqAnyse = 0;
                                    qBall.add(SUMqBall);            SUMqBall = 0;
                                    qBnyse.add(SUMqBnyse);          SUMqBnyse = 0;
                                    qAll.add(SUMqALL);              SUMqALL = 0;
                                    qNYSE.add(SUMqNYSE);            SUMqNYSE = 0;
                                    qAllNBBO.add(SUMqFilteredAll);  SUMqFilteredAll = 0;
                                    qNyseNBBO.add(SUMqFilteredNYSE);SUMqFilteredNYSE = 0;
                                    qAskNBOO.add(bestAsk);  // don't change best ask
                                    qBidNBBO.add(bestBid);  // don't change best ask
                                    bids = new TreeMap<Double, ArrayList<String[]>>(); asks = new TreeMap<Double, ArrayList<String[]>>();

                                    time1 += interval;
                                    nextTime = (time2 - time1) / interval;
                                }
                                if (Bid != Double.parseDouble(lineData[3]) || BidSize != Integer.parseInt(lineData[5])){
                                    if (Double.parseDouble(lineData[3]) > 0.0 && Integer.parseInt(lineData[5]) > 0){
                                        Bid = Double.parseDouble(lineData[3]); BidSize = Integer.parseInt(lineData[5]);
                                        if (bids.containsKey(Bid)){
                                            bids.get(Bid).add(lineData);
                                        } else {
                                            newLine = new ArrayList<String[]>();
                                            newLine.add(lineData);
                                            bids.put(Bid, newLine);
                                        }
                                        if(lineData[8].equals("N") || lineData[8].equals("P")){ // NYSE and ARCA
                                            SUMqNYSE++;
                                            SUMqBnyse++;
                                        }
                                        SUMqALL++;
                                        SUMqBall++;
                                    }
                                }
                                if (Ask != Double.parseDouble(lineData[4]) || AskSize != Integer.parseInt(lineData[6])){
                                    if (Double.parseDouble(lineData[4]) > 0.0 && Integer.parseInt(lineData[6]) > 0){
                                        Ask = Double.parseDouble(lineData[4]); AskSize = Integer.parseInt(lineData[6]);
                                        if (asks.containsKey(Ask)){
                                            asks.get(Ask).add(lineData);
                                        } else {
                                            newLine = new ArrayList<String[]>();
                                            newLine.add(lineData);
                                            asks.put(Ask, newLine);
                                        }
                                        if(lineData[8].equals("N") || lineData[8].equals("P")){ // NYSE and ARCA
                                            SUMqNYSE++;
                                            SUMqAnyse++;
                                        }
                                        SUMqALL++;
                                        SUMqAall++;
                                    }
                                }
                            }
                        }

                        if (!quotesTrades.containsKey(date) && !date.equals("nullDate")){   // for the last row of the last date
                            double[] filteredBids = nbbOfilter.bestBid(bids);
                            double[] filteredAsks = nbbOfilter.bestAsk(asks);
                            SUMqFilteredAll = (long)(filteredBids[1] + filteredAsks[1]);
                            SUMqFilteredNYSE = (long)(filteredBids[2] + filteredAsks[2]);
                            if (filteredAsks[0] >= filteredBids[0]){
                                bestAsk = filteredAsks[0];
                                bestBid = filteredBids[0];
                            }
                            qAall.add(SUMqAall);            SUMqAall = 0;
                            qAnyse.add(SUMqAnyse);          SUMqAnyse = 0;
                            qBall.add(SUMqBall);            SUMqBall = 0;
                            qBnyse.add(SUMqBnyse);          SUMqBnyse = 0;
                            qAll.add(SUMqALL);              SUMqALL = 0;
                            qNYSE.add(SUMqNYSE);            SUMqNYSE = 0;
                            qAllNBBO.add(SUMqFilteredAll);  SUMqFilteredAll = 0;
                            qNyseNBBO.add(SUMqFilteredNYSE);SUMqFilteredNYSE = 0;
                            qAskNBOO.add(bestAsk);  // don't change best ask
                            qBidNBBO.add(bestBid);  // don't change best ask
                            bids = new TreeMap<Double, ArrayList<String[]>>(); asks = new TreeMap<Double, ArrayList<String[]>>();
                            long nextTime = (timeEnd - time1) / interval;
                            while (nextTime > 0){   // setting empty time slots with 0's
                                filteredBids = nbbOfilter.bestBid(bids);
                                filteredAsks = nbbOfilter.bestAsk(asks);
                                SUMqFilteredAll = (long)(filteredBids[1] + filteredAsks[1]);
                                SUMqFilteredNYSE = (long)(filteredBids[2] + filteredAsks[2]);
                                if (filteredAsks[0] >= filteredBids[0]){
                                    bestAsk = filteredAsks[0];
                                    bestBid = filteredBids[0];
                                }
                                qAall.add(SUMqAall);            SUMqAall = 0;
                                qAnyse.add(SUMqAnyse);          SUMqAnyse = 0;
                                qBall.add(SUMqBall);            SUMqBall = 0;
                                qBnyse.add(SUMqBnyse);          SUMqBnyse = 0;
                                qAll.add(SUMqALL);              SUMqALL = 0;
                                qNYSE.add(SUMqNYSE);            SUMqNYSE = 0;
                                qAllNBBO.add(SUMqFilteredAll);  SUMqFilteredAll = 0;
                                qNyseNBBO.add(SUMqFilteredNYSE);SUMqFilteredNYSE = 0;
                                qAskNBOO.add(bestAsk);  // don't change best ask
                                qBidNBBO.add(bestBid);  // don't change best ask
                                bids = new TreeMap<Double, ArrayList<String[]>>(); asks = new TreeMap<Double, ArrayList<String[]>>();

                                time1 += interval;
                                nextTime = (timeEnd - time1) / interval;
                            }
                            ArrayList[] quotesTradesData = {qAall, qAnyse, qBall, qBnyse, qAll, qNYSE, nAll, nNYSE, qAskNBOO, qBidNBBO, qAllNBBO, qNyseNBBO, nAllNBOO, nNyseNBBO, nPrices, nQuantities};
                            quotesTrades.put(date, quotesTradesData);

                            qAll = new ArrayList(nInterval);     qAall = new ArrayList(nInterval);    qBall = new ArrayList(nInterval);
                            qNYSE = new ArrayList(nInterval);    qAnyse = new ArrayList(nInterval);   qBnyse = new ArrayList(nInterval);
                            qAllNBBO = new ArrayList(nInterval); qNyseNBBO = new ArrayList(nInterval);qAskNBOO = new ArrayList(nInterval);  qBidNBBO = new ArrayList(nInterval);
                            bids = new TreeMap<Double, ArrayList<String[]>>(); asks = new TreeMap<Double, ArrayList<String[]>>();
                            Ask = 999.0; Bid = 999.0; AskSize = 999; BidSize = 999; bestAsk = 999.0; bestBid = 999.0;
                        }
                        br.close();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ParseException p){
                    p.printStackTrace();
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
                        String date = "nullDate"; // is string in data at position [1]
                        long time1 = sdf.parse(tradingStart).getTime(); // past 5 sec threshold
                        int intervalIndex = 0;
                        long timeEnd = sdf.parse(tradingEnd).getTime();
                        long time2 = time1; // current time
                        br.readLine();
                        ArrayList[] QuotesTradesData;

                        while ((line = br.readLine()) != null) {
                            lineData = line.split(",");
                            if (!lineData[1].equals(date)){             // new unique date, collect data
                                nAll.add(SUMnALL);              SUMnALL = 0;
                                nNYSE.add(SUMnNYSE);            SUMnNYSE = 0;
                                nAllNBOO.add(SUMnFilteredAll);  SUMnFilteredAll = 0;
                                nNyseNBBO.add(SUMnFilteredNYSE);SUMnFilteredNYSE = 0;
                                double[] filteredTransactions = nbbOfilter.vwap(transactionsInterval);
                                if (filteredTransactions[0] > 0.0){
                                    vwapPrice = filteredTransactions[0];
                                    vwapQuantity = filteredTransactions[1];
                                }
                                nPrices.add(vwapPrice); nQuantities.add(vwapQuantity);
                                transactionsInterval = new ArrayList<String[]>();

                                if (quotesTrades.containsKey(date) && !date.equals("nullDate")){    // if date still not in quotesTrades
                                    long nextTime = (timeEnd - time1) / interval;
                                    while (nextTime > 0){   // setting empty time slots with 0's
                                        nAll.add(SUMnALL);              SUMnALL = 0;
                                        nNYSE.add(SUMnNYSE);            SUMnNYSE = 0;
                                        nAllNBOO.add(SUMnFilteredAll);  SUMnFilteredAll = 0;
                                        nNyseNBBO.add(SUMnFilteredNYSE);SUMnFilteredNYSE = 0;
                                        filteredTransactions = nbbOfilter.vwap(transactionsInterval);
                                        if (filteredTransactions[0] > 0.0){
                                            vwapPrice = filteredTransactions[0];
                                            vwapQuantity = filteredTransactions[1];
                                        }
                                        nPrices.add(vwapPrice); nQuantities.add(vwapQuantity);
                                        transactionsInterval = new ArrayList<String[]>();

                                        time1 += interval;
                                        nextTime = (timeEnd - time1) / interval;
                                        intervalIndex++;
                                    }
                                    QuotesTradesData = quotesTrades.get(date);
                                    QuotesTradesData[6] = nAll;
                                    QuotesTradesData[7] = nNYSE;
                                    QuotesTradesData[12] = nAllNBOO;
                                    QuotesTradesData[13] = nNyseNBBO;
                                    QuotesTradesData[14] = nPrices;
                                    QuotesTradesData[15] = nQuantities;
                                }
                                nAll = new ArrayList(nInterval);        SUMnALL = 0;
                                nNYSE = new ArrayList(nInterval);       SUMnNYSE = 0;
                                nAllNBOO = new ArrayList(nInterval);    SUMnFilteredAll = 0;
                                nNyseNBBO = new ArrayList(nInterval);   SUMnFilteredNYSE = 0;
                                nPrices = new ArrayList(nInterval);
                                nQuantities = new ArrayList(nInterval);
                                intervalIndex = 0;

                                date = lineData[1];                     // new date
                                System.out.println(date);

                                if (quotesTrades.containsKey(date)){
                                    QuotesTradesData = quotesTrades.get(date);
                                    asks4Filtering = nbbOfilter.bestAsk(QuotesTradesData[8]);
                                    bids4Filtering = nbbOfilter.bestBid(QuotesTradesData[9]);
                                }
                                time1 = sdf.parse(tradingStart).getTime();
                                time2 = sdf.parse(lineData[2]).getTime();
                                long nextTime = (Math.min(time2, timeEnd) - time1) / interval;
                                while (nextTime > 0){   // setting empty time slots with 0's
                                    nAll.add(SUMnALL);              SUMnALL = 0;
                                    nNYSE.add(SUMnNYSE);            SUMnNYSE = 0;
                                    nAllNBOO.add(SUMnFilteredAll);  SUMnFilteredAll = 0;
                                    nNyseNBBO.add(SUMnFilteredNYSE);SUMnFilteredNYSE = 0;
                                    filteredTransactions = nbbOfilter.vwap(transactionsInterval);
                                    if (filteredTransactions[0] > 0.0){
                                        vwapPrice = filteredTransactions[0];
                                        vwapQuantity = filteredTransactions[1];
                                    }
                                    nPrices.add(vwapPrice); nQuantities.add(vwapQuantity);
                                    transactionsInterval = new ArrayList<String[]>();
                                    time1 += interval;
                                    nextTime = (Math.min(time2, timeEnd) - time1) / interval;
                                    intervalIndex++;
                                }
                                if (time2 > timeEnd){
                                    continue;
                                }
                                if (Integer.parseInt(lineData[6]) > 2){
                                    continue;
                                }
                                if(lineData[8].equals("N") || lineData[8].equals("P")){ // NYSE and ARCA // TODO: filter transactions here
                                    SUMnNYSE++;
                                } else {
                                    SUMnALL++;
                                }
                                if (Double.parseDouble(lineData[3]) > bids4Filtering[intervalIndex] && Double.parseDouble(lineData[3]) < asks4Filtering[intervalIndex]){    // NBBO condition violated
                                    transactions.add(lineData);
                                    transactionsInterval.add(lineData);
                                    if(lineData[8].equals("N") || lineData[8].equals("P")){ // NYSE and ARCA // TODO: filter transactions here
                                        SUMnFilteredNYSE++;
                                    } else {
                                        SUMnFilteredAll++;
                                    }
                                }
                            } else {                        // still the same date, new line of quote
                                time2 = sdf.parse(lineData[2]).getTime();
                                if (time2 > timeEnd){
                                    continue;
                                }
                                if (Integer.parseInt(lineData[6]) > 2){
                                    continue;
                                }
                                long nextTime = (time2 - time1) / interval;
                                while (nextTime > 0){   // TODO: filter transactions here
                                    nAll.add(SUMnALL);              SUMnALL = 0;
                                    nNYSE.add(SUMnNYSE);            SUMnNYSE = 0;
                                    nAllNBOO.add(SUMnFilteredAll);  SUMnFilteredAll = 0;
                                    nNyseNBBO.add(SUMnFilteredNYSE);SUMnFilteredNYSE = 0;
                                    double[] filteredTransactions = nbbOfilter.vwap(transactionsInterval);
                                    if (filteredTransactions[0] > 0.0){
                                        vwapPrice = filteredTransactions[0];
                                        vwapQuantity = filteredTransactions[1];
                                    }
                                    nPrices.add(vwapPrice); nQuantities.add(vwapQuantity);
                                    transactionsInterval = new ArrayList<String[]>();
                                    time1 += interval;
                                    nextTime = (time2 - time1) / interval;
                                    intervalIndex++;
                                }
                                if(lineData[8].equals("N") || lineData[8].equals("P")){ // NYSE and ARCA // TODO: filter transactions here
                                    SUMnNYSE++;
                                } else {
                                    SUMnALL++;
                                }
                                if (Double.parseDouble(lineData[3]) > bids4Filtering[intervalIndex] && Double.parseDouble(lineData[3]) < asks4Filtering[intervalIndex]){    // NBBO condition violated
                                    transactions.add(lineData);
                                    transactionsInterval.add(lineData);
                                    if(lineData[8].equals("N") || lineData[8].equals("P")){ // NYSE and ARCA // TODO: filter transactions here
                                        SUMnFilteredNYSE++;
                                    } else {
                                        SUMnFilteredAll++;
                                    }
                                }
                            }
                        }
                        if (quotesTrades.containsKey(date) && !date.equals("nullDate")){   // if date still not in quotesTrades
                            System.out.println("Last date");
                            nAll.add(SUMnALL);              SUMnALL = 0;
                            nNYSE.add(SUMnNYSE);            SUMnNYSE = 0;
                            nAllNBOO.add(SUMnFilteredAll);  SUMnFilteredAll = 0;
                            nNyseNBBO.add(SUMnFilteredNYSE);SUMnFilteredNYSE = 0;
                            double[] filteredTransactions = nbbOfilter.vwap(transactionsInterval);
                            if (filteredTransactions[0] > 0.0){
                                vwapPrice = filteredTransactions[0];
                                vwapQuantity = filteredTransactions[1];
                            }
                            nPrices.add(vwapPrice); nQuantities.add(vwapQuantity);
                            transactionsInterval = new ArrayList<String[]>();
                            long nextTime = (timeEnd - time1) / interval;
                            while (nextTime > 0){   // setting empty time slots with 0's
                                nAll.add(SUMnALL);              SUMnALL = 0;
                                nNYSE.add(SUMnNYSE);            SUMnNYSE = 0;
                                nAllNBOO.add(SUMnFilteredAll);  SUMnFilteredAll = 0;
                                nNyseNBBO.add(SUMnFilteredNYSE);SUMnFilteredNYSE = 0;
                                filteredTransactions = nbbOfilter.vwap(transactionsInterval);
                                if (filteredTransactions[0] > 0.0){
                                    vwapPrice = filteredTransactions[0];
                                    vwapQuantity = filteredTransactions[1];
                                }
                                nPrices.add(vwapPrice); nQuantities.add(vwapQuantity);
                                transactionsInterval = new ArrayList<String[]>();
                                time1 += interval;
                                nextTime = (timeEnd - time1) / interval;
                            }
                            QuotesTradesData = quotesTrades.get(date);       // TODO: put nSUMS here first?
                            QuotesTradesData[6] = nAll;
                            QuotesTradesData[7] = nNYSE;
                            QuotesTradesData[12] = nAllNBOO;
                            QuotesTradesData[13] = nNyseNBBO;
                            QuotesTradesData[14] = nPrices;
                            QuotesTradesData[15] = nQuantities;
                        }
                        nAll = new ArrayList(nInterval);            SUMnALL = 0;
                        nNYSE = new ArrayList(nInterval);           SUMnNYSE = 0;
                        nAllNBOO = new ArrayList(nInterval);        SUMnFilteredAll = 0;
                        nNyseNBBO = new ArrayList(nInterval);       SUMnFilteredNYSE = 0;
                        nPrices = new ArrayList(nInterval);
                        nQuantities = new ArrayList(nInterval);


                        br.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ParseException p){
                    p.printStackTrace();
                }
            }

            allQuotesTrades.put(company, quotesTrades);
            //quotesTrades = allQuotesTrades.get("vodka");

            try{
                String outputFileName = folder + "quotesTrades\\" + company + "_burst" + ".csv";
                FileWriter writer = new FileWriter(outputFileName, true);
                Iterator dates = quotesTrades.keySet().iterator();
                String date;
                int sz;
                int szArray;
                boolean sizesOK;
                while (dates.hasNext()){
                    date = (String) dates.next();
                    ArrayList[] tradesQuotesDay = quotesTrades.get(date);
                    sz = tradesQuotesDay[0].size();
                    szArray = tradesQuotesDay.length;
                    sizesOK = true;
                    int j = 0;
                    while (sizesOK && (j < szArray)){
                        sizesOK = tradesQuotesDay[j].size() == sz;
                        j++;
                    }
                    if (sizesOK){
                        for (int i = 0; i < sz; i++){
                            writer.write(date + "," + tradesQuotesDay[0].get(i) + "," + tradesQuotesDay[1].get(i) + "," + tradesQuotesDay[2].get(i) + "," + tradesQuotesDay[3].get(i) +
                                    "," + tradesQuotesDay[4].get(i) + "," + tradesQuotesDay[5].get(i) + "," + tradesQuotesDay[6].get(i) + "," + tradesQuotesDay[7].get(i) +
                                    "," + tradesQuotesDay[8].get(i) + "," + tradesQuotesDay[9].get(i) + "," + tradesQuotesDay[10].get(i) + "," + tradesQuotesDay[11].get(i) +
                                    "," + tradesQuotesDay[12].get(i) + "," + tradesQuotesDay[13].get(i) + "," + tradesQuotesDay[14].get(i) +"," + tradesQuotesDay[15].get(i) + "\r");
                        }
                    } else {
                        System.out.println("Incomplete data gathering for date and company: " + date + " " + company);
                    }
                }
                writer.close();
            } catch (Exception e){
                e.printStackTrace();
                System.exit(1);
            }
            try{
                String outputFileName = folder + "quotesTrades\\" + company + "_filtered" + ".csv";
                FileWriter writer = new FileWriter(outputFileName, true);
                int sz = transactions.size();
                String s;
                String[] sa =  new String[9];
                for (int i = 0; i < sz; i++){
                    s = new String();
                    sa = transactions.get(i);
                    for (int j = 0; j < 9; j++){
                        s = s + sa[j] + ",";
                    }
                    writer.write(s + "\r");
                }
                writer.close();



            } catch (Exception e){
                e.printStackTrace();
                System.exit(1);
            }
            /*try {
                FileOutputStream fos = new FileOutputStream(folder + "quotesTrades\\" + company + "_filtered" + ".zip");
                ZipOutputStream zos = new ZipOutputStream(fos);

                String file5Name = folder + "quotesTrades\\" + company + "_filtered" + ".csv";
                addToZipFile(file5Name, zos);
                zos.close();
                fos.close();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }*/
            transactions = new ArrayList<String[]>();
            quotesTrades = new TreeMap<String, ArrayList[]>();
        }
    }
    public static void addToZipFile(String fileName, ZipOutputStream zos) throws FileNotFoundException, IOException {

        System.out.println("Writing '" + fileName + "' to zip file");

        File file = new File(fileName);
        FileInputStream fis = new FileInputStream(file);
        ZipEntry zipEntry = new ZipEntry(fileName);
        zos.putNextEntry(zipEntry);

        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zos.write(bytes, 0, length);
        }

        zos.closeEntry();
        fis.close();
    }
}
