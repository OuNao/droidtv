package com.chrulri.droidtv;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.chrulri.droidtv.utils.ProcessUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import android.widget.Toast;

public class StreamService extends Service {
	public static final String EXTRA_CHANNELCONFIG = "channelconfig";
	private String channelconfig;
    private static final String TAG = (StreamActivity.class.getSimpleName());

    static final int MUMUDVB = R.raw.mumudvb;
    static final String MUMUDVB_CONFIG_CONTENT = "freq=%d\ndelivery_system=DVBT\nmulticast_ipv4=0\nunicast=1\nport_http=1234\nip_http=0.0.0.0\nautoconfiguration=full\nautoconf_sid_list=%d\n";
    static final String MUMUDVB_CONFIG_FILENAME = "mumudvb.conf";
    private Process mumudvb;
    
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		channelconfig = intent.getStringExtra(EXTRA_CHANNELCONFIG);
		try {
            Log.d(TAG, ">>> startStream(" + channelconfig + ")");
            try {
                // config file
                File configFile = new File(getCacheDir(), MUMUDVB_CONFIG_FILENAME);
                PrintWriter writer = new PrintWriter(configFile);
                // sNAME/iFREQ/iServiceID
                String[] params = channelconfig.split(":");
                // check config length
                if (params.length != 3) {
                	writer.close();
                    throw new IOException("invalid DVB params count[" + params.length + "]");
                }
                // parse config
                int freq = tryParseInt(params[1], "frequency");
                int sid = tryParseInt(params[2], "service ID");
                // print config
                writer.println(String.format(MUMUDVB_CONFIG_CONTENT, freq/1000000, sid));
                writer.close();
                // run mumudvb
                Log.d(TAG, "mumudvb(" + configFile + "," + freq + ")");
                mumudvb = ProcessUtils.runBinary(StreamService.this, MUMUDVB,
                		"-d", "-c", configFile.getAbsolutePath());
                Toast.makeText(this, "StreamService Started. Open " + "http://127.0.0.1:1234/bysid/" + params[2] + " on player", Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                Log.e(TAG, "starting stream failed", e);
                //ErrorUtils.error(this, "failed to start streaming", e);
                this.stopSelf();
            }
        } finally {
            Log.d(TAG, "<<< startStream");
        }
		return Service.START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onDestroy() {
	    Log.d(TAG, ">>> stopStream");
        ProcessUtils.terminate(mumudvb);
        Toast.makeText(this, "StreamService Destroyed", Toast.LENGTH_LONG).show();
        Log.d(TAG, "<<< stopStream");
	}

	private static int tryParseInt(String str, String paramName)
            throws IOException {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            throw new IOException(
                    "error while parsing " + paramName + " (" + str + ")");
        }
    }
}
