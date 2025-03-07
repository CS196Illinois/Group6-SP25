import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class LlamaRecipeGenerator {
    public static void main(String[] args) {
        String apiKey = "4138c36b-8184-4838-ac51-4b9a0afd7507";
        String ingredients = "chicken, rice, garlic";

        try {
            // Replace with your official LLaMA endpoint
            URL url = new URL("https://api.meta.ai/v1/llama/generate");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String prompt = "Give me a recipe using these ingredients: " + ingredients;

            String requestBody = "{\n" +
                    "  \"prompt\": \"" + prompt + "\",\n" +
                    "  \"temperature\": 0.7,\n" +
                    "  \"max_tokens\": 512\n" +
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

            System.out.println("Response from LLaMA API:");
            System.out.println(response.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
