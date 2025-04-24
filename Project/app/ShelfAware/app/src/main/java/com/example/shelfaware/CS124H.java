/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */
package com.example.shelfaware;
import android.util.Log;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import org.json.JSONObject;

/**
 *
 * @author sunilbuddaraju
 */
public class CS124H {

    public static String getRecipes(String entered) {
        String apiKey = "sk-ejjakfwjwfuncdsdupjlkfxhymrprzajuhjxnxofmqtcpadu";

        String ingredients = entered;

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

                StringBuilder errorResponse = new StringBuilder();
                while (scanner.hasNext()) {
                    errorResponse.append(scanner.nextLine());
                }
                scanner.close();
                // Log the error response to see what went wrong
                Log.e("API Error", errorResponse.toString());
                return "Error: " + errorResponse.toString(); // Return the error message to the UI
            }

            StringBuilder response = new StringBuilder();
            while (scanner.hasNext()) {
                response.append(scanner.nextLine());
            }
            scanner.close();

            // Log the entire response for debugging
            Log.d("API Response", response.toString());

            JSONObject responseJSON = new JSONObject(response.toString());
            Log.d("API JSON Response", responseJSON.toString()); // Log the JSON object for debugging

            // String reply = responseJSON.getString("choices");
            /*
            String reply = responseJSON.getJSONArray("choices").getJSONObject(0).getString("text");
            return reply;
            */
            // Handle missing 'text' field more gracefully
            if (responseJSON.has("choices") && responseJSON.getJSONArray("choices").length() > 0) {
                String recipeContent = responseJSON.getJSONArray("choices").getJSONObject(0)
                        .getJSONObject("message").optString("content", "No recipe found.");
                return recipeContent;
            } else {
                return "No recipe found."; // Handle case where "choices" is empty
            } // <-- this code fixed the issue

            // System.out.println("Response from DeepSeek:");
            // System.out.println(response.toString());


        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage(); // <-- return error message
        }
    }
}
