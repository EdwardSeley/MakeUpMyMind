package com.example.seley.makeupmymind;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.ReceiverCallNotAllowedException;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transfermanager.Download;
import com.amazonaws.mobileconnectors.s3.transfermanager.TransferManager;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.iterable.S3Objects;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.util.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static android.R.attr.bitmap;
import static com.example.seley.makeupmymind.R.id.suggestion;



public class RecommendationsActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_TAKE_PHOTO = 1;
    private AmazonS3 s3;
    Uri photoURI;
    String imageFileName;
    String answers = "";
    public TextView suggestionTextView;
    public static String suggestionText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(uiOptions);
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
        setContentView(R.layout.activity_recommendations);
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        suggestionTextView = (TextView) findViewById(R.id.suggestion);
        Intent intent = getIntent();
        if (intent != null)
        {
            answers = intent.getStringExtra("answers");
        }
        dispatchTakePictureIntent();
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        imageFileName = answers + "_" + timeStamp + "_" + Build.SERIAL;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        //mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent()  {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                photoURI = Uri.fromFile(photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            File imageFile = new File(photoURI.getPath());
            uploadToAmazon(imageFile);
            try {
                retrieveFromAmazon();
                //writeToTextView(file);
            }
            catch (IOException ex)
            {}
        }
    }

    private void uploadToAmazon(final File imageFile) {

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try  {
                    BasicAWSCredentials credentials = new BasicAWSCredentials
                            ("AKIAINFEJ7ILDZ54XOOQ", "0q+Q3Ll2OSBjzPFG0dmJ4AfB0rmaB3QjseIOJjea");
                    s3 = new AmazonS3Client(credentials);

                    PutObjectRequest objectRequest = new PutObjectRequest(
                            "make-up-your-mind",
                            imageFileName + ".jpg",
                            imageFile
                    );
                    PutObjectResult result = s3.putObject(objectRequest);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();

    }

    private void retrieveFromAmazon() throws IOException {
        final Thread thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                final String filePath = "tmp/" + imageFileName + ".txt";
                final ProgressDialog dialog = ProgressDialog.show(RecommendationsActivity.this, "Loading", "Please wait...", true);
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        dialog.cancel();
                        DownloadImage image = new DownloadImage();
                        image.execute(filePath);
                        if (DownloadImage.fileExists)
                        {
                            suggestionTextView.setText(suggestionText);
                        }
                    }
                }, 25000);
            }

        });
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                final String filePath = "tmp/" + imageFileName + ".txt";
                DownloadImage image = new DownloadImage();
                image.execute(filePath);
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        DownloadImage image = new DownloadImage();
                        image.execute(filePath);
                    }
                }, 20000);   //20 seconds


            }
        });

        thread1.run();
        thread.run();
    }


}

class DownloadImage extends AsyncTask<String,String,String> {
    private final String accessKey="AKIAINFEJ7ILDZ54XOOQ";
    private final String secretKey="0q+Q3Ll2OSBjzPFG0dmJ4AfB0rmaB3QjseIOJjea";
    private final String bucketName="make-up-your-mind-response";
    private final String folderName="tmp";
    public String fileText;
    public static boolean fileExists = false;
    @Override
    protected String doInBackground(String... params) {

        String fileName = params[0];
        File file = new File(Environment.DIRECTORY_DOCUMENTS + fileName);
        AmazonS3Client s3Client = new AmazonS3Client(new BasicAWSCredentials(accessKey,secretKey));
        GetObjectRequest objectRequest;

        if (s3Client.doesObjectExist(bucketName, fileName))
        {
            fileExists = true;
            Log.d("HELP", "object exists");
            String accessKey="AKIAINFEJ7ILDZ54XOOQ";
            String secretKey="0q+Q3Ll2OSBjzPFG0dmJ4AfB0rmaB3QjseIOJjea";
            String bucketName="make-up-your-mind-response";
            s3Client = new AmazonS3Client(new BasicAWSCredentials(accessKey,secretKey));
            GetObjectRequest request = new GetObjectRequest(bucketName,
                    fileName);
            S3Object object = s3Client.getObject(request);
            InputStream in = object.getObjectContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            fileText = "";
            String text = "";
            try {
                while ((text = reader.readLine()) != null )
                    fileText = fileText + text + '\n';
            }
            catch (Exception ex){}
            Log.d("HELP", "Read Content: " + fileText);

            RecommendationsActivity.suggestionText = fileText;
            return "";

        }
        else
            Log.d("HELP", "Not found!");

        return "";

    }

    protected void onPostExecute(String feed) {

        return ;
    }
}
