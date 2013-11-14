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

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;

import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.Window;

public final class TwitterApp {
  //
  // This code is heavily based on
  // http://abhinavasblog.blogspot.com/2011/06/for-all-my-code-thirsty-friends-twitter.html
  //
  private final Context context;
  private final Twitter twitter;
  private final TwitterSession session;
  private final CommonsHttpOAuthConsumer httpOauthConsumer;
  private final OAuthProvider httpOauthprovider;
  private final String consumerKey;
  private final String secretKey;
  private final ProgressDialog progressDialog;
  private TwitterAuthListener listener;
  private AccessToken accessToken;

  public static final String CALLBACK_URL = "twitterapp://connect";
  private static final String TWITTER_ACCESS_TOKEN_URL =
      "https://api.twitter.com/oauth/access_token";
  private static final String TWITTER_AUTHORZE_URL = "https://api.twitter.com/oauth/authorize";
  private static final String TWITTER_REQUEST_URL = "https://api.twitter.com/oauth/request_token";

  public TwitterApp(Context context, String consumerKey, String secretKey) {
    this.context = context;
    this.consumerKey = consumerKey;
    this.secretKey = secretKey;

    twitter = new TwitterFactory().getInstance();
    session = new TwitterSession(context);
    progressDialog = new ProgressDialog(context);
    progressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

    httpOauthConsumer = new CommonsHttpOAuthConsumer(consumerKey, secretKey);

    String request_url = TWITTER_REQUEST_URL;
    String access_token_url = TWITTER_ACCESS_TOKEN_URL;
    String authorize_url = TWITTER_AUTHORZE_URL;

    httpOauthprovider = new DefaultOAuthProvider(request_url, access_token_url, authorize_url);
    accessToken = session.getAccessToken();

    configureToken();
  }

  public void setListener(TwitterAuthListener listener) {
    this.listener = listener;
  }

  private void configureToken() {
    if (accessToken != null) {
      twitter.setOAuthConsumer(consumerKey, secretKey);
      twitter.setOAuthAccessToken(accessToken);
    }
  }

  public boolean hasAccessToken() {
    return (accessToken == null) ? false : true;
  }

  public void resetAccessToken() {
    if (accessToken != null) {
      session.resetAccessToken();

      accessToken = null;
    }
  }

  public String getUsername() {
    return session.getUsername();
  }

  public void updateStatus(String status) throws Exception {
    try {
      twitter.updateStatus(status);
    } catch (TwitterException e) {
      throw e;
    }
  }

  public void authorize() {
    progressDialog.setMessage("Initializing ...");
    progressDialog.show();

    new Thread() {
      @Override
      public void run() {
        String authUrl = "";
        int what = 1;

        try {
          authUrl = httpOauthprovider.retrieveRequestToken(httpOauthConsumer, CALLBACK_URL);
          what = 0;
        } catch (Exception e) {
          e.printStackTrace();
        }
        handler.sendMessage(handler.obtainMessage(what, 1, 0, authUrl));
      }
    }.start();
  }

  public void processToken(String callbackUrl) {
    progressDialog.setMessage("Finalizing ...");
    progressDialog.show();

    final String verifier = getVerifier(callbackUrl);

    new Thread() {
      @Override
      public void run() {
        int what = 1;

        try {
          httpOauthprovider.retrieveAccessToken(httpOauthConsumer, verifier);

          accessToken =
              new AccessToken(httpOauthConsumer.getToken(), httpOauthConsumer.getTokenSecret());

          configureToken();

          User user = twitter.verifyCredentials();

          session.storeAccessToken(accessToken, user.getName());

          what = 0;
        } catch (Exception e) {
          e.printStackTrace();
        }

        handler.sendMessage(handler.obtainMessage(what, 2, 0));
      }
    }.start();
  }

  private String getVerifier(String callbackUrl) {
    String verifier = "";

    try {
      callbackUrl = callbackUrl.replace("twitterapp", "http");

      URL url = new URL(callbackUrl);
      String query = url.getQuery();

      String array[] = query.split("&");

      for (String parameter : array) {
        String v[] = parameter.split("=");

        if (URLDecoder.decode(v[0]).equals(oauth.signpost.OAuth.OAUTH_VERIFIER)) {
          verifier = URLDecoder.decode(v[1]);
          break;
        }
      }
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }

    return verifier;
  }

  private void showLoginDialog(String url) {
    final TwitterAuthListener listener = new TwitterAuthListener() {
      @Override
      public void onComplete(String value) {
        processToken(value);
      }
      @Override
      public void onError(String value) {
        TwitterApp.this.listener.onError("Failed opening authorization page");
      }
    };

    new TwitterDialog(context, url, listener).show();
  }

  private Handler handler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      progressDialog.dismiss();

      if (msg.what == 1) {
        if (msg.arg1 == 1)
          listener.onError("Error getting request token");
        else
          listener.onError("Error getting access token");
      } else {
        if (msg.arg1 == 1)
          showLoginDialog((String) msg.obj);
        else
          listener.onComplete("");
      }
    }
  };
}