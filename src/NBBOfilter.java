import java.io.*;
import java.util.*;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;

/**
 * Created by rojcek on 17.02.2015.
 * If you pass NBBOfilter two files (quotes and trades), it
 */
public class NBBOfilter {
    int nWindows;
    int nIntervals;
    int t3, t4, q5, q6, q8;

    public NBBOfilter(int nw, int ni, int q5, int q6, int q8, int t3, int t4) {
        this.nWindows = nw;
        this.nIntervals = ni;
        this.q5 = q5;
        this.q6 = q6;
        this.q8 = q8;
        this.t3 = t3;
        this.t4 = t4;
    }

    public double[] bestBid(TreeMap<Double, ArrayList<String[]>> bids, double upper, double lower){
        double[] returnValue = new double[3];
        returnValue[0] = 999.0;
        if (bids.isEmpty()){
            return returnValue;
        }
        double bestBid = 999.0;
        long bestSize = 0;
        double bestAll = 0.0;
        double bestNyse = 0.0;
        Iterator bidPrices = bids.descendingKeySet().iterator();
        Iterator linesIterator;
        double cumVolume = 0.0;
        long cumShares = 0;
        String[] line;
        int sz = (int) Math.max(1.0, upper * (bids.size()));    // 85% best bidPrices weighted by volume
        int low = (int) Math.max(0.0, lower * (bids.size()));    // 85% best bidPrices weighted by volume
        ArrayList lines;
        for (int i = 0; i < sz; i++){
            if (i < low){
                continue;
            }
            bestBid = (Double) bidPrices.next();
            lines = bids.get(bestBid);
            linesIterator = lines.iterator();
            while (linesIterator.hasNext()){
                bestAll++;
                line = (String[]) linesIterator.next();
                bestSize = Long.parseLong(line[q5]);
                cumVolume += (bestBid * bestSize);
                cumShares += bestSize;
                if(line[q8].equals("N") || line[q8].equals("P")){ // NYSE and ARCA
                    bestNyse++;
                }
            }
        }
        returnValue[0] = (cumVolume / cumShares);
        returnValue[1] = bestAll;
        returnValue[2] = bestNyse;
        return returnValue;
    }

    public double[] bestAsk(TreeMap<Double, ArrayList<String[]>> asks, double upper, double lower){
        double[] returnValue = new double[3];
        returnValue[0] = -999.0;
        if (asks.isEmpty()){
            return returnValue;
        }
        double bestAsk = 999.0;
        long bestSize = 0;
        double bestAll = 0.0;
        double bestNyse = 0.0;
        Iterator askPrices = asks.keySet().iterator();
        Iterator linesIterator;
        double cumVolume = 0.0;
        long cumShares = 0;
        String[] line;
        int sz = (int) Math.max(1.0, upper * (asks.size()));    // 85% best bidPrices weighted by volume
        int low = (int) Math.max(0.0, lower * (asks.size()));    // 85% best bidPrices weighted by volume
        ArrayList lines;
        for (int i = 0; i < sz; i++){
            if (i < low){
                continue;
            }
            bestAsk = (Double) askPrices.next();
            lines = asks.get(bestAsk);  // TODO: check if works
            linesIterator = lines.iterator();
            while (linesIterator.hasNext()){
                bestAll++;
                line = (String[]) linesIterator.next();
                bestSize = Long.parseLong(line[q6]);
                if (bestSize == 0 || bestAsk < 0.0){
                    System.out.println("check");
                }
                cumVolume += (bestAsk * bestSize);
                cumShares += bestSize;
                if(line[q8].equals("N") || line[q8].equals("P")){ // NYSE and ARCA
                    bestNyse++;
                }
            }
        }
        returnValue[0] = (cumVolume / cumShares);
        returnValue[1] = bestAll;
        returnValue[2] = bestNyse;

        if (returnValue[0] == 0.0){
            System.out.println("check");
        }
        return returnValue;
    }

    public double[] bestBid(ArrayList bids){
        double[] returnValue = new double[nIntervals];
        double bestBid = 99999999.9;
        for (int i = 0; i < nIntervals; i++){
            bestBid = (Double) bids.get(i);
            int j = Math.max(0, i - nWindows);
            while (j < i){
                bestBid = Math.min(bestBid, (Double) bids.get(j));
                j++;
            }
            returnValue[i] = bestBid;
        }

        return returnValue;
    }

    public double[] bestAsk(ArrayList asks){
        double[] returnValue = new double[nIntervals];
        double bestAsk = 0.0;
        for (int i = 0; i < nIntervals; i++){
            bestAsk = (Double) asks.get(i);
            int j = Math.max(0, i - nWindows);
            while (j < i){
                bestAsk = Math.max(bestAsk, (Double) asks.get(j));
                j++;
            }
            returnValue[i] = bestAsk;
        }

        return returnValue;
    }

    public double[] vwap(ArrayList<String[]> transactions){
        double[] returnValue = {0.0,999.0};
        if (transactions.isEmpty()){
            return returnValue;
        }
        Iterator lines = transactions.iterator();
        double cumVolume = 0.0;
        int cumShares = 0;
        double price;
        int quantity;
        String[] line;
        while (lines.hasNext()){
            line = (String[]) lines.next();
            price = Double.parseDouble(line[t3]);
            quantity = Integer.parseInt(line[t4]);
            cumVolume += (price * quantity);
            cumShares += quantity;
        }
        returnValue[0] = (cumVolume / cumShares);           // Volume Weighted Average Price
        returnValue[1] = (cumVolume / returnValue[0]);      // Number of shares

        return returnValue;
    }

    public void printTransactions(String companyName, String folder, ArrayList<String[]> transactionsToday){


    }
}
