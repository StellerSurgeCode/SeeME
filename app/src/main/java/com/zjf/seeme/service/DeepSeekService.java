package com.zjf.seeme.service;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zjf.seeme.data.PrefsManager;
import com.zjf.seeme.data.entity.KeywordRule;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DeepSeekService {

    private static final String TAG = "DeepSeekService";
    private static final String API_URL = "https://api.deepseek.com/chat/completions";
    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final String apiKey;
    private final String customPrompt;
    private List<KeywordRule> keywordRules;

    public static class ClassificationResult {
        public String category;
        public int importance;
        public String reason;
        public boolean isTodo;

        public ClassificationResult(String category, int importance, String reason) {
            this.category = category;
            this.importance = importance;
            this.reason = reason;
            this.isTodo = false;
        }

        public ClassificationResult(String category, int importance, String reason, boolean isTodo) {
            this.category = category;
            this.importance = importance;
            this.reason = reason;
            this.isTodo = isTodo;
        }
    }

    public DeepSeekService(PrefsManager prefs) {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.apiKey = prefs.getApiKey();
        this.customPrompt = prefs.getCustomPrompt();
    }

    public void setKeywordRules(List<KeywordRule> rules) {
        this.keywordRules = rules;
    }

    public ClassificationResult classifyMessage(String appName, String title, String content,
                                                 String sender, long timestamp) {
        return classifyMessage(appName, title, content, sender, timestamp, null);
    }

    public ClassificationResult classifyMessage(String appName, String title, String content,
                                                 String sender, long timestamp,
                                                 List<String> recentContext) {
        String systemPrompt = buildSystemPrompt();
        String userMessage = buildUserMessage(appName, title, content, sender, timestamp, recentContext);

        try {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", "deepseek-chat");
            requestBody.addProperty("temperature", 0.3);
            requestBody.addProperty("max_tokens", 500);

            JsonArray messages = new JsonArray();

            JsonObject sysMsg = new JsonObject();
            sysMsg.addProperty("role", "system");
            sysMsg.addProperty("content", systemPrompt);
            messages.add(sysMsg);

            JsonObject userMsg = new JsonObject();
            userMsg.addProperty("role", "user");
            userMsg.addProperty("content", userMessage);
            messages.add(userMsg);

            requestBody.add("messages", messages);

            RequestBody body = RequestBody.create(requestBody.toString(), JSON_TYPE);
            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "API call failed: " + response.code());
                    return fallbackClassification(appName, title, content);
                }

                String responseBody = response.body() != null ? response.body().string() : "";
                ClassificationResult result = parseResponse(responseBody);

                result.importance = applyKeywordBoost(result.importance, appName, title, content, sender);
                return result;
            }
        } catch (IOException e) {
            Log.e(TAG, "Network error", e);
            return fallbackClassification(appName, title, content);
        } catch (Exception e) {
            Log.e(TAG, "Classification error", e);
            return fallbackClassification(appName, title, content);
        }
    }

    private String buildSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个手机通知消息分类助手。你需要对收到的通知消息进行分类和重要性评估。\n\n");
        sb.append("分类规则：\n");
        sb.append("1. 工作 - 与工作、办公、会议、项目、同事沟通、客户相关的消息\n");
        sb.append("2. 生活 - 与日常生活、家庭、健康、出行、快递、外卖、银行、缴费相关的消息\n");
        sb.append("3. 娱乐 - 与游戏、视频、音乐、社交媒体娱乐内容相关的消息\n");
        sb.append("4. 广告推销 - 商家促销、优惠券、营销推广、垃圾信息、拼团砍价\n");
        sb.append("5. 综合 - 无法明确归类到以上类别的消息\n");
        sb.append("如果消息不属于以上任何类别，请创建一个新的合适的类别名称（简短2-4字）。\n\n");

        sb.append("重要性评分规则（1-10分）：\n");
        sb.append("- 10分：紧急且重要，需要立即处理（如紧急工作消息、安全警报、验证码）\n");
        sb.append("- 8-9分：重要消息（如重要人物消息、工作通知、账户异常）\n");
        sb.append("- 6-7分：较重要（如普通工作消息、快递到达、重要APP通知）\n");
        sb.append("- 4-5分：一般消息（如普通社交消息、新闻推送）\n");
        sb.append("- 2-3分：不太重要（如娱乐通知、系统更新提示）\n");
        sb.append("- 1分：不重要（如广告推销、垃圾信息）\n\n");

        if (keywordRules != null && !keywordRules.isEmpty()) {
            sb.append("【重要】用户设定的关键词规则（必须遵守）：\n");
            for (KeywordRule rule : keywordRules) {
                sb.append("- 当消息的");
                switch (rule.getMatchField()) {
                    case "title": sb.append("标题"); break;
                    case "content": sb.append("内容"); break;
                    case "sender": sb.append("发送者名称"); break;
                    case "app": sb.append("来源应用"); break;
                    default: sb.append("标题或内容"); break;
                }
                sb.append("中包含「").append(rule.getKeyword()).append("」时，");
                if (rule.getAiHint() != null && !rule.getAiHint().isEmpty()) {
                    sb.append(rule.getAiHint());
                }
                if (rule.getImportanceBoost() > 0) {
                    sb.append("，重要性评分应提高").append(rule.getImportanceBoost()).append("分");
                } else if (rule.getImportanceBoost() < 0) {
                    sb.append("，重要性评分应降低").append(Math.abs(rule.getImportanceBoost())).append("分");
                }
                sb.append("\n");
            }
            sb.append("\n");
        }

        if (customPrompt != null && !customPrompt.isEmpty()) {
            sb.append("【重要】用户的额外自定义规则：\n");
            sb.append(customPrompt);
            sb.append("\n\n");
        }

        sb.append("待办识别规则（务必严格遵守）：\n");
        sb.append("只有当消息中明确包含以下要素之一时，才标记 is_todo 为 true：\n");
        sb.append("1. 有明确的行动要求（如「开会」「提交」「回复」「缴费」「还款」「签到」「取件」等动词）\n");
        sb.append("2. 有明确的时间约定（如「下午3点」「明天」「周五前」「等会儿」等时间词）\n");
        sb.append("3. 有明确的截止日期或deadline\n");
        sb.append("以下情况不应标记为待办：\n");
        sb.append("- 普通聊天、闲聊、问候（如「在吗」「吃了吗」「哈哈」）\n");
        sb.append("- 纯信息通知、新闻推送、广告\n");
        sb.append("- 没有具体行动要求的消息（如「今天天气不错」）\n");
        sb.append("- 系统通知、应用更新提示\n");
        sb.append("请结合上下文综合判断，只有真正需要用户采取行动的消息才标记为待办。\n\n");

        sb.append("请严格按照以下JSON格式回复，不要包含其他内容：\n");
        sb.append("{\"category\": \"分类名称\", \"importance\": 数字, \"reason\": \"简短原因\", \"is_todo\": true或false}");

        return sb.toString();
    }

    private String buildUserMessage(String appName, String title, String content,
                                     String sender, long timestamp,
                                     List<String> recentContext) {
        StringBuilder sb = new StringBuilder();

        if (recentContext != null && !recentContext.isEmpty()) {
            sb.append("=== 该应用/联系人近期通知历史（从旧到新，用于上下文参考）===\n");
            for (String ctx : recentContext) {
                sb.append(ctx).append("\n");
            }
            sb.append("=== 历史结束 ===\n\n");
            sb.append("请结合以上历史上下文，对下面这条最新通知进行分类：\n\n");
        }

        sb.append("来源应用: ").append(appName != null ? appName : "未知").append("\n");

        if (sender != null && !sender.isEmpty()) {
            sb.append("发送者: ").append(sender).append("\n");
        }

        sb.append("通知标题: ").append(title != null ? title : "无标题").append("\n");
        sb.append("通知内容: ").append(content != null ? content : "无内容").append("\n");

        if (timestamp > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            sb.append("通知时间: ").append(sdf.format(new Date(timestamp)));
        }

        return sb.toString();
    }

    private int applyKeywordBoost(int baseImportance, String appName, String title,
                                   String content, String sender) {
        if (keywordRules == null || keywordRules.isEmpty()) return baseImportance;

        int boost = 0;
        for (KeywordRule rule : keywordRules) {
            if (!rule.isEnabled()) continue;

            String keyword = rule.getKeyword().toLowerCase();
            boolean matched = false;

            switch (rule.getMatchField()) {
                case "title":
                    matched = title != null && title.toLowerCase().contains(keyword);
                    break;
                case "content":
                    matched = content != null && content.toLowerCase().contains(keyword);
                    break;
                case "sender":
                    matched = sender != null && sender.toLowerCase().contains(keyword);
                    break;
                case "app":
                    matched = appName != null && appName.toLowerCase().contains(keyword);
                    break;
                default:
                    matched = (title != null && title.toLowerCase().contains(keyword))
                            || (content != null && content.toLowerCase().contains(keyword));
                    break;
            }

            if (matched) {
                boost += rule.getImportanceBoost();
            }
        }

        return Math.max(1, Math.min(10, baseImportance + boost));
    }

    private ClassificationResult fallbackClassification(String appName, String title, String content) {
        String combined = ((title != null ? title : "") + " " + (content != null ? content : "")).toLowerCase();

        if (combined.contains("优惠") || combined.contains("促销") || combined.contains("折扣")
                || combined.contains("红包") || combined.contains("砍价") || combined.contains("拼团")) {
            return new ClassificationResult("广告推销", 1, "本地关键词匹配");
        }
        if (combined.contains("会议") || combined.contains("工作") || combined.contains("项目")
                || combined.contains("审批") || combined.contains("打卡")) {
            return new ClassificationResult("工作", 7, "本地关键词匹配");
        }
        if (combined.contains("快递") || combined.contains("外卖") || combined.contains("到达")
                || combined.contains("缴费") || combined.contains("银行")) {
            return new ClassificationResult("生活", 6, "本地关键词匹配");
        }

        return new ClassificationResult("综合", 5, "AI不可用，默认分类");
    }

    private ClassificationResult parseResponse(String responseBody) {
        try {
            JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray choices = root.getAsJsonArray("choices");
            if (choices != null && choices.size() > 0) {
                String aiContent = choices.get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content").getAsString().trim();

                String jsonStr = aiContent;
                if (aiContent.contains("```")) {
                    int start = aiContent.indexOf("{");
                    int end = aiContent.lastIndexOf("}");
                    if (start >= 0 && end > start) {
                        jsonStr = aiContent.substring(start, end + 1);
                    }
                } else if (!aiContent.startsWith("{")) {
                    int start = aiContent.indexOf("{");
                    int end = aiContent.lastIndexOf("}");
                    if (start >= 0 && end > start) {
                        jsonStr = aiContent.substring(start, end + 1);
                    }
                }

                JsonObject result = JsonParser.parseString(jsonStr).getAsJsonObject();
                String category = result.has("category") ? result.get("category").getAsString() : "综合";
                int importance = result.has("importance") ? result.get("importance").getAsInt() : 5;
                String reason = result.has("reason") ? result.get("reason").getAsString() : "";
                boolean isTodo = result.has("is_todo") && result.get("is_todo").getAsBoolean();

                importance = Math.max(1, Math.min(10, importance));
                return new ClassificationResult(category, importance, reason, isTodo);
            }
        } catch (Exception e) {
            Log.e(TAG, "Parse response error", e);
        }
        return new ClassificationResult("综合", 5, "解析失败");
    }
}
