package com.example.shelfaware;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
// import org.json.JSONObject;

interface MyCallback {
    void onResult(String result);
}

/**
 *
 * @author sunilbuddaraju
 */
public class Cs124hproject {

    public static void getRecipe(MyCallback callback) {
        String apiKey = "sk-ejjakfwjwfuncdsdupjlkfxhymrprzajuhjxnxofmqtcpadu";

        String ingredients = "tomatoes, onions, spinach, eggs, chicken breast";

        String requestBody = "{\n" +
        "  \"model\": \"deepseek-ai/DeepSeek-R1-Distill-Qwen-1.5B\",\n" +
        "  \"stream\": false,\n" +
        "  \"max_tokens\": 2047,\n" +
        "  \"temperature\": 0.7,\n" +
        "  \"top_p\": 0.7,\n" +
        "  \"top_k\": 50,\n" +
        "  \"frequency_penalty\": 0.5,\n" +
        "  \"n\": 1,\n" +
        "  \"messages\": [\n" +
        "    {\"role\": \"system\", \"content\": \"You are a helpful chef.\"},\n" +
        "    {\"role\": \"user\", \"content\": \"Give me a recipe using these ingredients: " + ingredients + "\"}\n" +
        "  ],\n" +
        "  \"stop\": []\n" +
        "}";

        String toReturn = "Error";

        try {
            URL url = new URL("https://api.siliconflow.cn/v1/chat/completions");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            Scanner scanner;
            if (conn.getResponseCode() >= 200 && conn.getResponseCode() < 300) {
                scanner = new Scanner(conn.getInputStream());
            } else {
                scanner = new Scanner(conn.getErrorStream());
            }

            StringBuilder response = new StringBuilder();
            while (scanner.hasNext()) {
                response.append(scanner.nextLine());
            }
            scanner.close();


            // JSONObject responseJSON = new JSONObject(response.toString());
            // String reply = responseJSON.getString("choices");

//            System.out.println("Response from DeepSeek:");
//            System.out.println(response.toString());

            callback.onResult(response.toString());


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
