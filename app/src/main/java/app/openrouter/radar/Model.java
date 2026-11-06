package app.openrouter.radar;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class Model {
    public String id = "";
    public String name = "";
    public String desc = "";
    public long created = 0;
    public long ctx = 0;
    public String modality = "";
    public double promptPrice = -1;
    public double completionPrice = -1;
    public double cachePrice = -1;
    public double discount = 0;

    public boolean isFree() { return promptPrice == 0 && completionPrice == 0; }

    // model-activity: 最近7天 prompt+completion tokens 总和; 无数据返回-1
    public static long weeklyTokens(String json) {
        try {
            org.json.JSONArray a = new org.json.JSONObject(json).getJSONObject("data").getJSONArray("analytics");
            int n = Math.min(7, a.length());
            long sum = 0;
            for (int i = a.length() - n; i < a.length(); i++) {
                org.json.JSONObject d = a.getJSONObject(i);
                sum += d.optLong("total_prompt_tokens", 0) + d.optLong("total_completion_tokens", 0);
            }
            return sum;
        } catch (Exception e) { return -1; }
    }
    public static String tokText(long v) {
        if (v < 0) return "—";
        if (v >= 1000000000L) return String.format("%.1fB", v / 1e9);
        if (v >= 1000000L) return String.format("%.0fM", v / 1e6);
        if (v >= 1000L) return String.format("%.0fK", v / 1e3);
        return String.valueOf(v);
    }

    // 从 /endpoints 提取最快供应商: {throughput tok/s, latency ms}; 无数据返回null
    public static double[] speedOf(String json) {
        try {
            org.json.JSONArray eps = new org.json.JSONObject(json).getJSONObject("data").getJSONArray("endpoints");
            double bestThr = -1, bestLat = -1;
            for (int i = 0; i < eps.length(); i++) {
                org.json.JSONObject e = eps.getJSONObject(i);
                double thr = e.optDouble("throughput_last_30m", -1);
                double lat = e.optDouble("latency_last_30m", -1);
                if (thr > bestThr) bestThr = thr;
                if (lat > 0 && (bestLat < 0 || lat < bestLat)) bestLat = lat;
            }
            if (bestThr <= 0 && bestLat <= 0) return null;
            return new double[]{bestThr, bestLat};
        } catch (Exception e) { return null; }
    }
    public String priceText() {
        if (isFree()) return "免费";
        if (promptPrice < 0) return "—";
        return String.format("$%.2f / $%.2f /1M", promptPrice * 1e6, completionPrice * 1e6);
    }
    public String dateText() {
        if (created <= 0) return "";
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(created * 1000L);
        return String.format("%04d-%02d-%02d", c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH)+1, c.get(java.util.Calendar.DAY_OF_MONTH));
    }
    public String score() {
        int s = 0;
        if (isFree()) s += 40;
        s += Math.min(30, (int)(created / 86400 % 30));
        s += Math.min(30, (int)(ctx / 200000 * 10));
        return String.valueOf(s);
    }

    public static List<Model> parse(String json) throws Exception {
        List<Model> out = new ArrayList<>();
        JSONObject root = new JSONObject(json);
        JSONArray arr = root.getJSONArray("data");
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            Model m = new Model();
            m.id = o.optString("id", "");
            m.name = o.optString("name", m.id);
            m.desc = o.optString("description", "");
            m.created = o.optLong("created", 0);
            m.ctx = o.optLong("context_length", 0);
            JSONObject arch = o.optJSONObject("architecture");
            if (arch != null) m.modality = arch.optString("modality", "");
            JSONObject pricing = o.optJSONObject("pricing");
            if (pricing != null) {
                m.promptPrice = priced(pricing.optString("prompt", "-1"));
                m.completionPrice = priced(pricing.optString("completion", "-1"));
                m.cachePrice = priced(pricing.optString("input_cache_read", "-1"));
            }
            if (!m.id.isEmpty()) out.add(m);
        }
        return out;
    }
    static double priced(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return -1; }
    }
}
