package app.openrouter.radar;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class Api {
    public static final String URL = "https://openrouter.ai/api/v1/models";
    public static String fetchActivity(String modelId) {
        for (int attempt = 0; attempt < 2; attempt++) {
            String r = fetchActivityOnce(modelId);
            if (r != null) return r;
        }
        return null;
    }
    static String fetchActivityOnce(String modelId) {
        try {
            String enc = android.net.Uri.encode(modelId, "");
            HttpURLConnection c = (HttpURLConnection) new URL("https://openrouter.ai/api/frontend/v1/stats/model-activity?permaslug=" + enc + "&variant=standard").openConnection();
            c.setConnectTimeout(8000); c.setReadTimeout(12000);
            c.setInstanceFollowRedirects(true);
            c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36");
            c.setRequestProperty("Accept", "application/json, text/plain, */*");
            c.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            c.setRequestProperty("Referer", "https://openrouter.ai/" + modelId);
            c.setRequestProperty("sec-fetch-site", "same-origin");
            c.setRequestProperty("sec-fetch-mode", "cors");
            if (c.getResponseCode() / 100 != 2) { c.disconnect(); return null; }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"))) {
                String line; while ((line = r.readLine()) != null) sb.append(line);
            }
            return sb.toString();
        } catch (Exception e) { return null; }
    }

    public static String fetchEndpoints(String modelId) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL("https://openrouter.ai/api/v1/models/" + modelId + "/endpoints").openConnection();
            c.setConnectTimeout(8000); c.setReadTimeout(12000);
            c.setRequestProperty("User-Agent", "ModelRadar/1.0");
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"))) {
                String line; while ((line = r.readLine()) != null) sb.append(line);
            }
            return sb.toString();
        } catch (Exception e) { return null; }
    }

    public static List<Model> fetch() throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(URL).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        c.setRequestProperty("User-Agent", "ModelRadar/1.0");
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"))) {
            String line; while ((line = r.readLine()) != null) sb.append(line);
        }
        return Model.parse(sb.toString());
    }
}
