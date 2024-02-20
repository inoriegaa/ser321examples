import org.junit.Test;
import static org.junit.Assert.*;
import org.json.JSONObject;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class ServerTest {

  Socket sock;
  OutputStream out;
  ObjectOutputStream os;

  DataInputStream in;


  // Establishing a connection to the server, make sure you start the server on localhost and 8888
  @org.junit.Before
  public void setUp() throws Exception {
    // Establish connection to server and create in/out streams
    sock = new Socket("localhost", 8888); // connect to host and socket

    // get output channel
    out = sock.getOutputStream();

    // create an object output writer (Java only)
    os = new ObjectOutputStream(out);

    // setup input stream
    in = new DataInputStream(sock.getInputStream());
  }

  @org.junit.After
  public void close() throws Exception {
    if (out != null)  out.close();
    if (sock != null) sock.close();
  }

  @Test
  public void addRequest() throws IOException {
    // create a correct req for server
    JSONObject req = new JSONObject();
    req.put("type", "add");
    req.put("num1", "1");
    req.put("num2", "2");

    // write the whole message
    os.writeObject(req.toString());
    // make sure it wrote and doesn't get cached in a buffer
    os.flush();

    String i = (String) in.readUTF();
    // assuming I get correct JSON back
    JSONObject res = new JSONObject(i);

    // test response
    assertTrue(res.getBoolean("ok"));
    assertEquals("add", res.getString("type"));
    assertEquals(3, res.getInt("result"));

    // Wrong request to server num2 missing
    JSONObject req2 = new JSONObject();
    req2.put("type", "add");
    req2.put("num1", "1");
    // write the whole message
    os.writeObject(req2.toString());
    // make sure it wrote and doesn't get cached in a buffer
    os.flush();

    i = (String) in.readUTF();
    // assuming I get correct JSON back
    res = new JSONObject(i);

    System.out.println(res);

    // test response
    assertFalse(res.getBoolean("ok"));
    assertEquals("Field num2 does not exist in request", res.getString("message"));

    // Wrong request to server num1 missing
    JSONObject req3 = new JSONObject();
    req3.put("type", "add");
    req3.put("num2", "1");
    // write the whole message
    os.writeObject(req3.toString());
    // make sure it wrote and doesn't get cached in a buffer
    os.flush();

    i = (String) in.readUTF();
    // assuming I get correct JSON back
    res = new JSONObject(i);

    System.out.println(res);

    // test response
    assertFalse(res.getBoolean("ok"));
    assertEquals("Field num1 does not exist in request", res.getString("message"));

    // Wrong request to server num1 num2 missing
    JSONObject req4 = new JSONObject();
    req4.put("type", "add");
    // write the whole message
    os.writeObject(req4.toString());
    // make sure it wrote and doesn't get cached in a buffer
    os.flush();

    i = (String) in.readUTF();
    // assuming I get correct JSON back
    res = new JSONObject(i);

    System.out.println(res);

    // test response
    assertFalse(res.getBoolean("ok"));
    assertEquals("Field num1 does not exist in request", res.getString("message"));

    // Wrong request to server num2 missing
    JSONObject req5 = new JSONObject();
    req5.put("type", "add");
    req5.put("num1", "hello");
    req5.put("num2", "2");
    // write the whole message
    os.writeObject(req5.toString());
    // make sure it wrote and doesn't get cached in a buffer
    os.flush();

    i = (String) in.readUTF();
    // assuming I get correct JSON back
    res = new JSONObject(i);

    System.out.println(res);

    // test response
    assertFalse(res.getBoolean("ok"));
    assertEquals("Field num1/num2 needs to be of type: int", res.getString("message"));
  }

  @Test
  public void echoRequest() throws IOException {
    // valid request with data
    JSONObject req1 = new JSONObject();
    req1.put("type", "echo");
    req1.put("data", "gimme this back!");
    // write the whole message
    os.writeObject(req1.toString());
    // make sure it wrote and doesn't get cached in a buffer
    os.flush();
    String i = (String) in.readUTF();
    // assuming I get correct JSON back
    JSONObject res = new JSONObject(i);
    // test response
    assertTrue(res.getBoolean("ok"));
    assertEquals("echo", res.getString("type"));
    assertEquals("Here is your echo: gimme this back!", res.getString("echo"));

    // Invalid request - no data sent
    JSONObject req2 = new JSONObject();
    req2.put("type", "echo");
    // write the whole message
    os.writeObject(req2.toString());
    // make sure it wrote and doesn't get cached in a buffer
    os.flush();
    i = (String) in.readUTF();
    // assuming I get correct JSON back
    res = new JSONObject(i);
    System.out.println(res);
    // test response
    assertFalse(res.getBoolean("ok"));
    assertEquals("Field data does not exist in request", res.getString("message"));
  }

  @Test
  public void addManyRequest() throws IOException {
    // create a correct req for server
    JSONObject req = new JSONObject();
    req.put("type", "addmany");
    List<String> myList = Arrays.asList(
      "12",
      "15",
      "111",
      "42"
    );
    req.put("nums", myList);
    // write the whole message
    os.writeObject(req.toString());
    // make sure it wrote and doesn't get cached in a buffer
    os.flush();
    String i = (String) in.readUTF();
    // assuming I get correct JSON back
    JSONObject res = new JSONObject(i);
    // test response
    assertTrue(res.getBoolean("ok"));
    assertEquals("addmany", res.getString("type"));
    assertEquals(180, res.getInt("result"));

    // Invalid request to server
    JSONObject req2 = new JSONObject();
    req2.put("type", "addmany");
    myList = Arrays.asList(
      "two",
      "15",
      "111",
      "42"
    );
    req2.put("nums", myList);
    // write the whole message
    os.writeObject(req2.toString());
    // make sure it wrote and doesn't get cached in a buffer
    os.flush();
    i = (String) in.readUTF();
    // assuming I get correct JSON back
    res = new JSONObject(i);
    System.out.println(res);
    // test response
    assertFalse(res.getBoolean("ok"));
    assertEquals("Values in array need to be ints", res.getString("message"));
  }

  @Test
  public void notJSON() throws IOException {
    // create a correct req for server
    os.writeObject("a");

    String i = (String) in.readUTF();
    // assuming I get correct JSON back
    JSONObject res = new JSONObject(i);

    // test response
    assertFalse(res.getBoolean("ok"));
    assertEquals("req not JSON", res.getString("message"));

    // calling the other test to make sure server continues to work and the "continue" does what it is supposed to do
    addRequest();
  }

  @Test
  public void charCountRequest() throws IOException {
    // valid request with findchar = false
    JSONObject req = new JSONObject();
    req.put("type", "charcount");
    req.put("findchar", false);
    req.put("count", "hello WORLD");
    // write the whole message
    os.writeObject(req.toString());
    // make sure it wrote and doesn't get cached in a buffer
    os.flush();
    String i = (String) in.readUTF();
    // assuming I get correct JSON back
    JSONObject res = new JSONObject(i);
    // test response
    assertTrue(res.getBoolean("ok"));
    assertEquals("charcount", res.getString("type"));
    assertEquals(11, res.getInt("result"));

    // valid request with findchar = true
    req = new JSONObject();
    req.put("type", "charcount");
    req.put("findchar", true);
    req.put("find", "l");
    req.put("count", "hello WORLD");
    os.writeObject(req.toString());
    os.flush();
    i = (String) in.readUTF();
    res = new JSONObject(i);
    System.out.println(res);
    assertTrue(res.getBoolean("ok"));
    assertEquals("charcount", res.getString("type"));
    assertEquals(3, res.getInt("result"));

    // Invalid request - no findchar sent 
    req = new JSONObject();
    req.put("type", "charcount");
    req.put("count", "hello");
    os.writeObject(req.toString());
    os.flush();
    i = (String) in.readUTF();
    res = new JSONObject(i);
    System.out.println(res);
    assertFalse(res.getBoolean("ok"));
    assertEquals("Field findchar does not exist in request", res.getString("message"));

    // Invalid requesst - findchar not a boolean
    req = new JSONObject();
    req.put("type", "charcount");
    req.put("findchar", 1);
    req.put("count", "hello");
    os.writeObject(req.toString());
    os.flush();
    i = (String) in.readUTF();
    res = new JSONObject(i);
    System.out.println(res);
    assertFalse(res.getBoolean("ok"));
    assertEquals("Field findchar needs to be of type: boolean", res.getString("message"));

    // Invalid request - no count sent
    req = new JSONObject();
    req.put("type", "charcount");
    req.put("findchar", false);
    os.writeObject(req.toString());
    os.flush();
    i = (String) in.readUTF();
    res = new JSONObject(i);
    System.out.println(res);
    assertFalse(res.getBoolean("ok"));
    assertEquals("Field count does not exist in request", res.getString("message"));

    // Invalid request - count not a string
    req = new JSONObject();
    req.put("type", "charcount");
    req.put("findchar", false);
    req.put("count", 123);
    os.writeObject(req.toString());
    os.flush();
    i = (String) in.readUTF();
    res = new JSONObject(i);
    System.out.println(res);
    assertFalse(res.getBoolean("ok"));
    assertEquals("Field count needs to be of type: String", res.getString("message"));

    // Invalid request - no find sent
    req = new JSONObject();
    req.put("type", "charcount");
    req.put("findchar", true);
    req.put("count", "hello");
    os.writeObject(req.toString());
    os.flush();
    i = (String) in.readUTF();
    res = new JSONObject(i);
    System.out.println(res);
    assertFalse(res.getBoolean("ok"));
    assertEquals("Field find does not exist in request", res.getString("message"));

    // Invalid request - find not a string
    req = new JSONObject();
    req.put("type", "charcount");
    req.put("findchar", true);
    req.put("find", 'l');
    req.put("count", "hello");
    os.writeObject(req.toString());
    os.flush();
    i = (String) in.readUTF();
    res = new JSONObject(i);
    System.out.println(res);
    assertFalse(res.getBoolean("ok"));
    assertEquals("Field find needs to be of type: String", res.getString("message"));

    // Invalid request - find not a single character
    req = new JSONObject();
    req.put("type", "charcount");
    req.put("findchar", true);
    req.put("find", "he");
    req.put("count", "hello");
    os.writeObject(req.toString());
    os.flush();
    i = (String) in.readUTF();
    res = new JSONObject(i);
    System.out.println(res);
    assertFalse(res.getBoolean("ok"));
    assertEquals("Field find needs to be a single character", res.getString("message"));
  }

  @Test
  public void storyboardRequest() throws IOException {
    /*
     * For this test to work you must remove entry from 'user' in
     * www/storyboard.json
     *
     * */
    // valid request with view = false
    JSONObject req = new JSONObject();
    req.put("type", "storyboard");
    req.put("view", false);
    req.put("name", "user");
    req.put("story", "test story");
    // write the whole message
    os.writeObject(req.toString());
    // make sure it wrote and doesn't get cached in a buffer
    os.flush();
    String i = (String) in.readUTF();
    // assuming I get correct JSON back
    JSONObject res = new JSONObject(i);
    // test response
    assertTrue(res.getBoolean("ok"));
    assertEquals("storyboard", res.getString("type"));
    assertEquals("[\"user\"]", res.getJSONArray("users").toString());
    assertEquals("[\"test story\"]", res.getJSONArray("storyboard").toString());

    // valid request with view = true
    req = new JSONObject();
    req.put("type", "storyboard");
    req.put("view", true);
    os.writeObject(req.toString());
    os.flush();
    i = (String) in.readUTF();
    res = new JSONObject(i);
    assertTrue(res.getBoolean("ok"));
    assertEquals("storyboard", res.getString("type"));
    assertEquals("[\"user\"]", res.getJSONArray("users").toString());
    assertEquals("[\"test story\"]", res.getJSONArray("storyboard").toString());

    // invalid request - no view sent
    req = new JSONObject();
    req.put("type", "storyboard");
    req.put("name", "user");
    req.put("story", "test");
    os.writeObject(req.toString());
    os.flush();
    i = (String) in.readUTF();
    res = new JSONObject(i);
    assertFalse(res.getBoolean("ok"));
    assertEquals("Field view does not exist in request", res.getString("message"));

    // invalid request - view not a boolean
    req = new JSONObject();
    req.put("type", "storyboard");
    req.put("view", 1);
    os.writeObject(req.toString());
    os.flush();
    i = (String) in.readUTF();
    res = new JSONObject(i);
    assertFalse(res.getBoolean("ok"));
    assertEquals("Field view needs to be of type: boolean", res.getString("message"));

    // invalid request - no name sent
    req = new JSONObject();
    req.put("type", "storyboard");
    req.put("view", false);
    req.put("story", "test");
    os.writeObject(req.toString());
    os.flush();
    i = (String) in.readUTF();
    res = new JSONObject(i);
    assertFalse(res.getBoolean("ok"));
    assertEquals("Field name does not exist in request", res.getString("message"));

    // invalid request - name not a string
    req = new JSONObject();
    req.put("type", "storyboard");
    req.put("view", false);
    req.put("name", 123);
    req.put("story", "test");
    os.writeObject(req.toString());
    os.flush();
    i = (String) in.readUTF();
    res = new JSONObject(i);
    assertFalse(res.getBoolean("ok"));
    assertEquals("Field name needs to be of type: String", res.getString("message"));

    // invalid request - repeated name
    req = new JSONObject();
    req.put("type", "storyboard");
    req.put("view", false);
    req.put("name", "user");
    req.put("story", "test");
    os.writeObject(req.toString());
    os.flush();
    i = (String) in.readUTF();
    res = new JSONObject(i);
    assertFalse(res.getBoolean("ok"));
    assertEquals("User user has already contributed.", res.getString("message"));

    // invalid request - no story sent
    req = new JSONObject();
    req.put("type", "storyboard");
    req.put("view", false);
    req.put("name", "user1");
    os.writeObject(req.toString());
    os.flush();
    i = (String) in.readUTF();
    res = new JSONObject(i);
    assertFalse(res.getBoolean("ok"));
    assertEquals("Field story does not exist in request", res.getString("message"));

    // invalid request - story not a string
    req = new JSONObject();
    req.put("type", "storyboard");
    req.put("view", false);
    req.put("name", "user1");
    req.put("story", 123);
    os.writeObject(req.toString());
    os.flush();
    i = (String) in.readUTF();
    res = new JSONObject(i);
    assertFalse(res.getBoolean("ok"));
    assertEquals("Field story needs to be of type: String", res.getString("message"));
  } 
}
