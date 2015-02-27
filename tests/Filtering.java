import java.io.*;
import java.text.ParseException;
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
 * This class implements TAQ data filters as described in book of Ait-Sahalia and Jacod
 * Output is NBBO series, transactions series and series of midquotes plus some quotes and trades cumulants
 */
public class Filtering {
    public static void main(String [] args) {
        double timeStart = System.nanoTime();
        String folder = "C:\\Users\\rojcek\\Documents\\School\\SFI\\_paper2b NYSE event\\DATA\\";
        String[] companies = null;                                              // holder for the names or tickers of the companies
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

        for (String company : companies){
            for (int i = 0; i < 5; i++){
                String outputFileName = folder + "quotesFiles3.csv";
                try{
                    FileWriter writer1 = new FileWriter(outputFileName, true);
                    writer1.write(company + "," + company + "_0.zip," + company + "_1.zip," + company + "_2.zip," + company + "_3.zip," + company + "_4.zip," + "\r");
                    writer1.close();
                }
                catch (Exception e){
                    e.printStackTrace();
                    System.exit(1);
                }
                outputFileName = folder + "tradesFiles3.csv";
                try{
                    FileWriter writer1 = new FileWriter(outputFileName, true);
                    writer1.write(company + "," + company + "_0.zip," + company + "_1.zip," + company + "_2.zip," + company + "_3.zip," + "\r");
                    writer1.close();
                }
                catch (Exception e){
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        }

        double timeEnd = System.nanoTime();
        System.out.println("Time elapsed: " + (timeEnd - timeStart));
    }
}