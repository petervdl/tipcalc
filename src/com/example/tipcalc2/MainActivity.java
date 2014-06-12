package com.example.tipcalc2;
// Peter van der Linden, calculates tips,  June 8 2014
// sounds from: I forget where, but try https://www.google.com/?gws_rd=ssl#q=sound+effects+free&safe=off
// icons from  http://www.webdesigndevelopers.co.nz/web_design_tutorials_website_resources.htm
// time spent 3 hrs on Friday trying to get the XML right for UI
// 5 hours on Sunday completing the code & debugging it.
// 1 hour adding sound and vibration
// 2 more hours on Sunday, debugging silent failure caused by using a string resource where string needed in Java.
// 1 hour Monday finding and editing icons for button images.
// 2 hour Monday hooking up image buttons, experimenting with positions on screen, etc
// 3 hour Monday making the split work, and don't forget you need to round a bill up (to pay it fully when splitting)
// 3 hour Mon/Tues to save the Tip percent to a file
// 3 hours Tues, final smoothing, polishing, testing, fixing more bugs, improving look

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import com.immersion.uhl.Launcher;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	static EditText et1_total;
	static EditText et2_tip_percent;
	static EditText et3_tip_amount;
	static TextView et4_total_with_tip;
	static EditText et5_split;
	static EditText et6_per_person;
	static TextView tv3_tip_amount;
	static TextView tv4_total_with_tip;
	static TextView tv5_split;
	static TextView tv6_per_person;
	static boolean calculate = true;   
	static private boolean isFirstTimeGetFocused = true;

	static private float fTotalnTip;

	private Launcher vibes;
	private SoundPool soundPool;
	private HashMap<Integer, Integer> soundPoolMap;
	private static final int CLUNKYNOISE = 1;
	private float volume;

	@SuppressLint("UseSparseArrays")
	private void initSounds() {
		vibes = new Launcher(this);
		soundPool = new SoundPool(4, AudioManager.STREAM_MUSIC, 100);
		soundPoolMap = new HashMap<Integer, Integer>();
		soundPoolMap.put(CLUNKYNOISE, soundPool.load(getBaseContext(), R.raw.clunking, 1));
		volume = 0.75f;   //   this number is a good volume level

	}
	private void playEffect() {
		// plays a clunky sound as it calculates
		/* Play the sound with the correct volume */
		vibes.play(56);
		soundPool.play(soundPoolMap.get(CLUNKYNOISE), volume, volume, 1, 0, 1f);     
	}

	public void hideSoftKeyboard() {
		if(getCurrentFocus()!=null) {
			InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
			inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
		}
	}

	private String storedTipPct ="";
	private final String fname ="tip.txt";

	private void setTipPctInFile(String newValue) {
		if (newValue.equals(storedTipPct))  return; // no need to write same value

		try {
			FileOutputStream fos = openFileOutput(fname, Context.MODE_PRIVATE);
			fos.write(newValue.getBytes());
			Log.e("tipcalc2","have written val="+newValue +" to file="+fname);
			fos.close();
			storedTipPct = newValue;
		} catch (IOException e) {
			Log.e("tipcalc2", "ioExcpn with file, msg="+e.getMessage());
			Toast.makeText(this, "could not save tip value to file, " +e.getMessage(), Toast.LENGTH_SHORT).show();
			e.printStackTrace(); // goes to log
		}
	}

	private String getTipPctFromFile() {
		String s = "";
		try {
			FileInputStream fis  = openFileInput(fname);
			int c;
			while( (c = fis.read()) != -1 ) {
				s = s + Character.toString((char)c);
			}
			fis.close();
		} catch (Exception e) {
			e.printStackTrace();
			s = ""; // if the input didn't parse OK, set default.
		}
		return s;
	}

	@Override
	protected void onPause() {
		super.onPause();
		setTipPctInFile(et2_tip_percent.getText().toString());
	}

	private Context ctxt;

	private void calculate() {
		//this method calculates the tip amount and the new total, and
		// puts them on the screen.
		float fTotal = 0.0f;
		float fTip_pct = 0.0f;
		float fTip = 0.0f;
		float fTotalIncTip = 0.0f;

		String total = et1_total.getText().toString();
		String tip = et2_tip_percent.getText().toString();
		et1_total.setTextColor(Color.BLACK);
		et2_tip_percent.setTextColor(Color.RED);
		try {
			fTotal = Float.parseFloat(total); 
			fTip_pct = Float.parseFloat(tip);
			fTip = (fTotal * fTip_pct) / 100.0f;
			// round tip to 2 dec places
			int i = (int) (fTip * 100.00f);
			fTip = ((float) i )/ 100.0f;
			// format the result to 2 dec places, non scientific notation
			String result = String.format("%6.2f", fTip);   
			et3_tip_amount.setText(result);

			// now calc and show the total with tip
			fTotalnTip = fTotal + fTip;
			result = String.format("%6.2f", fTotalnTip);
			et4_total_with_tip.setText(result);
		}
		catch(Exception e) {
			// ignore exceptions - just means there isn't a valid number in the field yet.
			//  Log.e("tipcalc2", "Exception working on tip: " + e.toString());
		}
	}

	private void listenForNewAmt () {
		et1_total.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			// bring up keyboard whenever someone changes amount
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
				}
			}
		});
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ctxt = this;
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.activity_main);
		initSounds();

		// use a less formal font on the alphabetic fields
		Typeface face;
		face = Typeface.createFromAsset(getAssets(), "ambershaie.ttf");
		TextView tv1_total = (TextView) findViewById(R.id.tv1_total);
		tv1_total.setTypeface(face);
		TextView tv2_tip_percent = (TextView) findViewById(R.id.tv2_tip_percent);
		tv2_tip_percent.setTypeface(face);
		tv3_tip_amount = (TextView) findViewById(R.id.tv3_tip_amount);
		tv3_tip_amount.setTypeface(face);
		tv4_total_with_tip = (TextView) findViewById(R.id.tv4_total_with_tip);
		tv4_total_with_tip.setTypeface(face);
		tv5_split = (TextView) findViewById(R.id.tv5_split);
		tv5_split.setTypeface(face);
		tv6_per_person = (TextView) findViewById(R.id.tv6_per_person);
		tv6_per_person.setTypeface(face);

		// get the editable fields
		et1_total = (EditText) findViewById(R.id.et1_total);
		et2_tip_percent = (EditText) findViewById(R.id.et2_tip_percent);
		et2_tip_percent.setText(getTipPctFromFile());
		et3_tip_amount = (EditText) findViewById(R.id.et3_tip_amount);
		et4_total_with_tip = (TextView) findViewById(R.id.et4_total_with_tip);
		et5_split = (EditText) findViewById(R.id.et5_split);
		et6_per_person = (EditText) findViewById(R.id.et6_per_person);

		// bring the keyboard up
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

		TextWatcher myTextWatcher = new TextWatcher() {
			// this handles the entry of values into the "bill amount" field
			// it does the calculations, but doesn't display them yet
			// the result display is triggered by the "tip me" button
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {				
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				calculate();
			}
		};

		et1_total.addTextChangedListener(myTextWatcher);

		et1_total.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if(hasFocus && isFirstTimeGetFocused){
					et1_total.setText("");
					isFirstTimeGetFocused = false;
				}
			}
		}	);

		et2_tip_percent.setOnFocusChangeListener(new OnFocusChangeListener() {
			// anytime you leave the tip field, update the preference
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if( !hasFocus ){
					setTipPctInFile(et2_tip_percent.getText().toString());
				}
			}
		}	);

		connectImageButtons();

		connectSplitButton();

		// when you calculate "each person pays" or enter "split", dismiss the keyboard
		// when you leave the app, blank out check amount but save other fields.
	}

	private void connectSplitButton(){
		TextWatcher splitFieldWatcher = new TextWatcher() {
			// this handles the entry of the Split, ie. # of people to share the bill with
			// and calculates the per-person share
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {				
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				// this handles changes in the et5_split field
				// When the split changes, the per-person share changes.
				String count = et5_split.getText().toString();
				float fCount = 0.0f;
				float fShare = 0.0f;
				calculate(); // gets all the values from the fields.
				try {
					fCount = Float.parseFloat(count); 
					if (fCount==1.0f) fShare = fTotalnTip;
					else { 
						fShare = fTotalnTip / fCount;
						// round the share to 2 places
						int share = (int) (fShare * 100.0f);
						fShare = ((float) share) /100.0f;
						// Add 1 cent if (share * count) smaller than the bill!
						if ( (fShare * fCount) < fTotalnTip ) fShare += 0.01f;

					}

					DecimalFormat df = new DecimalFormat("####0.00");    // 2 dec places
					// format the result to 2 decimal places, non scientific notation
					// df.setRoundingMode(RoundingMode.HALF_UP);
					String result = df.format(fShare);
					et6_per_person.setText(result);
					et6_per_person.setVisibility(View.VISIBLE);  
					tv6_per_person.setVisibility(View.VISIBLE);
					hideSoftKeyboard();
				}
				catch(Exception e) {
					// ignore exceptions - just means there isn't a valid number in the field yet.
					//  Log.e("tipcalc2", "Exception working on tip: " + e.toString());
				}
			}

		};
		et5_split.addTextChangedListener(splitFieldWatcher);
	}

	private ImageButton spkron;
	private ImageButton up, down;
	private static boolean spkrState = true;

	private void connectImageButtons() {
		spkron = (ImageButton) findViewById(R.id.spkron);
		up = (ImageButton) findViewById(R.id.upBtn);
		down = (ImageButton) findViewById(R.id.downBtn);
		up.setOnClickListener(  new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// when up button pressed, increment tip%
				String tip = et2_tip_percent.getText().toString();
				float fTippct = 0.0f;
				try {
					fTippct = Float.parseFloat(tip);
					fTippct++;
					// format the result to 2 digits, non scientific notation
					String result = String.format("%2.0f", fTippct);
					et2_tip_percent.setText(result);
				} catch(Exception e) {
					Log.e("tipcalc2", "Exception on tip incr button: " + e.toString());
				}
			}
		});

		down.setOnClickListener(  new View.OnClickListener() {
			// when down button pressed, decrement tip%
			@Override
			public void onClick(View v) {
				// decrement tip percent
				String tip = et2_tip_percent.getText().toString();
				float fTippct = 0.0f;
				try {
					fTippct = Float.parseFloat(tip);
					fTippct--;
					// format the result to 2 digits, non scientific notation
					String result = String.format("%2.0f", fTippct);
					et2_tip_percent.setText(result);
				} catch(Exception e) {
					Log.e("tipcalc2", "Exception on tip decr button: " + e.toString());
				}
			}
		});

		spkron.setOnClickListener(  new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// when the speaker icon is pressed, flip the "X" in the background.
				if (spkrState) {
					spkron.setBackground(getResources().getDrawable(R.drawable.spkroff));
				} else {
					spkron.setBackground(getResources().getDrawable(R.drawable.spkron));
				}
				spkrState= !spkrState;
			}
		});

		ImageButton btn =  (ImageButton) findViewById(R.id.btn);
		btn.setOnClickListener(  new View.OnClickListener() {
			// this is the handler for the Calculate/Clear button.
			@Override
			public void onClick(View v) {

				// act on the button press, show fields and calculate new values
				ImageButton b = (ImageButton) v;
				if (calculate) {  
					// we get here when "calculate" is pressed
					calcButtonPress();
				} else {
					// we get here when "clear" is pressed
					clearButtonPress();
				}
				calculate = !calculate; // Save state of current button label ("calculate" or "clear")
			}
		});
	}

	private void calcButtonPress() {
		// "calculate" button has been pressed
		ImageButton b =  (ImageButton) findViewById(R.id.btn);
		hideSoftKeyboard();
		if (spkrState) {
			playEffect();
			try {
				Thread.sleep(550);  //let noise complete before revealing totals
			} catch (Exception e) {
				// doesn't matter if interrupted
			};
		}
		b.setImageDrawable(getResources().getDrawable(R.drawable.btn1));
		tv3_tip_amount.setVisibility(View.VISIBLE);
		et3_tip_amount.setVisibility(View.VISIBLE);
		tv4_total_with_tip.setVisibility(View.VISIBLE);
		et4_total_with_tip.setVisibility(View.VISIBLE);
		try {
			Thread.sleep(500);  //let noise complete before revealing totals
		} catch (Exception e) {
			// doesn't matter if interrupted
		};
		// now bring up split fields
		tv5_split.setVisibility(View.VISIBLE);
		et5_split.setVisibility(View.VISIBLE);

	}

	private void clearButtonPress() {
		// "clear" button has been pressed
		ImageButton btn =  (ImageButton) findViewById(R.id.btn);
		btn.setImageDrawable(getResources().getDrawable(R.drawable.btn));
		tv3_tip_amount.setVisibility(View.INVISIBLE);
		et3_tip_amount.setText("0.00");
		et3_tip_amount.setVisibility(View.INVISIBLE);
		tv4_total_with_tip.setVisibility(View.INVISIBLE);
		et4_total_with_tip.setText("0.00");
		et4_total_with_tip.setVisibility(View.INVISIBLE);
		tv5_split.setVisibility(View.INVISIBLE);
		// reset the split to 1 person
		et5_split.setText("1");
		et5_split.setVisibility(View.INVISIBLE);
		tv6_per_person.setVisibility(View.INVISIBLE);
		et6_per_person.setText("0.00");
		et6_per_person.setVisibility(View.INVISIBLE);
		// bring up keyboard
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
	}

}