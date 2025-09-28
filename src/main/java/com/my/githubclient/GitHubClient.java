package com.my.githubclient;

import android.util.Base64;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.io.ByteArrayOutputStream;

/**
 * مكتبة مطورة للتعامل مع GitHub API (قراءة/تعديل/إنشاء/حذف ملفات)
 */
public class GitHubClient {

    private String token;
    private String owner;
    private String repo;
    private String branch = "main";

    public GitHubClient(String token, String owner, String repo) {
        this.token = token;
        this.owner = owner;
        this.repo = repo;
    }

    /** تغيير الفرع */
    public void setBranch(String branch) {
        this.branch = branch;
    }

/** رفع ملف (صورة / PDF / أي نوع) من InputStream */
public String uploadFile(String path, InputStream fileStream, String message) throws Exception {
    // قراءة الملف كـ byte[]
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    byte[] data = new byte[1024];
    int nRead;
    while ((nRead = fileStream.read(data, 0, data.length)) != -1) {
        buffer.write(data, 0, nRead);
    }
    buffer.flush();
    byte[] fileBytes = buffer.toByteArray();
    fileStream.close();

    // تحويل Base64
    String base64Content = Base64.encodeToString(fileBytes, Base64.NO_WRAP);

    // تجهيز JSON body
    JSONObject body = new JSONObject();
    body.put("message", message);
    body.put("content", base64Content);
    body.put("branch", branch);

    // URL API
    String url = "https://api.github.com/repos/" + owner + "/" + repo + "/contents/" + path;

    // PUT request لرفع الملف
    String response = httpRequest("PUT", url, body.toString());

    JSONObject json = new JSONObject(response);
    JSONObject contentObj = json.getJSONObject("content");
    return contentObj.getString("download_url"); // الرابط المباشر للملف
}


    /** جلب SHA لملف */
    public String getFileSha(String path) throws Exception {
        String url = "https://api.github.com/repos/" + owner + "/" + repo + "/contents/" + path + "?ref=" + branch;
        String response = httpRequest("GET", url, null);

        JSONObject json = new JSONObject(response);
        return json.getString("sha");
    }

    /** جلب محتوى ملف كنص */
    public String getFileContent(String path) throws Exception {
        String url = "https://api.github.com/repos/" + owner + "/" + repo + "/contents/" + path + "?ref=" + branch;
        String response = httpRequest("GET", url, null);

        JSONObject json = new JSONObject(response);
        String base64Content = json.getString("content");
        byte[] decoded = Base64.decode(base64Content, Base64.DEFAULT);
        return new String(decoded, "UTF-8");
    }

    /** تعديل ملف موجود وإرجاع محتواه الجديد */
    public String updateFile(String path, String newContent, String sha, String message) throws Exception {
        String url = "https://api.github.com/repos/" + owner + "/" + repo + "/contents/" + path;

        JSONObject body = new JSONObject();
        body.put("message", message);
        body.put("content", Base64.encodeToString(newContent.getBytes("UTF-8"), Base64.NO_WRAP));
        body.put("sha", sha);
        body.put("branch", branch);

        // نعمل PUT للتعديل
        String response = httpRequest("PUT", url, body.toString());

        // GitHub بيرجع JSON فيه معلومات الملف الجديد
        JSONObject json = new JSONObject(response);
        JSONObject contentObj = json.getJSONObject("content");

        // ممكن نجيب download_url لو حابب تستخدمه
        String downloadUrl = contentObj.getString("download_url");

        // وأخيراً نجيب المحتوى الجديد
        return getFileContent(path);
    }

    /** إنشاء ملف جديد */
    public String createFile(String path, String content, String message) throws Exception {
        String url = "https://api.github.com/repos/" + owner + "/" + repo + "/contents/" + path;

        JSONObject body = new JSONObject();
        body.put("message", message);
        body.put("content", Base64.encodeToString(content.getBytes("UTF-8"), Base64.NO_WRAP));
        body.put("branch", branch);

        String response = httpRequest("PUT", url, body.toString());
        // GitHub بيرجع JSON فيه معلومات الملف الجديد
        JSONObject json = new JSONObject(response);
        JSONObject contentObj = json.getJSONObject("content");

        // ممكن نجيب download_url لو حابب تستخدمه
        String downloadUrl = contentObj.getString("download_url");

        // وأخيراً نجيب المحتوى الجديد
        return getFileContent(path);        
    }

    /** حذف ملف */
    public String deleteFile(String path, String sha, String message) throws Exception {
        String url = "https://api.github.com/repos/" + owner + "/" + repo + "/contents/" + path;

        JSONObject body = new JSONObject();
        body.put("message", message);
        body.put("sha", sha);
        body.put("branch", branch);

        return httpRequest("DELETE", url, body.toString());
    }

    /** طلب HTTP داخلي */
    private String httpRequest(String method, String urlString, String body) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        setDefaultHeaders(conn);
        conn.setDoInput(true);

        if (body != null) {
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes("UTF-8"));
            }
        }

        int responseCode = conn.getResponseCode();
        InputStream inputStream = (responseCode < 400) ? conn.getInputStream() : conn.getErrorStream();

        StringBuilder sb = new StringBuilder();
        try (Scanner in = new Scanner(inputStream)) {
            while (in.hasNext()) {
                sb.append(in.nextLine());
            }
        }

        if (responseCode >= 400) {
            throw new Exception("HTTP Error " + responseCode + ": " + sb.toString());
        }

        return sb.toString();
    }

    /** تهيئة الهيدرز الافتراضية */
    private void setDefaultHeaders(HttpURLConnection conn) {
        conn.setRequestProperty("Authorization", "token " + token);
        conn.setRequestProperty("Accept", "application/vnd.github+json");
    }
      }
