package com.smarttechnologies.topupchap;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;


public class UssdCaller
{
	private static final String TAG = "UssdCaller";
	public static final int REQUEST_CALL_PHONE_PERMISSION = 123; // Public constant

	public static boolean checkPhoneCallPermission(Context context) {
		// Check if the CALL_PHONE permission is already granted
		if (context.checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
			Log.i(TAG, "CALL_PHONE permission already granted.");
			return true;
		} else {
			Log.w(TAG, "CALL_PHONE permission not granted.");
			return false;
		}
	}

	public static void requestCallPhonePermission(Activity activity) {
		if (activity != null) {
			activity.requestPermissions(new String[]{Manifest.permission.CALL_PHONE},
										REQUEST_CALL_PHONE_PERMISSION);
		} else {
			Log.e(TAG, "Activity context is null. Cannot request permissions.");
			// Handle the error appropriately, perhaps by informing the user.
			Toast.makeText(activity.getApplicationContext(),
						   "This app needs call phone permission. Please grant it in settings.",
						   Toast.LENGTH_LONG).show();
		}
	}

	public static void makeUssdCallsAsync(Context context, String[] listOfUssdCodes, int delayMillis) {
		if (!checkPhoneCallPermission(context)) {
			Log.w(TAG, "CALL_PHONE permission not granted. Cannot initiate USSD calls.");
			// The calling Activity should handle requesting the permission
			// and then call makeUssdCalls again if granted.
			Toast.makeText(context,
						   "Please grant phone call permission to make USSD calls.",
						   Toast.LENGTH_LONG).show();
			return;
		}

		new UssdCallTask(context, listOfUssdCodes, delayMillis).execute();
	}

	private static class UssdCallTask extends AsyncTask<Void, Integer, Void> {
		private final Context context;
		private final String[] ussdCodes;
		private final int delay;

		UssdCallTask(Context context, String[] ussdCodes, int delay) {
			this.context = context.getApplicationContext(); // Use application context
			this.ussdCodes = ussdCodes;
			this.delay = delay;
		}

		@Override
		protected Void doInBackground(Void... voids) {
			for (int i = 0; i < ussdCodes.length; i++) {
				String ussdCode = ussdCodes[i];
				if (ussdCode == null || ussdCode.trim().isEmpty()) {
					Log.w(TAG, "Skipping empty or null USSD code.");
					continue;
				}

				String encodedUssdCode = Uri.encode(ussdCode);
				String ussdUri = "tel:" + encodedUssdCode;
				Intent callIntent = new Intent(Intent.ACTION_CALL);
				callIntent.setData(Uri.parse(ussdUri));
				callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Important for calling from AsyncTask with app context

				Log.d(TAG, "Attempting to call USSD: " + ussdCode + " (Encoded: " + encodedUssdCode + ")");

				try {
					context.startActivity(callIntent);
					Log.i(TAG, "Initiated call for USSD: " + ussdCode);
					publishProgress(i + 1, ussdCodes.length); // Publish progress after each call
				} catch (SecurityException e) {
					Log.e(TAG, "SecurityException while making call for " + ussdCode + ": " + e.getMessage(), e);
					Toast.makeText(context,
								   "Phone call permission denied. Cannot make USSD call.",
								   Toast.LENGTH_LONG).show();
					return null; // Stop further USSD calls
				}

				try {
					Thread.sleep(delay);
				} catch (InterruptedException e) {
					Log.e(TAG, "Thread interrupted during delay after calling " + ussdCode + ": " + e.getMessage(), e);
					Thread.currentThread().interrupt();
					break; // Stop processing further USSD codes if interrupted
				}
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			// Update the UI with the progress of the USSD calls
			Toast.makeText(context, "Processing top-up " + values[0] + " of " + values[1], Toast.LENGTH_SHORT).show();
		}

		@Override
		protected void onPostExecute(Void result) {
			Log.i(TAG, "Finished processing all USSD codes in background.");
			Toast.makeText(context, "Finished attempting all top-ups.", Toast.LENGTH_SHORT).show();
			// Optionally, you can send a broadcast or use a callback to inform the Activity
			// that the process is complete.
		}
	}
}

