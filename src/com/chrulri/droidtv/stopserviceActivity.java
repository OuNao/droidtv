package com.chrulri.droidtv;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class stopserviceActivity extends Activity {
	@Override
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	@Override
    protected void onStart() {
        super.onStart();
		stopService(new Intent(this, StreamService.class));
		finish();
	}   
}
