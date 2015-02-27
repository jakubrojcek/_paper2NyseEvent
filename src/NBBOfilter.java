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

    public NBBOfilter(int nw, int ni) {
        this.nWindows = nw;
        this.nIntervals = ni;
    }

    public double[] bestBid(TreeMap<Double, ArrayList<String[]>> bids){
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
        int sz = (int) Math.max(1.0, 0.15 * (bids.size()));    // 85% best bidPrices weighted by volume
        ArrayList lines;
        for (int i = 0; i < sz; i++){
            bestBid = (Double) bidPrices.next();
            lines = bids.get(bestBid);
            linesIterator = lines.iterator();
            while (linesIterator.hasNext()){
                bestAll++;
                line = (String[]) linesIterator.next();
                bestSize = Long.parseLong(line[5]);
                cumVolume += (bestBid * bestSize);
                cumShares += bestSize;
                if(line[8].equals("N") || line[8].equals("P")){ // NYSE and ARCA
                    bestNyse++;
                }
            }
        }
        returnValue[0] = (cumVolume / cumShares);
        returnValue[1] = bestAll;
        returnValue[2] = bestNyse;
        return returnValue;
    }

    public double[] bestAsk(TreeMap<Double, ArrayList<String[]>> asks){
        double[] returnValue = new double[3];
        returnValue[0] = 999.0;
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
        int sz = (int) Math.max(1.0, 0.15 * (asks.size()));    // 85% best bidPrices weighted by volume
        ArrayList lines;
        for (int i = 0; i < sz; i++){
            bestAsk = (Double) askPrices.next();
            lines = asks.get(bestAsk);  // TODO: check if works
            linesIterator = lines.iterator();
            while (linesIterator.hasNext()){
                bestAll++;
                line = (String[]) linesIterator.next();
                bestSize = Long.parseLong(line[6]);
                if (bestSize == 0 || bestAsk < 0.0){
                    System.out.println("check");
                }
                cumVolume += (bestAsk * bestSize);
                cumShares += bestSize;
                if(line[8].equals("N") || line[8].equals("P")){ // NYSE and ARCA
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
            price = Double.parseDouble(line[3]);
            quantity = Integer.parseInt(line[4]);
            cumVolume += (price * quantity);
            cumShares += quantity;
        }
        returnValue[0] = (cumVolume / cumShares);
        returnValue[1] = (cumVolume / returnValue[0]);

        return returnValue;
    }

    public void printTransactions(String companyName, String folder, ArrayList<String[]> transactionsToday){


    }
}
