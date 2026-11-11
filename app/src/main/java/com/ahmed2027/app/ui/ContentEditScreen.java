package com.ahmed2027.app.ui;

import android.app.Activity;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.ahmed2027.app.R;
import com.ahmed2027.app.core.Items;

import java.util.List;

/** Script (hook / body / call to action), caption and hashtags for one content item. Stored in items.body, parts split by "\n---\n". */
public final class ContentEditScreen extends ScreenBase {
    private static final String SEP = "\n---\n";
    private final EditText[] f = new EditText[5];

    public ContentEditScreen(Activity a, Host host, final long id) {
        super(a, host, R.string.creator_edit);
        final Items.Item it = Items.get(a, id);
        String[] parts = it == null ? new String[0] : it.body.split("\n---\n", -1);
        int[] labels = {R.string.cs_hook, R.string.cs_body, R.string.cs_cta, R.string.cs_caption, R.string.cs_hashtags};
        int[] hints = {R.string.cs_hook_hint, R.string.cs_body_hint, R.string.cs_cta_hint, R.string.cs_caption_hint, R.string.cs_hashtags_hint};
        if (it != null) col.addView(text(it.title, 18, R.color.text_primary), wrap(4));
        for (int i = 0; i < 5; i++) {
            col.addView(new SectionTitle(a, a.getString(labels[i])));
            f[i] = new EditText(a);
            f[i].setHint(hints[i]);
            f[i].setHintTextColor(a.getColor(R.color.text_secondary));
            f[i].setTextColor(a.getColor(R.color.text_primary));
            f[i].setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            f[i].setMinLines(i == 1 ? 4 : 2);
            f[i].setBackground(Ui.rounded(a, a.getColor(R.color.card), 16));
            f[i].setPadding(Ui.dp(a, 16), Ui.dp(a, 12), Ui.dp(a, 16), Ui.dp(a, 12));
            if (i < parts.length) f[i].setText(parts[i]);
            col.addView(f[i], wrap(0));
        }
        col.addView(button(R.string.cs_insert_tags, false, new View.OnClickListener() {
            @Override public void onClick(View v) { insertTags(); } }), wrap(12));
        col.addView(button(R.string.action_save, true, new View.OnClickListener() {
            @Override public void onClick(View v) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 5; i++) { if (i > 0) sb.append(SEP); sb.append(f[i].getText().toString().trim().replace("\n---\n", "\n--\n")); }
                Items.setBody(ContentEditScreen.this.a, id, sb.toString());
                Toast.makeText(ContentEditScreen.this.a, R.string.reflect_saved, Toast.LENGTH_SHORT).show();
                ContentEditScreen.this.host.pop();
            } }), wrap(8));
    }

    private void insertTags() {
        final List<Items.Item> sets = Items.list(a, "hashtag_set", null, 30);
        if (sets.isEmpty()) {
            Dialogs.prompt(a, R.string.cs_new_set, R.string.cs_set_name, false, false, null, new Dialogs.Text() {
                @Override public void on(final String name) {
                    Dialogs.prompt(a, R.string.cs_new_set, R.string.cs_hashtags_hint, true, false, f[4].getText().toString(), new Dialogs.Text() {
                        @Override public void on(String tags) { Items.addFull(a, "hashtag_set", name, tags, "saved", 0, null); Toast.makeText(a, R.string.reflect_saved, Toast.LENGTH_SHORT).show(); } });
                } });
            return;
        }
        String[] labels = new String[sets.size() + 1];
        for (int i = 0; i < sets.size(); i++) labels[i] = sets.get(i).title;
        labels[sets.size()] = a.getString(R.string.cs_new_set);
        Dialogs.choose(a, a.getString(R.string.cs_insert_tags), labels, new Dialogs.Index() {
            @Override public void on(int i) {
                if (i < sets.size()) { String cur = f[4].getText().toString().trim(); f[4].setText((cur.isEmpty() ? "" : cur + " ") + sets.get(i).body); }
                else Dialogs.prompt(a, R.string.cs_new_set, R.string.cs_set_name, false, false, null, new Dialogs.Text() {
                    @Override public void on(final String name) {
                        Items.addFull(a, "hashtag_set", name, f[4].getText().toString().trim(), "saved", 0, null);
                        Toast.makeText(a, R.string.reflect_saved, Toast.LENGTH_SHORT).show(); } });
            } });
    }
}
