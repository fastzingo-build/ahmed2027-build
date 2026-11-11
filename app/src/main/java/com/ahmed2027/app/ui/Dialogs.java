package com.ahmed2027.app.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.InputType;
import android.widget.EditText;

import com.ahmed2027.app.R;

/** Small reusable dialogs. */
public final class Dialogs {
    public interface Text { void on(String text); }
    public interface Index { void on(int index); }

    private Dialogs() {}

    public static void prompt(Activity a, int titleRes, int hintRes, boolean multiline, boolean number,
                              String initial, final Text cb) {
        final EditText et = new EditText(a);
        et.setHint(hintRes);
        if (number) et.setInputType(InputType.TYPE_CLASS_NUMBER);
        else et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                | (multiline ? InputType.TYPE_TEXT_FLAG_MULTI_LINE : 0));
        if (!multiline) et.setSingleLine(true);
        else et.setMinLines(3);
        if (initial != null) et.setText(initial);
        new AlertDialog.Builder(a).setTitle(titleRes).setView(et)
                .setPositiveButton(R.string.action_save, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        String t = et.getText().toString().trim();
                        if (!t.isEmpty()) cb.on(t);
                    }
                }).setNegativeButton(R.string.action_cancel, null).show();
    }

    public static void choose(Activity a, CharSequence title, String[] labels, final Index cb) {
        new AlertDialog.Builder(a).setTitle(title).setItems(labels, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface d, int which) { cb.on(which); }
        }).setNegativeButton(R.string.action_cancel, null).show();
    }

    public static void message(Activity a, int titleRes, CharSequence msg) {
        new AlertDialog.Builder(a).setTitle(titleRes).setMessage(msg).setPositiveButton(R.string.action_done, null).show();
    }

    /** Countdown dialog; the guidance line changes every lineEverySec seconds. Cancels its timer on dismiss. */
    public static void timer(Activity a, int titleRes, String[] lines, int totalSec, int lineEverySec, Runnable onFinish) {
        timer(a, titleRes, lines, totalSec, lineEverySec, onFinish, null);
    }

    /** onClose runs whenever the dialog goes away (finished or cancelled). */
    public static void timer(final Activity a, final int titleRes, final String[] lines, final int totalSec,
                             final int lineEverySec, final Runnable onFinish, final Runnable onClose) {
        final AlertDialog dlg = new AlertDialog.Builder(a).setTitle(titleRes)
                .setMessage(lines[0] + "\n\n" + fmt(totalSec))
                .setNegativeButton(R.string.action_cancel, null).create();
        final android.os.CountDownTimer t = new android.os.CountDownTimer(totalSec * 1000L, 1000) {
            @Override public void onTick(long ms) {
                int left = (int) ((ms + 999) / 1000);
                int idx = Math.min(lines.length - 1, (totalSec - left) / Math.max(1, lineEverySec));
                dlg.setMessage(lines[idx] + "\n\n" + fmt(left));
            }
            @Override public void onFinish() {
                dlg.setMessage(a.getString(R.string.timer_done));
                if (onFinish != null) onFinish.run();
            }
        };
        dlg.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override public void onDismiss(DialogInterface d) { t.cancel(); if (onClose != null) onClose.run(); }
        });
        dlg.show();
        t.start();
    }

    public static String fmt(int sec) { return String.format(java.util.Locale.US, "%02d:%02d", sec / 60, sec % 60); }

    /** One step at a time, with Back / Next; onDone runs after the last step. */
    public static void steps(final Activity a, final int titleRes, final String[] steps, final Runnable onDone) {
        showStep(a, titleRes, steps, 0, onDone);
    }

    private static void showStep(final Activity a, final int titleRes, final String[] steps, final int i, final Runnable onDone) {
        boolean last = i == steps.length - 1;
        AlertDialog.Builder b = new AlertDialog.Builder(a).setTitle(a.getString(titleRes) + "  " + (i + 1) + "/" + steps.length)
                .setMessage(steps[i]).setCancelable(true)
                .setPositiveButton(last ? R.string.action_done : R.string.ob_next, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        if (last) { if (onDone != null) onDone.run(); } else showStep(a, titleRes, steps, i + 1, onDone);
                    }
                });
        if (i > 0) b.setNeutralButton(R.string.ob_back, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface d, int w) { showStep(a, titleRes, steps, i - 1, onDone); }
        });
        b.setNegativeButton(R.string.action_cancel, null).show();
    }
}
