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
    private static final String TAG = (StreamActivity.class.getSimpleName()+"caramba");

    static final int DVBLAST = R.raw.dvblast_2_1_0;
    static final String UDP_IP = "127.0.0.1";
    static final int UDP_PORT = 1555;
    static final String DVBLAST_CONFIG_CONTENT = UDP_IP + ":" + UDP_PORT + " 1 %d";
    static final String DVBLAST_CONFIG_FILENAME = "dvblast.conf";
    static final String DVBLAST_SOCKET = "droidtv.socket";
    private Process dvblast;
	
    
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		channelconfig = intent.getStringExtra(EXTRA_CHANNELCONFIG);
	    //TODO do something useful
		try {
            Log.d(TAG, ">>> startStream2(" + channelconfig + ")");
            try {
                // config file
                File configFile = new File(getCacheDir(), DVBLAST_CONFIG_FILENAME);
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
                writer.println(String.format(DVBLAST_CONFIG_CONTENT, sid));
                writer.close();
                // run dvblast
                Log.d(TAG, "dvblast(" + configFile + "," + freq + ")");
                dvblast = ProcessUtils.runBinary(StreamService.this, DVBLAST,
                		"-r", DVBLAST_SOCKET,
                		"-xxml", "-c", configFile.getAbsolutePath(),
                        "-f" + freq, "-q");
                Toast.makeText(this, "StreamService Started. Open rtp://217.0.0.1:1555 on player", Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                Log.e(TAG, "starting stream failed", e);
                //ErrorUtils.error(this, "failed to start streaming", e);
                this.stopSelf();
            }
        } finally {
            Log.d(TAG, "<<< startStream2");
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
	    Log.d(TAG, ">>> stopStream2");
        ProcessUtils.terminate(dvblast);
        Toast.makeText(this, "StreamService Destroyed", Toast.LENGTH_LONG).show();
        Log.d(TAG, "<<< stopStream2");
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
