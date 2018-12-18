package com.example.android.photoblog;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import id.zelory.compressor.Compressor;

public class MakePost extends AppCompatActivity {

    private static final int MAX_LENGHT = 100;
    ImageView postImage;
    EditText postDecs;
    Button postButton;
    private Uri postImageUri = null;
    private StorageReference storageReference;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth mAuth;
    Bitmap compressor;
    Bitmap bitmaps;
    String current_user_id;
    String download_url;
    private static final int PICK_IMAGE_REQUEST = 342;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_make_post);

        storageReference = FirebaseStorage.getInstance().getReference();
        firebaseFirestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        postImage = (ImageView) findViewById(R.id.postImage);
        postDecs = (EditText) findViewById(R.id.describePost);
        postButton = (Button) findViewById(R.id.post);
        current_user_id = mAuth.getCurrentUser().getUid();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        postImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
           /* CropImage.activity()
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setMinCropResultSize(512,512)
                    .setAspectRatio(1,1)
                    .start(MakePost.this);*/

                Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, PICK_IMAGE_REQUEST);
            }
        });

        postButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String postdecs = postDecs.getText().toString();
                if (!TextUtils.isEmpty(postdecs) && postImageUri != null) {

                    Toast.makeText(MakePost.this, "post btn fired", Toast.LENGTH_SHORT).show();

                    final String randomName = UUID.randomUUID().toString();
                    final StorageReference filePath = storageReference.child("post_images").child(randomName + ".jpg");




                                File imageFile = new File(postImageUri.getPath());
                                try {
                                    bitmaps = new Compressor(MakePost.this)
                                            .setMaxHeight(180)
                                            .setMaxWidth(180)
                                            .setQuality(10)
                                            .compressToBitmap(imageFile);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                bitmaps.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                                byte[] thumbData = baos.toByteArray();


                                UploadTask uploadTask = storageReference.child("post_images").child(randomName + ".jpg").putBytes(thumbData);

                                Toast.makeText(MakePost.this, "uploadTask:"+ uploadTask.toString(), Toast.LENGTH_SHORT).show();

                                Task<Uri> urlTask = uploadTask.continueWithTask(
                                        new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                                    @Override
                                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {


                                        Toast.makeText(MakePost.this, "urlTask snapshot;" +task, Toast.LENGTH_SHORT).show();
                                        Log.d("urlTask snapShot", String.valueOf(task));

                                        return filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                            @Override
                                            public void onSuccess(Uri uri) {

                                                download_url = uri.toString();
                                                Log.d("downloadUrl", download_url);
                                                Toast.makeText(MakePost.this, "downloadUrl:"+download_url, Toast.LENGTH_SHORT).show();

                                                Map<String, Object> postMap = new HashMap<>();
                                                postMap.put("image_url", download_url);
                                                postMap.put("desc", postdecs);
                                                postMap.put("user_id", current_user_id);
                                                postMap.put("timeStamp", FieldValue.serverTimestamp().toString());

                                                firebaseFirestore.collection("Posts").add(postMap).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<DocumentReference> task) {

                                                        if (task.isSuccessful()) {
                                                            Toast.makeText(MakePost.this, "Post Added Successfully", Toast.LENGTH_SHORT).show();
                                                            Intent intent = new Intent(MakePost.this, MainActivity.class);
                                                            startActivity(intent);
                                                            finish();

                                                        } else {
                                                            String Error = task.getException().getMessage();
                                                            Toast.makeText(MakePost.this, "Error: " + Error, Toast.LENGTH_LONG).show();
                                                        }
                                                    }
                                                });
                                            }
                                        });
                                    }
                                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Uri> task) {


                                        Toast.makeText(MakePost.this, "urlTask snapshot;" +task, Toast.LENGTH_SHORT).show();
                                        Log.d("urlTask snapShot", String.valueOf(task));

                                    }
                                });

                                uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {




                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {


                                    }
                                });

                }
            }
        });

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            postImageUri = data.getData();


            String realPath = ImageFilePath.getPath(MakePost.this, data.getData());
          //  File imageFile = new File(Arrays.toString(realPath.getBytes()));
            byte[] byteArray = realPath.getBytes();
            Bitmap bitm = BitmapFactory.decodeByteArray(byteArray,0,byteArray.length);
            postImage.setImageBitmap(bitm);
//
            try {
                 bitmaps = MediaStore.Images.Media.getBitmap(getContentResolver(), postImageUri);

                postImage.setImageBitmap(bitmaps);
                postImage.setImageURI(postImageUri);
                postImage.setVisibility(View.VISIBLE);

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "Something Went Wrong", Toast.LENGTH_SHORT).show();
        }
        }
}


