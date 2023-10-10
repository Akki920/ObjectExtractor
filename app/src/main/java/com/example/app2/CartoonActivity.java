package com.example.app2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;

import com.example.app2.ml.Cartoon;
import com.example.app2.ml.WBCcartoon;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.image.TensorImage;

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
            Mat img = new Mat();
            Utils.bitmapToMat(scaledBitmap,img);
            Imgproc.cvtColor(img,img,Imgproc.COLOR_RGBA2BGR);
            Bitmap processedBitmap = Bitmap.createBitmap(512,512, Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(img,processedBitmap);
            img.release();


            try {
                WBCcartoon model = WBCcartoon.newInstance(CartoonActivity.this);

                // Creates inputs for reference.
                TensorImage sourceImage = TensorImage.fromBitmap(processedBitmap);

                // Runs model inference and gets result.
                WBCcartoon.Outputs outputs = model.process(sourceImage);
                TensorImage cartoonizedImage = outputs.getCartoonizedImageAsTensorImage();
                Bitmap cartoonizedImageBitmap = cartoonizedImage.getBitmap();

                // Releases model resources if no longer used.
                outBitmap = Bitmap.createScaledBitmap(cartoonizedImageBitmap,originalWidth,originalHeight,true);
                model.close();
            } catch (IOException e) {
                // TODO Handle the exception
                Log.e("Cartoon Model","Error");
            }
            imgView.setImageBitmap(outBitmap);
        }


    }
}