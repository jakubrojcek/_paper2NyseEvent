/**
 * Created by rojcek on 21.01.14.
 * Shifted wavelet implementation for detecting bursts
 */
public class SwtTradesQuotes {
    public static void main(String [] args) {

        int[] x = {0,0,1,1,0,0,1,1,1,1,1,0,0,0,0,0};
        for (int i = 0; i < 16; i++){
            System.out.print(x[i] + ",");
        }
        System.out.println();
        int size = 16;
        int a = 4;
        int[] b = x;
        int[][] SWT = new int[4][16];
        for (int i = 0; i < a; i++){
            System.out.println("x for i = " + i + " is:");
            for (int j = 0; j < size - 1; j++){
                SWT[i][j]=b[j]+b[j+1];
                System.out.print(SWT[i][j] + ",");
            }
            System.out.println();
            System.out.println("b for i = " + i + " is:");
            for (int j = 0; j < SWT[i].length / 2; j++){
                b[j]=SWT[i][2 * j];
                System.out.print(b[j] + ",");
            }
            System.out.println();
            System.out.println(i);
        }
    }
}
