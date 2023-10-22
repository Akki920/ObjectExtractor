package com.example.app2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;


import com.example.app2.ml.LiteModelCartoonganDr1;


import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.model.Model;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class CartoonActivity extends AppCompatActivity {

    ImageView imgView;
    Button cpyButton;

    Bitmap cartoonizedImageBitmap;
    Bitmap bitmap;
    Bitmap outBitmap;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cartoon);

        cpyButton = (Button) findViewById(R.id.buttonCopy);
        imgView = (ImageView) findViewById(R.id.cartoonImage) ;



        String imageUriString = getIntent().getStringExtra("imgURI");
        if (imageUriString != null) {
            Uri imageUri = Uri.parse(imageUriString);
//            bitmap = Bitmap.createBitmap(320, 320, Bitmap.Config.ARGB_8888);
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Log.d("Cartoon","Cartoon Reached");
            int originalHeight = bitmap.getHeight();
            int originalWidth = bitmap.getWidth();

            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap,512,512,true);
            Bitmap xx = trans(scaledBitmap);
//            Mat img = new Mat();
//            Utils.bitmapToMat(scaledBitmap,img);
//            Imgproc.cvtColor(img,img,Imgproc.COLOR_RGBA2BGR);
//            Bitmap processedBitmap = Bitmap.createBitmap(512,512, Bitmap.Config.ARGB_8888);
//            Utils.matToBitmap(img,processedBitmap);
//            img.release();


            try {
                LiteModelCartoonganDr1 model = LiteModelCartoonganDr1.newInstance(this);

                // Creates inputs for reference.
                TensorImage sourceImage = TensorImage.fromBitmap(xx);

                // Runs model inference and gets result.
                LiteModelCartoonganDr1.Outputs outputs = model.process(sourceImage);
                TensorImage cartoonizedImage = outputs.getCartoonizedImageAsTensorImage();
                cartoonizedImageBitmap = cartoonizedImage.getBitmap();
                outBitmap = Bitmap.createScaledBitmap(cartoonizedImageBitmap,originalWidth,originalHeight,true );
                // Releases model resources if no longer used.
                model.close();
            } catch (Exception e) {
                // TODO Handle the exception
                e.printStackTrace();
                Log.e("Cartoon Model","Error");
            }


            Bitmap x =  trans(outBitmap);
            imgView.setImageBitmap(x);

            cpyButton.setOnClickListener(view -> {
//                ClipboardManager clipboard = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
//                ClipData clip = ClipData.newPlainText("Image", "Image from app"); // Plain text to provide a label
//
//                clip.addItem(new ClipData.Item(bitmapToString(outBitmap))); // Custom function to convert bitmap to string
//
//                clipboard.setPrimaryClip(clip);

                String path = MediaStore.Images.Media.insertImage(getContentResolver(), outBitmap, "Image from app", null);
                Uri uri = Uri.parse(path);

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("image/*");
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);

                startActivity(Intent.createChooser(shareIntent, "Share Image"));
            });
        }else{
            Log.e("String Error Uri","err");
        }


    }
    private String bitmapToString(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] b = baos.toByteArray();
        return Base64.encodeToString(b, Base64.DEFAULT);
    }
    private Bitmap trans(Bitmap inp){
        int backgroundColorToRemove = Color.rgb(15,19,21);

        Bitmap modifiedBitmap = inp.copy(Bitmap.Config.ARGB_8888, true);

        for (int x = 0; x < modifiedBitmap.getWidth(); x++) {
            for (int y = 0; y < modifiedBitmap.getHeight(); y++) {
                int pixelColor = modifiedBitmap.getPixel(x, y);
                if (pixelColor == backgroundColorToRemove) {
                    modifiedBitmap.setPixel(x, y, Color.TRANSPARENT); // or set to another color
                }
            }
        }
        return modifiedBitmap;
    }
}