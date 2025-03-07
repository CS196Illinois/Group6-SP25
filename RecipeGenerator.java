import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class RecipeGenerator {
    public static void main(String[] args) {
        String apiKey = "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"; 
        String ingredients = "chicken, rice, garlic";

        try {
            URL url = new URL("https://api.openai.com/v1/chat/completions");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String requestBody = "{\n" +
                    "  \"model\": \"gpt-3.5-turbo\",\n" +
                    "  \"messages\": [\n" +
                    "    {\"role\": \"system\", \"content\": \"You are a helpful chef.\"},\n" +
                    "    {\"role\": \"user\", \"content\": \"Give me a recipe using these ingredients: " + ingredients + "\"}\n" +
                    "  ],\n" +
                    "  \"temperature\": 0.7\n" +
                    "}";

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

            System.out.println("Response from OpenAI:");
            System.out.println(response.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
