package lk.javainstitute.lankarent;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;


public class WelcomeActivity extends AppCompatActivity {
    private ConstraintLayout constraintLayout3;
    private ConstraintLayout constraintLayout2;
    private SpringAnimation springAnimation;
    private ValueAnimator loopAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_welcome);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.tenant_fragment_container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        hideSystemUI();

        constraintLayout3 = findViewById(R.id.constraintLayout3);
        constraintLayout2 = findViewById(R.id.constraintLayout2);

        Button tenantButton = findViewById(R.id.tenantButton);
        Button landlordButton = findViewById(R.id.landlordButton);

        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        landlordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                animateLayoutWithSpring(constraintLayout3, true); // Expand constraintLayout3
                animateLayoutWithSpring(constraintLayout2, false); // Contract constraintLayout2

                editor.putString("userRole", "landlord");
                editor.apply();

                cancelAnimations();

                // Cancel animations
                if (loopAnimator != null) {
                    loopAnimator.cancel();
                }
                if (springAnimation != null) {
                    springAnimation.cancel();
                }

                Intent intent = new Intent(WelcomeActivity.this, SignupActivity.class);
                startActivity(intent);
            }
        });

        tenantButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                animateLayoutWithSpring(constraintLayout2, true); // Expand constraintLayout3
                animateLayoutWithSpring(constraintLayout3, false); // Contract constraintLayout2

                editor.putString("userRole", "tenant");
                editor.apply();

                cancelAnimations();

                // Cancel animations
                if (loopAnimator != null) {
                    loopAnimator.cancel();
                }
                if (springAnimation != null) {
                    springAnimation.cancel();
                }

                Intent intent = new Intent(WelcomeActivity.this, SignupActivity.class);
                startActivity(intent);
            }
        });
    }


    private void animateLayoutWithSpring(ConstraintLayout layout, boolean isExpanding) {
        // Define the target scale and alpha values
        float targetScale = isExpanding ? 1.2f : 1f;
        float targetAlpha = isExpanding ? 0.8f : 1f;

        // Create a SpringAnimation for scaleX and scaleY
        SpringAnimation scaleXAnimation = new SpringAnimation(layout, SpringAnimation.SCALE_X);
        SpringAnimation scaleYAnimation = new SpringAnimation(layout, SpringAnimation.SCALE_Y);

        // Configure the spring force for smoothness
        SpringForce springForceX = new SpringForce(targetScale);
        springForceX.setStiffness(SpringForce.STIFFNESS_LOW); // Adjust stiffness for smoothness
        springForceX.setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY); // No bouncing

        SpringForce springForceY = new SpringForce(targetScale);
        springForceY.setStiffness(SpringForce.STIFFNESS_LOW);
        springForceY.setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY);

        scaleXAnimation.setSpring(springForceX);
        scaleYAnimation.setSpring(springForceY);

        // Start the scale animations
        scaleXAnimation.start();
        scaleYAnimation.start();

        // Create a SpringAnimation for alpha
        SpringAnimation alphaAnimation = new SpringAnimation(layout, SpringAnimation.ALPHA);
        SpringForce alphaSpringForce = new SpringForce(targetAlpha);
        alphaSpringForce.setStiffness(SpringForce.STIFFNESS_LOW);
        alphaSpringForce.setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY);

        alphaAnimation.setSpring(alphaSpringForce);
        alphaAnimation.start();
    }

    private void cancelAnimations() {
        if (loopAnimator != null) {
            loopAnimator.cancel();
        }
        if (springAnimation != null) {
            springAnimation.cancel();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        ImageView keyImage = findViewById(R.id.keyImage);

        springAnimation = new SpringAnimation(keyImage, DynamicAnimation.TRANSLATION_X);
        SpringForce springForce = new SpringForce();
        springForce.setStiffness(SpringForce.STIFFNESS_LOW);
        springForce.setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);
        springForce.setFinalPosition(-100f);

        springAnimation.setSpring(springForce);

        // Create a loop animator to repeat the spring animation
        loopAnimator = ValueAnimator.ofFloat(0, 1);
        loopAnimator.setDuration(1000);
        loopAnimator.setRepeatMode(ValueAnimator.REVERSE);
        loopAnimator.setRepeatCount(ValueAnimator.INFINITE);
        loopAnimator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            keyImage.setTranslationX(value * 100f - 50f); // Adjust the value as needed
        });

        // Start the loop animator
        loopAnimator.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Cancel the animations when the activity is paused to avoid memory leaks
        if (loopAnimator != null) {
            loopAnimator.cancel();
        }
        if (springAnimation != null) {
            springAnimation.cancel();
        }
    }

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }
}
