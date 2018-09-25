package com.abbyy.mobile.sample;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.graphics.Color;
import java.util.Date;
import java.util.HashMap;

public class ResultActivity extends Activity {

    private TextView nameTextView;
    private TextView countryTextView;
    private TextView birthDateView;
    private TextView sexTextView;
    private TextView expiryDateView;
    private TextView passportNoView;
    private TextView NationalityView;
    private TextView passportTypeView;

    private TextView line1;
    private TextView line2;
    private TextView line3;

    private TextView dateReceived;
    private TextView dateSend;
    private TextView errorStatus;
    private TextView doctype;
    private TextView optionalData1;
    private TextView optionalData2;
    private TextView errorCodeShow;

    private TextView personalNo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        nameTextView = (TextView) findViewById( R.id.nameView );
        countryTextView = (TextView) findViewById( R.id.countryView );
        birthDateView = (TextView) findViewById( R.id.birthDateView );
        sexTextView = (TextView) findViewById( R.id.sexView );
        expiryDateView = (TextView) findViewById( R.id.expiryView );
        passportNoView = (TextView) findViewById( R.id.passportNo );
        NationalityView = (TextView) findViewById( R.id.NationalityView );
        passportTypeView = (TextView) findViewById( R.id.typeView );

        line1 = (TextView) findViewById( R.id.textLine1 );
        line2 = (TextView) findViewById( R.id.textLine2 );
        line3 = (TextView) findViewById( R.id.textLine3 );

        dateReceived = (TextView) findViewById( R.id.dateReceived );
        dateSend = (TextView) findViewById( R.id.dateSend );
        errorStatus = (TextView) findViewById( R.id.errorStatus );
        doctype = (TextView) findViewById( R.id.doctype );
        optionalData1 = (TextView) findViewById( R.id.optionalData1 );
        optionalData2 = (TextView) findViewById( R.id.optionalData2 );
        errorCodeShow = (TextView) findViewById( R.id.errorCodeShow );
        personalNo = (TextView) findViewById( R.id.personalNo );


        Button buttonBack = (Button) findViewById(R.id.button_back);

        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {

            String name = bundle.getString("name");
            String surname = bundle.getString("surname");
            String country = bundle.getString("country");
            String birthDate = bundle.getString("birthDate");
            String sex = bundle.getString("sex");
            String expiry = bundle.getString("expiry");
            String passportNo = bundle.getString("passportNo");
            String nationality = bundle.getString("nationality");
            String type = bundle.getString("type");

            String line1S = bundle.getString("line1");
            String line2S = bundle.getString("line2");
            String line3S = bundle.getString("line3");

            String dateReceivedS = bundle.getString("dateReceived");
            String dateSendS = bundle.getString("dateSend");
            String errorStatusS = bundle.getString("errorStatus");
            String doctypeS = bundle.getString("doctype");
            String optionalData1S = bundle.getString("optionalData1") ;
            String optionalData2S = bundle.getString("optionalData2");
            String errorCodeShowS =  bundle.getString("errorCodeShow");
            String personalNoS =  bundle.getString("personalNo");

            String errorCode = bundle.getString("error");
            if(errorCode.charAt(0) == '0' ){
                passportNoView.setBackgroundColor(Color.parseColor("#99cc00"));
            }else{
                passportNoView.setBackgroundColor(Color.parseColor("#FF0000"));
            }

            if(errorCode.charAt(1) == '0' ){
                birthDateView.setBackgroundColor(Color.parseColor("#99cc00"));
            }else{
                birthDateView.setBackgroundColor(Color.parseColor("#FF0000"));
            }

            if(errorCode.charAt(2) == '0' ){
                expiryDateView.setBackgroundColor(Color.parseColor("#99cc00"));
            }else{
                expiryDateView.setBackgroundColor(Color.parseColor("#FF0000"));
            }

            if(errorCode.charAt(3) != '0' ){
                line3.setBackgroundColor(Color.parseColor("#FF0000"));
                line3.setText("Check Digit Error On last : " + errorCode);
            }


            passportNoView.setText("Passport No. : " + passportNo);
            passportTypeView.setText("Type : " + type);
            nameTextView.setText("Name : " + name + "\nSurname : " + surname);
            countryTextView.setText("Country : " + country);
            NationalityView.setText("Nationality : " + nationality);
            birthDateView.setText("Date of Birth : " + birthDate.substring(0, 2) + " / " +
            birthDate.substring(2, 4) + " / " + birthDate.substring(4, 6));
            sexTextView.setText("Sex : " + sex);
            expiryDateView.setText("Expiry of Date : " + expiry.substring(0, 2) + " / " +
                    expiry.substring(2, 4) + " / " + expiry.substring(4, 6));
            line1.setText(line1S);
            line2.setText(line2S);
            line3.setText(line3S);

            personalNo.setText(personalNoS);
            dateReceived.setText("Date Time Recieve : " +dateReceivedS);
            dateSend.setText("Date Time Send : " +dateSendS);
            errorStatus.setText("Error Status : " +errorStatusS);
            doctype.setText("Doc Type :" +doctypeS);
            optionalData1.setText("Option 1 : " +optionalData1S);
            optionalData2.setText("Option 2 : " +optionalData2S);
            errorCodeShow.setText("Error Code : " + errorCodeShowS);
        }
    }
}