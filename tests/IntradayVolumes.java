import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by rojcek on 18.05.2016.
 */
public class IntradayVolumes {
    public static void main(String [] args) {
        double timeBeginAlgo = System.nanoTime();
        //String folder = "C:\\Users\\rojcek\\Documents\\School\\SFI\\_paper2b NYSE event\\DATA\\";
        String folder = "E:\\_paper2 Nyse Event\\";
        boolean printTrades = false;    // print filtered transactions
        boolean printSummary = true;    // print summary
        String tag = "best";            // tag for naming files
        String mode = "seconds";   // data in "seconds" or "milliseconds"?
        int q1 = 1, q2 = 2, q3 = 3, q4 = 4, q5 = 5, q6 = 6, q7 = 7, q8 = 8;
        int t1 = 1, t2 = 2, t3 = 3, t4 = 4, t5 = 5, t6 = 6, t7 = 7, t8 = 8;
        int interval = 10000;             // the length of the interval is 5 sec or 5000 millisec, 468000 = 7.8' or 50 intervals
        int intervalFilter = 90000;     // the length of interval looking back to NBBOs to discard transaction, 90s reporting obligation period
        int quoteDelay = 0;             // positive delay shifts quotes backwards (from 1->0), negative delay shifts quotes forwards (0->1)
        int nWindow = (intervalFilter / interval) + 1;      // length of window in terms of basic intervals we use
        double upper = 0.2;            // upper limit of the share according to best price which to use for bid/ask computation
        double lower = 0.0;             // lower limit of the share according to best price which to use for bid/ask computation
        SimpleDateFormat sdf;           // format of the date to use
        String tradingStart;
        String tradingEnd;
        if (mode.equals("milliseconds")){
            sdf = new SimpleDateFormat("HH:mm:ss.SSS"); // format of the date to use
            tradingStart = "9:30:00.000";
            tradingEnd = "16:00:00.000";
            q1 = 0; q2 = 1; q3 = 5; q4 = 7; q5 = 6; q6 = 8; q7 = 9; q8 = 2;
            t1 = 0; t2 = 1; t3 = 7; t4 = 6; t5 = 6; t6 = 9; t7 = 5; t8 = 2;
        } else if (mode.equals("seconds")){
            sdf = new SimpleDateFormat("HH:mm:ss"); // format of the date to use
            tradingStart = "9:30:00";
            tradingEnd = "16:00:00";
        } else {
            return;
        }

        int nInterval = 1;                  // length of holders per day
        try {
            nInterval = 1 + (int)(sdf.parse(tradingEnd).getTime() - sdf.parse(tradingStart).getTime()) / interval;
        } catch (Exception ex){
            ex.printStackTrace();
        }
        ArrayList<Double> GoodQuotes = new ArrayList<Double>(6);
        GoodQuotes.add(1.0);GoodQuotes.add(2.0);GoodQuotes.add(6.0);
        GoodQuotes.add(10.0);GoodQuotes.add(12.0);GoodQuotes.add(23.0);

        NBBOfilter nbbOfilter = new NBBOfilter(nWindow, nInterval, q5, q6, q8, t3, t4);      // filter used to determine the NBBO and whether to keep a transaction
        HashMap<String, String[]> companysFiles = new HashMap<String, String[]>();      // HashMap holding company's name and a string of quotesFiles and a string of tradesFiles
        TreeMap<String, ArrayList[]> quotesTrades = new TreeMap<String, ArrayList[]>();    // KEY: date, #quotes, #trades, bid, ask per interval, keys are sorted
        //HashMap<String, TreeMap> allQuotesTrades = new HashMap<String, TreeMap>();     // company and previous HashMap
        TreeMap<Double, ArrayList<String[]>> bids = new TreeMap<Double, ArrayList<String[]>>();    // holder for transaction lines for bid as a key for an interval
        TreeMap<Double, ArrayList<String[]>> asks = new TreeMap<Double, ArrayList<String[]>>();    // holder for transaction lines for ask as a key for an interval
        ArrayList<String[]> transactions = new ArrayList<String[]>();           // holder for transactions for a day
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
        ArrayList nAlliso = new ArrayList(nInterval);   // # number of ISO trades per 5 second intervals
        ArrayList nAllNBOO = new ArrayList(nInterval);  // # number of remaining trades per 5 second intervals
        ArrayList nNYSE = new ArrayList(nInterval);     // # number of trades at NYSE per 5 second intervals
        ArrayList nNYSEiso = new ArrayList(nInterval);  // # number of trades at NYSE per 5 second intervals
        ArrayList nNyseNBBO = new ArrayList(nInterval); // # number of remaining trades at NYSE per 5 second intervals
        ArrayList nPrices = new ArrayList(nInterval);   // average transaction prices per 5 second intervals
        ArrayList nQuantities = new ArrayList(nInterval);       // average transaction quantities per 5 second intervals
        ArrayList MidQuotes = new ArrayList(nInterval);         // mid quotes per 5 second intervals
        ArrayList OrderFlowSell = new ArrayList(nInterval);     // order flow to sell per 5 second intervals
        ArrayList OrderFlowBuy = new ArrayList(nInterval);      // order flow to buy per 5 second intervals
        ArrayList OrderFlowSellNyse = new ArrayList(nInterval); // order flow to sell per 5 second intervals
        ArrayList OrderFlowBuyNyse = new ArrayList(nInterval);  // order flow to buy per 5 second intervals
        ArrayList QuotedSpread = new ArrayList(nInterval);      // order flow to buy per 5 second intervals
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
        long SUMnALLiso = 0;                            // interval sum of ISO trades all
        long SUMnNYSEiso = 0;                           // interval sum of ISO trades NYSE
        long SUMnFilteredAll = 0;                       // interval sum of remaining trades all
        long SUMnFilteredNYSE = 0;                      // interval sum of remaining trades NYSE
        double bestBid = 0.0;                           // best bid in the interval
        double bestAsk = -999.0;                         // best bid in the interval
        double vwapPrice = 0.0;                         // vwap price in the interval
        double vwapQuantity = 999.0;                    // vwap quantity in the interval
        double orderFlowSell = 999.0;                   // order flow to sell in the interval
        double orderFlowBuy = 999.0;                    // order flow to buy in the interval
        double orderFlowSellNYSE = 999.0;               // order flow to sell in the interval
        double orderFlowBuyNYSE = 999.0;                // order flow to buy in the interval
        double[] bids4Filtering = new double[nInterval];// to determine if to keep transaction or not
        double[] asks4Filtering = new double[nInterval];// to determine if to keep transaction or not

        double Ask;
        double Bid;
        int AskSize;
        int BidSize;
        double QS = 0.0;        // holder for cumulated quoted spread
        int nQS = 0;            // number of quoted spread updates
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
            //transactions = new ArrayList<String[]>();

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
                        long time1 = sdf.parse(tradingStart).getTime() + quoteDelay; // past 5 sec threshold
                        long timeEnd = sdf.parse(tradingEnd).getTime() + quoteDelay;
                        long time2 = time1; // current time
                        br.readLine(); // this is just header
                        Ask = -999.0; Bid = 999.0; AskSize = 999; BidSize = 999;

                        while ((line = br.readLine()) != null) {
                            lineData = line.split(",");
                            if (!lineData[q1].equals(date)){         // new unique date, collect data
                                double[] filteredBids = nbbOfilter.bestBid(bids, upper, lower);
                                double[] filteredAsks = nbbOfilter.bestAsk(asks, upper, lower);
                                SUMqFilteredAll = (long)(filteredBids[1] + filteredAsks[1]);
                                SUMqFilteredNYSE = (long)(filteredBids[2] + filteredAsks[2]);
                                if (filteredAsks[0] > filteredBids[0]){
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
                                MidQuotes.add((double)((bestAsk + bestBid) / 2));
                                QuotedSpread.add(QS/Math.max(1, nQS)); QS = 0.0; nQS = 0;
                                bids = new TreeMap<Double, ArrayList<String[]>>(); asks = new TreeMap<Double, ArrayList<String[]>>();
                                if (!quotesTrades.containsKey(date) && !date.equals("nullDate")){   // if date still not in quotesTrades
                                    long nextTime = (timeEnd - time1) / interval;
                                    while (nextTime > 0){   // setting empty time slots with 0's
                                        filteredBids = nbbOfilter.bestBid(bids, upper, lower);
                                        filteredAsks = nbbOfilter.bestAsk(asks, upper, lower);
                                        SUMqFilteredAll = (long)(filteredBids[1] + filteredAsks[1]);
                                        SUMqFilteredNYSE = (long)(filteredBids[2] + filteredAsks[2]);
                                        if (filteredAsks[0] > filteredBids[0]){
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
                                        MidQuotes.add((double)((bestAsk + bestBid) / 2));
                                        QuotedSpread.add(QS/Math.max(1, nQS)); QS = 0.0; nQS = 0;
                                        bids = new TreeMap<Double, ArrayList<String[]>>(); asks = new TreeMap<Double, ArrayList<String[]>>();
                                        time1 += interval;
                                        nextTime = (timeEnd - time1) / interval;
                                    }
                                    ArrayList[] quotesTradesData = {qAall, qAnyse, qBall, qBnyse, qAll, qNYSE, nAll, nNYSE, qAskNBOO, qBidNBBO, qAllNBBO,
                                            qNyseNBBO, nAllNBOO, nNyseNBBO, nPrices, nQuantities, MidQuotes, OrderFlowSell, OrderFlowBuy, OrderFlowSellNyse, OrderFlowBuyNyse, QuotedSpread, nAlliso, nNYSEiso};
                                    quotesTrades.put(date, quotesTradesData);
                                }
                                qAll = new ArrayList(nInterval);     qAall = new ArrayList(nInterval);    qBall = new ArrayList(nInterval);
                                qNYSE = new ArrayList(nInterval);    qAnyse = new ArrayList(nInterval);   qBnyse = new ArrayList(nInterval);
                                qAllNBBO = new ArrayList(nInterval); qNyseNBBO = new ArrayList(nInterval);qAskNBOO = new ArrayList(nInterval);
                                qBidNBBO = new ArrayList(nInterval); MidQuotes = new ArrayList(nInterval);OrderFlowSell = new ArrayList(nInterval);
                                OrderFlowBuy = new ArrayList(nInterval);OrderFlowSellNyse = new ArrayList(nInterval); OrderFlowBuyNyse = new ArrayList(nInterval);
                                QuotedSpread = new ArrayList(nInterval);
                                bids = new TreeMap<Double, ArrayList<String[]>>(); asks = new TreeMap<Double, ArrayList<String[]>>();
                                Ask = -999; Bid = 999; AskSize = 999; BidSize = 999; bestAsk = -999.0; bestBid = 999.0;

                                date = lineData[q1]; // new date
                                time1 = sdf.parse(tradingStart).getTime() + quoteDelay;
                                time2 = sdf.parse(lineData[q2]).getTime();
                                long nextTime = (Math.min(time2, timeEnd) - time1) / interval;
                                while (nextTime > 0){   // setting empty time slots with 0's
                                    filteredBids = nbbOfilter.bestBid(bids, upper, lower);
                                    filteredAsks = nbbOfilter.bestAsk(asks, upper, lower);
                                    SUMqFilteredAll = (long)(filteredBids[1] + filteredAsks[1]);
                                    SUMqFilteredNYSE = (long)(filteredBids[2] + filteredAsks[2]);
                                    if (filteredAsks[0] > filteredBids[0]){
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
                                    MidQuotes.add((double)((bestAsk + bestBid) / 2));
                                    QuotedSpread.add(QS/Math.max(1, nQS)); QS = 0.0; nQS = 0;
                                    bids = new TreeMap<Double, ArrayList<String[]>>(); asks = new TreeMap<Double, ArrayList<String[]>>();
                                    time1 += interval;
                                    nextTime = (Math.min(time2, timeEnd) - time1) / interval;
                                }
                                if (time2 > timeEnd){
                                    continue;                      // TODO: can't break if I have more dates in 1 file
                                }
                                //if (GoodQuotes.contains(Double.parseDouble(lineData[q7]))) {     // determines if those quotes are eligible
                                if (Bid != Double.parseDouble(lineData[q3]) || BidSize != Integer.parseInt(lineData[q5])) {
                                    if (Double.parseDouble(lineData[q3]) > 0.0 && Integer.parseInt(lineData[q5]) > 0) {
                                        Bid = Double.parseDouble(lineData[q3]);
                                        BidSize = Integer.parseInt(lineData[q5]);
                                        QS += Ask - Bid;
                                        nQS++;
                                        if (bids.containsKey(Bid)) {
                                            bids.get(Bid).add(lineData);
                                        } else {
                                            newLine = new ArrayList<String[]>();
                                            newLine.add(lineData);
                                            bids.put(Bid, newLine);
                                        }
                                        if (lineData[q8].equals("N") || lineData[q8].equals("P")) { // NYSE and ARCA
                                            SUMqNYSE++;
                                            SUMqBnyse++;
                                        }
                                        SUMqALL++;
                                        SUMqBall++;
                                    }
                                }
                                if (Ask != Double.parseDouble(lineData[q4]) || AskSize != Integer.parseInt(lineData[q6])) {
                                    if (Double.parseDouble(lineData[q4]) > 0.0 && Integer.parseInt(lineData[q6]) > 0) {
                                        Ask = Double.parseDouble(lineData[q4]);
                                        AskSize = Integer.parseInt(lineData[q6]);
                                        QS += Ask - Bid;
                                        nQS++;
                                        if (asks.containsKey(Ask)) {
                                            asks.get(Ask).add(lineData);
                                        } else {
                                            newLine = new ArrayList<String[]>();
                                            newLine.add(lineData);
                                            asks.put(Ask, newLine);
                                        }
                                        if (lineData[q8].equals("N") || lineData[q8].equals("P")) { // NYSE and ARCA
                                            SUMqNYSE++;
                                            SUMqAnyse++;
                                        }
                                        SUMqALL++;
                                        SUMqAall++;
                                    }
                                }
                                //}
                                System.out.println(date);
                            } else {                        // still the same date, new line of quote
                                time2 = sdf.parse(lineData[q2]).getTime();
                                if (time2 > timeEnd){
                                    continue;
                                }
                                long nextTime = (time2 - time1) / interval;
                                while (nextTime > 0){                   // putting zeros and past quotes, where no data
                                    double[] filteredBids = nbbOfilter.bestBid(bids, upper, lower);
                                    double[] filteredAsks = nbbOfilter.bestAsk(asks, upper, lower);
                                    SUMqFilteredAll = (long)(filteredBids[1] + filteredAsks[1]);
                                    SUMqFilteredNYSE = (long)(filteredBids[2] + filteredAsks[2]);
                                    if (filteredAsks[0] > filteredBids[0]){    // TODO: does this preclude 999.0 quotes?
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
                                    MidQuotes.add((double)((bestAsk + bestBid) / 2));
                                    QuotedSpread.add(QS/Math.max(1, nQS)); QS = 0.0; nQS = 0;
                                    bids = new TreeMap<Double, ArrayList<String[]>>(); asks = new TreeMap<Double, ArrayList<String[]>>();

                                    time1 += interval;
                                    nextTime = (time2 - time1) / interval;
                                }
                                //if (GoodQuotes.contains(Double.parseDouble(lineData[q7]))){      // determines if those quotes are eligible
                                if (Bid != Double.parseDouble(lineData[q3]) || BidSize != Integer.parseInt(lineData[q5])) {
                                    if (Double.parseDouble(lineData[q3]) > 0.0 && Integer.parseInt(lineData[q5]) > 0) {
                                        Bid = Double.parseDouble(lineData[q3]);
                                        BidSize = Integer.parseInt(lineData[q5]);
                                        QS += Ask - Bid;
                                        nQS++;
                                        if (bids.containsKey(Bid)) {
                                            bids.get(Bid).add(lineData);
                                        } else {
                                            newLine = new ArrayList<String[]>();
                                            newLine.add(lineData);
                                            bids.put(Bid, newLine);
                                        }
                                        if (lineData[q8].equals("N") || lineData[q8].equals("P")) { // NYSE and ARCA
                                            SUMqNYSE++;
                                            SUMqBnyse++;
                                        }
                                        SUMqALL++;
                                        SUMqBall++;
                                    }
                                }
                                if (Ask != Double.parseDouble(lineData[q4]) || AskSize != Integer.parseInt(lineData[q6])) {
                                    if (Double.parseDouble(lineData[q4]) > 0.0 && Integer.parseInt(lineData[q6]) > 0) {
                                        Ask = Double.parseDouble(lineData[q4]);
                                        AskSize = Integer.parseInt(lineData[q6]);
                                        QS += Ask - Bid;
                                        nQS++;
                                        if (asks.containsKey(Ask)) {
                                            asks.get(Ask).add(lineData);
                                        } else {
                                            newLine = new ArrayList<String[]>();
                                            newLine.add(lineData);
                                            asks.put(Ask, newLine);
                                        }
                                        if (lineData[q8].equals("N") || lineData[q8].equals("P")) { // NYSE and ARCA
                                            SUMqNYSE++;
                                            SUMqAnyse++;
                                        }
                                        SUMqALL++;
                                        SUMqAall++;
                                    }
                                }
                                //}
                            }
                        }

                        if (!quotesTrades.containsKey(date) && !date.equals("nullDate")){   // for the last row of the last date
                            double[] filteredBids = nbbOfilter.bestBid(bids, upper, lower);
                            double[] filteredAsks = nbbOfilter.bestAsk(asks, upper, lower);
                            SUMqFilteredAll = (long)(filteredBids[1] + filteredAsks[1]);
                            SUMqFilteredNYSE = (long)(filteredBids[2] + filteredAsks[2]);
                            if (filteredAsks[0] > filteredBids[0]){
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
                            MidQuotes.add((double)((bestAsk + bestBid) / 2));
                            QuotedSpread.add(QS/Math.max(1, nQS)); QS = 0.0; nQS = 0;
                            bids = new TreeMap<Double, ArrayList<String[]>>(); asks = new TreeMap<Double, ArrayList<String[]>>();
                            long nextTime = (timeEnd - time1) / interval;
                            while (nextTime > 0){   // setting empty time slots with 0's
                                filteredBids = nbbOfilter.bestBid(bids, upper, lower);
                                filteredAsks = nbbOfilter.bestAsk(asks, upper, lower);
                                SUMqFilteredAll = (long)(filteredBids[1] + filteredAsks[1]);
                                SUMqFilteredNYSE = (long)(filteredBids[2] + filteredAsks[2]);
                                if (filteredAsks[0] > filteredBids[0]){
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
                                MidQuotes.add((double)((bestAsk + bestBid) / 2));
                                QuotedSpread.add(QS/Math.max(1, nQS)); QS = 0.0; nQS = 0;
                                bids = new TreeMap<Double, ArrayList<String[]>>(); asks = new TreeMap<Double, ArrayList<String[]>>();

                                time1 += interval;
                                nextTime = (timeEnd - time1) / interval;
                            }
                            ArrayList[] quotesTradesData = {qAall, qAnyse, qBall, qBnyse, qAll, qNYSE, nAll, nNYSE, qAskNBOO, qBidNBBO,
                                    qAllNBBO, qNyseNBBO, nAllNBOO, nNyseNBBO, nPrices, nQuantities, MidQuotes, OrderFlowSell, OrderFlowBuy, OrderFlowSellNyse, OrderFlowBuyNyse, QuotedSpread, nAlliso, nNYSEiso};
                            quotesTrades.put(date, quotesTradesData);

                            qAll = new ArrayList(nInterval);     qAall = new ArrayList(nInterval);    qBall = new ArrayList(nInterval);
                            qNYSE = new ArrayList(nInterval);    qAnyse = new ArrayList(nInterval);   qBnyse = new ArrayList(nInterval);
                            qAllNBBO = new ArrayList(nInterval); qNyseNBBO = new ArrayList(nInterval);qAskNBOO = new ArrayList(nInterval);
                            qBidNBBO = new ArrayList(nInterval); MidQuotes = new ArrayList(nInterval);OrderFlowSell = new ArrayList(nInterval);
                            OrderFlowBuy = new ArrayList(nInterval); OrderFlowSellNyse = new ArrayList(nInterval); OrderFlowBuyNyse = new ArrayList(nInterval);
                            QuotedSpread = new ArrayList(nInterval);
                            bids = new TreeMap<Double, ArrayList<String[]>>(); asks = new TreeMap<Double, ArrayList<String[]>>();
                            Ask = -999.0; Bid = 999.0; AskSize = 999; BidSize = 999; bestAsk = -999.0; bestBid = 999.0;
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
                            if (!lineData[t1].equals(date)){             // new unique date, collect data
                                nAll.add(SUMnALL);              SUMnALL = 0;
                                nNYSE.add(SUMnNYSE);            SUMnNYSE = 0;
                                nAlliso.add(SUMnALLiso);        SUMnALLiso = 0;
                                nNYSEiso.add(SUMnNYSEiso);      SUMnNYSEiso = 0;
                                nAllNBOO.add(SUMnFilteredAll);  SUMnFilteredAll = 0;
                                nNyseNBBO.add(SUMnFilteredNYSE);SUMnFilteredNYSE = 0;
                                OrderFlowSell.add(orderFlowSell);           orderFlowSell = 0.0;
                                OrderFlowBuy.add(orderFlowBuy);             orderFlowBuy = 0.0;
                                OrderFlowSellNyse.add(orderFlowSellNYSE);   orderFlowSellNYSE = 0.0;
                                OrderFlowBuyNyse.add(orderFlowBuyNYSE);     orderFlowBuyNYSE = 0.0;
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
                                        nAlliso.add(SUMnALLiso);        SUMnALLiso = 0;
                                        nNYSEiso.add(SUMnNYSEiso);      SUMnNYSEiso = 0;
                                        nAllNBOO.add(SUMnFilteredAll);  SUMnFilteredAll = 0;
                                        nNyseNBBO.add(SUMnFilteredNYSE);SUMnFilteredNYSE = 0;
                                        OrderFlowSell.add(orderFlowSell);           orderFlowSell = 0.0;
                                        OrderFlowBuy.add(orderFlowBuy);             orderFlowBuy = 0.0;
                                        OrderFlowSellNyse.add(orderFlowSellNYSE);   orderFlowSellNYSE = 0.0;
                                        OrderFlowBuyNyse.add(orderFlowBuyNYSE);     orderFlowBuyNYSE = 0.0;
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
                                    QuotesTradesData[17] = OrderFlowSell;
                                    QuotesTradesData[18] = OrderFlowBuy;
                                    QuotesTradesData[19] = OrderFlowSellNyse;
                                    QuotesTradesData[20] = OrderFlowBuyNyse;
                                    QuotesTradesData[22] = nAlliso;
                                    QuotesTradesData[23] = nNYSEiso;
                                }
                                nAll = new ArrayList(nInterval);        SUMnALL = 0;
                                nNYSE = new ArrayList(nInterval);       SUMnNYSE = 0;
                                nAlliso = new ArrayList(nInterval);     SUMnALLiso = 0;
                                nNYSEiso = new ArrayList(nInterval);    SUMnNYSEiso = 0;
                                nAllNBOO = new ArrayList(nInterval);    SUMnFilteredAll = 0;
                                nNyseNBBO = new ArrayList(nInterval);   SUMnFilteredNYSE = 0;
                                nPrices = new ArrayList(nInterval);
                                nQuantities = new ArrayList(nInterval);
                                OrderFlowSell = new ArrayList(nInterval);   orderFlowSell = 0.0;
                                OrderFlowBuy = new ArrayList(nInterval);    orderFlowBuy = 0.0;
                                OrderFlowSellNyse = new ArrayList(nInterval);orderFlowSellNYSE = 0.0;
                                OrderFlowBuyNyse = new ArrayList(nInterval);orderFlowBuyNYSE = 0.0;
                                intervalIndex = 0;

                                date = lineData[t1];                     // new date
                                System.out.println(date);

                                if (quotesTrades.containsKey(date)){
                                    QuotesTradesData = quotesTrades.get(date);
                                    asks4Filtering = nbbOfilter.bestAsk(QuotesTradesData[8]);
                                    bids4Filtering = nbbOfilter.bestBid(QuotesTradesData[9]);
                                    MidQuotes = QuotesTradesData[16];
                                }
                                time1 = sdf.parse(tradingStart).getTime();
                                time2 = sdf.parse(lineData[t2]).getTime();
                                long nextTime = (Math.min(time2, timeEnd) - time1) / interval;
                                while (nextTime > 0){   // setting empty time slots with 0's
                                    nAll.add(SUMnALL);              SUMnALL = 0;
                                    nNYSE.add(SUMnNYSE);            SUMnNYSE = 0;
                                    nAlliso.add(SUMnALLiso);        SUMnALLiso = 0;
                                    nNYSEiso.add(SUMnNYSEiso);      SUMnNYSEiso = 0;
                                    nAllNBOO.add(SUMnFilteredAll);  SUMnFilteredAll = 0;
                                    nNyseNBBO.add(SUMnFilteredNYSE);SUMnFilteredNYSE = 0;
                                    OrderFlowSell.add(orderFlowSell);           orderFlowSell = 0.0;
                                    OrderFlowBuy.add(orderFlowBuy);             orderFlowBuy = 0.0;
                                    OrderFlowSellNyse.add(orderFlowSellNYSE);   orderFlowSellNYSE = 0.0;
                                    OrderFlowBuyNyse.add(orderFlowBuyNYSE);     orderFlowBuyNYSE = 0.0;
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
                                if (Integer.parseInt(lineData[t6]) > 2){
                                    continue;
                                }
                                if(lineData[t8].equals("N") || lineData[t8].equals("P")){ // NYSE and ARCA // TODO: filter transactions here
                                    SUMnNYSE++;
                                    if (lineData[t7].contains("F")){
                                        SUMnNYSEiso++;
                                    }
                                } else {
                                    SUMnALL++;
                                    if (lineData[t7].contains("F")){
                                        SUMnALLiso++;
                                    }
                                }
                                if (Double.parseDouble(lineData[t3]) > bids4Filtering[intervalIndex] && Double.parseDouble(lineData[t3]) < asks4Filtering[intervalIndex]){    // NBBO condition violated
                                    if(printTrades){transactions.add(lineData);}
                                    transactionsInterval.add(lineData);

                                    if(lineData[t8].equals("N") || lineData[t8].equals("P")){ // NYSE and ARCA // TODO: filter transactions here
                                        SUMnFilteredNYSE++;
                                        if (Double.parseDouble(lineData[t3]) > (Double) MidQuotes.get(intervalIndex)){
                                            orderFlowBuyNYSE += Double.parseDouble(lineData[t4]);
                                        } else if (Double.parseDouble(lineData[t3]) < (Double) MidQuotes.get(intervalIndex)) {
                                            orderFlowSellNYSE += Double.parseDouble(lineData[t4]);
                                        }
                                    } else {
                                        SUMnFilteredAll++;
                                        if (Double.parseDouble(lineData[t3]) > (Double) MidQuotes.get(intervalIndex)){
                                            orderFlowBuy += Double.parseDouble(lineData[t4]);
                                        } else if (Double.parseDouble(lineData[t3]) < (Double) MidQuotes.get(intervalIndex)) {
                                            orderFlowSell += Double.parseDouble(lineData[t4]);
                                        }
                                    }
                                }
                            } else {                        // still the same date, new line of quote
                                time2 = sdf.parse(lineData[t2]).getTime();
                                if (time2 > timeEnd){
                                    continue;
                                }
                                if (Integer.parseInt(lineData[t6]) > 2){
                                    continue;
                                }
                                long nextTime = (time2 - time1) / interval;
                                while (nextTime > 0){   // TODO: filter transactions here
                                    nAll.add(SUMnALL);              SUMnALL = 0;
                                    nNYSE.add(SUMnNYSE);            SUMnNYSE = 0;
                                    nAlliso.add(SUMnALLiso);        SUMnALLiso = 0;
                                    nNYSEiso.add(SUMnNYSEiso);      SUMnNYSEiso = 0;
                                    nAllNBOO.add(SUMnFilteredAll);  SUMnFilteredAll = 0;
                                    nNyseNBBO.add(SUMnFilteredNYSE);SUMnFilteredNYSE = 0;
                                    OrderFlowSell.add(orderFlowSell);           orderFlowSell = 0.0;
                                    OrderFlowBuy.add(orderFlowBuy);             orderFlowBuy = 0.0;
                                    OrderFlowSellNyse.add(orderFlowSellNYSE);   orderFlowSellNYSE = 0.0;
                                    OrderFlowBuyNyse.add(orderFlowBuyNYSE);     orderFlowBuyNYSE = 0.0;
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
                                if(lineData[t8].equals("N") || lineData[t8].equals("P")){ // NYSE and ARCA // TODO: filter transactions here
                                    SUMnNYSE++;
                                    if (lineData[t7].contains("F")){
                                        SUMnNYSEiso++;
                                    }
                                } else {
                                    SUMnALL++;
                                    if (lineData[t7].contains("F")){
                                        SUMnALLiso++;
                                    }
                                }
                                if (Double.parseDouble(lineData[t3]) > bids4Filtering[intervalIndex] && Double.parseDouble(lineData[t3]) < asks4Filtering[intervalIndex]){    // NBBO condition violated
                                    if(printTrades){transactions.add(lineData);}
                                    transactionsInterval.add(lineData);
                                    if(lineData[t8].equals("N") || lineData[t8].equals("P")){ // NYSE and ARCA // TODO: filter transactions here
                                        SUMnFilteredNYSE++;
                                        if (Double.parseDouble(lineData[t3]) > (Double) MidQuotes.get(intervalIndex)){
                                            orderFlowBuyNYSE += Double.parseDouble(lineData[t4]);
                                        } else if (Double.parseDouble(lineData[t3]) < (Double) MidQuotes.get(intervalIndex)) {
                                            orderFlowSellNYSE += Double.parseDouble(lineData[t4]);
                                        }
                                    } else {
                                        SUMnFilteredAll++;
                                        if (Double.parseDouble(lineData[t3]) > (Double) MidQuotes.get(intervalIndex)){
                                            orderFlowBuy += Double.parseDouble(lineData[t4]);
                                        } else if (Double.parseDouble(lineData[t3]) < (Double) MidQuotes.get(intervalIndex)) {
                                            orderFlowSell += Double.parseDouble(lineData[t4]);
                                        }
                                    }
                                }
                            }
                        }
                        if (quotesTrades.containsKey(date) && !date.equals("nullDate")){   // if date still not in quotesTrades
                            System.out.println("Last date");
                            nAll.add(SUMnALL);              SUMnALL = 0;
                            nNYSE.add(SUMnNYSE);            SUMnNYSE = 0;
                            nAlliso.add(SUMnALLiso);        SUMnALLiso = 0;
                            nNYSEiso.add(SUMnNYSEiso);      SUMnNYSEiso = 0;
                            nAllNBOO.add(SUMnFilteredAll);  SUMnFilteredAll = 0;
                            nNyseNBBO.add(SUMnFilteredNYSE);SUMnFilteredNYSE = 0;
                            OrderFlowSell.add(orderFlowSell);           orderFlowSell = 0.0;
                            OrderFlowBuy.add(orderFlowBuy);             orderFlowBuy = 0.0;
                            OrderFlowSellNyse.add(orderFlowSellNYSE);   orderFlowSellNYSE = 0.0;
                            OrderFlowBuyNyse.add(orderFlowBuyNYSE);     orderFlowBuyNYSE = 0.0;
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
                                nAlliso.add(SUMnALLiso);        SUMnALLiso = 0;
                                nNYSEiso.add(SUMnNYSEiso);      SUMnNYSEiso = 0;
                                nAllNBOO.add(SUMnFilteredAll);  SUMnFilteredAll = 0;
                                nNyseNBBO.add(SUMnFilteredNYSE);SUMnFilteredNYSE = 0;
                                OrderFlowSell.add(orderFlowSell);           orderFlowSell = 0.0;
                                OrderFlowBuy.add(orderFlowBuy);             orderFlowBuy = 0.0;
                                OrderFlowSellNyse.add(orderFlowSellNYSE);   orderFlowSellNYSE = 0.0;
                                OrderFlowBuyNyse.add(orderFlowBuyNYSE);     orderFlowBuyNYSE = 0.0;
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
                            QuotesTradesData[17] = OrderFlowSell;
                            QuotesTradesData[18] = OrderFlowBuy;
                            QuotesTradesData[19] = OrderFlowSellNyse;
                            QuotesTradesData[20] = OrderFlowBuyNyse;
                            QuotesTradesData[22] = nAlliso;
                            QuotesTradesData[23] = nNYSEiso;
                        }
                        nAll = new ArrayList(nInterval);            SUMnALL = 0;
                        nNYSE = new ArrayList(nInterval);           SUMnNYSE = 0;
                        nAlliso  = new ArrayList(nInterval);        SUMnALLiso = 0;
                        nNYSEiso  = new ArrayList(nInterval);       SUMnNYSEiso = 0;
                        nAllNBOO = new ArrayList(nInterval);        SUMnFilteredAll = 0;
                        nNyseNBBO = new ArrayList(nInterval);       SUMnFilteredNYSE = 0;
                        nPrices = new ArrayList(nInterval);
                        nQuantities = new ArrayList(nInterval);
                        OrderFlowSell = new ArrayList(nInterval);   orderFlowSell = 0.0;
                        OrderFlowBuy = new ArrayList(nInterval);    orderFlowBuy = 0.0;
                        OrderFlowSellNyse = new ArrayList(nInterval);orderFlowSellNYSE = 0.0;
                        OrderFlowBuyNyse = new ArrayList(nInterval);orderFlowBuyNYSE = 0.0;

                        bids4Filtering = new double[nInterval];// to determine if to keep transaction or not
                        asks4Filtering = new double[nInterval];// to determine if to keep transaction or not
                        MidQuotes = new ArrayList(nInterval);
                        br.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ParseException p){
                    p.printStackTrace();
                }
            }

            //allQuotesTrades.put(company, quotesTrades);
            //quotesTrades = allQuotesTrades.get("vodka");
            if (printSummary) {
                try {
                    //folder = "E:\\_paper2 Nyse Event\\";
                    //folder = "C:\\Users\\rojcek\\Documents\\School\\SFI\\_paper2b NYSE event\\DATA\\";
                    String outputFileName = folder + "quotesTrades\\" + company + interval + tag + "_burst" + ".csv";
                    //String outputFileName = folder + company + interval + tag + "_burst2" + ".csv";
                    FileWriter writer = new FileWriter(outputFileName, true);
                    Iterator dates = quotesTrades.keySet().iterator();
                    String date;
                    int sz;
                    int szArray;
                    boolean sizesOK;
                    while (dates.hasNext()) {
                        date = (String) dates.next();
                        ArrayList[] tradesQuotesDay = quotesTrades.get(date);
                        sz = tradesQuotesDay[0].size();
                        szArray = tradesQuotesDay.length;
                        sizesOK = true;
                        int j = 0;
                        while (sizesOK && (j < szArray)) {
                            sizesOK = tradesQuotesDay[j].size() == sz;
                            j++;
                        }
                        if (sizesOK) {
                            for (int i = 0; i < sz; i++) {
                                writer.write(date + "," + tradesQuotesDay[0].get(i) + "," + tradesQuotesDay[1].get(i) + "," + tradesQuotesDay[2].get(i) + "," + tradesQuotesDay[3].get(i) +
                                        "," + tradesQuotesDay[4].get(i) + "," + tradesQuotesDay[5].get(i) + "," + tradesQuotesDay[6].get(i) + "," + tradesQuotesDay[7].get(i) +
                                        "," + tradesQuotesDay[8].get(i) + "," + tradesQuotesDay[9].get(i) + "," + tradesQuotesDay[10].get(i) + "," + tradesQuotesDay[11].get(i) +
                                        "," + tradesQuotesDay[12].get(i) + "," + tradesQuotesDay[13].get(i) + "," + tradesQuotesDay[14].get(i) + "," + tradesQuotesDay[15].get(i) +
                                        "," + tradesQuotesDay[16].get(i) + "," + tradesQuotesDay[17].get(i) + "," + tradesQuotesDay[18].get(i) + "," + tradesQuotesDay[19].get(i) +
                                        "," + tradesQuotesDay[20].get(i) + "," + tradesQuotesDay[21].get(i) + "\r");// "," + tradesQuotesDay[22].get(i) + "," + tradesQuotesDay[23].get(i) + "\r");
                            }
                        } else {
                            System.out.println("Incomplete data gathering for date and company: " + date + " " + company);
                        }
                    }
                    writer.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }

            if (printTrades) {
                try {
                    String outputFileName = folder + "quotesTrades\\" + company + interval + "_filtered" + ".csv";
                    FileWriter writer = new FileWriter(outputFileName, true);
                    int sz = transactions.size();
                    String s;
                    String[] sa = new String[9];
                    for (int i = 0; i < sz; i++) {
                        s = new String();
                        sa = transactions.get(i);
                        for (int j = 0; j < 9; j++) {
                            s = s + sa[j] + ",";
                        }
                        writer.write(s + "\r");
                    }
                    writer.close();


                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
            transactions = new ArrayList<String[]>();
            //transactions.clear();
            quotesTrades = new TreeMap<String, ArrayList[]>();
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
