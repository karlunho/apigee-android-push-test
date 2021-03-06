package com.ganyo.pushtest;

import android.content.Context;

import android.util.Log;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.usergrid.android.client.Client;
import org.usergrid.android.client.callbacks.ApiResponseCallback;
import org.usergrid.android.client.callbacks.DeviceRegistrationCallback;
import org.usergrid.java.client.entities.Device;
import org.usergrid.java.client.entities.Entity;
import org.usergrid.java.client.response.ApiResponse;
import org.usergrid.java.client.utils.JsonUtils;

import java.util.HashMap;
import java.util.Map;

import static com.ganyo.pushtest.Util.*;
import static com.ganyo.pushtest.Settings.*;

public final class AppServices {

  private static Client client;
  private static Device device;

  static synchronized Client getClient() {
    if (client == null) {
      client = new Client();
      client.setApiUrl(API_URL);
      client.setOrganizationId(ORG);
      client.setApplicationId(APP);
    }
    return client;
  }

  static void login(final Context context) {

    if (USER != null) {
      Client client = AppServices.getClient();
      client.authorizeAppUserAsync(USER, PASSWORD, new ApiResponseCallback() {

        @Override
        public void onResponse(ApiResponse apiResponse) {
          Log.i(TAG, "authorize response: " + apiResponse);
          registerPush(context);
        }

        @Override
        public void onException(Exception e) {
          Log.i(TAG, "authorize exception: " + e);
        }
      });
    } else {
      registerPush(context);
    }
  }

  /**
   * Register this user/device pair on App Services.
   */
  static void register(final Context context, final String regId) {
    Log.i(TAG, "registering device: " + regId);

    Map<String, Object> properties = new HashMap<String, Object>();
    String notifierKey = NOTIFIER + ".notifier.id";
    properties.put(notifierKey, regId);
    getClient().registerDeviceAsync(context, properties, new DeviceRegistrationCallback() {

      @Override
      public void onResponse(Device device) {
        Log.i(TAG, "register response: " + device);
        AppServices.device = device;

        // optionally connect device to current User
        if (getClient().getLoggedInUser() != null) {
          getClient().connectEntitiesAsync("users", getClient().getLoggedInUser().getUuid().toString(),
                                           "devices", device.getUuid().toString(),
                                           new ApiResponseCallback() {
            @Override
            public void onResponse(ApiResponse apiResponse) {
              Log.i(TAG, "connect response: " + apiResponse);
            }

            @Override
            public void onException(Exception e) {
              Log.i(TAG, "connect exception: " + e);
            }
          });
        }
      }

      @Override
      public void onException(Exception e) {
        Log.i(TAG, "register exception: " + e);
      }

      @Override
      public void onDeviceRegistration(Device device) { /* this won't be called */}
    });
  }

  static void sendMyselfANotification() {
    if (device != null) {
      String entityPath = "devices/" + device.getUuid().toString() + "/notifications";
      Entity notification = new Entity(entityPath);

      HashMap<String,String> payloads = new HashMap<String, String>();
      payloads.put("google", "Hi there!");
      notification.setProperty("payloads", JsonUtils.toJsonNode(payloads));
      getClient().createEntityAsync(notification, new ApiResponseCallback() {

        @Override
        public void onResponse(ApiResponse apiResponse) {
          Log.i(TAG, "send response: " + apiResponse);
        }

        @Override
        public void onException(Exception e) {
          Log.i(TAG, "send exception: " + e);
        }
      });
    }
  }

  /**
   * Unregister this device within the server.
   */
  static void unregister(final Context context, final String regId) {
    Log.i(TAG, "unregistering device: " + regId);
    register(context, "");
  }
}
