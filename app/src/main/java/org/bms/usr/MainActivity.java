package org.bms.usr;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private boolean backPressed = false;
    private static final long BACK_PRESS_INTERVAL = 2000;
    private final Handler handler = new Handler();
    private final Runnable resetBackPressed = () -> backPressed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button buttonSearch = findViewById(R.id.buttonSearch);
        Button buttonSettings = findViewById(R.id.buttonSettings);

        buttonSearch.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, WiFiSearchActivity.class)));
        buttonSettings.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, WiFiSettingsActivity.class)));

        // 1. Отримуємо OnBackPressedDispatcher
        OnBackPressedDispatcher dispatcher = getOnBackPressedDispatcher();

        // 2. Створюємо новий OnBackPressedCallback для обробки подій "назад"
        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled */) {
            @Override
            public void handleOnBackPressed() {
                if (backPressed) {
                    // Другий жест "назад" протягом 2 секунд
                    handler.removeCallbacks(resetBackPressed);
                    finishAffinity();
                    return;
                }
                backPressed = true;
                // Показуємо повідомлення лише на першому натисканні
                Toast.makeText(MainActivity.this, R.string.press_again_to_exit, Toast.LENGTH_SHORT).show();
                handler.postDelayed(resetBackPressed, BACK_PRESS_INTERVAL);
            }
        };

        // 3. Додаємо callback до dispatcher. 'this' вказує, що callback діє, поки активність активна.
        dispatcher.addCallback(this, callback);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuHelper.prepareMenu(menu, this.getClass());
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_search) {
            startActivity(new Intent(this, WiFiSearchActivity.class));
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, WiFiSettingsActivity.class));
            return true;
        } else if (id == R.id.action_about) {
            showAboutDialog();
            return true;
        } else if (id == R.id.action_exit) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: Activity is about to become visible.");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Activity is now interactive.");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: Activity is in background, but still visible.");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: Activity is no longer visible.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Activity is being destroyed.");
    }


    private void showAboutDialog() {
        String aboutMessage = getString(R.string.about_dialog_message) + "\n\n" +
                getString(R.string.github_url);
        SpannableString spannableString = new SpannableString(aboutMessage);
        String githubUrl = getString(R.string.github_url);
        int startIndex = aboutMessage.indexOf(githubUrl);
        int endIndex = startIndex + githubUrl.length();

        if (startIndex >= 0) {
            spannableString.setSpan(new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl));
                    startActivity(browserIntent);
                }
            }, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        TextView messageTextView = new TextView(this);
        messageTextView.setText(spannableString);
        messageTextView.setMovementMethod(LinkMovementMethod.getInstance()); // This is crucial for making the link clickable

        // Convert 16dp to pixels
        int paddingInDp = 16;
        int paddingInPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, paddingInDp, getResources().getDisplayMetrics());
        // Set padding for TextView (left, top, right, bottom)
        messageTextView.setPadding(paddingInPx, paddingInPx, paddingInPx, paddingInPx);

        new AlertDialog.Builder(this)
                .setTitle(R.string.about_dialog_title)
                .setView(messageTextView)
                .setPositiveButton(R.string.button_ok, null)
                .show();
    }
}