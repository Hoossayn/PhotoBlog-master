package com.example.android.photoblog;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;
import id.zelory.compressor.Compressor;

public class AccountSetup extends AppCompatActivity {

    private CircleImageView profileImage;
    private Uri profileImageUri = null;
    EditText setupUserName;
    Button setUpSave;
    private String user_id;
    private boolean isChanged = false;
    private StorageReference storageReference;
    private FirebaseAuth mAuth;
    private ProgressBar setUpProgressBar;
    private FirebaseFirestore firebaseFirestore;
    private Bitmap compressedImageFile;
    UploadTask imageStorage;
    //final StorageReference profilefilePath = null;
    String profilepicUrl;
    String userName;
    private static final int PICK_IMAGE_REQUEST = 111;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_setup);

        Toolbar setUpToolbar = findViewById(R.id.accountSettingsToolbar);
//        setSupportActionBar(setUpToolbar);
        getSupportActionBar().setTitle("Account Setup");

        profileImage = (CircleImageView) findViewById(R.id.profileimage);
        setupUserName = (EditText) findViewById(R.id.accoutnSetupUserName);
        setUpSave = (Button) findViewById(R.id.accountSetupsave);
        setUpProgressBar = findViewById(R.id.setUp_progressBar);
        mAuth = FirebaseAuth.getInstance();
        user_id = mAuth.getCurrentUser().getUid();


        storageReference = FirebaseStorage.getInstance().getReference();
        //storageReference = FirebaseStorage.getInstance().getReference().child("profile_images");
        firebaseFirestore = FirebaseFirestore.getInstance();


        firebaseFirestore.collection("Users").document(user_id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {

                if (task.isSuccessful()) {

                    if (task.getResult().exists()) {

                        String name = task.getResult().getString("name");
                        String image = task.getResult().getString("image");
                        setupUserName.setText(name);

                        profileImageUri = Uri.parse(image);
                      /*  RequestOptions placeHolderRequest = new RequestOptions();
                      //  placeHolderRequest.placeholder(R.drawable.com_facebook_profile_picture_blank_portrait);
                        Glide.with(AccountSetup.this).setDefaultRequestOptions(placeHolderRequest).load(image).into(profileImage);*/
                        Picasso.get().load(profilepicUrl).into(profileImage);
                    }
                } else {

                    String Error = task.getException().getMessage();
                    Toast.makeText(AccountSetup.this, "FireStore Error: " + Error, Toast.LENGTH_LONG).show();
                }
            }
        });


        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ContextCompat.checkSelfPermission(AccountSetup.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(AccountSetup.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                        return;
                    } else {

                        cropImage();

                    }
                } else {

                    cropImage();
                }
            }
        });

        setUpSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                userName = setupUserName.getText().toString();
                if (!TextUtils.isEmpty(userName) && profileImageUri != null) {
                    setUpProgressBar.setVisibility(View.VISIBLE);

                    Toast.makeText(AccountSetup.this, "save btn fired", Toast.LENGTH_SHORT).show();


                    if (isChanged) {

                        user_id = mAuth.getCurrentUser().getUid();
                        File newImageFile = new File(profileImageUri.getPath());
                        try {

                            compressedImageFile = new Compressor(AccountSetup.this)
                                    .setMaxHeight(125)
                                    .setMaxWidth(125)
                                    .setQuality(50)
                                    .compressToBitmap(newImageFile);

                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        compressedImageFile.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                        byte[] thumbData = baos.toByteArray();


                        imageStorage = storageReference.child("profile_images").child(user_id + ".jpg").putBytes(thumbData);

                        Toast.makeText(AccountSetup.this, "profile pics Upload" + imageStorage.toString(), Toast.LENGTH_SHORT).show();


                        final String randomName = UUID.randomUUID().toString();
                        final StorageReference profilefilePath = storageReference.child("profile_images").child(randomName + ".jpg");

                        Task<Uri> urlTask = imageStorage.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                            @Override
                            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {

                                Toast.makeText(AccountSetup.this, "send To FireStore is reached", Toast.LENGTH_SHORT).show();

                                return profilefilePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {


                                    @Override
                                    public void onSuccess(Uri uri) {

                                        Toast.makeText(AccountSetup.this, "send To FireStore onSuccess reached", Toast.LENGTH_SHORT).show();

                                        profilepicUrl = uri.toString();


                                        Map<String, String> userMap = new HashMap<>();
                                        userMap.put("name", userName);
                                        userMap.put("image", profilepicUrl);

                                        Toast.makeText(AccountSetup.this, "profilePic:" + profilepicUrl, Toast.LENGTH_LONG).show();

                                        firebaseFirestore.collection("Users").document(user_id).set(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {

                                                if (task.isSuccessful()) {

                                                    Intent intent = new Intent(AccountSetup.this, MainActivity.class);
                                                    startActivity(intent);
                                                    finish();

                                                } else {
                                                    String Error = task.getException().getMessage();
                                                    Toast.makeText(AccountSetup.this, "Error: " + Error, Toast.LENGTH_LONG).show();
                                                }

                                                setUpProgressBar.setVisibility(View.GONE);
                                            }
                                        });

                                    }
                                });

                            }


                        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                            @Override
                            public void onComplete(@NonNull Task<Uri> task) {

                                profilefilePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {

                                        String ss = uri.toString();
                                        Toast.makeText(AccountSetup.this, "new downloadUrl" + ss, Toast.LENGTH_LONG).show();
                                    }
                                });


                            }
                        });


                    } else {
                        //sendToFireStore();
                    }
                }

            }
        });
    }

