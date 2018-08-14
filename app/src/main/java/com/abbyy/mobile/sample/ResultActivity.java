package com.abbyy.mobile.sample;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.graphics.Color;
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
            birthDateView.setText("Date of Birth : " + birthDate.substring(4, 6) + " / " +
            birthDate.substring(2, 4) + " / 19" + birthDate.substring(0, 2));
            sexTextView.setText("Sex : " + sex);
            expiryDateView.setText("Expiry of Date : " + expiry.substring(4, 6) + " / " +
                    expiry.substring(2, 4) + " / 20" + expiry.substring(0, 2));
            line1.setText(line1S);
            line2.setText(line2S);
            line3.setText(line3S);        }
    }
}