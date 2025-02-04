package com.zegocloud.demo.bestpractice.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;

import androidx.appcompat.app.AppCompatActivity;

import com.faceunity.core.entity.FURenderOutputData;
import com.faceunity.nama.FURenderer;
import com.zegocloud.demo.bestpractice.activity.livestreaming.LiveStreamEntryActivity;
import com.zegocloud.demo.bestpractice.databinding.ActivityMainBinding;
import com.zegocloud.demo.bestpractice.internal.ZEGOCallInvitationManager;
import com.zegocloud.demo.bestpractice.internal.ZEGOLiveStreamingManager;
import com.zegocloud.demo.bestpractice.internal.sdk.ZEGOSDKManager;
import com.zegocloud.demo.bestpractice.internal.sdk.basic.ZEGOSDKUser;

import java.nio.ByteBuffer;

import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.callback.IZegoCustomVideoProcessHandler;
import im.zego.zegoexpress.constants.ZegoPublishChannel;
import im.zego.zegoexpress.constants.ZegoVideoBufferType;
import im.zego.zegoexpress.entity.ZegoCustomVideoProcessConfig;
import im.zego.zegoexpress.entity.ZegoVideoFrameParam;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private ByteBuffer byteBuffer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        ZEGOSDKUser localUser = ZEGOSDKManager.getInstance().expressService.getCurrentUser();
        binding.liveUserinfoUserid.setText(localUser.userID);
        binding.liveUserinfoUsername.setText(localUser.userName);

      /*  binding.buttonCall.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, CallEntryActivity.class));
        });*/

        binding.buttonLivestreaming.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, LiveStreamEntryActivity.class));
        });

    /*    binding.buttonLiveaudioroom.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, LiveAudioRoomEntryActivity.class));
        });*/
        // if use LiveStreaming,init after user login,can receive pk request.
        ZEGOLiveStreamingManager.getInstance().init();
        // if use Call invitation,init after user login,can receive call request.
        ZEGOCallInvitationManager.getInstance().init(this);

        initFaceUnity();
    }

    private void initFaceUnity() {
        FURenderer.getInstance().setup(this);
        //  ZegoCustomVideoProcessConfig config = new ZegoCustomVideoProcessConfig();
        //config.bufferType = ZegoVideoBufferType.GL_TEXTURE_2D;


        // Create a configuration that uses raw data (e.g. NV21)
        ZegoCustomVideoProcessConfig config = new ZegoCustomVideoProcessConfig();
        config.bufferType = ZegoVideoBufferType.GL_TEXTURE_2D_AND_RAW_DATA;  // Use the raw data mode (e.g., NV21)

// Enable custom video processing on the MAIN channel.
        ZEGOSDKManager.getInstance().expressService.enableCustomVideoProcessing(true, config, ZegoPublishChannel.MAIN);

// Set the custom video process handler.
        ZEGOSDKManager.getInstance().expressService.setCustomVideoProcessHandler(new IZegoCustomVideoProcessHandler() {
            @Override
            public void onStart(ZegoPublishChannel channel) {
                // Initialization if needed.
            }

            @Override
            public void onStop(ZegoPublishChannel channel) {
                // Cleanup if needed.
            }

            @Override
            public void onCapturedUnprocessedRawData(ByteBuffer data, int[] dataLength, ZegoVideoFrameParam param, long referenceTimeMillisecond, ZegoPublishChannel channel) {
                super.onCapturedUnprocessedRawData(data, dataLength, param, referenceTimeMillisecond, channel);

                int totalLength = 0;
                for (int len : dataLength) {
                    totalLength += len;
                }

                // Create a byte array to hold the frame data.
                byte[] frameByteArray = new byte[totalLength];

                // Copy data from the ByteBuffer into the byte array.
                // Make sure the buffer is at the correct position.
                data.rewind();  // Reset the position if needed.
                data.get(frameByteArray);

                // ---- Step 2: Process the frame with FaceUnity ----
                // param.width and param.height should contain the frame dimensions.
                FURenderOutputData fuRenderOutputData = FURenderer.getInstance().onDrawFrameSingleInput(frameByteArray, param.width, param.height, true);
                // Allocate buffer for processed data
                if (byteBuffer == null) {
                    byteBuffer = ByteBuffer.allocateDirect(fuRenderOutputData.getImage().getBuffer().length);
                }
                byteBuffer.put(fuRenderOutputData.getImage().getBuffer());
                byteBuffer.flip();


                ZegoExpressEngine.getEngine().sendCustomVideoCaptureRawData(byteBuffer, byteBuffer.limit(), param, SystemClock.elapsedRealtime());
            }


            // If your SDK version only supports texture callbacks, you will need Option 2.
        });


/*
        ZEGOSDKManager.getInstance().expressService.enableCustomVideoProcessing(true, config, ZegoPublishChannel.MAIN);
        ZEGOSDKManager.getInstance().expressService.setCustomVideoProcessHandler(new IZegoCustomVideoProcessHandler() {
            @Override
            public void onStart(ZegoPublishChannel channel) {

            }

            @Override
            public void onStop(ZegoPublishChannel channel) {
            }

            @Override
            public void onCapturedUnprocessedTextureData(int textureID, int width, int height,
                                                         long referenceTimeMillisecond, ZegoPublishChannel channel) {

                FURenderKit.getInstance()

                FURenderInputData inputData = new FURenderInputData(width, height);
                inputData.setTexture(new FURenderInputData.FUTexture(FUInputTextureEnum.FU_ADM_FLAG_EXTERNAL_OES_TEXTURE, textureID));

                FURenderInputData.FURenderConfig config = inputData.getRenderConfig();
                config.setExternalInputType(FUExternalInputEnum.EXTERNAL_INPUT_TYPE_CAMERA);
                config.setInputBufferMatrix(FUTransformMatrixEnum.CCROT0);
                config.setInputTextureMatrix(FUTransformMatrixEnum.CCROT0);
                config.setCameraFacing(CameraFacingEnum.CAMERA_FRONT);

                FURenderOutputData fuRenderOutputData = FURenderKit.getInstance().renderWithInput(inputData);

                // Prepare parameters for Zego's video frame (processed by FaceUnity)
                ZegoVideoFrameParam param = new ZegoVideoFrameParam();
                param.width = width;
                param.height = height;
                param.strides[0] = width;
                param.strides[1] = height;
                param.format = ZegoVideoFrameFormat.NV21;
                param.rotation = 180;


                long now = SystemClock.elapsedRealtime();

                // Allocate buffer for processed data
                if (byteBuffer == null) {
                    byteBuffer = ByteBuffer.allocateDirect(fuRenderOutputData.getImage().getBuffer().length);
                }
                byteBuffer.put(fuRenderOutputData.getImage().getBuffer());
                byteBuffer.flip();

                // Send the processed video data to Zego
                ZegoExpressEngine.getEngine().sendCustomVideoCaptureRawData(byteBuffer, byteBuffer.limit(), param, now);

            }
        });*/
    }

    protected void onPause() {
        super.onPause();
        if (isFinishing()) {
            ZEGOSDKManager.getInstance().disconnectUser();
            ZEGOLiveStreamingManager.getInstance().removeUserData();
            ZEGOLiveStreamingManager.getInstance().removeUserListeners();
            ZEGOCallInvitationManager.getInstance().removeUserData();
            ZEGOCallInvitationManager.getInstance().removeUserListeners();
        }
    }
}