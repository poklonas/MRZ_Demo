package com.abbyy.mobile.sample;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.abbyy.mobile.rtr.Engine;
import com.abbyy.mobile.rtr.ITextCaptureService;
import com.abbyy.mobile.rtr.Language;

import java.util.ArrayList;
import java.util.List;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {

	// Licensing
	private static final String licenseFileName = "AbbyyRtrSdk.license";

	///////////////////////////////////////////////////////////////////////////////
	// Some application settings that can be changed to modify application behavior:
	// The camera zoom. Optically zooming with a good camera often improves results
	// even at close range and it might be required at longer ranges.
	private static final int cameraZoom = 1;
	// The default behavior in this sample is to start recognition when application is started or
	// resumed. You can turn off this behavior or remove it completely to simplify the application
	private static final boolean startRecognitionOnAppStart = true;
	// Area of interest specified through margin sizes relative to camera preview size
	private static final int areaOfInterestMargin_PercentOfWidth = 4;
	private static final int areaOfInterestMargin_PercentOfHeight = 25;
	// A subset of available languages shown in the UI. See all available languages in Language enum.
	// To show all languages in the UI you can substitute the list below with:
	// Language[] languages = Language.values();
	private Language[] languages = {
		Language.English,
	};
	///////////////////////////////////////////////////////////////////////////////

	// The 'Abbyy RTR SDK Engine' and 'Text Capture Service' to be used in this sample application
	private Engine engine;
	private ITextCaptureService textCaptureService;

	// The camera and the preview surface
	private Camera camera;
	private SurfaceViewWithOverlay surfaceViewWithOverlay;
	private SurfaceHolder previewSurfaceHolder;

	// Actual preview size and orientation
	private Camera.Size cameraPreviewSize;
	private int orientation;

	// Auxiliary variables
	private boolean inPreview = false; // Camera preview is started
	private boolean stableResultHasBeenReached; // Stable result has been reached
	private boolean startRecognitionWhenReady; // Start recognition next time when ready (and reset this flag)
	private Handler handler = new Handler(); // Posting some delayed actions;

	// UI components
	private Button startButton; // The start button
	private TextView warningTextView; // Show warnings from recognizer
	private TextView errorTextView; // Show errors from recognizer
	private TextView resultTextView;


	// Text displayed on start button
	private static final String BUTTON_TEXT_START = "Start";
	private static final String BUTTON_TEXT_STOP = "Stop";
	private static final String BUTTON_TEXT_STARTING = "Starting...";

	// Show Data after recognition
	private String TD3_Document_Code;
	private String TD3_Issuing_State;
	private String TD3_Document_number;
	private String TD3_Nationality;
	private String TD3_Date_of_birth;
	private String TD3_Sex;
	private String TD3_Date_of_expiry;
	private String passportNo;

	private HashMap<Character, Integer> hmap = new HashMap<Character, Integer>();

	// To communicate with the Text Capture Service we will need this callback:
	private ITextCaptureService.Callback textCaptureCallback = new ITextCaptureService.Callback() {

		@Override
		public void onRequestLatestFrame( byte[] buffer )
		{
			// The service asks to fill the buffer with image data for the latest frame in NV21 format.
			// Delegate this task to the camera. When the buffer is filled we will receive
			// Camera.PreviewCallback.onPreviewFrame (see below)
			camera.addCallbackBuffer( buffer );
		}

		@Override
		public void onFrameProcessed( ITextCaptureService.TextLine[] lines,
			ITextCaptureService.ResultStabilityStatus resultStatus, ITextCaptureService.Warning warning )
		{
			// Frame has been processed. Here we process recognition results. In this sample we
			// stop when we get stable result. This callback may continue being called for some time
			// even after the service has been stopped while the calls queued to this thread (UI thread)
			// are being processed. Just ignore these calls:
			if( !stableResultHasBeenReached ) {
				// Show the warning from the service if any. The warnings are intended for the user
				// to take some action (zooming in, checking recognition language, etc.)
				surfaceViewWithOverlay.setLines( lines, resultStatus );
//				warningTextView.setText( warning != null ? warning.name() : "" );
				warningTextView.setText( warning != null ? warning.name() : "Process...." );

				if( resultStatus == ITextCaptureService.ResultStabilityStatus.Available ) {
					// Stable result has been reached. Stop the service
					get_result(lines, resultStatus);
//					stopRecognition();
//					stableResultHasBeenReached = true;
//
//					get_result(lines, resultStatus);

					// Show result to the user. In this sample we whiten screen background and play
					// the same sound that is used for pressing buttons
//					surfaceViewWithOverlay.setFillBackground( true );
//					startButton.playSoundEffect( android.view.SoundEffectConstants.CLICK );
				}
			}
		}

		@Override
		public void onError( Exception e )
		{
			// An error occurred while processing. Log it. Processing will continue
			Log.e( getString( R.string.app_name ), "Error: " + e.getMessage() );
			if( BuildConfig.DEBUG ) {
				// Make the error easily visible to the developer
				String message = e.getMessage();
				if( message == null ) {
					message = "Unspecified error while creating the service. See logcat for details.";
				} else {
					if( message.contains( "ChineseJapanese.rom" ) ) {
						message = "Chinese, Japanese and Korean are available in EXTENDED version only. Contact us for more information.";
					}
					if( message.contains( "Russian.edc" ) ) {
						message = "Cyrillic script languages are available in EXTENDED version only. Contact us for more information.";
					} else if( message.contains( ".trdic" ) ) {
						message = "Translation is available in EXTENDED version only. Contact us for more information.";
					}
				}
				errorTextView.setText( message );
			}
		}
	};

	//Reader result from text recognition
	private void get_result(ITextCaptureService.TextLine[] lines,
							ITextCaptureService.ResultStabilityStatus resultStatus){
		if (lines.length >= 2){
			String data1 = lines[lines.length - 2].Text;
			String data2 = lines[lines.length - 1].Text;
			String lines1 = data1.replace("«","<<");
			String lines2 = data2.replace("«","<<");
			int sizeLine1 = lines1.length();
			int sizeLine2 = lines2.length();
			if(lines.length >= 3){
				String data3 = lines[lines.length - 3].Text;
				String lines3 = data3.replace("«","<<");
				int sizeLine3 = lines3.length();
				if(sizeLine3 == 30 && sizeLine3 == sizeLine2 && sizeLine3 == sizeLine1){
					MRZ_Type1_Show(lines3, lines1, lines2);
				}
			}
			if(sizeLine1 == sizeLine2) {
			//String dumy1 = "P<SWEHAAKANSSON<<NILS<ERIK<MATTIAS<<<<<<<<<<";
			//String dummy2 = "90478012<3SWE4908300M2009292194908306212<<56";
			//MRZ_Type1and3_Show(dumy1, dummy2);
				if(sizeLine1 == 44) {
					MRZ_Type3_Show(lines1, lines2);
				}else if(sizeLine1 == 36){
					MRZ_Type2_Show(lines1, lines2);
				}
			}
		}
	}

//	private int sumCheckDigitForPPID(String line){
//		int sum = 0;
//		int state = 7;
//		int[] tranfer = tranferValueForPPID(line);
//		for (int i = 0 ; i < line.length() ; i++){
//			sum += (state * tranfer[i]);
//			switch (state) {
//				case 7:
//					state = 3;
//					break;
//				case 3:
///					state = 1;
	//				break;
	//			case 1:
	//				state = 7;
	//				break;
	//			default:
	//				break;
	//		}
	//	}
	//	return sum;
	//}

//	private int sumCheckDigitForEXP(String line){
//		int sum = 0;
//		int state = 7;
//		int[] tranfer = tranferValueForEXP(line);
//		for (int i = 0 ; i < line.length() ; i++){
//			sum += (state * tranfer[i]);
//			switch (state) {
//				case 7:
//					state = 3;
//					break;
//				case 3:
//					state = 1;
//					break;
///				case 1:
	//				state = 7;
	//				break;
	//			default:
	//				break;
	//		}
	//	}
	//	return sum;
	//}

//	private int sumCheckDigitForBD(String line){
//		int sum = 0;
//		int state = 7;
//		int[] tranfer = tranferValueForBD(line);
//		for (int i = 0 ; i < line.length() ; i++){
//			sum += (state * tranfer[i]);
//			switch (state) {
//				case 7:
//					state = 3;
//					break;
///				case 3:
//					state = 1;
//					break;
//				case 1:
//					state = 7;
//					break;
//				default:
//					break;
//			}
//		}
//		return sum;
//	}

	private int sumCheckDigitForAll(String line){
		int sum = 0;
		int state = 7;
		int[] tranfer = tranferValueForall(line);
		for (int i = 0 ; i < line.length() ; i++){
			sum += (state * tranfer[i]);
			switch (state) {
				case 7:
					state = 3;
					break;
				case 3:
					state = 1;
					break;
				case 1:
					state = 7;
					break;
				default:
					break;
			}
		}
		return sum;
	}

//	private int sumCheckDigitForAlltd2(String line){
//		int sum = 0;
//		int state = 7;
//		int[] tranfer = tranferValueForall(line);
//		for (int i = 0 ; i < line.length() ; i++){
//			sum += (state * tranfer[i]);
//			switch (state) {
//				case 7:
//					state = 3;
//					break;
//				case 3:
//					state = 1;
//					break;
//				case 1:
//					state = 7;
//					break;
//				default:
//					break;
//			}
//		}
//		return sum;
//	}

//	private int sumCheckDigitForAlltd1(String line){
//		int sum = 0;
//		int state = 7;
//		int[] tranfer = tranferValueForall(line);
//		for (int i = 0 ; i < line.length() ; i++){
//			sum += (state * tranfer[i]);
//			switch (state) {
//				case 7:
//					state = 3;
//	/				break;
///				case 3:
//					state = 1;
//					break;
//				case 1:
//					state = 7;
//					break;
//				default:
//					break;
//			}
//		}
//		return sum;
//	}

	private int charToInt(char c){
		switch (c) {
			case '1':
				return 1;
			case '2':
				return 2;
			case '3':
				return 3;
			case '4':
				return 4;
			case '5':
				return 5;
			case '6':
				return 6;
			case '7':
				return 7;
			case '8':
				return 8;
			case '9':
				return 9;
			case 'A':
				return 10;
			case 'B':
				return 11;
			case 'C':
				return 12;
			case 'D':
				return 13;
			case 'E':
				return 14;
			case 'F':
				return 15;
			case 'G':
				return 16;
			case 'H':
				return 17;
			case 'I':
				return 18;
			case 'J':
				return 19;
			case 'K':
				return 20;
			case 'L':
				return 21;
			case 'M':
				return 22;
			case 'N':
				return 23;
			case 'O':
				return 24;
			case 'P':
				return 25;
			case 'Q':
				return 26;
			case 'R':
				return 27;
			case 'S':
				return 28;
			case 'T':
				return 29;
			case 'U':
				return 30;
			case 'V':
				return 31;
			case 'W':
				return 32;
			case 'X':
				return 33;
			case 'Y':
				return 34;
			case 'Z':
				return 35;
			case '<':
				return 0;
			default:
				return 0;
		}
	}

//	private int[] tranferValueForPPID(String line){
//		int[] result = new int[9];
//		result[8] = charToInt(line.charAt(8));
//		result[7] = charToInt(line.charAt(7));
//		result[6] = charToInt(line.charAt(6));
//		result[5] = charToInt(line.charAt(5));
//		result[4] = charToInt(line.charAt(4));
//		result[3] = charToInt(line.charAt(3));
//		result[2] = charToInt(line.charAt(2));
//		result[1] = charToInt(line.charAt(1));
//		result[0] = charToInt(line.charAt(0));
//		return result;
//	}

//	private int[] tranferValueForEXP(String line){
//		int[] result = new int[6];
//		result[5] = charToInt(line.charAt(5));
//		result[4] = charToInt(line.charAt(4));
//		result[3] = charToInt(line.charAt(3));
//		result[2] = charToInt(line.charAt(2));
//		result[1] = charToInt(line.charAt(1));
//		result[0] = charToInt(line.charAt(0));
//		return result;
//	}

//	private int[] tranferValueForBD(String line){
//		int[] result = new int[6];
//		result[5] = charToInt(line.charAt(5));
//		result[4] = charToInt(line.charAt(4));
//		result[3] = charToInt(line.charAt(3));
//		result[2] = charToInt(line.charAt(2));
//		result[1] = charToInt(line.charAt(1));
//		result[0] = charToInt(line.charAt(0));
//		return result;
//	}

//	private int[] tranferValueForallTD3(String line){
//		int[] result = new int[39];
//		result[38] = charToInt(line.charAt(38));
//		result[37] = charToInt(line.charAt(37));
//		result[36] = charToInt(line.charAt(36));
//		result[35] = charToInt(line.charAt(35));
//		result[34] = charToInt(line.charAt(34));
//		result[33] = charToInt(line.charAt(33));
//		result[32] = charToInt(line.charAt(32));
//		result[31] = charToInt(line.charAt(31));
//		result[30] = charToInt(line.charAt(30));
//		result[29] = charToInt(line.charAt(29));
//		result[28] = charToInt(line.charAt(28));
//		result[27] = charToInt(line.charAt(27));
//		result[26] = charToInt(line.charAt(26));
//		result[25] = charToInt(line.charAt(25));
//		result[24] = charToInt(line.charAt(24));
//		result[23] = charToInt(line.charAt(23));
//		result[22] = charToInt(line.charAt(22));
//		result[21] = charToInt(line.charAt(21));
//		result[20] = charToInt(line.charAt(20));
//		result[19] = charToInt(line.charAt(19));
//		result[18] = charToInt(line.charAt(18));
//		result[17] = charToInt(line.charAt(17));
//		result[16] = charToInt(line.charAt(16));
//		result[15] = charToInt(line.charAt(15));
//		result[14] = charToInt(line.charAt(14));
//		result[13] = charToInt(line.charAt(13));
//		result[12] = charToInt(line.charAt(12));
//		result[11] = charToInt(line.charAt(11));
//		result[10] = charToInt(line.charAt(10));
//		result[9] = charToInt(line.charAt(9));
//		result[8] = charToInt(line.charAt(8));
//		result[7] = charToInt(line.charAt(7));
//		result[6] = charToInt(line.charAt(6));
//		result[5] = charToInt(line.charAt(5));
//		result[4] = charToInt(line.charAt(4));
//		result[3] = charToInt(line.charAt(3));
//		result[2] = charToInt(line.charAt(2));
//		result[1] = charToInt(line.charAt(1));
//		result[0] = charToInt(line.charAt(0));
//		return result;
//	}

	private int[] tranferValueForall(String line){
		int[] result = new int[line.length()];
        for (int i = 0 ; i < line.length() ; i++){
            result[i] =  charToInt(line.charAt(i));
        }
		return result;
	}

//	private int[] tranferValueForallTD1(String line){
//		int[] result = new int[52];
//		result[51] = charToInt(line.charAt(51));
//		result[50] = charToInt(line.charAt(50));
//		result[49] = charToInt(line.charAt(49));
//		result[48] = charToInt(line.charAt(48));
//		result[47] = charToInt(line.charAt(47));
//		result[46] = charToInt(line.charAt(46));
//		result[45] = charToInt(line.charAt(45));
//		result[44] = charToInt(line.charAt(44));
//		result[43] = charToInt(line.charAt(43));
//		result[42] = charToInt(line.charAt(42));
//		result[41] = charToInt(line.charAt(40));
//		result[40] = charToInt(line.charAt(40));
//		result[39] = charToInt(line.charAt(39));
//		result[38] = charToInt(line.charAt(38));
//		result[37] = charToInt(line.charAt(37));
//		result[36] = charToInt(line.charAt(36));
//		result[35] = charToInt(line.charAt(35));
//		result[34] = charToInt(line.charAt(34));
//		result[33] = charToInt(line.charAt(33));
//		result[32] = charToInt(line.charAt(32));
//		result[31] = charToInt(line.charAt(31));
//		result[30] = charToInt(line.charAt(30));
//		result[29] = charToInt(line.charAt(29));
//		result[28] = charToInt(line.charAt(28));
//		result[27] = charToInt(line.charAt(27));
//		result[26] = charToInt(line.charAt(26));
//		result[25] = charToInt(line.charAt(25));
//		result[24] = charToInt(line.charAt(24));
//		result[23] = charToInt(line.charAt(23));
//		result[22] = charToInt(line.charAt(22));
//		result[21] = charToInt(line.charAt(21));
//		result[20] = charToInt(line.charAt(20));
//		result[19] = charToInt(line.charAt(19));
///		result[18] = charToInt(line.charAt(18));
//		result[17] = charToInt(line.charAt(17));
//		result[16] = charToInt(line.charAt(16));
//		result[15] = charToInt(line.charAt(15));
//		result[14] = charToInt(line.charAt(14));
///		result[13] = charToInt(line.charAt(13));
//		result[12] = charToInt(line.charAt(12));
//		result[11] = charToInt(line.charAt(11));
//		result[10] = charToInt(line.charAt(10));
//		result[9] = charToInt(line.charAt(9));
//		result[8] = charToInt(line.charAt(8));
//		result[7] = charToInt(line.charAt(7));
//		result[6] = charToInt(line.charAt(6));
///		result[5] = charToInt(line.charAt(5));
//		result[4] = charToInt(line.charAt(4));
//		result[3] = charToInt(line.charAt(3));
//		result[2] = charToInt(line.charAt(2));
//		result[1] = charToInt(line.charAt(1));
//		result[0] = charToInt(line.charAt(0));
//		return result;
//	}

	private char checkCharOnlyInt(char c){
		if(c =='O' || c == 'o'){
			return '0';
		}else if(c == 'Z' || c == 'z'){
			return '2';
		}else if(c == 'i' || c == 'I'){
			return '1';
		}else{
			return c;
		}
	}

	private String checkTextOnlySex(String c){
		if(c.charAt(0) =='H' || c.charAt(0) == 'h'){
			return "M";
		}else if(c.charAt(0) == 'N' || c.charAt(0) == 'n'){
			return "M";
		}else{
			return c;
		}
	}

	private String checkTextOnlyInt(String lines){
		String result = "";
		for (int i = 0 ; i < lines.length() ; i++){
			if(lines.charAt(i) =='O' || lines.charAt(i) == 'o'){
				result += '0';
			}else if(lines.charAt(i) == 'Z' || lines.charAt(i) == 'z'){
				result += '2';
			}else if(lines.charAt(i) == 'i' || lines.charAt(i) == 'I'){
				result += '1';
			}else{
				result += lines.charAt(i);
			}
		}
		return result;
	}

	private String checkTextName(String lines){
		String result = "";
		for (int i = 0 ; i < lines.length() ; i++){
			if(lines.charAt(i) =='*'){
				result += 'A';
			}else{
				result += lines.charAt(i);
			}
		}
		return result;
	}

	private String checkTextPassportNumber(String lines){
		String result = "";
		for (int i = 0 ; i < lines.length() ; i++){
			if(lines.charAt(i) =='*'){
				result += 'A';
			}else{
				result += lines.charAt(i);
			}
		}
		return result;
	}

	private void MRZ_Type3_Show(String lines1, String lines2){
		warningTextView.setText( "" );
		stopRecognition();
		stableResultHasBeenReached = true;
		surfaceViewWithOverlay.setFillBackground( true );
		String getName = lines1.substring(5);
		String[] parts = getName.split("<");
		String name ="";
		String type = (lines1.substring(0,2)).replace("<", "");

		String errorCodeShow = "";

		if(lines1.charAt(0) == 'p' || lines1.charAt(0) == 'P'){
		}else{
			errorCodeShow += " 2 :";
		}


		for (int i = 1; parts.length > i; i++){
			if (parts[i] != " "){
				name += parts[i];
				name += " ";
			}
		}


		String	 birthDate = checkTextOnlyInt(lines2.substring(13, 19));
		String	 passportID = checkTextPassportNumber((lines2.substring(0, 9)));
		String	 expire = checkTextOnlyInt(lines2.substring(21, 27));

		Intent intent = new Intent(MainActivity.this, ResultActivity.class);
		intent.putExtra("type", type);
		intent.putExtra("name", checkTextName(name));
		intent.putExtra("surname", parts[0]);
		intent.putExtra("country", checkTextName(lines1.substring(2, 5).replace("<", "")));
		intent.putExtra("nationality", checkTextName(lines2.substring(10, 13).replace("<", "")));
		intent.putExtra("birthDate", birthDate);
		intent.putExtra("sex", checkTextOnlySex(lines2.substring(20, 21)));
		intent.putExtra("expiry", expire);
		intent.putExtra("passportNo", passportID.replace("<", ""));

		String errorCode = "";

		int	checkBD = sumCheckDigitForAll(birthDate);
		int	checkEXP = sumCheckDigitForAll(expire);
		int	checkPPID = sumCheckDigitForAll(passportID);
		String checkLine = lines2.substring(0, 10)+lines2.substring(13, 20)+lines2.substring(21, 43);
		int	checkAll = sumCheckDigitForAll(checkLine);
		int checkPersonal = sumCheckDigitForAll(lines2.substring(28, 42));

		if(charToInt(checkCharOnlyInt(lines2.charAt(9))) != (checkPPID%10) ){
			errorCode += "1";
			errorCodeShow += " 4 :";
		}else{
			errorCode += "0";
		}

		if( charToInt(checkCharOnlyInt(lines2.charAt(19))) != (checkBD%10) ){
			errorCode += "1";
			errorCodeShow += " 5 :";
		}else{
			errorCode += "0";
		}

		if( charToInt(checkCharOnlyInt(lines2.charAt(27))) != (checkEXP%10) ){
			errorCode += "1";
			errorCodeShow += " 6 :";
		}else{
			errorCode += "0";
		}
		if( charToInt(checkCharOnlyInt(lines2.charAt(43))) != (checkAll%10) ){
			errorCode += "1";
			errorCodeShow += " 8 :";
			intent.putExtra("errorStatus", "1");
		}else{
			errorCode += "0";
			intent.putExtra("errorStatus", "0");
		}

		if( charToInt(checkCharOnlyInt(lines2.charAt(42))) != (checkPersonal%10) ){
			//errorCode += "1";
			errorCodeShow += " 7 :";
		}else{
			errorCode += "0";
		}

		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		intent.putExtra("dateReceived", dateFormat.format(date));
		intent.putExtra("dateSend", dateFormat.format(date));
		String doctype = "3";
		intent.putExtra("doctype", doctype);
		intent.putExtra("errorCodeShow", errorCodeShow);
		intent.putExtra("line1", lines1);
		intent.putExtra("line2", lines2);
		intent.putExtra("error", errorCode);
		startActivity(intent);
	}

	private void MRZ_Type2_Show(String lines1, String lines2){
		warningTextView.setText( "" );
		stopRecognition();
		stableResultHasBeenReached = true;
		surfaceViewWithOverlay.setFillBackground( true );
		String getName = lines1.substring(5);
		String[] parts = getName.split("<");
		String name ="";
		String type = (lines1.substring(0,2)).replace("<", "");

		for (int i = 1; parts.length > i; i++){
			if (parts[i] != " "){
				name += parts[i];
				name += " ";
			}
		}

		String errorCodeShow = "";

		if(lines1.charAt(0) == 'A' || lines1.charAt(0) == 'C' || lines1.charAt(0) == 'I' || lines1.charAt(0) == 'a' || lines1.charAt(0) != 'c' || lines1.charAt(0) == 'i'){

		}else{
			errorCodeShow += " 2 :";
		}

		String	 birthDate = checkTextOnlyInt(lines2.substring(13, 19));
		String	 passportID = checkTextPassportNumber((lines2.substring(0, 9)));
		String	 expire = checkTextOnlyInt(lines2.substring(21, 27));

		Intent intent = new Intent(MainActivity.this, ResultActivity.class);
		intent.putExtra("type", type);
		intent.putExtra("name", checkTextName(name));
		intent.putExtra("surname", parts[0]);
		intent.putExtra("country", checkTextName(lines1.substring(2, 5).replace("<", "")));
		intent.putExtra("nationality", checkTextName(lines2.substring(10, 13).replace("<", "")));
		intent.putExtra("birthDate", birthDate);
		intent.putExtra("sex", checkTextOnlySex(lines2.substring(20,21)));
		intent.putExtra("expiry", expire);
		intent.putExtra("passportNo", passportID.replace("<", ""));

		String errorCode = "";

		int	checkBD = sumCheckDigitForAll(birthDate);
		int	checkEXP = sumCheckDigitForAll(expire);
		int	checkPPID = sumCheckDigitForAll(passportID);
		String checkLine = lines2.substring(0, 10)+lines2.substring(13, 20)+lines2.substring(21, 35);
		int	checkAll = sumCheckDigitForAll(checkLine);

		if(charToInt(checkCharOnlyInt(lines2.charAt(9))) != (checkPPID%10) ){
			errorCode += "1";
			errorCodeShow += " 4 :";
		}else{
			errorCode += "0";
		}

		if( charToInt(checkCharOnlyInt(lines2.charAt(19))) != (checkBD%10) ){
			errorCode += "1";
			errorCodeShow += " 5 :";
		}else{
			errorCode += "0";
		}

		if( charToInt(checkCharOnlyInt(lines2.charAt(27))) != (checkEXP%10) ){
			errorCode += "1";
			errorCodeShow += " 6 :";
		}else{
			errorCode += "0";
		}

		if( charToInt(checkCharOnlyInt(lines2.charAt(35))) != (checkAll%10) ){
			errorCode += "1";
			errorCodeShow += " 8 :";
			intent.putExtra("errorStatus", "1");
		}else{
			errorCode += "0";
			intent.putExtra("errorStatus", "0");
		}
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		intent.putExtra("dateReceived", dateFormat.format(date));
		intent.putExtra("dateSend", dateFormat.format(date));
		String doctype = "2";
		intent.putExtra("doctype", doctype);
		intent.putExtra("errorCodeShow", errorCodeShow);
		intent.putExtra("line1", lines1);
		intent.putExtra("line2", lines2);
		intent.putExtra("error", errorCode);
		startActivity(intent);
	}

	private void MRZ_Type1_Show(String lines1, String lines2, String lines3){
		warningTextView.setText( "" );
		stopRecognition();
		stableResultHasBeenReached = true;
		surfaceViewWithOverlay.setFillBackground( true );
		String getName = lines3;
		String[] parts = getName.split("<");
		String name ="";
		String type = (lines1.substring(0,2)).replace("<", "");

		for (int i = 1; parts.length > i; i++){
			if (parts[i] != " "){
				name += parts[i];
				name += " ";
			}
		}

		String errorCodeShow = "";

		if(lines1.charAt(0) == 'A' || lines1.charAt(0) == 'C' || lines1.charAt(0) == 'I' || lines1.charAt(0) == 'a' || lines1.charAt(0) != 'c' || lines1.charAt(0) == 'i'){

		}else{
			errorCodeShow += " 2 :";
		}

		String birthDate = checkTextOnlyInt(lines2.substring(0, 6));
		String passportID = checkTextPassportNumber(lines1.substring(5, 14));
		String expire = checkTextOnlyInt(lines2.substring(8, 14));

		Intent intent = new Intent(MainActivity.this, ResultActivity.class);
		intent.putExtra("type", type);
		intent.putExtra("name", checkTextName(name));
		intent.putExtra("surname", parts[0]);
		intent.putExtra("country", checkTextName(lines1.substring(2,5).replace("<", "")));
		intent.putExtra("nationality", checkTextName(lines2.substring(15, 18).replace("<", "")));
		intent.putExtra("birthDate", birthDate);
		intent.putExtra("sex", checkTextOnlySex(lines2.substring(7,8)));
		intent.putExtra("expiry", expire);
		intent.putExtra("passportNo", passportID.replace("<", ""));

		String errorCode = "";
		int checkBD = sumCheckDigitForAll(birthDate);
		int checkEXP = sumCheckDigitForAll(expire);
		int checkPPID = sumCheckDigitForAll(passportID);
		String checkLine = lines1.substring(5, 30)+lines2.substring(0, 7)+lines2.substring(8, 15)+lines2.substring(18, 29);
		int	checkAll = sumCheckDigitForAll(checkLine);


		if( charToInt(checkCharOnlyInt(lines1.charAt(14))) != (checkPPID%10) ){
			errorCode += "1";
			errorCodeShow += " 4 :";
		}else{
			errorCode += "0";
		}

		if( charToInt(checkCharOnlyInt(lines2.charAt(6))) != (checkBD%10) ){
			errorCode += "1";
			errorCodeShow += " 5 :";
		}else{
			errorCode += "0";
		}
		if( charToInt(checkCharOnlyInt(lines2.charAt(14))) != (checkEXP%10) ){
			errorCode += "1";
			errorCodeShow += " 6 :";
		}else{
			errorCode += "0";
		}

		if( charToInt(checkCharOnlyInt(lines2.charAt(29))) != (checkAll%10) ){
			errorCode += "1";
			errorCodeShow += " 8 :";
			intent.putExtra("errorStatus", "1");
		}else {
            errorCode += "0";
			intent.putExtra("errorStatus", "0");
        }

		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		intent.putExtra("dateReceived", dateFormat.format(date));
		intent.putExtra("dateSend", dateFormat.format(date));
		String doctype = "1";
		intent.putExtra("doctype", doctype);
		intent.putExtra("errorCodeShow", errorCodeShow);
		intent.putExtra("error", errorCode);
        intent.putExtra("line1", lines1);
        intent.putExtra("line2", lines2);
        intent.putExtra("line3", lines3);
		startActivity(intent);
	}


	// This callback will be used to obtain frames from the camera
	private Camera.PreviewCallback cameraPreviewCallback = new Camera.PreviewCallback() {
		@Override
		public void onPreviewFrame( byte[] data, Camera camera )
		{
			// The buffer that we have given to the camera in ITextCaptureService.Callback.onRequestLatestFrame
			// above have been filled. Send it back to the Text Capture Service
			textCaptureService.submitRequestedFrame( data );
		}
	};

	// This callback is used to configure preview surface for the camera
	SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {

		@Override
		public void surfaceCreated( SurfaceHolder holder )
		{
			// When surface is created, store the holder
			previewSurfaceHolder = holder;
		}

		@Override
		public void surfaceChanged( SurfaceHolder holder, int format, int width, int height )
		{
			// When surface is changed (or created), attach it to the camera, configure camera and start preview
			if( camera != null ) {
				setCameraPreviewDisplayAndStartPreview();
			}
		}

		@Override
		public void surfaceDestroyed( SurfaceHolder holder )
		{
			// When surface is destroyed, clear previewSurfaceHolder
			previewSurfaceHolder = null;
		}
	};

	// Start recognition when autofocus completes (used when continuous autofocus is not enabled)
	private Camera.AutoFocusCallback startRecognitionCameraAutoFocusCallback = new Camera.AutoFocusCallback() {
		@Override
		public void onAutoFocus( boolean success, Camera camera )
		{
			onAutoFocusFinished( success, camera );
			startRecognition();
		}
	};

	// Simple autofocus callback
	private Camera.AutoFocusCallback simpleCameraAutoFocusCallback = new Camera.AutoFocusCallback() {
		@Override
		public void onAutoFocus( boolean success, Camera camera )
		{
			onAutoFocusFinished( success, camera );
		}
	};

	// Enable 'Start' button and switching to continuous focus mode (if possible) when autofocus completes
	private Camera.AutoFocusCallback finishCameraInitialisationAutoFocusCallback = new Camera.AutoFocusCallback() {
		@Override
		public void onAutoFocus( boolean success, Camera camera )
		{
			onAutoFocusFinished( success, camera );
			startButton.setText( BUTTON_TEXT_START );
			startButton.setEnabled( true );
			if( startRecognitionWhenReady ) {
				startRecognition();
				startRecognitionWhenReady = false;
			}
		}
	};

	// Autofocus by tap
	private View.OnClickListener clickListener = new View.OnClickListener() {
		@Override public void onClick( View v )
		{
			// if BUTTON_TEXT_STARTING autofocus is already in progress, it is incorrect to interrupt it
			if( !startButton.getText().equals( BUTTON_TEXT_STARTING ) ) {
				autoFocus( simpleCameraAutoFocusCallback );
			}
		}
	};

	private void onAutoFocusFinished( boolean success, Camera camera )
	{
		if( isContinuousVideoFocusModeEnabled( camera ) ) {
			setCameraFocusMode( Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO );
		} else {
			if( !success ) {
				autoFocus( simpleCameraAutoFocusCallback );
			}
		}
	}

	// Start autofocus (used when continuous autofocus is disabled)
	private void autoFocus( Camera.AutoFocusCallback callback )
	{
		if( camera != null ) {
			try {
				setCameraFocusMode( Camera.Parameters.FOCUS_MODE_AUTO );
				camera.autoFocus( callback );
			} catch( Exception e ) {
				Log.e( getString( R.string.app_name ), "Error: " + e.getMessage() );
			}
		}
	}

	// Checks that FOCUS_MODE_CONTINUOUS_VIDEO supported
	private boolean isContinuousVideoFocusModeEnabled( Camera camera )
	{
		return camera.getParameters().getSupportedFocusModes().contains( Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO );
	}

	// Sets camera focus mode and focus area
	private void setCameraFocusMode( String mode )
	{
		// Camera sees it as rotated 90 degrees, so there's some confusion with what is width and what is height)
		int width = 0;
		int height = 0;
		int halfCoordinates = 1000;
		int lengthCoordinates = 2000;
		Rect area = surfaceViewWithOverlay.getAreaOfInterest();
		switch( orientation ) {
			case 0:
			case 180:
				height = cameraPreviewSize.height;
				width = cameraPreviewSize.width;
				break;
			case 90:
			case 270:
				width = cameraPreviewSize.height;
				height = cameraPreviewSize.width;
				break;
		}

		camera.cancelAutoFocus();
		Camera.Parameters parameters = camera.getParameters();
		// Set focus and metering area equal to the area of interest. This action is essential because by defaults camera
		// focuses on the center of the frame, while the area of interest in this sample application is at the top
		List<Camera.Area> focusAreas = new ArrayList<>();
		Rect areasRect;

		switch( orientation ) {
			case 0:
				areasRect = new Rect(
					-halfCoordinates + area.left * lengthCoordinates / width,
					-halfCoordinates + area.top * lengthCoordinates / height,
					-halfCoordinates + lengthCoordinates * area.right / width,
					-halfCoordinates + lengthCoordinates * area.bottom / height
				);
				break;
			case 180:
				areasRect = new Rect(
					halfCoordinates - area.right * lengthCoordinates / width,
					halfCoordinates - area.bottom * lengthCoordinates / height,
					halfCoordinates - lengthCoordinates * area.left / width,
					halfCoordinates - lengthCoordinates * area.top / height
				);
				break;
			case 90:
				areasRect = new Rect(
					-halfCoordinates + area.top * lengthCoordinates / height,
					halfCoordinates - area.right * lengthCoordinates / width,
					-halfCoordinates + lengthCoordinates * area.bottom / height,
					halfCoordinates - lengthCoordinates * area.left / width
				);
				break;
			case 270:
				areasRect = new Rect(
					halfCoordinates - area.bottom * lengthCoordinates / height,
					-halfCoordinates + area.left * lengthCoordinates / width,
					halfCoordinates - lengthCoordinates * area.top / height,
					-halfCoordinates + lengthCoordinates * area.right / width
				);
				break;
			default:
				throw new IllegalArgumentException();
		}

		focusAreas.add( new Camera.Area( areasRect, 800 ) );
		if( parameters.getMaxNumFocusAreas() >= focusAreas.size() ) {
			parameters.setFocusAreas( focusAreas );
		}
		if( parameters.getMaxNumMeteringAreas() >= focusAreas.size() ) {
			parameters.setMeteringAreas( focusAreas );
		}

		parameters.setFocusMode( mode );

		// Commit the camera parameters
		camera.setParameters( parameters );
	}

	// Attach the camera to the surface holder, configure the camera and start preview
	private void setCameraPreviewDisplayAndStartPreview()
	{
		try {
			camera.setPreviewDisplay( previewSurfaceHolder );
		} catch( Throwable t ) {
			Log.e( getString( R.string.app_name ), "Exception in setPreviewDisplay()", t );
		}
		configureCameraAndStartPreview( camera );
	}

	// Stop preview and release the camera
	private void stopPreviewAndReleaseCamera()
	{
		if( camera != null ) {
			camera.setPreviewCallbackWithBuffer( null );
			stopPreview();
			camera.release();
			camera = null;
		}
	}

	// Stop preview if it is running
	private void stopPreview()
	{
		if( inPreview ) {
			camera.stopPreview();
			inPreview = false;
		}
	}

	// Show error on startup if any
	private void showStartupError( String message )
	{
		new AlertDialog.Builder( this )
			.setTitle( "ABBYY RTR SDK" )
			.setMessage( message )
			.setIcon( android.R.drawable.ic_dialog_alert )
			.show()
			.setOnDismissListener( new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss( DialogInterface dialog )
				{
					MainActivity.this.finish();
				}
			} );
	}

	// Load ABBYY RTR SDK engine and configure the text capture service
	private boolean createTextCaptureService()
	{
		// Initialize the engine and text capture service
		try {
			engine = Engine.load( this, licenseFileName );
			textCaptureService = engine.createTextCaptureService( textCaptureCallback );

			return true;
		} catch( java.io.IOException e ) {
			// Troubleshooting for the developer
			Log.e( getString( R.string.app_name ), "Error loading ABBYY RTR SDK:", e );
			showStartupError( "Could not load some required resource files. Make sure to configure " +
				"'assets' directory in your application and specify correct 'license file name'. See logcat for details." );
		} catch( Engine.LicenseException e ) {
			// Troubleshooting for the developer
			Log.e( getString( R.string.app_name ), "Error loading ABBYY RTR SDK:", e );
			showStartupError( "License not valid. Make sure you have a valid license file in the " +
				"'assets' directory and specify correct 'license file name' and 'application id'. See logcat for details." );
		} catch( Throwable e ) {
			// Troubleshooting for the developer
			Log.e( getString( R.string.app_name ), "Error loading ABBYY RTR SDK:", e );
			showStartupError( "Unspecified error while loading the engine. See logcat for details." );
		}

		return false;
	}

	// Start recognition
	private void startRecognition()
	{
		// Do not switch off the screen while text capture service is running
		previewSurfaceHolder.setKeepScreenOn( true );
		// Get area of interest (in coordinates of preview frames)
		Rect areaOfInterest = new Rect( surfaceViewWithOverlay.getAreaOfInterest() );
		// Clear error message
		errorTextView.setText( "" );
		// Start the service
		textCaptureService.start( cameraPreviewSize.width, cameraPreviewSize.height, orientation, areaOfInterest );
		// Change the text on the start button to 'Stop'
		startButton.setText( BUTTON_TEXT_STOP );
		startButton.setEnabled( true );
	}

	// Stop recognition
	void stopRecognition()
	{
		// Disable the 'Stop' button
		startButton.setEnabled( false );

		// Stop the service asynchronously to make application more responsive. Stopping can take some time
		// waiting for all processing threads to stop
		new AsyncTask<Void, Void, Void>() {
			protected Void doInBackground( Void... params )
			{
				textCaptureService.stop();
				return null;
			}

			protected void onPostExecute( Void result )
			{
				if( previewSurfaceHolder != null ) {
					// Restore normal power saving behaviour
					previewSurfaceHolder.setKeepScreenOn( false );
				}
				// Change the text on the stop button back to 'Start'
				startButton.setText( BUTTON_TEXT_START );
				startButton.setEnabled( true );
			}
		}.execute();
	}

	// Clear recognition results
	void clearRecognitionResults()
	{
		stableResultHasBeenReached = false;
		surfaceViewWithOverlay.setLines( null, ITextCaptureService.ResultStabilityStatus.NotReady );
		surfaceViewWithOverlay.setFillBackground( false );
	}

	// Returns orientation of camera
	private int getCameraOrientation()
	{
		Display display = getWindowManager().getDefaultDisplay();
		int orientation = 0;
		switch( display.getRotation() ) {
			case Surface.ROTATION_0:
				orientation = 0;
				break;
			case Surface.ROTATION_90:
				orientation = 90;
				break;
			case Surface.ROTATION_180:
				orientation = 180;
				break;
			case Surface.ROTATION_270:
				orientation = 270;
				break;
		}
		for( int i = 0; i < Camera.getNumberOfCameras(); i++ ) {
			Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
			Camera.getCameraInfo( i, cameraInfo );
			if( cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK ) {
				return ( cameraInfo.orientation - orientation + 360 ) % 360;
			}
		}
		// If Camera.open() succeed, this point of code never reached
		return -1;
	}

	private void configureCameraAndStartPreview( Camera camera )
	{
		// Setting camera parameters when preview is running can cause crashes on some android devices
		stopPreview();

		// Configure camera orientation. This is needed for both correct preview orientation
		// and recognition
		orientation = getCameraOrientation();
		camera.setDisplayOrientation( orientation );

		// Configure camera parameters
		Camera.Parameters parameters = camera.getParameters();

		// Select preview size. The preferred size for Text Capture scenario is 1080x720. In some scenarios you might
		// consider using higher resolution (small text, complex background) or lower resolution (better performance, less noise)
		cameraPreviewSize = null;
		for( Camera.Size size : parameters.getSupportedPreviewSizes() ) {
			if( size.height <= 720 || size.width <= 720 ) {
				if( cameraPreviewSize == null ) {
					cameraPreviewSize = size;
				} else {
					int resultArea = cameraPreviewSize.width * cameraPreviewSize.height;
					int newArea = size.width * size.height;
					if( newArea > resultArea ) {
						cameraPreviewSize = size;
					}
				}
			}
		}
		parameters.setPreviewSize( cameraPreviewSize.width, cameraPreviewSize.height );

		// Zoom
		parameters.setZoom( cameraZoom );
		// Buffer format. The only currently supported format is NV21
		parameters.setPreviewFormat( ImageFormat.NV21 );
		// Default focus mode
		parameters.setFocusMode( Camera.Parameters.FOCUS_MODE_AUTO );

		// Done
		camera.setParameters( parameters );

		// The camera will fill the buffers with image data and notify us through the callback.
		// The buffers will be sent to camera on requests from recognition service (see implementation
		// of ITextCaptureService.Callback.onRequestLatestFrame above)
		camera.setPreviewCallbackWithBuffer( cameraPreviewCallback );

		// Clear the previous recognition results if any
		clearRecognitionResults();

		// Width and height of the preview according to the current screen rotation
		int width = 0;
		int height = 0;
		switch( orientation ) {
			case 0:
			case 180:
				width = cameraPreviewSize.width;
				height = cameraPreviewSize.height;
				break;
			case 90:
			case 270:
				width = cameraPreviewSize.height;
				height = cameraPreviewSize.width;
				break;
		}

		// Configure the view scale and area of interest (camera sees it as rotated 90 degrees, so
		// there's some confusion with what is width and what is height)
		surfaceViewWithOverlay.setScaleX( surfaceViewWithOverlay.getWidth(), width );
		surfaceViewWithOverlay.setScaleY( surfaceViewWithOverlay.getHeight(), height );
		// Area of interest
//		int marginWidth = ( areaOfInterestMargin_PercentOfWidth * width ) / 100;
//		int marginHeight = ( areaOfInterestMargin_PercentOfHeight * height ) / 100;
//		surfaceViewWithOverlay.setAreaOfInterest(
//			new Rect( marginWidth, marginHeight - marginHeight / 4, width - marginWidth,
//				height - marginHeight / 2 ) );
        surfaceViewWithOverlay.setAreaOfInterest(
                new Rect( 0, 0, width, height ) );

		// Start preview
		camera.startPreview();

		setCameraFocusMode( Camera.Parameters.FOCUS_MODE_AUTO );
		autoFocus( finishCameraInitialisationAutoFocusCallback );

		inPreview = true;
	}

	// Initialize recognition language spinner in the UI with available languages
	private void initializeRecognitionLanguageSpinner()
	{
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences( this );
		final Spinner languageSpinner = (Spinner) findViewById( R.id.recognitionLanguageSpinner );

		// Make the collapsed spinner the size of the selected item
		ArrayAdapter<String> adapter = new ArrayAdapter<String>( MainActivity.this, R.layout.spinner_item ) {
			@Override
			public View getView( int position, View convertView, ViewGroup parent )
			{
				View view = super.getView( position, convertView, parent );
				RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
					RelativeLayout.LayoutParams.WRAP_CONTENT,
					RelativeLayout.LayoutParams.WRAP_CONTENT );
				view.setLayoutParams( params );
				return view;
			}
		};

		// Stored preference
		final String recognitionLanguageKey = "RecognitionLanguage";
		String selectedLanguage = preferences.getString( recognitionLanguageKey, "English" );

		// Fill the spinner with available languages selecting the previously chosen language
		int selectedIndex = -1;
		for( int i = 0; i < languages.length; i++ ) {
			String name = languages[i].name();
			adapter.add( name );
			if( name.equalsIgnoreCase( selectedLanguage ) ) {
				selectedIndex = i;
			}
		}
		if( selectedIndex == -1 ) {
			adapter.insert( selectedLanguage, 0 );
			selectedIndex = 0;
		}

		languageSpinner.setAdapter( adapter );

		if( selectedIndex != -1 ) {
			languageSpinner.setSelection( selectedIndex );
		}

		// The callback to be called when a language is selected
		languageSpinner.setOnItemSelectedListener( new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected( AdapterView<?> parent, View view, int position, long id )
			{
				String recognitionLanguage = (String) parent.getItemAtPosition( position );
				if( textCaptureService != null ) {
					// Reconfigure the recognition service each time a new language is selected
					// This is also called when the spinner is first shown
					textCaptureService.setRecognitionLanguage( Language.valueOf( recognitionLanguage ) );
					clearRecognitionResults();
				}
				if( !preferences.getString( recognitionLanguageKey, "" ).equalsIgnoreCase( recognitionLanguage ) ) {
					// Store the selection in preferences
					SharedPreferences.Editor editor = preferences.edit();
					editor.putString( recognitionLanguageKey, recognitionLanguage );
					editor.commit();
				}
			}

			@Override
			public void onNothingSelected( AdapterView<?> parent )
			{
			}
		} );
	}

	// The 'Start' and 'Stop' button
	public void onStartButtonClick( View view )
	{
		if( startButton.getText().equals( BUTTON_TEXT_STOP ) ) {
			stopRecognition();
		} else {
			clearRecognitionResults();
			startButton.setEnabled( false );
			startButton.setText( BUTTON_TEXT_STARTING );
			if( !isContinuousVideoFocusModeEnabled( camera ) ) {
				autoFocus( startRecognitionCameraAutoFocusCallback );
			} else {
				startRecognition();
			}
		}
	}

	@Override
	public void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );


		hmap.put('1', 1);
		hmap.put('2', 2);
		hmap.put('3', 3);
		hmap.put('4', 4);
		hmap.put('5', 5);
		hmap.put('6', 6);
		hmap.put('7', 7);
		hmap.put('8', 8);
		hmap.put('9', 9);
		hmap.put('A', 10);
		hmap.put('B', 11);
		hmap.put('C', 12);
		hmap.put('D', 13);
		hmap.put('E', 14);
		hmap.put('F', 15);
		hmap.put('G', 16);
		hmap.put('H', 17);
		hmap.put('I', 18);
		hmap.put('J', 19);
		hmap.put('K', 20);
		hmap.put('L', 21);
		hmap.put('M', 22);
		hmap.put('N', 23);
		hmap.put('O', 24);
		hmap.put('P', 25);
		hmap.put('Q', 26);
		hmap.put('R', 27);
		hmap.put('S', 28);
		hmap.put('T', 29);
		hmap.put('U', 30);
		hmap.put('V', 31);
		hmap.put('W', 32);
		hmap.put('X', 33);
		hmap.put('Y', 34);
		hmap.put('Z', 35);
		hmap.put('<', 0);

		setContentView( R.layout.activity_main );

		// Retrieve some ui components
		warningTextView = (TextView) findViewById( R.id.warningText );
		errorTextView = (TextView) findViewById( R.id.errorText );
		resultTextView = (TextView) findViewById( R.id.resultText );
		startButton = (Button) findViewById( R.id.startButton );

		// Initialize the recognition language spinner
		initializeRecognitionLanguageSpinner();

		// Manually create preview surface. The only reason for this is to
		// avoid making it public top level class
		RelativeLayout layout = (RelativeLayout) startButton.getParent();

		surfaceViewWithOverlay = new SurfaceViewWithOverlay( this );
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
			RelativeLayout.LayoutParams.MATCH_PARENT,
			RelativeLayout.LayoutParams.MATCH_PARENT );
		surfaceViewWithOverlay.setLayoutParams( params );
		// Add the surface to the layout as the bottom-most view filling the parent
		layout.addView( surfaceViewWithOverlay, 0 );

		// Create text capture service
		if( createTextCaptureService() ) {
			// Set the callback to be called when the preview surface is ready.
			// We specify it as the last step as a safeguard so that if there are problems
			// loading the engine the preview will never start and we will never attempt calling the service
			surfaceViewWithOverlay.getHolder().addCallback( surfaceCallback );
		}

		layout.setOnClickListener( clickListener );
	}

	@Override
	public void onResume()
	{
		super.onResume();
		// Reinitialize the camera, restart the preview and recognition if required
		startButton.setEnabled( false );
		clearRecognitionResults();
		startRecognitionWhenReady = startRecognitionOnAppStart;
		camera = Camera.open();
		if( previewSurfaceHolder != null ) {
			setCameraPreviewDisplayAndStartPreview();
		}
	}

	@Override
	public void onPause()
	{
		// Clear all pending actions
		handler.removeCallbacksAndMessages( null );
		// Stop the text capture service
		if( textCaptureService != null ) {
			textCaptureService.stop();
		}
		startButton.setText( BUTTON_TEXT_START );
		// Clear recognition results
		clearRecognitionResults();
		stopPreviewAndReleaseCamera();
		super.onPause();
	}

	// Surface View combined with an overlay showing recognition results and 'progress'
	static class SurfaceViewWithOverlay extends SurfaceView {
		private Point[] quads;
		private String[] lines;
		private Rect areaOfInterest;
		private int stability;
		private int scaleNominatorX = 1;
		private int scaleDenominatorX = 1;
		private int scaleNominatorY = 1;
		private int scaleDenominatorY = 1;
		private Paint textPaint;
		private Paint lineBoundariesPaint;
		private Paint backgroundPaint;
		private Paint areaOfInterestPaint;

		public SurfaceViewWithOverlay( Context context )
		{
			super( context );
			this.setWillNotDraw( false );

			lineBoundariesPaint = new Paint();
			lineBoundariesPaint.setStyle( Paint.Style.STROKE );
			lineBoundariesPaint.setARGB( 255, 128, 128, 128 );
			textPaint = new Paint();
			areaOfInterestPaint = new Paint();
			areaOfInterestPaint.setARGB( 100, 0, 0, 0 );
			areaOfInterestPaint.setStyle( Paint.Style.FILL );
		}

		public void setScaleX( int nominator, int denominator )
		{
			scaleNominatorX = nominator;
			scaleDenominatorX = denominator;
		}

		public void setScaleY( int nominator, int denominator )
		{
			scaleNominatorY = nominator;
			scaleDenominatorY = denominator;
		}

		public void setFillBackground( Boolean newValue )
		{
			if( newValue ) {
				backgroundPaint = new Paint();
				backgroundPaint.setStyle( Paint.Style.FILL );
				backgroundPaint.setARGB( 100, 255, 255, 255 );
			} else {
				backgroundPaint = null;
			}
			invalidate();
		}

		public void setAreaOfInterest( Rect newValue )
		{
			areaOfInterest = newValue;
			invalidate();
		}

		public Rect getAreaOfInterest()
		{
			return areaOfInterest;
		}

		public void setLines( ITextCaptureService.TextLine[] lines,
			ITextCaptureService.ResultStabilityStatus resultStatus )
		{
			if( lines != null && scaleDenominatorX > 0 && scaleDenominatorY > 0 ) {
				this.quads = new Point[lines.length * 4];
				this.lines = new String[lines.length];
				for( int i = 0; i < lines.length; i++ ) {
					ITextCaptureService.TextLine line = lines[i];
					for( int j = 0; j < 4; j++ ) {
						this.quads[4 * i + j] = new Point(
							( scaleNominatorX * line.Quadrangle[j].x ) / scaleDenominatorX,
							( scaleNominatorY * line.Quadrangle[j].y ) / scaleDenominatorY
						);
					}
					this.lines[i] = " ";
				}
				switch( resultStatus ) {
					case NotReady:
						textPaint.setARGB( 255, 128, 0, 0 );
						break;
					case Available:
						textPaint.setARGB( 255, 128, 128, 0 );
						break;
				}
				stability = resultStatus.ordinal();

			} else {
				stability = 0;
				this.lines = null;
				this.quads = null;
			}
			this.invalidate();
		}

		@Override
		protected void onDraw( Canvas canvas )
		{
			super.onDraw( canvas );
			int width = canvas.getWidth();
			int height = canvas.getHeight();
			canvas.save();
			// If there is any result
			if( lines != null ) {
				// Shade (whiten) the background when stable
				if( backgroundPaint != null ) {
					canvas.drawRect( 0, 0, width, height, backgroundPaint );
				}
			}
			if( areaOfInterest != null ) {
				// Shading and clipping the area of interest
				int left = ( areaOfInterest.left * scaleNominatorX ) / scaleDenominatorX;
				int right = ( areaOfInterest.right * scaleNominatorX ) / scaleDenominatorX;
				int top = ( areaOfInterest.top * scaleNominatorY ) / scaleDenominatorY;
				int bottom = ( areaOfInterest.bottom * scaleNominatorY ) / scaleDenominatorY;
				canvas.drawRect( 0, 0, width, top, areaOfInterestPaint );
				canvas.drawRect( 0, bottom, width, height, areaOfInterestPaint );
				canvas.drawRect( 0, top, left, bottom, areaOfInterestPaint );
				canvas.drawRect( right, top, width, bottom, areaOfInterestPaint );
				canvas.drawRect( left, top, right, bottom, lineBoundariesPaint );
				canvas.clipRect( left, top, right, bottom );
			}
			// If there is any result
			if( lines != null ) {
				// Draw the text lines
				for( int i = 0; i < lines.length; i++ ) {
					// The boundaries
					int j = 4 * i;
					Path path = new Path();
					Point p = quads[j + 0];
					path.moveTo( p.x, p.y );
					p = quads[j + 1];
					path.lineTo( p.x, p.y );
					p = quads[j + 2];
					path.lineTo( p.x, p.y );
					p = quads[j + 3];
					path.lineTo( p.x, p.y );
					path.close();
					canvas.drawPath( path, lineBoundariesPaint );

					// The skewed text (drawn by coordinate transform)
					canvas.save();
					Point p0 = quads[j + 0];
					Point p1 = quads[j + 1];
					Point p3 = quads[j + 3];

					int dx1 = p1.x - p0.x;
					int dy1 = p1.y - p0.y;
					int dx2 = p3.x - p0.x;
					int dy2 = p3.y - p0.y;

					int sqrLength1 = dx1 * dx1 + dy1 * dy1;
					int sqrLength2 = dx2 * dx2 + dy2 * dy2;

					double angle = 180 * Math.atan2( dy2, dx2 ) / Math.PI;
					double xskew = ( dx1 * dx2 + dy1 * dy2 ) / Math.sqrt( sqrLength2 );
					double yskew = Math.sqrt( sqrLength1 - xskew * xskew );

					textPaint.setTextSize( (float) yskew );
					String line = lines[i];
					Rect textBounds = new Rect();
					textPaint.getTextBounds( lines[i], 0, line.length(), textBounds );
					double xscale = Math.sqrt( sqrLength2 ) / textBounds.width();

					canvas.translate( p0.x, p0.y );
					canvas.rotate( (float) angle );
					canvas.skew( -(float) ( xskew / yskew ), 0.0f );
					canvas.scale( (float) xscale, 1.0f );

					canvas.drawText( lines[i], 0, 0, textPaint );
					canvas.restore();
				}
			}
			canvas.restore();
		}
	}
}