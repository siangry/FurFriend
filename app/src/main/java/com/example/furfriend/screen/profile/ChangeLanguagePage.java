package com.example.furfriend.screen.profile;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.example.furfriend.BaseActivity;
import com.example.furfriend.Constants;
import com.example.furfriend.R;

import java.util.Locale;

public class ChangeLanguagePage extends BaseActivity {

    private RadioGroup languageGroup;
    private RadioButton englishBtn, malayBtn, chineseBtn;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_language_page);

        languageGroup = findViewById(R.id.languageGroup);
        englishBtn = findViewById(R.id.radioEnglish);
        malayBtn = findViewById(R.id.radioMalay);
        chineseBtn = findViewById(R.id.radioChinese);
        btnBack = findViewById(R.id.btnBack);

        String lang = getSharedPreferences("Settings", MODE_PRIVATE)
                .getString("My_Lang", Constants.EN);
        setLocale(lang);

        switch (lang) {
            case Constants.EN:
                englishBtn.setChecked(true);
                break;
            case Constants.MS:
                malayBtn.setChecked(true);
                break;
            case Constants.ZH:
                chineseBtn.setChecked(true);
                break;
        }

        languageGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String selectedLang;
            if (checkedId == R.id.radioEnglish) {
                selectedLang = Constants.EN;
            } else if (checkedId == R.id.radioMalay) {
                selectedLang = Constants.MS;
            } else if (checkedId == R.id.radioChinese) {
                selectedLang = Constants.ZH;
            } else {
                selectedLang = Constants.EN;
            }
            setLocale(selectedLang);
            recreate();
        });

        btnBack.setOnClickListener(v -> {
            getOnBackPressedDispatcher().onBackPressed();
        });
    }

    private void setLocale(String langCode) {
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        String currentLang = prefs.getString("My_Lang", Constants.EN);

        if (!currentLang.equals(langCode)) {
            Locale locale = new Locale(langCode);
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            config.setLocale(locale);
            getResources().updateConfiguration(config, getResources().getDisplayMetrics());

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("My_Lang", langCode);
            editor.apply();
        }
    }

}