package kie.com;

import com.sun.deploy.util.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.imageio.ImageIO;
import javax.net.ssl.HttpsURLConnection;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.*;


public class Main {

    String web;
    String thsrURL = "https://irs.thsrc.com.tw";
    HashMap<String, String> cookies = new HashMap<>();
    static final String WINDOW_SEAT = "radio20";
    static final String AISLE_SEAT = "radio22";
    static final String SEAT_NO_PREF = "radio18";

    public static void main(String[] args) {
        Main main = new Main();
        main.run();
    }

    static String cookieToString(HashMap<String, String> cookie) {
        StringBuilder builder = new StringBuilder();
        Iterator<Map.Entry<String, String>> entries = cookie.entrySet().iterator();
        if (entries.hasNext()) {
            Map.Entry<String, String> e = entries.next();
            builder.append(e.getValue());
        }
        while (entries.hasNext()) {
            Map.Entry<String, String> e = entries.next();
            builder.append(";").append(e.getValue());
        }


        return builder.toString();
    }

    void addCookie(List<String> c) {

        for (String s : c) {
            String d = s.split(";")[0];
            String[] entry = d.split("=");
            if (entry.length == 2) {
                this.cookies.put(entry[0], s);
            }
        }
    }


    void run() {
        web = downloadWeb(thsrURL + "/IMINT?locale=tw");
        Document document = Jsoup.parse(web);
        Element captcha = document.getElementById("BookingS1Form_homeCaptcha_passCode");
        Element bookingForm = document.getElementById("BookingS1Form");

//        Element startStation = bookingForm.getElementsByAttributeValue("name", "selectStartStation").get(0);
        getCaptchaAnswer(captcha);
        Scanner scanner = new Scanner(System.in);
        String out = null;

        try {
            out = generateForm("1", "2", SEAT_NO_PREF, URLEncoder.encode("2018/11/10", "UTF-8"), "600A", "1", scanner.next());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

//        out = "sdff";
        if (out != null) {
            String location = bookingForm.attr("action");
            String result = post(thsrURL + "/IMINT/"
                    + location.substring(location.indexOf("?wicket")), Collections.singletonList(out));
            System.out.println(result);
        }


//        System.out.println(web);
    }

    private String generateForm(String startStation, String desStation, String seatPref, String toDate, String toTime, String ticketNum, String captcha) {
        String out = "BookingS1Form%3Ahf%3A0=&selectStartStation=" + startStation +
                "&selectDestinationStation=" + desStation +
                "&trainCon%3AtrainRadioGroup=0" +
                "&seatCon%3AseatRadioGroup=" + seatPref +
                "&bookingMethod=0" +
                "&toTimeInputField=" + toDate +
                "&toTimeTable=" + toTime +
                "&toTrainIDInputField=" +
                "&backTimeInputField=" + toDate +
                "&backTimeTable=" +
                "&backTrainIDInputField=" +
                "&ticketPanel%3Arows%3A0%3AticketAmount=" + ticketNum + "F" +
                "&ticketPanel%3Arows%3A1%3AticketAmount=0H" +
                "&ticketPanel%3Arows%3A2%3AticketAmount=0W" +
                "&ticketPanel%3Arows%3A3%3AticketAmount=0E" +
                "&homeCaptcha%3AsecurityCode=" + captcha +
                "&SubmitButton=%E9%96%8B%E5%A7%8B%E6%9F%A5%E8%A9%A2";
        return out;

    }

    private void getCaptchaAnswer(Element in) {
        String captchaURL = in.attr("src");
        BufferedImage bufferedImage = downloadImage(thsrURL + captchaURL);
        ImageIcon icon = new ImageIcon(bufferedImage);
        JFrame frame = new JFrame();
        frame.setLayout(new FlowLayout());
        frame.setSize(200, 300);
        JLabel lbl = new JLabel();
        lbl.setIcon(icon);
        frame.add(lbl);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);


    }

