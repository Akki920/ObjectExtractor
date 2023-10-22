package com.example.app2;

import static android.graphics.Bitmap.Config.ARGB_8888;
import static android.graphics.Bitmap.Config.RGBA_F16;
import static com.example.app2.U2netModel.bitmapToFloatArray;
import static org.opencv.core.Core.ROTATE_90_CLOCKWISE;
import static org.opencv.core.Core.multiply;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.model.Model;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
import com.example.app2.objectDetector;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Models extends AppCompatActivity {

    protected static ImageView imageViewM;
    private int HEIGHT = 320, WIDTH = 320;
    private Interpreter u2net_interpreter;
    Button cartoon;
    objectDetector obj;
    @SuppressLint("WrongThread")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_models);
        if(OpenCVLoader.initDebug()) Log.d("Loaded OpenCV","sucess");
        else Log.d("Error Loading","err");
        cartoon = (Button) findViewById(R.id.buttonCartoon);
        imageViewM = findViewById(R.id.imageViewModel);
        String imageUriString = getIntent().getStringExtra("imageUri");
        if (imageUriString != null) {
            Uri imageUri = Uri.parse(imageUriString);
            Bitmap bitmap ;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            int originalHeight = bitmap.getHeight();
            int originalWidth = bitmap.getWidth();

            Log.d("HEIGHT/WIDTH",Integer.toString(originalHeight)+Integer.toString(originalWidth));
            try {
                obj=new objectDetector(getAssets(),"yolo.tflite","label.txt",640);
                Log.d("MainActivity","Model is successfully loaded");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            try {
                u2net_interpreter = new Interpreter(loadModelFile("u2netp_320x320.tflite"));
                Log.d("U2net Interpreter","Loaded");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            u2net_interpreter.allocateTensors();
            Log.d("interpreter","ReachdeHere");

            /////
//            Mat mat_image = new Mat();
//            Utils.bitmapToMat(bitmap,mat_image);
//            Mat rotated_mat_image=new Mat();
//
//            Mat a = mat_image.t();
//            Core.flip(a,rotated_mat_image,1);
//            a.release();
//
//            Bitmap inp =null;
//            inp=Bitmap.createBitmap(rotated_mat_image.cols(),rotated_mat_image.rows(),Bitmap.Config.ARGB_8888);
//            Utils.matToBitmap(rotated_mat_image,inp);
//
//            int height=inp.getHeight();
//            int width=inp.getWidth();

//            Bitmap scaledBitmap=Bitmap.createScaledBitmap(inp,320,320,false);
            Bitmap scaledBitmap=Bitmap.createScaledBitmap(bitmap,320,320,false);
//            ByteBuffer byteBuffer= ByteBuffer.allocateDirect(4*320*320*3);
//            ByteBuffer outputBuffer = ByteBuffer.allocateDirect(409600);
//            scaledBitmap.copyPixelsToBuffer(byteBuffer);
//            byteBuffer.rewind();

//            imageViewM.setImageBitmap(scaledBitmap);

            float[][][][] inputStyle = bitmapToFloatArray(scaledBitmap);

            Log.d("inp Shape",Integer.toString(inputStyle[0].length));

// Create arrays with size 1,320,320,1
            float[][][][] output1 = new float[1][1][320][320];
            float[][][][] output2 = new float[1][1][320][320];
            float[][][][] output3 = new float[1][1][320][320];
            float[][][][] output4 = new float[1][1][320][320];
            float[][][][] output5 = new float[1][1][320][320];
            float[][][][] output6 = new float[1][1][320][320];

            Map<Integer, Object> outputs = new HashMap<>();
            outputs.put(0, output1);
            outputs.put(1, output2);
            outputs.put(2, output3);
            outputs.put(3, output4);
            outputs.put(4, output5);
            outputs.put(5, output6);

// Runs model inference and gets result.
            Object[] array = {inputStyle};
            u2net_interpreter.runForMultipleInputsOutputs(array, outputs);

            Object out = outputs.get(0);
            Bitmap op = convertArrayToBitmapTensorFlow(output1,320,320);


            Mat image = new Mat(320,320,CvType.CV_8UC4);
            Utils.bitmapToMat(scaledBitmap,image);

            Mat grayscaleImage = new Mat();
            Utils.bitmapToMat(op,grayscaleImage);
            Core.flip(grayscaleImage,grayscaleImage,0);
            Core.rotate(grayscaleImage,grayscaleImage,ROTATE_90_CLOCKWISE);
            Mat thresholdedImage = new Mat();
            Imgproc.threshold(grayscaleImage, thresholdedImage, 120, 255,Imgproc.THRESH_BINARY);
            Utils.matToBitmap(thresholdedImage,op);

            Mat background_mask = new Mat();
//            Core.bitwise_not(thresholdedImage, background_mask);
            Core.bitwise_not(grayscaleImage, background_mask);

            Mat background = new Mat(320,320, CvType.CV_8UC4, new Scalar(255, 255, 255));
            Mat foreground = new Mat(320,320, CvType.CV_8UC4);

            image.convertTo(image, CvType.CV_8UC4);
            Log.d("Debug Info", "Image Size: " + image.size().toString() + ", Type: " + image.type());
            Log.d("Debug Info", "Foreground Size: " + foreground.size().toString() + ", Type: " + foreground.type());
            Log.d("Debug Info", "Background Mask Size: " + background_mask.size().toString() + ", Type: " + background_mask.type());


            try {
                Core.bitwise_and(image, thresholdedImage, foreground);
                Core.bitwise_and(background, background_mask, background);
            } catch (Exception e) {
                Log.e("Bitwise Error", e.toString());
            }

            Mat result_image = new Mat();
            Core.add(foreground, background, result_image);

            Utils.matToBitmap(result_image,op);
            Bitmap x = trans(op);
//            imageViewM.setImageBitmap(op);

            Bitmap outBitmap = Bitmap.createScaledBitmap(x,originalWidth,originalHeight,false);
            imageViewM.setImageBitmap(outBitmap);
            u2net_interpreter.close();

//            Bitmap resultBitmap = Bitmap.createBitmap(result_image.cols(), result_image.rows(), Bitmap.Config.ARGB_8888);
//            Utils.matToBitmap(result_image, resultBitmap);
//            imageViewM.setImageBitmap(resultBitmap);


//            imageViewM.setImageBitmap(op);
//
//            Log.d("Output",Integer.toString(op.getHeight()));
            Log.d("HERE","REACHED");



            File file = saveBitmapToFile(outBitmap);
            Uri opUri = Uri.fromFile(file);
Log.d("Done","Done");
            cartoon.setOnClickListener(view -> {
                Intent intent = new Intent(Models.this, CartoonActivity.class);
                intent.putExtra("imgURI", opUri.toString());
                startActivity(intent);

            });


//            u2net_interpreter.run(byteBuffer, outputBuffer);
//            outputBuffer.rewind();
//            Bitmap op = Bitmap.createBitmap(320, 320, ARGB_8888);
//            op.copyPixelsFromBuffer(byteBuffer);
//            Log.d("Output Buffer",Integer.toString(outputBuffer.remaining()));
//            imageViewM.setImageBitmap(op);


            ////


/////////////////////

//            bitmap = Bitmap.createScaledBitmap(bitmap, 320, 320, false);
//            Bitmap normalizedBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), RGBA_F16);
//
//            for (int y = 0; y < bitmap.getHeight(); y++) {
//                for (int x = 0; x < bitmap.getWidth(); x++) {
//                    int pixel = bitmap.getPixel(x, y);
//                    float red = Color.red(pixel);
//                    float green = Color.green(pixel);
//                    float blue = Color.blue(pixel);
//
//                    // Normalize and set the pixel
//
//                    red = (float) (red / 255.0 );
//                    green = (float) (green / 255.0 );
//                    blue = (float) (blue / 255.0 );
//
//                    normalizedBitmap.setPixel(x, y, Color.rgb(red, green, blue));
//                }
//            }

//            ByteBuffer inputBuffer = ByteBuffer.allocateDirect(1228800); // Assuming 3 channels (RGB)
//            inputBuffer.order(ByteOrder.nativeOrder());
//            normalizedBitmap.copyPixelsToBuffer(inputBuffer);
//            Log.d("Output Mat","Reached Here 2");
//            imageViewM.setImageBitmap(bitmap);
//            ByteBuffer outputBuffer = ByteBuffer.allocateDirect(409600);
//
//            u2net_interpreter.run(inputBuffer, outputBuffer);
//
//            Bitmap op = Bitmap.createBitmap(320, 320, RGBA_F16);
//            op.copyPixelsFromBuffer(inputBuffer);
//            imageViewM.setImageBitmap(op);

//            float[][][] normalizedPixels = new float[320][320][4]; // Assuming 3 channels (RGB)
//
////            outputBuffer.rewind(); // Reset position to beginning
//
//            for (int x = 0; x < 320; x++) {
//                for (int y = 0; y < 320; y++) {
//                    float r = outputBuffer.getFloat();
//                    float g = outputBuffer.getFloat();
//                    float b = outputBuffer.getFloat();
//
//                    normalizedPixels[x][y][0] = r;
//                    normalizedPixels[x][y][1] = g;
//                    normalizedPixels[x][y][2] = b;
//                }
//            }
//
//            for (int x = 0; x < 320; x++) {
//                for (int y = 0; y < 320; y++) {
//                    int r = (int) (normalizedPixels[x][y][0] * 255);
//                    int g = (int) (normalizedPixels[x][y][1] * 255);
//                    int b = (int) (normalizedPixels[x][y][2] * 255);
//
//                    int pixel = Color.rgb(r, g, b);
//                    op.setPixel(x, y, pixel);
//                }
//            }

//            float[][][][] outputArray = new float[1][1][320][320];
//            outputBuffer.rewind(); // Reset position to beginning
//            outputBuffer.asFloatBuffer().get(outputArray[0][0][0]);
//            imageViewM.setImageBitmap(op);


//            ///////////////////

//
//
//            int imageTensorIndex = 0;
//            DataType imageDataType = u2net_interpreter.getInputTensor(imageTensorIndex).dataType(); //tensorFlowLiteModel is interpreter which also inferences (loaded from XXXX.tflite)
//
//            TensorImage tfImage = new TensorImage(imageDataType);
//            tfImage.load(op);
//
////
//            int probabilityTensorIndex = 0;
//            int[] probabilityShape =u2net_interpreter.getOutputTensor(probabilityTensorIndex).shape();
//            DataType probabilityDataType = u2net_interpreter.getOutputTensor(probabilityTensorIndex).dataType();
//            TensorBuffer outputProbabilityBuffer = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType);
//
//            int outputTensorIdx = 0;
//
//            Tensor outputTensor = u2net_interpreter.getOutputTensor(outputTensorIdx);
//            u2net_interpreter.run(tfImage.getBuffer(),  outputProbabilityBuffer.getBuffer().rewind());
//
//            Log.d("Interpreter ",outputTensor.dataType().toString());
//
//
//            ByteBuffer byteBuffer = outputProbabilityBuffer.getBuffer();
//            byteBuffer.rewind(); // Ensure the buffer is at position 0
//
//            Bitmap output = Bitmap.createBitmap(320, 320, Bitmap.Config.ARGB_8888);
//
//            int[] intValues = new int[320 * 320];
//
//            for (int y = 0; y < 320; y++) {
//                for (int x = 0; x < 320; x++) {
//                    int idx = y * 320 + x;
//                    float red = byteBuffer.getFloat(idx * 3) * 255;
//                    float green = byteBuffer.getFloat(idx * 3 + 1) * 255;
//                    float blue = byteBuffer.getFloat(idx * 3 + 2) * 255;
//
//                    intValues[idx] = Color.rgb((int) red, (int) green, (int) blue);
//                }
//            }
//
//            output.setPixels(intValues, 0, 320, 0, 0, 320, 320);
//
//            imageViewM.setImageBitmap(output);
//            Log.d("interpreter","ReachdeHere2");


        }
    }

    private ByteBuffer loadModelFile(String model_name) throws IOException {

        Interpreter.Options options=new Interpreter.Options();
        options.setNumThreads(4);
        AssetFileDescriptor fileDescriptor = getAssets().openFd(model_name);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
    public static Bitmap convertArrayToBitmapTensorFlow(float[][][][] imageArray,int imageWidth, int imageHeight) {
        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap grayToneImage = Bitmap.createBitmap(imageWidth, imageHeight, conf);
        Bitmap blackToneImage = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);

        for (int x = 0; x < imageArray[0][0].length; x++) {
            for (int y = 0; y < imageArray[0][0][0].length; y++) {
                int color = Color.rgb(
                        (int) (imageArray[0][0][x][y] * 255),
                        (int) (imageArray[0][0][x][y] * 255),
                        (int) (imageArray[0][0][x][y] * 255)
                );
                grayToneImage.setPixel(x,y, color);
            }
        }
        return grayToneImage;
    }

    private Bitmap trans(Bitmap inp){
        int backgroundColorToRemove = Color.WHITE;

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
//    private void matt(Bitmap bmp){
//        Mat image = new Mat();
//        Utils.bitmapToMat(bmp,image);
//        Bitmap op = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), Bitmap.Config.ARGB_8888);
//        Mat grayscaleImage = new Mat();
//        Imgproc.cvtColor(image, grayscaleImage, Imgproc.COLOR_BGR2GRAY);
//
//// Apply thresholding to segment the foreground object
//        Mat thresholdedImage = new Mat();
//        Imgproc.threshold(grayscaleImage, thresholdedImage, 100, 255, Imgproc.THRESH_BINARY);
//
//// Create the alpha matte
//        Mat alphaMatte = new Mat();
//        thresholdedImage.convertTo(alphaMatte, CvType.CV_8U); // Convert to 8-bit format
//
//// Combine the alpha channel with the RGB channels
//        List<Mat> channels = new ArrayList<>();
//        Core.split(image, channels); // Split RGB channels
//        channels.add(alphaMatte); // Add alpha channel
//        Core.merge(channels, image);
//
//        Utils.matToBitmap(image,op);
//        imageViewM.setImageBitmap(op);
//    }


    private File saveBitmapToFile(Bitmap bitmap) {
        File filesDir = getFilesDir();
        File imageFile = new File(filesDir, "image.jpg");

        try {
            FileOutputStream out = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
            return imageFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