/*
    private void sendToFireStore() {

        final String randomName = UUID.randomUUID().toString();
        final StorageReference profilefilePath = storageReference.child("profile_images").child(randomName + ".jpg");

        Task<Uri> urlTask = imageStorage.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                Toast.makeText(AccountSetup.this, "send To FireStore is reached", Toast.LENGTH_SHORT).show();

               *//* return profilefilePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {



                    @Override
                    public void onSuccess(Uri uri) {

                        Toast.makeText(AccountSetup.this, "send To FireStore onSuccess reached" ,Toast.LENGTH_SHORT).show();

                        profilepicUrl = uri.toString();


                        Map<String, String > userMap = new HashMap<>();
                        userMap.put("name",userName);
                        userMap.put("image", profilepicUrl);

                        Toast.makeText(AccountSetup.this, "profilePic:"+ profilepicUrl,Toast.LENGTH_LONG).show();

                        firebaseFirestore.collection("Users").document(user_id).set(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {

                                if(task.isSuccessful()){

                                    Intent intent = new Intent(AccountSetup.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();

                                }else{
                                    String Error = task.getException().getMessage();
                                    Toast.makeText(AccountSetup.this,"Error: "+ Error, Toast.LENGTH_LONG).show();
                                }

                                setUpProgressBar.setVisibility(View.GONE);
                            }
                        });

                    }
                });*//*

               *//*return profilefilePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                   @Override
                   public void onSuccess(Uri uri) {

                       profilepicUrl = uri.toString();
                       Log.d("downloadUrl", profilepicUrl);
                       Toast.makeText(AccountSetup.this, "downloadUrl:"+profilepicUrl, Toast.LENGTH_SHORT).show();

                       Map<String, String > userMap = new HashMap<>();
                       userMap.put("name",userName);
                       userMap.put("image", profilepicUrl);

                       firebaseFirestore.collection("Users").add(userMap).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                           @Override
                           public void onComplete(@NonNull Task<DocumentReference> task) {

                               if (task.isSuccessful()) {
                                   Toast.makeText(AccountSetup.this, "Post Added Successfully", Toast.LENGTH_SHORT).show();
                                   Intent intent = new Intent(AccountSetup.this, MainActivity.class);
                                   startActivity(intent);
                                   finish();

                               } else {
                                   String Error = task.getException().getMessage();
                                   Toast.makeText(AccountSetup.this, "Error: " + Error, Toast.LENGTH_LONG).show();
                               }
                           }
                       });
                   }
               });*//*
                return null;
            }


        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {

                profilefilePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {

                        String ss = uri.toString();
                        Toast.makeText(AccountSetup.this, "new downloadUrl" + ss, Toast.LENGTH_LONG).show();
                    }
                });


            }
        });


    }*/


    private void cropImage() {
     /*   CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setAspectRatio(1,1)
                .start(AccountSetup.this);*/

        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        //intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            profileImageUri = data.getData();

            try {
                compressedImageFile = MediaStore.Images.Media.getBitmap(getContentResolver(), profileImageUri);


                profileImageUri = data.getData();
                profileImage.setImageBitmap(compressedImageFile);
                profileImage.setImageURI(profileImageUri);
                isChanged = true;
            } catch (IOException e) {
                e.printStackTrace();
            }


        } else {
            Toast.makeText(this, "Something Went Wrong", Toast.LENGTH_SHORT).show();
        }
    }
}