package com.zjf.seeme.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.zjf.seeme.R;
import com.zjf.seeme.data.PrefsManager;
import com.zjf.seeme.util.ThemeHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        TextView tvVersion = findViewById(R.id.tvVersion);
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            tvVersion.setText("v" + pInfo.versionName);
        } catch (Exception ignored) {}

        TextView tvEmail = findViewById(R.id.tvEmail);
        tvEmail.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("email", "zjf20110511@qq.com");
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "邮箱已复制到剪贴板", Toast.LENGTH_SHORT).show();
        });

        PrefsManager prefs = PrefsManager.getInstance(this);
        long signedTs = prefs.getAgreementSignedTimestamp();
        TextView tvAgreementStatus = findViewById(R.id.tvAgreementStatus);
        if (signedTs > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss", Locale.getDefault());
            tvAgreementStatus.setText("协议签署状态：已同意\n签署时间：" + sdf.format(new Date(signedTs)));
        } else {
            tvAgreementStatus.setText("协议签署状态：未签署");
        }

        MaterialButton btnViewAgreement = findViewById(R.id.btnViewAgreement);
        btnViewAgreement.setOnClickListener(v -> {
            Intent intent = new Intent(this, AgreementActivity.class);
            intent.putExtra("view_only", true);
            startActivity(intent);
        });
    }
}
