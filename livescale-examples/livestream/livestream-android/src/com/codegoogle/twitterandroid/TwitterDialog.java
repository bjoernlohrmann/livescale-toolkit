/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codegoogle.twitterandroid;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Display;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.tuberlin.cit.livestream.android.R;



public final class TwitterDialog extends Dialog {
  //
  // This code is heavily borrowed from
  // http://abhinavasblog.blogspot.com/2011/06/for-all-my-code-thirsty-friends-twitter.html
  //
  private static final float[] DIMENSIONS_LANDSCAPE = { 460, 260 };
  private static final float[] DIMENSIONS_PORTRAIT = { 280, 420 };
  private static final FrameLayout.LayoutParams FILL = new FrameLayout.LayoutParams(
      ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
  private static final int MARGIN = 4;
  private static final int PADDING = 2;

  private final String url;
  private final TwitterAuthListener listener;
  private ProgressDialog spinner;
  private WebView webView;
  private TextView title;
  private boolean progressDialogRunning = false;

  public TwitterDialog(Context context, String url, TwitterAuthListener listener) {
    super(context);

    this.url = url;
    this.listener = listener;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    spinner = new ProgressDialog(getContext());

    spinner.requestWindowFeature(Window.FEATURE_NO_TITLE);
    spinner.setMessage("Loading...");

    LinearLayout layout = new LinearLayout(getContext());

    layout.setOrientation(LinearLayout.VERTICAL);

    setUpTitle(layout);
    setUpWebView(layout);

    Display display = getWindow().getWindowManager().getDefaultDisplay();
    float scale = getContext().getResources().getDisplayMetrics().density;
    float[] dimensions =
        (display.getWidth() < display.getHeight()) ? DIMENSIONS_PORTRAIT : DIMENSIONS_LANDSCAPE;

    addContentView(layout, new FrameLayout.LayoutParams((int) (dimensions[0] * scale + 0.5f),
        (int) (dimensions[1] * scale + 0.5f)));
  }

  private void setUpTitle(LinearLayout layout) {
    requestWindowFeature(Window.FEATURE_NO_TITLE);

    Drawable icon = getContext().getResources().getDrawable(R.drawable.twitter_icon);

    title = new TextView(getContext());

    title.setText("Twitter");
    title.setTextColor(Color.WHITE);
    title.setTypeface(Typeface.DEFAULT_BOLD);
    title.setBackgroundColor(0xFFbbd7e9);
    title.setPadding(MARGIN + PADDING, MARGIN, MARGIN, MARGIN);
    title.setCompoundDrawablePadding(MARGIN + PADDING);
    title.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);

    layout.addView(title);
  }

  private void setUpWebView(LinearLayout layout) {
    webView = new WebView(getContext());

    webView.setVerticalScrollBarEnabled(false);
    webView.setHorizontalScrollBarEnabled(false);
    webView.setWebViewClient(new TwitterWebViewClient());
    webView.getSettings().setJavaScriptEnabled(true);
    webView.loadUrl(url);
    webView.setLayoutParams(FILL);

    layout.addView(webView);
  }

  private class TwitterWebViewClient extends WebViewClient {

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
      if (url.startsWith(TwitterApp.CALLBACK_URL)) {
        listener.onComplete(url);

        TwitterDialog.this.dismiss();

        return true;
      } else if (url.startsWith("authorize")) {
        return false;
      }
      return true;
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
      super.onReceivedError(view, errorCode, description, failingUrl);
      listener.onError(description);
      TwitterDialog.this.dismiss();
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
      super.onPageStarted(view, url, favicon);
      spinner.show();
      progressDialogRunning = true;
    }

    @Override
    public void onPageFinished(WebView view, String url) {
      super.onPageFinished(view, url);
      String titleText = webView.getTitle();
      if (titleText != null && titleText.length() > 0) {
        title.setText(titleText);
      }
      progressDialogRunning = false;
      spinner.dismiss();
    }
  }

  @Override
  protected void onStop() {
    progressDialogRunning = false;
    super.onStop();
  }

  public void onBackPressed() {
    if (!progressDialogRunning) {
      TwitterDialog.this.dismiss();
    }
  }
}
