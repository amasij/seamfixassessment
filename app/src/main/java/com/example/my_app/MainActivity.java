package com.example.my_app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.provider.Settings.Secure;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.my_app.Utils.Validator;
import com.example.my_app.enums.Gender;
import com.example.my_app.models.User;
import com.example.my_app.tasks.UploadTask;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static final Logger logger = LoggerFactory.getLogger(MainActivity.class);

    Button submitBtn, maleBtn, femaleBtn, uploadBtn, closeBtn;
    ProgressDialog dialog;
    Gender gender = Gender.MALE;
    ImageView imageView;
    FrameLayout avatarLayout;
    private FirebaseAuth auth;
    String currentPhotoPath;
    int MY_CAMERA_REQUEST_CODE = 100;
    int RESULT_GALLERY_IMAGE = 1;
    static final int REQUEST_IMAGE_CAPTURE = 2;
    TextView instruction;
    TextInputEditText firstNameInput, lastNameInput, phoneNumberInput, emailInput, passwordInput;
    byte[] currentImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        submitBtn = findViewById(R.id.submit);
        femaleBtn = findViewById(R.id.femaleBtn);
        uploadBtn = findViewById(R.id.uploadBtn);
        maleBtn = findViewById(R.id.maleBtn);
        imageView = findViewById(R.id.avatar);
        firstNameInput = findViewById(R.id.firstName);
        lastNameInput = findViewById(R.id.lastName);
        passwordInput = findViewById(R.id.password);
        emailInput = findViewById(R.id.email);
        phoneNumberInput = findViewById(R.id.phoneNumber);
        instruction = findViewById(R.id.instruction);
        avatarLayout = findViewById(R.id.avatarLayout);
        closeBtn = findViewById(R.id.closeBtn);
        auth = FirebaseAuth.getInstance();


        maleBtn.setOnClickListener((view) -> {
            gender = Gender.MALE;
            toggleButton(maleBtn, femaleBtn);
        });

        femaleBtn.setOnClickListener((view) -> {
            gender = Gender.FEMALE;
            toggleButton(femaleBtn, maleBtn);
        });

        uploadBtn.setOnClickListener((view) -> {
            selectImage();
        });

        closeBtn.setOnClickListener((view) -> {
            currentImage = null;
            uploadBtn.setVisibility(View.VISIBLE);
            avatarLayout.setVisibility(View.GONE);
            instruction.setText(R.string.no_image_instruction);
        });


        submitBtn.setOnClickListener((view) -> {
            String email = emailInput.getText().toString();
            String firstName = firstNameInput.getText().toString();
            String lastName = lastNameInput.getText().toString();
            String phoneNumber = phoneNumberInput.getText().toString();
            String password = passwordInput.getText().toString();

            if (Validator.isNotBlank(firstName, "First name", this) &&
                    Validator.isNotBlank(lastName, "Last name", this) &&
                    Validator.isValidPassword(password, this) &&
                    Validator.isValidEmail(email, this) &&
                    Validator.isValidPhoneNumber(phoneNumber, this) &&
                    Validator.isNotNull(currentImage, "Picture", this)
            ) {
                dialog = ProgressDialog.show(this, "",
                        "Submitting...", true);
                User user = new User();
                user.setBatteryLevel(getBatteryLevel(view.getContext()));
                user.setEmail(email);
                user.setGender(gender);
                user.setPhoneNumber(phoneNumber);
                user.setLastName(lastName);
                user.setFirstName(firstName);
                user.setFolderName(user.getFirstName() + "_" + user.getLastName());
                user.setDeviceId(Secure.getString(getContentResolver(), Secure.ANDROID_ID));
                user.setDateCreated(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                user.setImage(currentImage);
                user.setPassword(password);
                createFireBaseUser(user);
            }


        });

    }


    void createFireBaseUser(User user) {
        auth.createUserWithEmailAndPassword(user.getEmail(), user.getPassword())
                .addOnCompleteListener(this, (task) -> {

                    if (!task.isSuccessful()) {
                        dissmissDialog();
                        Validator.showMessage("Registration failed." + task.getException().getMessage(), this);
                    } else {
                        FirebaseUser firebaseUser = auth.getCurrentUser();
                        if (firebaseUser != null) {
                            user.setFirebaseUser(firebaseUser);
                            sendVerificationEmail(firebaseUser);
                        }
                        uploadToS3(user);

                    }
                });
    }

    void dissmissDialog() {
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    void sendVerificationEmail(FirebaseUser firebaseUser) {
        firebaseUser.sendEmailVerification()
                .addOnCompleteListener((task) -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "A verification email has been sent.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    void uploadToS3(User user) {
         new UploadTask(dialog).execute(user);


    }


    void toggleButton(Button on, Button off) {
        off.setBackgroundResource(R.drawable.button_unselected);
        off.setTextColor(0xFF000000);
        on.setBackgroundResource(R.drawable.button);
        on.setTextColor(getResources().getColor(R.color.colorWhite));
    }

    int getBatteryLevel(Context context) {
        if (Build.VERSION.SDK_INT >= 21) {

            BatteryManager bm = (BatteryManager) context.getSystemService(BATTERY_SERVICE);
            return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

        } else {

            IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, iFilter);

            int level = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
            int scale = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : -1;

            double batteryPct = level / (double) scale;

            return (int) (batteryPct * 100);
        }
    }

    private void selectImage() {
        final CharSequence[] options = {"Take Photo", "Choose from Gallery"};
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Upload Photo");
        builder.setItems(options,(dialog, item) -> {
                if (options[item].equals("Take Photo")) {
                    dispatchTakePictureIntent();
                }
                if (options[item].equals("Choose from Gallery")) {
                    Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(intent, RESULT_GALLERY_IMAGE);
                }
        });
        builder.show();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (resultCode == RESULT_OK) {

            if (requestCode == RESULT_GALLERY_IMAGE) {
                try {
                    final Uri imageUri = data.getData();
                    final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                    final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                    displayAndSaveImage(selectedImage);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Validator.showMessage("Something went wrong", this);
                }
            }

            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                galleryAddPic();
                scaleImage();
            }
        } else {
            Validator.showMessage("Unable to select picture", this);
        }
    }

    private void scaleImage() {
        int targetW = 280;
        int targetH = 280;

        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(currentPhotoPath, bmOptions);

        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        int scaleFactor = Math.max(1, Math.min(photoW / targetW, photoH / targetH));

        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath, bmOptions);
        displayAndSaveImage(bitmap);
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(currentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,".jpg",storageDir
        );
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void dispatchTakePictureIntent() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, MY_CAMERA_REQUEST_CODE);
            return;
        }
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,"com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private void displayAndSaveImage(Bitmap image) {
        uploadBtn.setVisibility(View.GONE);
        avatarLayout.setVisibility(View.VISIBLE);
        instruction.setText(R.string.image_instruction);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        currentImage = stream.toByteArray();
        imageView.setImageBitmap(image);
    }
}
