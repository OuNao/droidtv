/******************************************************************************
 *  DroidTV, live TV on Android devices with host USB port and a DVB tuner    *
 *  Copyright (C) 2012  Christian Ulrich <chrulri@gmail.com>                  *
 *                                                                            *
 *  This program is free software: you can redistribute it and/or modify      *
 *  it under the terms of the GNU General Public License as published by      *
 *  the Free Software Foundation, either version 3 of the License, or         *
 *  (at your option) any later version.                                       *
 *                                                                            *
 *  This program is distributed in the hope that it will be useful,           *
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of            *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             *
 *  GNU General Public License for more details.                              *
 *                                                                            *
 *  You should have received a copy of the GNU General Public License         *
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.     *
 ******************************************************************************/

package com.chrulri.droidtv;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.chrulri.droidtv.utils.ParallelTask;
import com.chrulri.droidtv.utils.ProcessUtils;

public class StreamActivity extends Activity implements OnClickListener{
    private static final String TAG = (StreamActivity.class.getSimpleName());
    
    private NotificationManager mNotificationManager;
    private int notificationID = 1333;
   
    private Button streamButton;
    private TextView streamText;
    
    public enum DvbType {
        ATSC, DVBT, DVBC, DVBS
    }

    public static final String EXTRA_CHANNELCONFIG = "channelconfig";

    static final int MUMUDVB_CHECKDELAY = 500;

    private String json;
    private String mChannelConfig;
    private String mChannelName;
    private String mFreq;
    private String mSid;
    private final Handler mHandler = new Handler();
    private AsyncStatusTask mStatusTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.stream);
        streamText = (TextView) findViewById(R.id.textView1);
        streamButton = (Button) findViewById(R.id.button1);
        streamButton.setOnClickListener(this);
        mChannelConfig = getIntent().getStringExtra(EXTRA_CHANNELCONFIG);
        Intent intent = new Intent(this, StreamService.class);
        intent.putExtra(StreamService.EXTRA_CHANNELCONFIG, mChannelConfig);
        String[] params = mChannelConfig.split(":");
        mChannelName = params[0];
        mFreq = params[1];
        mSid = params[2];
        startService(intent);
        addNotification();
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart");
        super.onStart();
        mStatusTask = new AsyncStatusTask();
        mStatusTask.execute();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
        ProcessUtils.finishTask(mStatusTask, false);
    }
 
    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        stopService(new Intent(this, StreamService.class));
        removeNotification();
    }
    @Override
    public void onClick(View v) {
        if (v == streamButton) {
        	Uri uriUrl = Uri.parse("http://127.0.0.1:1234/bysid/" + mSid);
            Intent launchBrowser = new Intent(Intent.ACTION_VIEW);
            launchBrowser.setDataAndType(uriUrl, "video/mpeg");
            startActivity(launchBrowser);
        }
    }


    class AsyncStatusTask extends ParallelTask {

        final String TAG = StreamActivity.TAG + "." + AsyncStatusTask.class.getSimpleName();

        @Override
        protected void doInBackground() {
            Log.d(TAG, ">>>");
            try {
                Thread.sleep(MUMUDVB_CHECKDELAY);
            } catch (InterruptedException e) {
                // nop
            }
            while (!isCancelled()) {
                try {
                	String urlString = "http://127.0.0.1:1234/monitor/signal_power.json";
                	HttpClient httpclient = new DefaultHttpClient();
                	HttpGet httpget = new HttpGet(urlString);
                	try {
                		HttpResponse response = httpclient.execute(httpget);
                		HttpEntity entity = response.getEntity();
                		if (entity != null) {
                			InputStream instream = entity.getContent();
                			InputStreamReader is = new InputStreamReader(instream);
                			StringBuilder sb=new StringBuilder();
                			BufferedReader br = new BufferedReader(is);
                			String read = br.readLine();
                			sb.append(read);
                			json = sb.toString();
                			instream.close();
                		}
                	}
                	catch (Exception e) {
                		Log.e(TAG, "monitor service access error", e);
                	}
                    publishProgress();
                }
                catch (Throwable t) {
                    Log.w(TAG, "Status", t);
                }
                // zZzZZZ..
                try {
                    Thread.sleep(MUMUDVB_CHECKDELAY);
                }
                catch (InterruptedException e) {
                    continue;
                }
            }
            Log.d(TAG, "<<<");
        }

        private void publishProgress() {
            Message msg = Message.obtain(mHandler, new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "status: " + json);
					updateStatus();
                }
            });
            mHandler.sendMessage(msg);
        }
    }

    private void updateStatus() {
    	if (json != null){
    		try {
    			JSONObject jsonObject = new JSONObject(json);
    			String str = "Reception Status";
    			str += "\nChannel name = " + mChannelName
    					+ "\nFrequency = " + mFreq
    					+ "\nService ID = " + mSid
    					+ "\nBit error rate = " + jsonObject.getLong("ber")
    					+ "\nSignal strength = " + jsonObject.getLong("strength")
    					+ "\nSignal to noise ratio = " + jsonObject.getLong("snr")
    					+ "\nUncorrected blocks = " + jsonObject.getLong("ub");
    			streamText.setText(str);
    		} catch (JSONException e) {
    			Log.e(TAG, "JSON parsing error", e);
    		}
    	}
    }

    private void addNotification() {
    	NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
    	Intent resultIntent = new Intent(this, stopserviceActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(stopserviceActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
           stackBuilder.getPendingIntent(
              0,
              PendingIntent.FLAG_UPDATE_CURRENT
           );
        mBuilder.setContentIntent(resultPendingIntent);
    	mBuilder.setAutoCancel(true);
        mBuilder.setSmallIcon(R.drawable.ic_launcher);
        mBuilder.setTicker("Streaming service running");
        mBuilder.setContentTitle("Channel streaming service running. Touch to stop service");
        mBuilder.setContentText("open http://127.0.0.1:1234/bysid/" + mSid + " on player to view the channel");
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(notificationID, mBuilder.build());
    }

    private void removeNotification() {
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    	mNotificationManager.cancel(notificationID);
    }

}
