package com.smarttechnologies.topupchap;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends Activity {

    private Button BtnTopUp;
    private EditText EdtTxtCodes;
    private static final int CALL_PHONE_PERMISSION_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Get a reference to the button
        BtnTopUp = findViewById(R.id.BtnTopUp);
        //Get a reference to the EditText
        EdtTxtCodes = findViewById(R.id.EdtTxtCodeArea);

        // Set an OnClickListener for the button
        BtnTopUp.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Toast.makeText(MainActivity.this, "Processing top-up requests...", Toast.LENGTH_SHORT).show();
					checkPhoneCallPermissionAndDial();
				}
			});
    }

    private void checkPhoneCallPermissionAndDial() {
        // Check if the CALL_PHONE permission is already granted
        if (!UssdCaller.checkPhoneCallPermission(this)) {
            // Permission is not granted, request it from the user
            UssdCaller.requestCallPhonePermission(this);

        } else {
            // Permission is granted, dial the USSD code asynchronously
            dialUSSDCodeAsync();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CALL_PHONE_PERMISSION_REQUEST_CODE) {
            // Check if the permission was granted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, dial the USSD code asynchronously
                dialUSSDCodeAsync();
            } else {
                // Permission denied, inform the user with a Toast
                Toast.makeText(this, "Phone call permission is required to check balance.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static String[] shuffleStringArray(String[] array) {
        Random random = new Random();
        int n = array.length;

        // Iterate from the last element down to the first
        for (int i = n - 1; i > 0; i--) {
            // Generate a random index j such that 0 <= j <= i
            int j = random.nextInt(i + 1);

            // Swap array[i] and array[j]
            String temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }
        return array; // The original array is now shuffled in place
    }

    private void dialUSSDCodeAsync() {

        String NumbersText = EdtTxtCodes.getText().toString();
        String numberwithoutspaces = NumbersText.replaceAll(" ", "");
        String[] Numbers = numberwithoutspaces.split("\\n");
        List<String> validNumbers = new ArrayList<>();

        if (Numbers.length > 0) {
            Toast.makeText(this, "Processing entered numbers...", Toast.LENGTH_SHORT).show();
            for (String number : Numbers) {
                String trimmedNumber = number.trim();
                if (trimmedNumber.length() == 16 && TextUtils.isDigitsOnly(trimmedNumber)) {
                    validNumbers.add(trimmedNumber);
                } else if (!trimmedNumber.isEmpty()) {
                    Log.w("Input Validation", "Discarding invalid number: '" + trimmedNumber + "'. Must be 16 digits.");
                    Toast.makeText(this, "Invalid number found: '" + trimmedNumber + "'. Please enter 16-digit numbers.", Toast.LENGTH_LONG).show();
                    // Optionally handle stopping further processing if an invalid number is found
                    // return;
                }
            }

            if (!validNumbers.isEmpty()) {
                Toast.makeText(this, "Attempting to top up " + validNumbers.size() + " valid numbers.", Toast.LENGTH_SHORT).show();
                String[] shuffledValidNumbers = shuffleStringArray(validNumbers.toArray(new String[0]));
                // Shuffle the valid numbers to randomize the order of top-up attempts.
                // This can help in scenarios where there might be temporary network issues
                // or rate limits associated with consecutive top-ups to the same recipient.
                String[] ussdCodes = new String[shuffledValidNumbers.length];
                for (int i = 0; i < shuffledValidNumbers.length; i++) {
                    ussdCodes[i] = "*141*" + shuffledValidNumbers[i] + "#";
                    Log.d("USSD Generation", "Generated code: " + ussdCodes[i]);
                }
                // Call the asynchronous USSD calling method
                UssdCaller.makeUssdCallsAsync(this, ussdCodes, 2000);
            } else {
                Toast.makeText(this, "No valid 16-digit numbers entered for top-up.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Please enter 16-digit numbers to top up.", Toast.LENGTH_SHORT).show();
        }
    }
}

