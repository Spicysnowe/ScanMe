package com.techtastic.scanme;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private static final String TAG= "MAIN_TAG";;
    TextInputEditText signUpUsername,signUpEmail,signUpPassword;
    Button signUpButton;
    TextView signInTV;
    FirebaseAuth fauth;
    FirebaseFirestore firebaseFirestore;
    String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        getSupportActionBar().hide();

        signUpUsername = findViewById(R.id.signUpUsername);
        signUpEmail = findViewById(R.id.signUpEmail);
        signUpPassword = findViewById(R.id.signUpPassword);
        signUpButton = findViewById(R.id.signUpButton);
        signInTV = findViewById(R.id.signInTV);

        fauth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        if(fauth.getCurrentUser() != null){
            startActivity(new Intent(getApplicationContext(),MainActivity.class));
            finish();
        }

        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = signUpEmail.getText().toString().trim();
                String password = signUpPassword.getText().toString().trim();
                String username= signUpUsername.getText().toString();

                if(TextUtils.isEmpty(email)){
                    signUpEmail.setError("Email is Required");
                    return;
                }
                if(TextUtils.isEmpty(password)){
                    signUpPassword.setError("Password is required");
                    return;
                }

                if (password.length()<8){
                    signUpPassword.setError("Password must be >=8 characters");
                    return;
                }

                //register the user in firebase

                fauth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){
                            Toast.makeText(SignUpActivity.this, "User registered successfully", Toast.LENGTH_SHORT).show();
                            userId=fauth.getCurrentUser().getUid();
                            DocumentReference documentReference =firebaseFirestore.collection("users").document(userId);
                            Map<String,Object> user = new HashMap<>();
                            user.put("username",username);
                            user.put("password", password);
                            user.put("scanned", new ArrayList<>());

                            documentReference.set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    Log.d(TAG, "onSuccess: user info is stored for user "+ userId);
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.d(TAG, "onFailure: "+ e.toString());
                                }
                            });


                            startActivity(new Intent(getApplicationContext(),MainActivity.class));

                        }else{
                            Toast.makeText(SignUpActivity.this, "Error! "+ task.getException().getMessage(), Toast.LENGTH_SHORT).show();


                        }
                    }
                });

            }
        });

        signInTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(),SigninActivity.class));
            }
        });

    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}