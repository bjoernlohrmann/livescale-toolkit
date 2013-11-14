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

import twitter4j.auth.AccessToken;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public final class TwitterSession {
  //
  // This code is heavily borrowed from
  // http://abhinavasblog.blogspot.com/2011/06/for-all-my-code-thirsty-friends-twitter.html
  //

  private static final String TWEET_AUTH_KEY = "auth_key";
  private static final String TWEET_AUTH_SECRET_KEY = "auth_secret_key";
  private static final String TWEET_USER_NAME = "user_name";
  private static final String SHARED = "Twitter_Preferences";

  private final SharedPreferences pref;
  private final Editor editor;

  public TwitterSession(Context context) {
    pref = context.getSharedPreferences(SHARED, Context.MODE_PRIVATE);
    editor = pref.edit();
  }

  public void storeAccessToken(AccessToken accessToken, String username) {
    editor.putString(TWEET_AUTH_KEY, accessToken.getToken());
    editor.putString(TWEET_AUTH_SECRET_KEY, accessToken.getTokenSecret());
    editor.putString(TWEET_USER_NAME, username);
    editor.commit();
  }

  public void resetAccessToken() {
    editor.putString(TWEET_AUTH_KEY, null);
    editor.putString(TWEET_AUTH_SECRET_KEY, null);
    editor.putString(TWEET_USER_NAME, null);
    editor.commit();
  }

  public String getUsername() {
    return pref.getString(TWEET_USER_NAME, "");
  }

  public AccessToken getAccessToken() {
    String token = pref.getString(TWEET_AUTH_KEY, null);
    String tokenSecret = pref.getString(TWEET_AUTH_SECRET_KEY, null);

    if (token != null && tokenSecret != null) {
      return new AccessToken(token, tokenSecret);
    } else {
      return null;
    }
  }
}