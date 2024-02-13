/*
Simple Web Server in Java which allows you to call 
localhost:9000/ and show you the root.html webpage from the www/root.html folder
You can also do some other simple GET requests:
1) /random shows you a random picture (well random from the set defined)
2) json shows you the response as JSON for /random instead the html page
3) /file/filename shows you the raw file (not as HTML)
4) /multiply?num1=3&num2=4 multiplies the two inputs and responses with the result
5) /github?query=users/amehlhase316/repos (or other GitHub repo owners) will lead to receiving
   JSON which will for now only be printed in the console. See the todo below

The reading of the request is done "manually", meaning no library that helps making things a 
little easier is used. This is done so you see exactly how to pars the request and 
write a response back
*/

package funHttpServer;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;
import java.util.LinkedHashMap;
import java.nio.charset.Charset;
import java.security.InvalidParameterException;

class WebServer {
    public static void main(String args[]) {
        WebServer server = new WebServer(9000);
    }

    /**
     * Main thread
     * 
     * @param port to listen on
     */
    public WebServer(int port) {
        ServerSocket server = null;
        Socket sock = null;
        InputStream in = null;
        OutputStream out = null;

        try {
            server = new ServerSocket(port);
            while (true) {
                sock = server.accept();
                out = sock.getOutputStream();
                in = sock.getInputStream();
                byte[] response = createResponse(in);
                out.write(response);
                out.flush();
                in.close();
                out.close();
                sock.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (sock != null) {
                try {
                    server.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Used in the "/random" endpoint
     */
    private final static HashMap<String, String> _images = new HashMap<>() {
        {
            put("streets", "https://iili.io/JV1pSV.jpg");
            put("bread", "https://iili.io/Jj9MWG.jpg");
        }
    };

    private Random random = new Random();

    /**
     * Reads in socket stream and generates a response
     * 
     * @param inStream HTTP input stream from socket
     * @return the byte encoded HTTP response
     */
    public byte[] createResponse(InputStream inStream) {

        byte[] response = null;
        BufferedReader in = null;

        try {

            // Read from socket's input stream. Must use an
            // InputStreamReader to bridge from streams to a reader
            in = new BufferedReader(new InputStreamReader(inStream, "UTF-8"));

            // Get header and save the request from the GET line:
            // example GET format: GET /index.html HTTP/1.1

            String request = null;

            boolean done = false;
            while (!done) {
                String line = in.readLine();

                System.out.println("Received: " + line);

                // find end of header("\n\n")
                if (line == null || line.equals(""))
                    done = true;
                // parse GET format ("GET <path> HTTP/1.1")
                else if (line.startsWith("GET")) {
                    int firstSpace = line.indexOf(" ");
                    int secondSpace = line.indexOf(" ", firstSpace + 1);

                    // extract the request, basically everything after the GET up to HTTP/1.1
                    request = line.substring(firstSpace + 2, secondSpace);
                }

            }
            System.out.println("FINISHED PARSING HEADER\n");

            // Generate an appropriate response to the user
            if (request == null) {
                response = "<html>Illegal request: no GET</html>".getBytes();
            } else {
                // create output buffer
                StringBuilder builder = new StringBuilder();
                // NOTE: output from buffer is at the end

                if (request.length() == 0) {
                    // shows the default directory page

                    // opens the root.html file
                    String page = new String(readFileInBytes(new File("www/root.html")));
                    // performs a template replacement in the page
                    page = page.replace("${links}", buildFileList());

                    // Generate response
                    builder.append("HTTP/1.1 200 OK\n");
                    builder.append("Content-Type: text/html; charset=utf-8\n");
                    builder.append("\n");
                    builder.append(page);

                } else if (request.equalsIgnoreCase("json")) {
                    // shows the JSON of a random image and sets the header name for that image

                    // pick a index from the map
                    int index = random.nextInt(_images.size());

                    // pull out the information
                    String header = (String) _images.keySet().toArray()[index];
                    String url = _images.get(header);

                    // Generate response
                    builder.append("HTTP/1.1 200 OK\n");
                    builder.append("Content-Type: application/json; charset=utf-8\n");
                    builder.append("\n");
                    builder.append("{");
                    builder.append("\"header\":\"").append(header).append("\",");
                    builder.append("\"image\":\"").append(url).append("\"");
                    builder.append("}");

                } else if (request.equalsIgnoreCase("random")) {
                    // opens the random image page

                    // open the index.html
                    File file = new File("www/index.html");

                    // Generate response
                    builder.append("HTTP/1.1 200 OK\n");
                    builder.append("Content-Type: text/html; charset=utf-8\n");
                    builder.append("\n");
                    builder.append(new String(readFileInBytes(file)));

                } else if (request.contains("file/")) {
                    // tries to find the specified file and shows it or shows an error

                    // take the path and clean it. try to open the file
                    File file = new File(request.replace("file/", ""));

                    // Generate response
                    if (file.exists()) { // success
                        builder.append("HTTP/1.1 200 OK\n");
                        builder.append("Content-Type: text/html; charset=utf-8\n");
                        builder.append("\n");
                        builder.append(
                                "Would theoretically be a file but removed this part, you do not have to do anything with it for the assignment");
                    } else { // failure
                        builder.append("HTTP/1.1 404 Not Found\n");
                        builder.append("Content-Type: text/html; charset=utf-8\n");
                        builder.append("\n");
                        builder.append("File not found: " + file);
                    }
                } else if (request.contains("multiply?")) {
                    // This multiplies two numbers, there is NO error handling, so when
                    // wrong data is given this just crashes

                    try {
                        Map<String, String> query_pairs = new LinkedHashMap<String, String>();
                        // extract path parameters
                        query_pairs = splitQuery(request.replace("multiply?", ""));

                        // extract required fields from parameters
                        Integer num1 = Integer.parseInt(query_pairs.get("num1"));
                        Integer num2 = Integer.parseInt(query_pairs.get("num2"));

                        // do math
                        Integer result = num1 * num2;

                        // Generate response
                        builder.append("HTTP/1.1 200 OK\n");
                        builder.append("Content-Type: text/html; charset=utf-8\n");
                        builder.append("\n");
                        builder.append("Result is: " + result);
                        if (query_pairs.size() > 2)
                            builder.append("""
                                    <html>
                                        <p>Extra values provided were ignored</p>
                                    </html>
                                    """);
                    } catch (Exception e) {
                        e.printStackTrace();

                        builder.append("HTTP/1.1 400 Bad Request\n");
                        builder.append("Content-Type: text/html; charset=utf-8\n");
                        builder.append("\n");
                        builder.append("""
                                <html>
                                  <h3>An error occurred while processing your request.</h3>
                                  <h4>Possible causes:</h4>
                                  <ul>
                                    <li>Integer value is too big</li>
                                    <li>Value provided is not an integer</li>
                                    <li>Incorrect use</li>
                                  </ul>
                                  <body>
                                    Correct use:
                                    <strong>/multiply?num1=X&num2=Y</strong>
                                    , where 'X' and 'Y' are replaced by integers.
                                  </body>
                                </html>
                                  """);
                    }
                } else if (request.contains("github?")) {
                    // pulls the query from the request and runs it with GitHub's REST API
                    // check out https://docs.github.com/rest/reference/
                    //
                    // HINT: REST is organized by nesting topics. Figure out the biggest one first,
                    // then drill down to what you care about
                    // "Owner's repo is named RepoName. Example: find RepoName's contributors"
                    // translates to
                    // "/repos/OWNERNAME/REPONAME/contributors"

                    try {
                        Map<String, String> query_pairs = new LinkedHashMap<String, String>();
                        query_pairs = splitQuery(request.replace("github?", ""));

                        if (!query_pairs.containsKey("query"))
                            throw new InvalidParameterException();
                        if (!query_pairs.get("query").startsWith("users")
                                || !query_pairs.get("query").endsWith("repos"))
                            throw new InvalidParameterException();

                        String json = fetchURL("https://api.github.com/" + query_pairs.get("query"));

                        if (json.isEmpty())
                            throw new NullPointerException();
                        System.out.println(json);

                        // Parse JSON
                        String result = "";
                        JSONArray jArray = new JSONArray(json);
                        for (int i = 0; i < jArray.length(); i++) {
                            JSONObject obj = jArray.getJSONObject(i);
                            String repo = String.format("""
                                    <html>
                                        <h4>Repo #%d:</h4>
                                        <ul>
                                            <li><strong>Name:</strong> %s</li>
                                            <li><strong>ID:</strong> %d</li>
                                            <li><strong>Owner:</strong> %s</li>
                                        </ul>
                                        <br>
                                    </html>
                                        """, i,
                                    obj.getString("name"),
                                    obj.getBigInteger("id"),
                                    obj.getJSONObject("owner").getString("login"));
                            result += repo;
                        }

                        builder.append("HTTP/1.1 200 OK\n");
                        builder.append("Content-Type: text/html; charset=utf-8\n");
                        builder.append("\n");
                        builder.append(result);
                    } catch (InvalidParameterException ipe) {
                        ipe.printStackTrace();
                        builder.append("HTTP/1.1 403 Forbidden\n");
                        builder.append("Content-type: text/html; charset=utf-8\n");
                        builder.append("\n");
                        builder.append("""
                                <html>
                                    <h3>An error occurred while processing your request.</h3>
                                    <h4>Possible causes:</h4>
                                    <ul>
                                        <li>Invalid or missing query</li>
                                    </ul>
                                    <p>Correct use:
                                     <strong>/github?query=users/USER/repos</strong>
                                      , where 'USER' is replaced by a GitHub user's name</p>
                                </html>
                                """);
                    } catch (NullPointerException npe) {
                        npe.printStackTrace();
                        builder.append("HTTP/1.1 404 Not Found\n");
                        builder.append("Content-Type: text/html; charset=utf-8\n");
                        builder.append("\n");
                        builder.append("""
                                <html>
                                    <h3>An error occurred while processing your request.</h3>
                                    <h4>Possible causes:</h4>
                                    <ul>
                                        <li>Queried user does not exist.</li>
                                        <li>Invalid query</li>
                                    </ul>
                                    <p>Correct use:
                                     <strong>/github?query=users/USER/repos</strong>
                                     , where 'USER' is replaced by a GitHub user's name.</p>
                                </html>
                                """);
                    } catch (Exception e) {
                        e.printStackTrace();
                        builder.append("HTTP/1.1 400 Bad Request\n");
                        builder.append("Content-Type: text/html; charset=utf-8\n");
                        builder.append("\n");
                        builder.append("""
                                <html>
                                    <h3>An error occurred while processing your request.</h3>
                                    <h4>Possible causes:</h4>
                                    <ul>
                                        <li>Missing argument</li>
                                    </ul>
                                    <p>Correct use:
                                     <strong>/github?query=users/USER/repos</strong>
                                     , where 'USER' is replaced by a GitHub user's name.</p>
                                </html>

                                    """);
                    }
                } else if (request.contains("compatible?")) {
                    try {
                        Map<String, String> query_pairs = new LinkedHashMap<String, String>();
                        query_pairs = splitQuery(request.replace("compatible?", ""));

                        String name1 = query_pairs.get("name1");
                        String name2 = query_pairs.get("name2");
                        if (name1 == null || name2 == null)
                            throw new Exception();
                        Random random = new Random();
                        double probability = random.nextDouble(100.00 - 20.0) + 20.0;
                        if (name1.charAt(0) == name2.charAt(0))
                            probability = probability + 10.0 > 100.0 ? 100.0 : probability + 10.0;
                        if (name1.substring(name1.length() - 1).equals("a")
                                || name2.substring(name2.length() - 1).equals("a"))
                            probability = probability - 20.0 < 0.0 ? 0.0 : probability - 20.0;

                        builder.append("HTTP/1.1 200 OK\n");
                        builder.append("Content-Type: text/html; charset=utf-8\n");
                        builder.append("\n");
                        builder.append(String.format("""
                                <html>
                                    <style>
                                        p {text-align: center;}
                                    </style>
                                    <p>%s + %s</p>
                                    <p style="font-size:50px;">%s</p>
                                    <p>%.2f%%</p>
                                    <p>Compatible</p>
                                </html>

                                    """, name1, name2, (probability > 50 ? "&#x1F496" : "&#128148"), probability));
                    } catch (Exception e) {
                        e.printStackTrace();
                        builder.append("HTTP/1.1 400 Bad Request\n");
                        builder.append("Content-Type: text/html; charset=utf-8\n");
                        builder.append("\n");
                        builder.append("""
                                <html>
                                    <h3>An error occurred while processing your request.</h3>
                                    <h4>Possible causes:</h4>
                                    <ul>
                                        <li>Invalid or missing arguments</li>
                                    </ul>
                                    <p>Correct use:
                                     <strong>/compatible?name1=X&name2=Y</strong>
                                      , where 'X' and 'Y' are replaced by a person's name.
                                </html>

                                    """);
                    }
                } else if (request.contains("chat?")) {
                    try {
                        
                        if (!request.equals("chat?")) {
                            Map<String, String> query_pairs = new LinkedHashMap<String, String>();
                            query_pairs = splitQuery(request.replace("chat?", ""));

                            String name = query_pairs.get("name");
                            String msg = query_pairs.get("msg");
                            if (name == null || name. isEmpty() || msg == null || msg.isEmpty())
                                throw new Exception();
                            String html = String.format("""
                                    <html>
                                    <p><strong>%s:</strong> %s</p>
                                    </html>
                                            """, name, msg);
                            FileWriter fileOut = new FileWriter("www/chat.html", true);
                            fileOut.write(html);
                            fileOut.close();
                        }

                        FileReader fileIn = new FileReader("www/chat.html");
                        String chat = "";
                        int i;
                        while ((i = fileIn.read()) != -1)
                            chat += (char) i;
                        fileIn.close();

                        builder.append("HTTP/1.1 200 OK\n");
                        builder.append("Content-Type: text/html; charset=utf-8\n");
                        builder.append("\n");
                        builder.append(chat);

                    } catch (Exception e) {
                        e.printStackTrace();
                        builder.append("HTTP/1.1 400 Bad Request\n");
                        builder.append("Content-Type: text/html; charset=utf-8\n");
                        builder.append("\n");
                        builder.append("""
                                <html>
                                    <h3>An error occurred while processing your request.</h3>
                                    <h4>Possible causes:</h4>
                                    <ul>
                                        <li>Invalid or missing arguments</li>
                                    </ul>
                                    <p>Correct uses:
                                     <strong>/chat?name=X&msg=Y</strong>
                                      , where 'X' is replaced by your name and 'Y' by a message.
                                </html>

                                    """);
                    }
                } else {
                    // if the request is not recognized at all

                    builder.append("HTTP/1.1 400 Bad Request\n");
                    builder.append("Content-Type: text/html; charset=utf-8\n");
                    builder.append("\n");
                    builder.append("I am not sure what you want me to do...");
                }

                // Output
                response = builder.toString().getBytes();
            }
        } catch (IOException e) {
            e.printStackTrace();
            response = ("<html>ERROR: " + e.getMessage() + "</html>").getBytes();
        }

        return response;
    }

    /**
     * Method to read in a query and split it up correctly
     * 
     * @param query parameters on path
     * @return Map of all parameters and their specific values
     * @throws UnsupportedEncodingException If the URLs aren't encoded with UTF-8
     */
    public static Map<String, String> splitQuery(String query) throws UnsupportedEncodingException {
        Map<String, String> query_pairs = new LinkedHashMap<String, String>();
        // "q=hello+world%2Fme&bob=5"
        String[] pairs = query.split("&");
        // ["q=hello+world%2Fme", "bob=5"]
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
                    URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
        }
        // {{"q", "hello world/me"}, {"bob","5"}}
        return query_pairs;
    }

    /**
     * Builds an HTML file list from the www directory
     * 
     * @return HTML string output of file list
     */
    public static String buildFileList() {
        ArrayList<String> filenames = new ArrayList<>();

        // Creating a File object for directory
        File directoryPath = new File("www/");
        filenames.addAll(Arrays.asList(directoryPath.list()));

        if (filenames.size() > 0) {
            StringBuilder builder = new StringBuilder();
            builder.append("<ul>\n");
            for (var filename : filenames) {
                builder.append("<li>" + filename + "</li>");
            }
            builder.append("</ul>\n");
            return builder.toString();
        } else {
            return "No files in directory";
        }
    }

    /**
     * Read bytes from a file and return them in the byte array. We read in blocks
     * of 512 bytes for efficiency.
     */
    public static byte[] readFileInBytes(File f) throws IOException {

        FileInputStream file = new FileInputStream(f);
        ByteArrayOutputStream data = new ByteArrayOutputStream(file.available());

        byte buffer[] = new byte[512];
        int numRead = file.read(buffer);
        while (numRead > 0) {
            data.write(buffer, 0, numRead);
            numRead = file.read(buffer);
        }
        file.close();

        byte[] result = data.toByteArray();
        data.close();

        return result;
    }

    /**
     *
     * a method to make a web request. Note that this method will block execution
     * for up to 20 seconds while the request is being satisfied. Better to use a
     * non-blocking request.
     * 
     * @param aUrl the String indicating the query url for the OMDb api search
     * @return the String result of the http request.
     *
     **/
    public String fetchURL(String aUrl) {
        StringBuilder sb = new StringBuilder();
        URLConnection conn = null;
        InputStreamReader in = null;
        try {
            URL url = new URL(aUrl);
            conn = url.openConnection();
            if (conn != null)
                conn.setReadTimeout(20 * 1000); // timeout in 20 seconds
            if (conn != null && conn.getInputStream() != null) {
                in = new InputStreamReader(conn.getInputStream(), Charset.defaultCharset());
                BufferedReader br = new BufferedReader(in);
                if (br != null) {
                    int ch;
                    // read the next character until end of reader
                    while ((ch = br.read()) != -1) {
                        sb.append((char) ch);
                    }
                    br.close();
                }
            }
            in.close();
        } catch (Exception ex) {
            System.out.println("Exception in url request:" + ex.getMessage());
        }
        return sb.toString();
    }
}
