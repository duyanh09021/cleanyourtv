package com.gathin.tvhome;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.CycleInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * High-performance, TV-optimized stealth PIN input dialog.
 *
 * Characteristics:
 * 1. Stealth display: Strictly masks input into 6 dots (empty/filled). NO numbers are ever printed on screen.
 * 2. Remote control friendly: Directly intercepts remote numeric keys (0-9) so parents can enter PIN
 *    without moving the on-screen focus cursor.
 * 3. D-Pad Keypad: On-screen 3x4 grid for remotes without numeric buttons.
 * 4. Auto-submit & visual feedback: Verifies immediately upon entering the 6th digit.
 */
public final class PinDialog {

    private static final int PIN_LENGTH = 6;

    private PinDialog() {}

    public static void show(Activity activity, String appLabel, Runnable onAuthorized) {
        if (activity == null || activity.isFinishing()) return;

        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_pin);
        dialog.setCanceledOnTouchOutside(false);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            float density = activity.getResources().getDisplayMetrics().density;
            window.setLayout((int) (400 * density), ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setDimAmount(0.7f);
        }

        TextView subtitle = dialog.findViewById(R.id.pin_subtitle);
        if (subtitle != null) {
            subtitle.setText(activity.getString(R.string.pin_lock_subtitle, appLabel != null ? appLabel : ""));
        }

        final TextView errorView = dialog.findViewById(R.id.pin_error);
        final LinearLayout dotsContainer = dialog.findViewById(R.id.dots_container);
        final ImageView[] dots = new ImageView[PIN_LENGTH];
        dots[0] = dialog.findViewById(R.id.dot_0);
        dots[1] = dialog.findViewById(R.id.dot_1);
        dots[2] = dialog.findViewById(R.id.dot_2);
        dots[3] = dialog.findViewById(R.id.dot_3);
        dots[4] = dialog.findViewById(R.id.dot_4);
        dots[5] = dialog.findViewById(R.id.dot_5);

        final StringBuilder pinBuffer = new StringBuilder();
        final Handler handler = new Handler(Looper.getMainLooper());
        final boolean[] isVerifying = {false};

        Runnable updateDots = () -> {
            int len = pinBuffer.length();
            for (int i = 0; i < PIN_LENGTH; i++) {
                if (dots[i] != null) {
                    dots[i].setImageResource(i < len ? R.drawable.pin_dot_filled : R.drawable.pin_dot_empty);
                }
            }
            if (errorView != null) {
                errorView.setVisibility(View.INVISIBLE);
            }
        };

        Runnable onFailed = () -> {
            for (ImageView dot : dots) {
                if (dot != null) dot.setImageResource(R.drawable.pin_dot_error);
            }
            if (errorView != null) {
                errorView.setVisibility(View.VISIBLE);
            }
            if (dotsContainer != null) {
                TranslateAnimation shake = new TranslateAnimation(0, 10, 0, 0);
                shake.setDuration(350);
                shake.setInterpolator(new CycleInterpolator(3));
                dotsContainer.startAnimation(shake);
            }
            handler.postDelayed(() -> {
                pinBuffer.setLength(0);
                updateDots.run();
                isVerifying[0] = false;
            }, 600);
        };

        Runnable checkAndVerify = () -> {
            if (pinBuffer.length() != PIN_LENGTH) return;
            isVerifying[0] = true;

            if (PinManager.verifyPin(pinBuffer.toString())) {
                for (ImageView dot : dots) {
                    if (dot != null) dot.setImageResource(R.drawable.pin_dot_success);
                }
                handler.postDelayed(() -> {
                    dialog.dismiss();
                    if (onAuthorized != null) onAuthorized.run();
                }, 200);
            } else {
                onFailed.run();
            }
        };

        java.util.function.Consumer<Integer> appendDigit = digit -> {
            if (isVerifying[0] || pinBuffer.length() >= PIN_LENGTH) return;
            pinBuffer.append(digit);
            updateDots.run();
            if (pinBuffer.length() == PIN_LENGTH) {
                checkAndVerify.run();
            }
        };

        Runnable deleteDigit = () -> {
            if (isVerifying[0] || pinBuffer.length() == 0) return;
            pinBuffer.deleteCharAt(pinBuffer.length() - 1);
            updateDots.run();
        };

        // Wire Keypad buttons
        int[] keyIds = {
            R.id.key_0, R.id.key_1, R.id.key_2, R.id.key_3, R.id.key_4,
            R.id.key_5, R.id.key_6, R.id.key_7, R.id.key_8, R.id.key_9
        };
        for (int i = 0; i <= 9; i++) {
            final int digit = i;
            View key = dialog.findViewById(keyIds[i]);
            if (key != null) {
                key.setOnClickListener(v -> appendDigit.accept(digit));
            }
        }

        View keyCancel = dialog.findViewById(R.id.key_cancel);
        if (keyCancel != null) {
            keyCancel.setOnClickListener(v -> dialog.dismiss());
        }

        View keyBackspace = dialog.findViewById(R.id.key_backspace);
        if (keyBackspace != null) {
            keyBackspace.setOnClickListener(v -> deleteDigit.run());
        }

        // Direct interception for remote number keys (0-9), numpad, backspace, and back key
        dialog.setOnKeyListener((d, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;

            if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
                appendDigit.accept(keyCode - KeyEvent.KEYCODE_0);
                return true;
            }
            if (keyCode >= KeyEvent.KEYCODE_NUMPAD_0 && keyCode <= KeyEvent.KEYCODE_NUMPAD_9) {
                appendDigit.accept(keyCode - KeyEvent.KEYCODE_NUMPAD_0);
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DEL) {
                deleteDigit.run();
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                dialog.dismiss();
                return true;
            }
            return false;
        });

        dialog.show();

        // Default initial focus on Key 5 (center of keypad for quick D-pad navigation)
        View initialFocus = dialog.findViewById(R.id.key_5);
        if (initialFocus != null) {
            initialFocus.requestFocus();
        }
    }
}
