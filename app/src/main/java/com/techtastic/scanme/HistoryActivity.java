package com.techtastic.scanme;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    FirebaseAuth fauth;
    FirebaseFirestore firebaseFirestore;
    String userId;
    DocumentReference documentReference;
    LinearLayout parentLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        getSupportActionBar().hide();

        parentLayout = findViewById(R.id.parentLayout);

        fauth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        userId=fauth.getCurrentUser().getUid();
        documentReference =firebaseFirestore.collection("users").document(userId);
        documentReference.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                if (value!=null && value.exists()){
                    List<String> dataArray = (List<String>) value.get("scanned");

                    // Clearing existing TextViews
                    parentLayout.removeAllViews();

                    // Create and populate TextViews for each array element
                    for (String element : dataArray) {

                        TextView textView = new TextView(getApplicationContext());
                        textView.setText(element);

                        textView.setTextColor(Color.WHITE);
                        textView.setTextSize(14);
                        textView.setPadding(30, 20, 20, 30);
                        textView.setBackgroundResource(R.drawable.iamgebtn_bg);

                        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        layoutParams.setMargins(0, 20, 0, 20); // Set the desired margin values in pixels


                        textView.setLayoutParams(layoutParams);

                        parentLayout.addView(textView);
                    }
                }
            }
        });
    }
}