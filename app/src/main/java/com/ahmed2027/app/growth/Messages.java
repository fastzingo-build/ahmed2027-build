package com.ahmed2027.app.growth;

import android.content.Context;

import com.ahmed2027.app.R;

import java.util.Random;

/** Non-shaming, varied encouragement lines (40+ per language in the string-array). */
public final class Messages {
    private static final Random R = new Random();
    private Messages() {}

    public static String pick(Context c) {
        String[] all = c.getResources().getStringArray(R.array.gentle_messages);
        return all[R.nextInt(all.length)];
    }
}
