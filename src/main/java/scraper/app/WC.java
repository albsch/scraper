package scraper.app;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class WC {

    static Map<String, Integer> counts = new HashMap<>();

    public static void main(String[] args) throws Exception {
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 100000; i++) {
            String file = "bible.txt";
            StringBuilder fileContent = new StringBuilder();
            Files.readAllLines(Paths.get(file)).forEach(fileContent::append);
            consumeLine(fileContent.toString());
        }

        long estimatedTime = System.currentTimeMillis() - startTime;
        System.out.println(counts.get("god"));
        System.out.println(estimatedTime);
    }

    private static void consumeLine(String line) {
        StringTokenizer tokenizer = new StringTokenizer(line, " ");
        try {
            while(true) {
                String token = tokenizer.nextToken();
                counts.merge(token, 1, Integer::sum);
            }
        } catch (Exception e) {}

    }
}
