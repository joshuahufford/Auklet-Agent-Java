package io.auklet.agent;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.*;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public final class Device {

    private Device(){ }

    private static String filename = "/AukletAuth";
    private static String client_id;
    private static String client_username;
    private static String client_password;
    private static String organization;

    public static void register_device(String folderPath){

        JSONParser parser = new JSONParser();

        try {
            Object obj = parser.parse(new FileReader(folderPath + filename));
            JSONObject jsonObject = (JSONObject) obj;
            setCreds(jsonObject);

        } catch (FileNotFoundException e) {
            JSONObject newObject = create_device();
            setCreds(newObject);
            writeCreds(folderPath + filename);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }

    private static JSONObject create_device(){
        HttpClient httpClient = HttpClientBuilder.create().build();

        try {
            JSONObject obj = new JSONObject();
            obj.put("mac_address_hash", Util.getMacAddressHash());
            obj.put("application", Auklet.AppId);
            HttpPost request = new HttpPost(Auklet.baseUrl + "/private/devices/");
            StringEntity params = new StringEntity(obj.toJSONString());
            request.addHeader("content-type", "application/json");
            request.addHeader("Authorization", "JWT "+Auklet.ApiKey);
            request.setEntity(params);
            HttpResponse response = httpClient.execute(request);

            String text = null;
            try (Scanner scanner = new Scanner(response.getEntity().getContent(), StandardCharsets.UTF_8.name())) {
                text = scanner.useDelimiter("\\A").next();
            }

            if(response.getStatusLine().getStatusCode() != 201){
                System.out.println("could not create a device and status code is: " + response.getStatusLine().getStatusCode());
                throw new Exception();
            }
            JSONParser parser = new JSONParser();
            JSONObject myResponse = (JSONObject) parser.parse(text);
            return myResponse;

            //handle response here...

        }catch (Exception ex) {

            //handle exception here
        }
        return null;
    }

    private static void setCreds(JSONObject jsonObject) {

        client_password = (String) jsonObject.get("client_password");

        client_username = (String) jsonObject.get("id");

        client_id = (String) jsonObject.get("client_id");

        organization = (String) jsonObject.get("organization");
    }

    private static void writeCreds(String filename){

        JSONObject obj = new JSONObject();
        obj.put("client_password", client_password);
        obj.put("id", client_username);
        obj.put("client_id", client_id);
        obj.put("organization", organization);

        try (FileWriter file = new FileWriter(filename)) {

            file.write(obj.toJSONString());
            file.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static String getClient_Username(){
        return client_username;
    }

    public static String getClient_Password(){
        return client_password;
    }

    public static String getClient_Id(){
        return client_id;
    }

    public static String getOrganization(){
        return organization;
    }

    public static void get_Certs(String folderPath) {


        try {
            HttpClient httpClient = HttpClientBuilder.create().build();
            URL newUrl = new URL(Auklet.baseUrl + "private/devices/certificates/");
            HttpURLConnection con = (HttpURLConnection) newUrl.openConnection();

            con.setRequestProperty("Authorization", "JWT " + Auklet.ApiKey);;
            con.setDoInput(true);
            con.setRequestMethod("GET");
            con.setInstanceFollowRedirects(true);

            con.getResponseCode();

            HttpGet request = new HttpGet(con.getURL().toURI());
            HttpResponse response = httpClient.execute(request);
            InputStream ca = response.getEntity().getContent();
            String text = null;
            try (Scanner scanner = new Scanner(ca, StandardCharsets.UTF_8.name())) {
                text = scanner.useDelimiter("\\A").next();
            }

            File file = new File(folderPath + "/CA");
            if (file.createNewFile())
            {
                BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath + "/CA"));
                writer.write(text);
                writer.close();
                System.out.println("CA File is created!");

            } else {
                System.out.println("CA File already exists.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }

    }
}