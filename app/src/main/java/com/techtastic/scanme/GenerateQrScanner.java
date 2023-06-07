package com.techtastic.scanme;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;

public class GenerateQrScanner extends AppCompatActivity {

    private TextView qrGenerateTextView;
    private ImageView qrGenerateImage;
    private Button qrGenBtn;
    private TextInputEditText qrGenEditText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_qr_scanner);
        qrGenerateTextView = findViewById(R.id.qrGenerateTextView);
        qrGenerateImage = findViewById(R.id.qrGenerateImage);
        qrGenBtn = findViewById(R.id.qrGenBtn);
        qrGenEditText = findViewById(R.id.qrGenEditText);

        qrGenBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String data = qrGenEditText.getText().toString();
                if(data.isEmpty()){
                    Toast.makeText(GenerateQrScanner.this, "Please enter some code to generate QR Code", Toast.LENGTH_SHORT).show();
                }
                else{
                    WindowManager manager = (WindowManager) getSystemService(WINDOW_SERVICE);
                    Display display = manager.getDefaultDisplay();
                    Point point= new Point();
                    display.getSize(point);
                    int width= point.x;
                    int height = point.y;
                    int dimen= width<height ? width: height;
                    dimen= dimen*3/4;


                }
            }
        });

    }
}