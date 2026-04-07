package lk.javainstitute.lankarent;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.tenant_fragment_container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;

        });
        checkUserStatus();

    }

    private void checkUserStatus() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        boolean isFirstLaunch = sharedPreferences.getBoolean("isFirstLaunch", true);
        boolean isSignedUp = sharedPreferences.getBoolean("isSignedUp", false);
        String userId = sharedPreferences.getString("userId", null);
        String userRole = sharedPreferences.getString("userRole", null);

        if (isFirstLaunch) {
            // First time launching the app
            startActivity(new Intent(this, WelcomeActivity.class));
            sharedPreferences.edit().putBoolean("isFirstLaunch", false).apply();
        } else if (!isSignedUp) {
            // User selected role but didn't sign up
            startActivity(new Intent(this, WelcomeActivity.class));
        } else if (userId != null && userRole != null) {
            // User is signed in, redirect based on role
            if (userRole.equals("landlord")) {
                startActivity(new Intent(this, LandlordDashboardActivity.class));
            } else {
                startActivity(new Intent(this, TenantDashboardActivity.class));
            }
        } else {
            // User is not signed in, go to Login screen
            startActivity(new Intent(this, LoginActivity.class));
        }

        finish(); // Close MainActivity
    }
}
