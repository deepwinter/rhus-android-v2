package org.calflora.observer;

import net.winterroot.android.wildflowers.R;
import android.os.Bundle;
import com.actionbarsherlock.view.*;

import android.webkit.WebView;

public class HelpActivity extends BaseActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_help);
	}

	@Override
	protected void onStart() {
		super.onStart();
		WebView webview = (WebView) findViewById(R.id.help_webview);
		webview.loadUrl("http://www.calflora.org/phone");
		//webview.loadUrl("www.calflora.org/phone/yosemite");

	}
	
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getSupportMenuInflater().inflate(R.menu.help, (Menu) menu);
		return true;
	}

}
