package ca.yorku.eecs;

import java.io.*;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.sun.net.httpserver.Headers;
import org.neo4j.driver.v1.*;
import com.sun.net.httpserver.HttpExchange;

class Utils {
    // use for extracting query params
    public static Map<String, String> splitQuery(String query) throws UnsupportedEncodingException {
        Map<String, String> query_pairs = new LinkedHashMap<String, String>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
        }
        return query_pairs;
    }

    // one possible option for extracting JSON body as String
    public static String convert(InputStream inputStream) throws IOException {
                
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            return br.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }

    // another option for extracting JSON body as String
    public static String getBody(HttpExchange he) throws IOException {
                InputStreamReader isr = new InputStreamReader(he.getRequestBody(), "utf-8");
            BufferedReader br = new BufferedReader(isr);
            
            int b;
            StringBuilder buf = new StringBuilder();
            while ((b = br.read()) != -1) {
                buf.append((char) b);
            }

            br.close();
            isr.close();
	    
        return buf.toString();
        }
    
    //Send HTTP Message to client
    public void sendString(HttpExchange request, String data, int restCode)
            throws IOException {
        request.sendResponseHeaders(restCode, data.length());
        OutputStream os = request.getResponseBody();
        os.write(data.getBytes());
        os.close();
    }

    public void sendResponse(HttpExchange request, String response, int statusCode) throws IOException {
        Headers headers = request.getResponseHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Access-Control-Allow-Origin", "*"); // Add this if you want to allow CORS

        request.sendResponseHeaders(statusCode, response.getBytes().length);
        try (OutputStream outputStream = request.getResponseBody()) {
            outputStream.write(response.getBytes());
        }
    }

    //Searching for a specific key-value pair in JSON
    public static String findJsonProperty(String jsonString, String target) {
        String targetString = "\"" + target + "\": ";
        int startIndex = jsonString.indexOf(targetString);
        if (startIndex != -1) {
            int startQuote = jsonString.indexOf("\"", startIndex + targetString.length());
            int endQuote = jsonString.indexOf("\"", startQuote + 1);
            if (endQuote != -1) {
                return jsonString.substring(startQuote + 1, endQuote);
            }
        }
        return null;
    }

    /*
    determining if the input string a valid rating number between 1 and 5 inclusive
    3 possible cases
    1. input string is null. Return -1 to indicate this case
    2. input string is not a valid integer or is not between 1 and 5 inclusive, in this case return 0.
    3. input string is a valid integer between 1 and 5 inclusive, in this case, we want the API controller to continue
       with the request. Return 1 to indicate this case
     */
    public static int isNumeric(String str) {
        //case 1
        if (str == null) {
            return -1;
        }
        int d;
        //case 2
        //try catch block to avoid exception when parsing string to int
        try {
            d = Integer.parseInt(str);
        } catch (NumberFormatException nfe) {
            return 0;
        }
        //checking for range validity
        if (d < 1 || d > 5) {
            return 0;
        }
        //case 3
        return 1;
    }

}
