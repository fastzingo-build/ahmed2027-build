package com.ahmed2027.app.ui;

import android.view.View;

/** What screens may ask of MainActivity. */
public interface Host {
    void goToTab(int tab);
    void startExport();
    void startImport();
    void finishOnboarding();
    /** Shows a sub-screen on top of the current tab; system Back (or pop()) returns. */
    void push(View screen);
    void pop();
}
