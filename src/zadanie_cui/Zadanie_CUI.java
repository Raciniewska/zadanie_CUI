package zadanie_cui;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Scanner;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author Basia
 */
public class Zadanie_CUI {

    /**
     * @param args the command line arguments
     */
    private static DecimalFormat df = new DecimalFormat("#.####");

    public static void main(String[] args) throws ParseException, IOException {

        String startDate = "", exitConfirmation, webAdress = "http://api.nbp.pl/api/exchangerates/rates/c/usd"
                + "/";
        Boolean nextSearch = true, correctDate = false;
        String welcome = "Witaj w programie zadanie_CUI wprowadz date od której mam przedstawić notowania kupna i sprzedazy waluty USD \n"
                + " Date wpisz w formacie rrrr-mm-dd";
        String wrongDate = "Podaj date ponownie w formacie rrrr-mm-dd", exit = "Jeżeli chcesz wyjsc nacisnij klawisz 'q' jezeli chcesz powtorzyc wyszukiwanie nacisnij dowolny inny klawisz";

        while (nextSearch) {
            System.out.println(welcome);

            while (!correctDate) { //reading the date given by the user
                startDate = new Scanner(System.in).nextLine();
                if (checkIfCorrect(startDate)) {
                    correctDate = true;
                } else {
                    System.out.println(wrongDate);
                }
            }
            correctDate = false;

            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            Date date = new Date();
            jsonGetRequest(webAdress + startDate + "/" + formatter.format(date)); //main program logic

            System.out.println(exit);
            exitConfirmation = new Scanner(System.in).nextLine();
            if (exitConfirmation.equalsIgnoreCase("q")) {
                nextSearch = false; //ending the program
            }
        }
    }

    public static Boolean checkIfCorrect(String givenDate) throws ParseException {
        //checking the date given by the user
        if (givenDate.matches("([0-9]{4})-([0-9]{2})-([0-9]{2})")) {
            if ((givenDate.charAt(5) > '0' && givenDate.charAt(6) > '2') || givenDate.charAt(5) > '1') {
                System.out.println("Blednie podany miesiac");
                return false;
            }
            if ((givenDate.charAt(8) >= '3' && givenDate.charAt(9) > '1') || givenDate.charAt(8) > '3') {
                System.out.println("Blednie podany dzien");
                return false;
            }
        } else {
            System.out.println("Bledny format wpisanej daty");
            return false;
        }
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Date now = new Date();
        Date startDate = new Date();
        try {
            startDate = formatter.parse(givenDate);
        } catch (java.text.ParseException ex) {
            System.out.println("Bledny format wpisanej daty");
            Logger.getLogger(Zadanie_CUI.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (startDate.after(now)) {
            System.out.println("Brak danych w tym okresie");
            return false;
        }
        long diffInMillies = Math.abs(now.getTime() - startDate.getTime());
        long diff = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
        if (diff >= 93) {
            System.out.println("Przekroczony limit 93 dni");
            return false;
        }
        return true;
    }

    private static String streamToString(InputStream inputStream) {
        String text = new Scanner(inputStream, "UTF-8").useDelimiter("\\Z").next();
        return text;
    }

    public static String jsonGetRequest(String urlQueryString) throws ParseException, IOException {
        String json = null;

        try {
            URL url = new URL(urlQueryString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("charset", "utf-8");
            connection.connect();
            if (connection.getResponseCode() < HttpURLConnection.HTTP_BAD_REQUEST) {
                InputStream inStream = connection.getInputStream();
                json = streamToString(inStream);
                JSONParser parser = new JSONParser();
                try {
                    Object data = parser.parse(json);
                    JSONObject JSONData = (JSONObject) data;
                    JSONArray array = (JSONArray) JSONData.get("rates");
                    System.out.println("Dzień       Cena Kupna   Cena Sprzedazy   Różnica Kupna   Różnica Sprzedaży");
                    df.setMinimumFractionDigits(4);
                    for (int i = 0; i < array.size(); i++) {
                        parseRateObject((JSONObject) array.get(i));
                        if (i == 0) {
                            System.out.println("-               -");
                        } else {
                            printBuyDiff((JSONObject) array.get(i - 1), (JSONObject) array.get(i));
                            printSellDiff((JSONObject) array.get(i - 1), (JSONObject) array.get(i));
                        }
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            } else {
                InputStream inStream = connection.getErrorStream();
                json = streamToString(inStream);
                System.out.println(json);
            }

        } catch (IOException ex) {
            System.out.println("Brak polaczenia z internetem");
        }
        return json;
    }

    private static void parseRateObject(JSONObject rate) {
        String effectiveDate = (String) rate.get("effectiveDate");
        System.out.print(effectiveDate + "  ");
        Double bid = (Double) rate.get("bid");
        System.out.print(df.format(bid) + "       ");
        Double ask = (Double) rate.get("ask");
        System.out.print(df.format(ask) + "           ");
    }

    private static void printBuyDiff(JSONObject rate1, JSONObject rate2) {
        Double bid1 = (Double) rate1.get("bid");
        Double bid2 = (Double) rate2.get("bid");
        System.out.print(df.format(bid2 - bid1) + "          ");
    }

    private static void printSellDiff(JSONObject rate1, JSONObject rate2) {
        Double ask1 = (Double) rate1.get("ask");
        Double ask2 = (Double) rate2.get("ask");
        System.out.println(df.format(ask2 - ask1));
    }
}
