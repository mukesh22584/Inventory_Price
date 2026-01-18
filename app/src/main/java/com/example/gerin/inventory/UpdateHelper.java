package com.example.gerin.inventory;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

public class UpdateHelper {

    private static final String REPO_URL = "https://api.github.com/repos/mukesh22584/Inventory_Price/releases/latest";
    private final Context context;

    public UpdateHelper(Context context) {
        this.context = context;
    }

    public void checkForUpdate() {
        new Thread(() -> {
            try {
                URL url = new URL(REPO_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json");

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    String latestVersionTag = jsonResponse.getString("tag_name");
                    String downloadUrl = jsonResponse.getString("html_url"); 

                    checkVersion(latestVersionTag, downloadUrl);
                }
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void checkVersion(String latestVersionTag, String downloadUrl) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            String currentVersion = pInfo.versionName;

            String cleanLatest = latestVersionTag.replaceAll("[^\\d.]", "");
            String cleanCurrent = currentVersion.replaceAll("[^\\d.]", "");

            if (isNewerVersion(cleanCurrent, cleanLatest)) {
                new Handler(Looper.getMainLooper()).post(() -> showUpdateDialog(downloadUrl, latestVersionTag));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isNewerVersion(String current, String latest) {
        String[] currentParts = current.split("\\.");
        String[] latestParts = latest.split("\\.");
        int length = Math.max(currentParts.length, latestParts.length);

        for (int i = 0; i < length; i++) {
            int curr = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
            int lat = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;

            if (lat > curr) return true;
            if (curr > lat) return false;
        }
        return false;
    }

    private void showUpdateDialog(String downloadUrl, String version) {
        new AlertDialog.Builder(context)
                .setTitle("Update Available")
                .setMessage("A new version (" + version + ") is available on GitHub.")
                .setPositiveButton("Download", (dialog, which) -> {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl));
                    context.startActivity(browserIntent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