    private String post(String url, List<String> data) {
        StringBuilder response = new StringBuilder();
        try {
            URL url1 = new URL(url);
            HttpsURLConnection connection = (HttpsURLConnection) url1.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("User-agent", "Chrome/68.0.3440.106 ");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", Integer.toString(data.get(0).length()));
            connection.setRequestProperty("Cache-Control", "max-age=0");
            connection.setRequestProperty("Connection", "keep-alive");
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
            connection.setRequestProperty("Origin", "https://irs.thsrc.com.tw");
            connection.setRequestProperty("Referer", "https://irs.thsrc.com.tw/IMINT?locale=tw");
            connection.setRequestProperty("Accept-Language", "zh-TW,zh;q=0.9,en-US;q=0.8,en;q=0.7,ny;q=0.6,gu;q=0.5");
            connection.setRequestProperty("Host", "irs.thsrc.com.tw");
            connection.setRequestProperty("Cookie", cookieToString(cookies));
            connection.setDoOutput(true);
            connection.setInstanceFollowRedirects(false);

            OutputStreamWriter os = new OutputStreamWriter(connection.getOutputStream(), "8859_1");
            os.write(data.get(0));
            os.flush();
            os.close();

            int status = connection.getResponseCode();
            System.out.println(status);
            if (status != HttpsURLConnection.HTTP_OK) {
                if (status == HttpsURLConnection.HTTP_MOVED_PERM ||
                        status == HttpsURLConnection.HTTP_MOVED_TEMP ||
                        status == HttpsURLConnection.HTTP_SEE_OTHER) {

                    String newUrl = connection.getHeaderField("Location");
                    List<String> list = connection.getHeaderFields().get("Set-Cookie");
                    list = new LinkedList<>(list);
//                    list.add(cookies);
//                    cookies = StringUtils.join(list, ";");

                    connection = (HttpsURLConnection) new URL(newUrl).openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("User-agent", "Chrome/68.0.3440.106 ");
                    connection.setRequestProperty("Cache-Control", "max-age=0");
                    connection.setRequestProperty("Connection", "keep-alive");
                    connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");

                    connection.setRequestProperty("Accept-Language", "zh-TW,zh;q=0.9,en-US;q=0.8,en;q=0.7,ny;q=0.6,gu;q=0.5");
                    connection.setRequestProperty("Host", "irs.thsrc.com.tw");
                    connection.setRequestProperty("Origin", "https://irs.thsrc.com.tw");
                    connection.setRequestProperty("Referer", "https://irs.thsrc.com.tw/IMINT?locale=tw");
                    connection.setInstanceFollowRedirects(false);
                    connection.setRequestProperty("Cookie", cookieToString(cookies));
                }
            }
            status = connection.getResponseCode();
            System.out.println(status);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine).append("\n");
            }
            in.close();
            connection.disconnect();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return response.toString();
    }

    private String downloadWeb(String url) {

        StringBuilder response = new StringBuilder();
        try {
            URL url1 = new URL(url);
            HttpsURLConnection connection = (HttpsURLConnection) url1.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-agent", "Chrome/68.0.3440.106");
            connection.setRequestProperty("Cache-Control", "max-age=0");
            connection.setRequestProperty("Connection", "keep-alive");
            connection.setRequestProperty("Accept-Language", "zh-TW,zh;q=0.9,en-US;q=0.8,en;q=0.7,ny;q=0.6,gu;q=0.5");
            connection.setInstanceFollowRedirects(false);

            int status = connection.getResponseCode();
            if (status != HttpsURLConnection.HTTP_OK) {
                if (status == HttpsURLConnection.HTTP_MOVED_PERM ||
                        status == HttpsURLConnection.HTTP_MOVED_TEMP ||
                        status == HttpsURLConnection.HTTP_SEE_OTHER) {

                    String newUrl = connection.getHeaderField("Location");
                    List<String> list = connection.getHeaderFields().get("Set-Cookie");
                    addCookie(list);

//                    list = new LinkedList<>(list);
//                    list.add(cookies);
//                    cookies = StringUtils.join(list, ";");

                    connection = (HttpsURLConnection) new URL(newUrl).openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("User-agent", "Chrome/68.0.3440.106 ");
                    connection.setRequestProperty("Cache-Control", "max-age=0");
                    connection.setRequestProperty("Connection", "keep-alive");
                    connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");

                    connection.setRequestProperty("Accept-Language", "zh-TW,zh;q=0.9,en-US;q=0.8,en;q=0.7,ny;q=0.6,gu;q=0.5");
                    connection.setRequestProperty("Host", "irs.thsrc.com.tw");
                    connection.setInstanceFollowRedirects(false);
                    connection.setRequestProperty("Cookie", cookieToString(cookies));
                    status = connection.getResponseCode();

                }
            }

            List<String> list = connection.getHeaderFields().get("Set-Cookie");
            addCookie(list);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine).append("\n");
            }
            in.close();
            connection.disconnect();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return response.toString();
    }

    private BufferedImage downloadImage(String url) {
        BufferedImage image = null;
        try {
            HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-agent", "Chrome/68.0.3440.106 ");
            connection.setRequestProperty("Cache-Control", "max-age=0");
            connection.setRequestProperty("Connection", "keep-alive");
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
            connection.setRequestProperty("Accept-Language", "zh-TW,zh;q=0.9,en-US;q=0.8,en;q=0.7,ny;q=0.6,gu;q=0.5");
            connection.setRequestProperty("Host", "irs.thsrc.com.tw");
            connection.setInstanceFollowRedirects(false);
            int status = connection.getResponseCode();
            if (status != HttpsURLConnection.HTTP_OK) {
                if (status == HttpsURLConnection.HTTP_MOVED_PERM ||
                        status == HttpsURLConnection.HTTP_MOVED_TEMP ||
                        status == HttpsURLConnection.HTTP_SEE_OTHER) {

                    String newUrl = connection.getHeaderField("Location");
                    List<String> list = connection.getHeaderFields().get("Set-Cookie");
                    list = new LinkedList<>(list);
//                    list.add(cookies);
                    connection = (HttpsURLConnection) new URL(newUrl).openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("User-agent", "Chrome/68.0.3440.106 ");
                    connection.setRequestProperty("Cache-Control", "max-age=0");
                    connection.setRequestProperty("Connection", "keep-alive");
                    connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
                    connection.setRequestProperty("Accept-Language", "zh-TW,zh;q=0.9,en-US;q=0.8,en;q=0.7,ny;q=0.6,gu;q=0.5");
                    connection.setRequestProperty("Cookie", cookieToString(cookies));
                }
            }

            status = connection.getResponseCode();
//            System.out.println(cookies);
            BufferedInputStream inputStream = new BufferedInputStream(connection.getInputStream());
            image = ImageIO.read(inputStream);
            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return image;
    }
}
