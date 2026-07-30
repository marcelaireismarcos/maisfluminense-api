package maisfluminense.vikkynsnorth.noticias.util;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import maisfluminense.vikkynsnorth.noticias.R;

public class MessageManager {
    public static void showSuccess(Context context, String message) {
        showToastSucesso(context, "✅ " + message, String.valueOf(R.color.success_green));
    }

    public static void showError(Context context, String message) {
        showToastErro(context, "❌ " + message, String.valueOf(R.color.error_red));
    }

    public static void showInfo(Context context, String message) {
        showToastInfo(context, "ℹ️ " + message, String.valueOf(R.color.info_blue));
    }

    public static void showWarning(Context context, String message) {
        showToastWarning(context, "⚠️ " + message, String.valueOf(R.color.warning_yellow));
    }

    private static void showToastSucesso(Context context, String message, String backgroundColor) {
        LayoutInflater inflater = LayoutInflater.from(context);
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        // Inflating the layout to show the custom toast
        View layout = inflater.inflate(R.layout.messagem_sucesso, null);

        // setting the string to the textView runtime.
        LinearLayout linear_layout = layout.findViewById(R.id.message_toast_container);;
        TextView text = layout.findViewById(R.id.text);

        text.setText(message);

        toast = new Toast(context);
        //setting the position of the toast message
        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
        // lenght of the toast message

        toast.setDuration(Toast.LENGTH_SHORT);

        // assigning the custom layout view to toast message
        toast.setView(layout);

        // showing the toast message using show method
        toast.show();
    }
    private static void showToastErro(Context context, String message, String backgroundColor) {
        LayoutInflater inflater = LayoutInflater.from(context);
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        // Inflating the layout to show the custom toast
        View layout = inflater.inflate(R.layout.messagem_erro, null);

        // setting the string to the textView runtime.
        LinearLayout linear_layout = layout.findViewById(R.id.message_toast_container);;
        TextView text = layout.findViewById(R.id.text);

        text.setText(message);

        toast = new Toast(context);
        //setting the position of the toast message
        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
        // lenght of the toast message

        toast.setDuration(Toast.LENGTH_SHORT);

        // assigning the custom layout view to toast message
        toast.setView(layout);

        // showing the toast message using show method
        toast.show();
    }
    private static void showToastInfo(Context context, String message, String backgroundColor) {
        LayoutInflater inflater = LayoutInflater.from(context);
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        // Inflating the layout to show the custom toast
        View layout = inflater.inflate(R.layout.messagem_info, null);

        // setting the string to the textView runtime.
        LinearLayout linear_layout = layout.findViewById(R.id.message_toast_container);;
        TextView text = layout.findViewById(R.id.text);

        text.setText(message);

        toast = new Toast(context);
        //setting the position of the toast message
        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
        // lenght of the toast message

        toast.setDuration(Toast.LENGTH_SHORT);

        // assigning the custom layout view to toast message
        toast.setView(layout);

        // showing the toast message using show method
        toast.show();
    }
    private static void showToastWarning(Context context, String message, String backgroundColor) {
        LayoutInflater inflater = LayoutInflater.from(context);
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        // Inflating the layout to show the custom toast
        View layout = inflater.inflate(R.layout.messagem_warning, null);

        // setting the string to the textView runtime.
        LinearLayout linear_layout = layout.findViewById(R.id.message_toast_container);;
        TextView text = layout.findViewById(R.id.text);

        text.setText(message);

        toast = new Toast(context);
        //setting the position of the toast message
        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
        // lenght of the toast message

        toast.setDuration(Toast.LENGTH_SHORT);

        // assigning the custom layout view to toast message
        toast.setView(layout);

        // showing the toast message using show method
        toast.show();
    }
}