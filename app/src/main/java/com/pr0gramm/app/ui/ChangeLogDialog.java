package com.pr0gramm.app.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.webkit.WebView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.pr0gramm.app.R;
import com.pr0gramm.app.Settings;

import de.cketti.library.changelog.ChangeLog;

/**
 */
public class ChangeLogDialog extends DialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ChangeLog changeLog = new ChangeLog(getActivity());
        String htmlSource = changeLog.getFullLog();

        WebView webView = new WebView(getActivity());
        webView.loadDataWithBaseURL(null, htmlSource, "text/html", "UTF-8", null);

        Settings settings = Settings.of(getActivity());

        return new MaterialDialog.Builder(getActivity())
                .customView(webView, false)
                .title(R.string.changelog_title)
                .positiveText(R.string.okay)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        changeLog.skipLogDialog();

                        if (settings.useBetaChannel()) {
                            showFeedbackReminderDialog();
                        }

                    }
                })
                .build();
    }

    private void showFeedbackReminderDialog() {
        new MaterialDialog.Builder(getActivity())
                .content(R.string.feedback_reminder)
                .positiveText(R.string.okay)
                .show();
    }
}
