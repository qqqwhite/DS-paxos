import java.util.Scanner;

public class ts {
    public static void main(String[] args)
    {

        /* Write your code here */
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter two strings:");

        String x = sc.nextLine();
        String y = sc.nextLine();

        if (x.length() != y.length())
        {
            System.out.println("error");
        }
        else
        {
            String result = "";

            for (int i = 0; i < x.length(); i++)
            {
                result = x.substring(i, i + 1) + result;
                result = y.substring(i, i + 1) + result;
            }
            System.out.println(result);
        }
    }
}
