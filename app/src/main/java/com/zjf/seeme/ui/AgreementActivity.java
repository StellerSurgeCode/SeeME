package com.zjf.seeme.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.zjf.seeme.R;
import com.zjf.seeme.data.PrefsManager;
import com.zjf.seeme.util.ThemeHelper;

public class AgreementActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);

        boolean viewOnly = getIntent().getBooleanExtra("view_only", false);
        PrefsManager prefs = PrefsManager.getInstance(this);

        if (!viewOnly && !prefs.isFirstLaunch()) {
            navigateToMain();
            return;
        }

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_agreement);

        TextView tvAgreement = findViewById(R.id.tvAgreement);
        MaterialCheckBox cbAgree = findViewById(R.id.cbAgree);
        MaterialButton btnAgree = findViewById(R.id.btnAgree);
        MaterialButton btnDisagree = findViewById(R.id.btnDisagree);

        tvAgreement.setText(getAgreementText());

        if (viewOnly) {
            cbAgree.setVisibility(View.GONE);
            btnAgree.setVisibility(View.GONE);
            btnDisagree.setText("返回");
            btnDisagree.setOnClickListener(v -> finish());
        } else {
            cbAgree.setOnCheckedChangeListener((buttonView, isChecked) -> {
                btnAgree.setEnabled(isChecked);
            });

            btnAgree.setOnClickListener(v -> {
                prefs.setFirstLaunch(false);
                prefs.setAgreementSignedTimestamp(System.currentTimeMillis());
                navigateToMain();
            });

            btnDisagree.setOnClickListener(v -> finishAffinity());
        }
    }

    private void navigateToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private String getAgreementText() {
        return "SeeME 用户服务协议与隐私政策\n\n"
                + "生效日期：2026年2月21日\n"
                + "版本：1.0\n\n"
                + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n"
                + "一、总则\n\n"
                + "1.1 本协议是您（以下简称用户）与 SeeME 应用（以下简称本应用）开发者之间关于使用本应用服务所订立的电子协议。本协议具有法律效力，请您仔细阅读。\n\n"
                + "1.2 您点击同意并继续按钮即表示您已充分阅读、理解并接受本协议的全部内容。如您不同意本协议的任何条款，请勿使用本应用。\n\n"
                + "1.3 本协议自用户点击同意之时起生效，在用户使用本应用服务期间持续有效。\n\n"
                + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n"
                + "二、服务说明\n\n"
                + "2.1 本应用提供手机通知消息的智能聚合、分类、重要性评估及管理服务。\n\n"
                + "2.2 本应用需要以下系统权限才能正常运行：\n"
                + "  (a) 通知监听权限 — 用于读取、管理和分类您的手机通知消息；\n"
                + "  (b) 后台运行权限 — 用于保持通知监听服务持续运行；\n"
                + "  (c) 网络访问权限 — 用于调用 AI 接口对消息进行智能分类；\n"
                + "  (d) 通知发送权限 — 用于向您推送消息汇总通知。\n\n"
                + "2.3 本应用使用第三方 AI 服务（DeepSeek）对通知内容进行分类分析。通知内容将以加密方式传输至 AI 服务接口进行处理。\n\n"
                + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n"
                + "三、隐私政策\n\n"
                + "3.1 数据收集范围\n"
                + "本应用仅收集以下通知信息：\n"
                + "  · 通知标题\n"
                + "  · 通知内容文本\n"
                + "  · 通知时间\n"
                + "  · 来源应用名称\n"
                + "  · 发送者名称（如有）\n\n"
                + "3.2 数据存储\n"
                + "所有通知数据均存储在用户设备本地数据库中，不会上传至任何服务器（AI 分类请求除外）。用户可随时在应用内删除所有数据。\n\n"
                + "3.3 数据传输\n"
                + "为实现智能分类功能，通知的标题和内容将通过加密连接（HTTPS）发送至 DeepSeek AI 接口进行分析。我们不会将您的数据用于任何其他目的。\n\n"
                + "3.4 数据安全\n"
                + "本应用采取合理的技术措施保护您的数据安全，包括但不限于本地加密存储、网络传输加密等。\n\n"
                + "3.5 用户权利\n"
                + "您有权随时：\n"
                + "  · 查看本应用收集的所有通知数据；\n"
                + "  · 删除任何或全部通知数据；\n"
                + "  · 撤销通知监听权限以停止数据收集；\n"
                + "  · 卸载本应用以删除所有本地数据。\n\n"
                + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n"
                + "四、用户义务\n\n"
                + "4.1 用户应合法使用本应用，不得利用本应用从事任何违反法律法规的活动。\n\n"
                + "4.2 用户理解并同意，本应用的消息分类和重要性评估由 AI 自动完成，可能存在误差，用户应自行判断消息的实际重要性。\n\n"
                + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n"
                + "五、免责声明\n\n"
                + "5.1 本应用按现状提供服务，不对服务的及时性、准确性、完整性作任何明示或暗示的保证。\n\n"
                + "5.2 因 AI 分类不准确导致用户错过重要消息的，开发者不承担任何责任。\n\n"
                + "5.3 因网络故障、设备故障、系统限制等不可抗力因素导致服务中断的，开发者不承担任何责任。\n\n"
                + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n"
                + "六、协议变更\n\n"
                + "6.1 开发者有权根据需要修改本协议内容，修改后的协议将在应用内公布。\n\n"
                + "6.2 如您继续使用本应用，即视为您接受修改后的协议。\n\n"
                + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n"
                + "七、其他\n\n"
                + "7.1 本协议的订立、执行和解释及争议的解决均适用中华人民共和国法律。\n\n"
                + "7.2 如本协议中的任何条款被认定为无效或不可执行，不影响其余条款的效力。\n\n"
                + "7.3 本协议自用户点击同意并继续时生效，具有电子合同的法律效力。根据《中华人民共和国民法典》第四百六十九条及《中华人民共和国电子签名法》相关规定，本电子协议与书面协议具有同等法律效力。\n\n"
                + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n"
                + "开发者：占俊凡\n"
                + "联系邮箱：zjf20110511@qq.com\n"
                + "签署时间：以用户点击同意时的系统时间为准\n";
    }
}
