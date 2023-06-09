package com.techtastic.scanme;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {




    private ImageButton cameraBtn, galleryBtn, scanbtn;
    private ImageView imageIv,scanline;
    private TextView resultTV,scanMeUsername;

    FirebaseAuth fauth;
    FirebaseFirestore firebaseFirestore;
    String userId;
    DocumentReference documentReference;


    //to handle result of Camera/Gallery permissions in OnRequestPermissionResults
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE =101;

    // arrays of permissions required to pick image from Gallery
    private String[] cameraPermissions;
    private String[] storagePermissions;

    //Uri of the image that we will take from Gallery/Camera
    private Uri imageUri = null;

    private BarcodeScannerOptions barcodeScannerOptions;
    private BarcodeScanner barcodeScanner;

    private static final String TAG= "MAIN_TAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();

        cameraBtn= findViewById(R.id.camerabtn);
        galleryBtn= findViewById(R.id.galleryBtn);
        imageIv = findViewById(R.id.imageiv);
        scanbtn = findViewById(R.id.scanBtn);
        resultTV = findViewById(R.id.resultTV);
        scanline = findViewById(R.id.scanline);
        scanMeUsername = findViewById(R.id.scanMeUsername);

        fauth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();


        //retrieving firestore data: username
        userId=fauth.getCurrentUser().getUid();
         documentReference =firebaseFirestore.collection("users").document(userId);
         documentReference.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
             @Override
             public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                 if(value!=null && value.exists()){
                     String name = value.getString("username");
                     String greeting = "Hi! " + name;
                     scanMeUsername.setText(greeting);
                 }


             }
         });


        //initialize the arrays of permissions required to pick image from gallery
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE };//image from camera: Camera and write_ext_storage permission
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};//image from gallery: write_ext_storage permission only


        barcodeScannerOptions = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                        .build();


        barcodeScanner = BarcodeScanning.getClient(barcodeScannerOptions);


        cameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (checkCameraPermission()){
                    imageIv.setVisibility(View.VISIBLE);
                    scanline.setVisibility(View.VISIBLE);
                    resultTV.setVisibility(View.GONE);
                    pickImageCamera();
                }
                else{
                    requestCameraPermission();
                }
            }
        });

        galleryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (checkStoragePermission()){
                    imageIv.setVisibility(View.VISIBLE);
                    scanline.setVisibility(View.VISIBLE);
                    resultTV.setVisibility(View.GONE);
                    pickImageGallery();
                }
                else{
                    requestStoragePermission();
                }

            }
        });

        scanbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (imageUri == null){
                    Toast.makeText(MainActivity.this, "Pick image first....", Toast.LENGTH_SHORT).show();
                }
                else{
                    detectResultFromImage();
                }

            }
        });

    }

    private void detectResultFromImage() {

        try{
            InputImage inputImage = InputImage.fromFilePath(this,imageUri);

            Task<List<Barcode>> barcodeResult = barcodeScanner.process(inputImage)
                    .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                        @Override
                        public void onSuccess(List<Barcode> barcodes) {
                            extractBarCodeQRCodeInfo(barcodes);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(MainActivity.this, "Failed scanning due to "+ e.getMessage(), Toast.LENGTH_SHORT).show();

                        }
                    });
        }
        catch (Exception e){
            Toast.makeText(this, "Failed due to "+e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void extractBarCodeQRCodeInfo(List<Barcode> barcodes) {

        for (Barcode barcode : barcodes){
            Rect bounds =  barcode.getBoundingBox();
            Point[] corners = barcode.getCornerPoints();


            String rawValue = barcode.getRawValue();
            Log.d(TAG,"extractBarCodeQRCodeInfo: rawValue"+ rawValue);

            imageIv.setVisibility(View.GONE);
            scanline.setVisibility(View.GONE);
            resultTV.setVisibility(View.VISIBLE);

            int valueType=barcode.getValueType();

            switch(valueType){
                case Barcode.TYPE_WIFI:{

                    Barcode.WiFi typeWifi = barcode.getWifi();

                    String ssid = ""+ typeWifi.getSsid();
                    String password = ""+ typeWifi.getPassword();
                    String encryptionType = ""+  typeWifi.getEncryptionType();

                    Log.d(TAG,"extractBarCodeQRCodeInfo: TYPE_WIFI: ");
                    Log.d(TAG,"extractBarCodeQRCodeInfo: ssid: "+ssid );
                    Log.d(TAG,"extractBarCodeQRCodeInfo: password: "+password );
                    Log.d(TAG,"extractBarCodeQRCodeInfo: encryptionType: "+encryptionType );

                    resultTV.setText("TYPE: TYPE_WIFI \nssid: "+ ssid+"\npassword: "+password+"\nencryptionType: "+encryptionType +"\nraw Value: "+rawValue);
                }
                break;
                case Barcode.TYPE_URL:{
                    Barcode.UrlBookmark typeUrl = barcode.getUrl();

                    String title = ""+  typeUrl.getTitle();
                    String url =  ""+ typeUrl.getUrl();

                    Log.d(TAG, "extractBarCodeQRCodeInfo: TYPE_URL: ");
                    Log.d(TAG, "extractBarCodeQRCodeInfo: title: "+title);
                    Log.d(TAG, "extractBarCodeQRCodeInfo: url: "+url);

                    resultTV.setText("TYPE: TYPE_URL \ntitle: "+ title+"\nurl: "+url+"\nraw Value: "+rawValue);
                }
                break;
                case Barcode.TYPE_EMAIL:{
                    Barcode.Email typeEmail = barcode.getEmail();

                    String title = ""+ typeEmail.getAddress();
                    String body = ""+ typeEmail.getBody();
                    String subject =  ""+ typeEmail.getSubject();

                    Log.d(TAG, "extractBarCodeQRCodeInfo: TYPE_EMAIL: ");
                    Log.d(TAG, "extractBarCodeQRCodeInfo: title: "+title);
                    Log.d(TAG, "extractBarCodeQRCodeInfo: body: "+body);
                    Log.d(TAG, "extractBarCodeQRCodeInfo: subject: "+subject);

                    resultTV.setText("TYPE: TYPE_EMAIL \ntitle: "+ title+"\nbody: "+body+"\nsubject: "+subject +"\nraw Value: "+rawValue);
                }
                break;
                case Barcode.TYPE_CONTACT_INFO:{
                    Barcode.ContactInfo typeContactInfo = barcode.getContactInfo();

                    String title = ""+ typeContactInfo.getTitle();
                    String organizer =  ""+ typeContactInfo.getOrganization();
                    String name = ""+ typeContactInfo.getName().getFirst()  +" "+ typeContactInfo.getName().getLast();
                    String phones = ""+ typeContactInfo.getPhones().get(0).getNumber();

                    Log.d(TAG, "extractBarCodeQRCodeInfo: TYPE_CONTACT_INFO: ");
                    Log.d(TAG, "extractBarCodeQRCodeInfo: title: "+title);
                    Log.d(TAG, "extractBarCodeQRCodeInfo: organizer: "+organizer);
                    Log.d(TAG, "extractBarCodeQRCodeInfo: name: "+name);
                    Log.d(TAG, "extractBarCodeQRCodeInfo: phones: "+phones);

                    resultTV.setText("TYPE: TYPE_CONTACT_INFO \ntitle: "+ title+"\norganizer: "+organizer+"\nname: "+name +"\nphones: "+phones+"\nraw Value: "+rawValue);

                }
                break;
                default:{
                    resultTV.setText("raw Value: "+rawValue);
                }
            }

           String newScannedData = resultTV.getText().toString();

            documentReference.get().addOnCompleteListener(task -> {

                if (task.isSuccessful()){
                    DocumentSnapshot document =task.getResult();
                    if (document.exists()){

                        // Retrieve the existing scanned data array
                        ArrayList<Object> previousData = (ArrayList<Object>) document.get("scanned");

                        // Add the new input to the scanned data array
                        previousData.add(newScannedData);

                        // Update the scanned data array in the Firestore document
                        documentReference.update("scanned",previousData)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Log.d(TAG, "onSuccess: Data stored Successfully: "+ newScannedData);
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.d(TAG, "onFailure: "+ e.toString());
                                    }
                                });
                    }
                }

            });





        }
    }

    private void pickImageGallery(){
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        galleryActivityResultLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> galleryActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {

                    if(result.getResultCode() == Activity.RESULT_OK){
                        Intent data = result.getData();
                        imageUri = data.getData();
                        Log.d(TAG,"onActivityResult: imageUri: " + imageUri);

                        imageIv.setImageURI(imageUri);

                    }
                    else{
                        Toast.makeText(MainActivity.this, "Cancelled", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private void pickImageCamera(){


        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, "Sample Title");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION,"Sample Image Description");

        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,contentValues);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        cameraActivityResultLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> cameraActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {


                    if(result.getResultCode() == Activity.RESULT_OK){
                        Intent data = result.getData();
                        Log.d(TAG, "onActivityResult: imageUri: "+imageUri);
                        imageIv.setImageURI(imageUri);

                    }
                    else{
                        Toast.makeText(MainActivity.this, "Cancelled....", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );


    private boolean checkStoragePermission(){
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
        return result;
    }

    private void requestStoragePermission (){
        ActivityCompat.requestPermissions(this,storagePermissions, STORAGE_REQUEST_CODE);
    }

    private  boolean checkCameraPermission(){
        boolean resultCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                ==PackageManager.PERMISSION_GRANTED;

        boolean resultStorage = ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)
                ==PackageManager.PERMISSION_GRANTED;

        return resultCamera && resultStorage;

    }


    private void requestCameraPermission(){
        ActivityCompat.requestPermissions(this,cameraPermissions,CAMERA_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case  CAMERA_REQUEST_CODE:{
                if (grantResults.length>0){
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if (cameraAccepted  && storageAccepted){
                        pickImageCamera();
                    }
                    else{
                        Toast.makeText(this, "Camera and Storage permissions are required...", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
            case  STORAGE_REQUEST_CODE:{

                if (grantResults.length>0){
                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if (storageAccepted){
                        pickImageGallery();
                    }
                    else {
                        Toast.makeText(this, "Storage Permission is required", Toast.LENGTH_SHORT).show();
                    }
                }

            }
            break;
        }
    }

    public void logout(View view) {
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(getApplicationContext(),SigninActivity.class));
        finish();
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    public void history(View view) {
        Intent intent = new Intent(this,HistoryActivity.class);
        startActivity(intent);
    }
}