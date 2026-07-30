package maisfluminense.vikkynsnorth.noticias.util;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import maisfluminense.vikkynsnorth.noticias.R;

/**
 * Centralized Toast utility — replaces duplicated toast code across all Activities and Fragments.
 */
public class ToastUtils {

    public static void showSuccess(Context context, String message) {
        show(context, message, R.color.cor_fundo_message_ok, R.drawable.ic_ok, Toast.LENGTH_SHORT);
    }

    public static void showError(Context context, String message) {
        show(context, message, R.color.cor_fundo_message_erro, R.drawable.ic_erro, Toast.LENGTH_LONG);
    }

    public static void showSuccessLong(Context context, String message) {
        show(context, message, R.color.cor_fundo_message_ok, R.drawable.ic_ok, Toast.LENGTH_LONG);
    }

    private static void show(Context context, String message, int bgColor, int icon, int duration) {
        try {
            LayoutInflater inflater = LayoutInflater.from(context);
            View layout = inflater.inflate(R.layout.messagem_custom, null);

            LinearLayout container = layout.findViewById(R.id.message_toast_container);
            TextView text = layout.findViewById(R.id.text);
            ImageView image = layout.findViewById(R.id.image);

            container.setBackground(ContextCompat.getDrawable(context, bgColor));
            image.setImageDrawable(ContextCompat.getDrawable(context, icon));
            text.setText(message);

            Toast toast = new Toast(context);
            toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
            toast.setDuration(duration);
            toast.setView(layout);
            toast.show();
        } catch (Exception ignored) {
            // Fallback to system toast if custom layout fails
            Toast.makeText(context, message, duration).show();
        }
    }
}
