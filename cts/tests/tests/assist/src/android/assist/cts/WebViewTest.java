/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.assist.cts;

import android.assist.common.Utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.util.Log;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 *  Test that the AssistStructure returned is properly formatted.
 */

public class WebViewTest extends AssistTestBase {
    private static final String TAG = "WebViewTest";
    private static final String TEST_CASE_TYPE = Utils.WEBVIEW;

    private boolean mWebViewSupported;
    private BroadcastReceiver mReceiver;
    private CountDownLatch mHasResumedLatch = new CountDownLatch(1);
    private CountDownLatch mTestWebViewLatch = new CountDownLatch(1);
    private CountDownLatch mReadyLatch = new CountDownLatch(1);

    public WebViewTest() {
        super();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setUpAndRegisterReceiver();
        startTestActivity(TEST_CASE_TYPE);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        if (mReceiver != null) {
            mContext.unregisterReceiver(mReceiver);
            mReceiver = null;
        }
    }

    private void setUpAndRegisterReceiver() {
        if (mReceiver != null) {
            mContext.unregisterReceiver(mReceiver);
        }
        mReceiver = new WebViewTestBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Utils.APP_3P_HASRESUMED);
        filter.addAction(Utils.ASSIST_RECEIVER_REGISTERED);
        filter.addAction(Utils.TEST_ACTIVITY_LOADED);
        mContext.registerReceiver(mReceiver, filter);
    }

    private void waitForOnResume() throws Exception {
        Log.i(TAG, "waiting for onResume() before continuing");
        if (!mHasResumedLatch.await(Utils.ACTIVITY_ONRESUME_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
            fail("Activity failed to resume in " + Utils.ACTIVITY_ONRESUME_TIMEOUT_MS + "msec");
        }
    }

    private void waitForTestActivity() throws Exception {
        Log.i(TAG, "waiting for webview in test activity to load");
        if (!mTestWebViewLatch.await(Utils.ACTIVITY_ONRESUME_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
            // wait for webView to load completely.
        }
    }

    public void testWebView() throws Exception {
        if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WEBVIEW)) {
            return;
        }
        mTestActivity.start3pApp(TEST_CASE_TYPE);
        mTestActivity.startTest(TEST_CASE_TYPE);
        waitForAssistantToBeReady(mReadyLatch);
        waitForOnResume();
        waitForTestActivity();
        startSession();
        waitForContext();
        verifyAssistDataNullness(false, false, false, false);
        verifyAssistStructure(Utils.getTestAppComponent(TEST_CASE_TYPE),
                false /*FLAG_SECURE set*/);
    }

    private class WebViewTestBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Utils.APP_3P_HASRESUMED) && mHasResumedLatch != null) {
                mHasResumedLatch.countDown();
            } else if (action.equals(Utils.ASSIST_RECEIVER_REGISTERED) && mReadyLatch != null) {
                mReadyLatch.countDown();
            } else if (action.equals(Utils.TEST_ACTIVITY_LOADED) && mTestWebViewLatch != null) {
                mTestWebViewLatch.countDown();
            }
        }
    }
}