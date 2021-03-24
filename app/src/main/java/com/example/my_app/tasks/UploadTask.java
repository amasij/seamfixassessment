package com.example.my_app.tasks;

import android.app.ProgressDialog;
import android.os.AsyncTask;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.example.my_app.models.User;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;

public class UploadTask extends AsyncTask<User, Void, String> {
    private static final Logger logger = LoggerFactory.getLogger(AsyncTask.class);
    ProgressDialog dialog;

    public UploadTask(ProgressDialog dialog) {
        this.dialog = dialog;
    }

    @Override
    protected String doInBackground(User... params) {
        AWSCredentials credentials = new BasicAWSCredentials(
                System.getenv("AWS_ACCESS_KEY") == null ? "AKIAXG5LPP2P5SHVI35Z" : System.getenv("AWS_ACCESS_KEY"),
                System.getenv("AWS_SECRET_KEY") == null ? "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" : System.getenv("AWS_SECRET_KEY")
        );
        AmazonS3 s3client = new AmazonS3Client(credentials);

        String bucketName = System.getenv("AWS_BUCKET_NAME") == null ? "candidateassessmentseamfix" : System.getenv("AWS_BUCKET_NAME");

        if (!s3client.doesBucketExist(bucketName)) {

            logger.info("Bucket name is not available. Try again with a different Bucket name.");
            s3client.createBucket(bucketName);
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            byte[] bytesToWrite = objectMapper.writeValueAsBytes(params[0]);
            ObjectMetadata omd = new ObjectMetadata();
            omd.setContentLength(bytesToWrite.length);

            s3client.putObject(bucketName, params[0].getFolderName() + "/data.json", new ByteArrayInputStream(bytesToWrite), omd);

        } catch (Exception e) {
            System.out.println("An error occured ===>." + e.getMessage());
        }
        return "";
    }

    @Override
    protected void onPostExecute(String result) {
        logger.info(result);
        this.dialog.dismiss();
    }


}