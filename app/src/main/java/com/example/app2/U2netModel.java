package com.example.app2;

import static android.graphics.Bitmap.Config.ARGB_8888;
import static android.graphics.Bitmap.Config.RGB_565;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;
import android.content.res.AssetManager;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class U2netModel extends AsyncTask<Bitmap,Mat,Bitmap> {
    private ImageView imageView = Models.imageViewM;
    private Context context;
    Interpreter u2net_interpreter;
    private int HEIGHT = 320, WIDTH = 320;

    public U2netModel(Context context){
        this.context = context;
    }
    @SuppressLint("WrongThread")
    @Override
    protected Bitmap doInBackground(Bitmap... bitmaps) {
        Bitmap bitmap = bitmaps[0];
        if(OpenCVLoader.initDebug()) Log.d("Loaded OpenCV","sucess");
        else Log.d("Error Loading","err");

        Mat mat = new Mat();
        Utils.bitmapToMat(bitmap,mat);

        // converting RGB to gray
        Imgproc.cvtColor(mat,mat,Imgproc.COLOR_RGB2GRAY);

        Utils.matToBitmap(mat,bitmap);
        imageView.setImageBitmap(bitmap);

        ///////////////////////

        try {
            u2net_interpreter = new Interpreter(loadModelFile("u2netp_320x320.tflite"));
            u2net_interpreter.allocateTensors();
            Log.d("Interpreter","Loaded");
            Bitmap op = Bitmap.createBitmap(320,320,ARGB_8888);


            float[][][][] inputStyle = bitmapToFloatArray(bitmap);
            float[][][][] outputStyle = bitmapToFloatArray(bitmap);
            u2net_interpreter.run(inputStyle,outputStyle);
            Log.d("Interpreter","Output:)");






            // Get input and output details

//            Mat image_1 = new Mat();
//            Utils.bitmapToMat(bitmap,image_1);
//            image_1.convertTo(image_1, CvType.CV_32F, 1.0 / 255.0);
//            onProgressUpdate(image_1);
//
//            if (image_1.rows() > HEIGHT || image_1.cols() > WIDTH) {
//                Imgproc.resize(image_1, image_1, new Size(WIDTH, HEIGHT));
//            }
//
//            Core.transpose(image_1, image_1);



//            Mat processed_image_1 = new Mat();
//
//            float[][][][] inputTensor = new float[1][HEIGHT][WIDTH][3];
//            matToFloat(processed_image_1,inputTensor);
//            Mat resizedMat = new Mat();
//            Size targetSize = new Size(320, 320);
//            Imgproc.resize(image_1, resizedMat, targetSize);
//
//            Mat batchedMat = new Mat(1, 320, CvType.CV_8UC3, Scalar.all(0));
//            resizedMat.copyTo(batchedMat.row(0));

//            Log.d("Size",Integer.toString(batchedMat.channels()));
//            Log.d("I/P",Integer.toString(u2net_interpreter.getInputTensorCount()));


        } catch (IOException e) {
            Log.e("Interpreter","error");
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Mat... mats) {
        super.onProgressUpdate(mats);

        Log.d("Back Progress","Reached Here");

//        Bitmap bitmap = Bitmap.createBitmap(mats[0].width(), mats[0].height(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(mats[0],bitmap);
//        if(bitmap != null){
//            imageView.setImageBitmap(bitmap);
//            Log.d("ProgressBG","Changed Image");
//        }else{
//            Log.e("PrintImage","error");
//        }

    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        super.onPostExecute(bitmap);
        imageView.setImageBitmap(bitmap);
    }

    private ByteBuffer loadModelFile(String model_name) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(model_name);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
    public Mat uriToMat(Context context, Uri uri) {
        Mat mat = new Mat();
        ContentResolver contentResolver = context.getContentResolver();
        try {
            InputStream inputStream = contentResolver.openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            Utils.bitmapToMat(bitmap, mat);
            bitmap.recycle();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mat;
    }
    public static float[][][][] bitmapToFloatArray(Bitmap bitmap) {
        int width = 320;
        int height = 320;
        int[] intValues = new int[width * height];
        bitmap.getPixels(intValues, 0, width, 0, 0, width, height);

        float[][][][] fourDimensionalArray = new float[1][3][320][320];

        for (int i = 0; i < width - 1; i++) {
            for (int j = 0; j < height - 1; j++) {
                int pixelValue = intValues[i * width + j];
                fourDimensionalArray[0][0][i][j] = (float) Color.red(pixelValue);
                fourDimensionalArray[0][1][i][j] = (float) Color.green(pixelValue);
                fourDimensionalArray[0][2][i][j] = (float) Color.blue(pixelValue);
            }
        }

        List<Float> oneDFloatArray = new ArrayList<>();

        for (float[][][] arr3D : fourDimensionalArray) {
            for (float[][] arr2D : arr3D) {
                for (float[] arr1D : arr2D) {
                    for (float val : arr1D) {
                        oneDFloatArray.add(val);
                    }
                }
            }
        }

        float maxValue = Collections.max(oneDFloatArray);

        float[][][][] finalFourDimensionalArray = new float[1][3][320][320];

        for (int i = 0; i < width - 1; i++) {
            for (int j = 0; j < height - 1; j++) {
                int pixelValue = intValues[i * width + j];
                finalFourDimensionalArray[0][0][i][j] = ((float) Color.red(pixelValue) / maxValue - 0.485f) / 0.229f;
                finalFourDimensionalArray[0][1][i][j] = ((float)Color.green(pixelValue) / maxValue - 0.456f) / 0.224f;
                finalFourDimensionalArray[0][2][i][j] = ((float)Color.blue(pixelValue) / maxValue - 0.406f) / 0.225f;
            }
        }

        return finalFourDimensionalArray;
    }
    public static Bitmap[] convertArrayToBitmapTensorFlow(
            float[][][][] imageArray,
            int imageWidth,
            int imageHeight
    ) {
        Bitmap[] bitmaps = new Bitmap[2];

        Bitmap greyToneImage = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
        Bitmap blackToneImage = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);

        for (int x = 0; x < imageArray[0].length; x++) {
            for (int y = 0; y < imageArray[0][0].length; y++) {
                int color = Color.rgb(
                        (int)(imageArray[0][x][y][0] * 255f),
                        (int)(imageArray[0][x][y][0] * 255f),
                        (int)(imageArray[0][x][y][0] * 255f)
                );
                greyToneImage.setPixel(y, x, color);

                int blackToneColor = Color.rgb(
                        (int)(imageArray[0][x][y][0] * 255f),
                        (int)(imageArray[0][x][y][0] * 255f),
                        (int)(imageArray[0][x][y][0] * 255f)
                );
                blackToneImage.setPixel(y, x, blackToneColor);
            }
        }

        bitmaps[0] = greyToneImage;
        bitmaps[1] = blackToneImage;

        return bitmaps;
    }
}
