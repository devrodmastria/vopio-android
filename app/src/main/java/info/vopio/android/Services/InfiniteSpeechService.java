//package info.vopio.android.Services;
//
//import android.app.Service;
//import android.content.Intent;
//import android.os.Binder;
//import android.os.IBinder;
//
//import androidx.annotation.Nullable;
//
//import com.google.api.gax.rpc.ClientStream;
//import com.google.api.gax.rpc.ResponseObserver;
//import com.google.cloud.speech.v1p1beta1.SpeechClient;
//import com.google.cloud.speech.v1p1beta1.StreamingRecognizeRequest;
//import com.google.cloud.speech.v1p1beta1.StreamingRecognizeResponse;
//
//import java.util.ArrayList;
//import javax.sound.sampled.TargetDataLine;
//
//public class InfiniteSpeechService extends Service {
//
//    private final InfiniteSpeechBinder thisBinder = new InfiniteSpeechBinder();
//
//    private static TargetDataLine targetDataLine;
//
//    @Nullable
//    @Override
//    public IBinder onBind(Intent intent) {
//        return thisBinder;
//    }
//
//    public static InfiniteSpeechService from(IBinder binder) {
//        return ((InfiniteSpeechBinder) binder).getService();
//    }
//
//    private class InfiniteSpeechBinder extends Binder {
//        InfiniteSpeechService getService() { return InfiniteSpeechService.this; }
//    }
//
//    public static void infiniteStreamingRecognize(String languageCode) throws Exception {
//
//        // Microphone Input buffering
//        class MicBuffer implements Runnable {
//
//            @Override
//            public void run() {
//
//                System.out.println("-->>Listening");
//                targetDataLine.start();
//                byte[] data = new byte[BYTES_PER_BUFFER];
//                while (targetDataLine.isOpen()) {
//                    try {
//                        int numBytesRead = targetDataLine.read(data, 0, data.length);
//                        if ((numBytesRead <= 0) && (targetDataLine.isOpen())) {
//                            continue;
//                        }
//                        sharedQueue.put(data.clone());
//                    } catch (InterruptedException e) {
//                        System.out.println("Microphone input buffering interrupted : " + e.getMessage());
//                    }
//                }
//            }
//        }
//
//        // Creating microphone input buffer thread
//        MicBuffer micRunnable = new MicBuffer();
//        Thread micThread = new Thread(micRunnable);
//        ResponseObserver<StreamingRecognizeResponse> responseObserver = null;
//        try (SpeechClient client = SpeechClient.create()) {
//            ClientStream<StreamingRecognizeRequest> clientStream;
//            responseObserver =
//                    new ResponseObserver<StreamingRecognizeResponse>() {
//
//                        ArrayList<StreamingRecognizeResponse> responses = new ArrayList<>();
//
//                        public void onStart(StreamController controller) {
//                            referenceToStreamController = controller;
//                        }
//
//                        public void onResponse(StreamingRecognizeResponse response) {
//                            responses.add(response);
//                            StreamingRecognitionResult result = response.getResultsList().get(0);
//                            Duration resultEndTime = result.getResultEndTime();
//                            resultEndTimeInMS =
//                                    (int)
//                                            ((resultEndTime.getSeconds() * 1000) + (resultEndTime.getNanos() / 1000000));
//                            double correctedTime =
//                                    resultEndTimeInMS - bridgingOffset + (STREAMING_LIMIT * restartCounter);
//
//                            SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
//                            if (result.getIsFinal()) {
//                                System.out.print(GREEN);
//                                System.out.print("\033[2K\r");
//                                System.out.printf(
//                                        "%s: %s [confidence: %.2f]\n",
//                                        convertMillisToDate(correctedTime),
//                                        alternative.getTranscript(),
//                                        alternative.getConfidence());
//                                isFinalEndTime = resultEndTimeInMS;
//                                lastTranscriptWasFinal = true;
//                            } else {
//                                System.out.print(RED);
//                                System.out.print("\033[2K\r");
//                                System.out.printf(
//                                        "%s: %s", convertMillisToDate(correctedTime), alternative.getTranscript());
//                                lastTranscriptWasFinal = false;
//                            }
//                        }
//
//                        public void onComplete() {}
//
//                        public void onError(Throwable t) {}
//                    };
//            clientStream = client.streamingRecognizeCallable().splitCall(responseObserver);
//
//            RecognitionConfig recognitionConfig =
//                    RecognitionConfig.newBuilder()
//                            .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
//                            .setLanguageCode(languageCode)
//                            .setSampleRateHertz(16000)
//                            .build();
//
//            StreamingRecognitionConfig streamingRecognitionConfig =
//                    StreamingRecognitionConfig.newBuilder()
//                            .setConfig(recognitionConfig)
//                            .setInterimResults(true)
//                            .build();
//
//            StreamingRecognizeRequest request =
//                    StreamingRecognizeRequest.newBuilder()
//                            .setStreamingConfig(streamingRecognitionConfig)
//                            .build(); // The first request in a streaming call has to be a config
//
//            clientStream.send(request);
//
//            try {
//                // SampleRate:16000Hz, SampleSizeInBits: 16, Number of channels: 1, Signed: true,
//                // bigEndian: false
//                AudioFormat audioFormat = new AudioFormat(16000, 16, 1, true, false);
//                DataLine.Info targetInfo =
//                        new Info(
//                                TargetDataLine.class,
//                                audioFormat); // Set the system information to read from the microphone audio
//                // stream
//
//                if (!AudioSystem.isLineSupported(targetInfo)) {
//                    System.out.println("Microphone not supported");
//                    System.exit(0);
//                }
//                // Target data line captures the audio stream the microphone produces.
//                targetDataLine = (TargetDataLine) AudioSystem.getLine(targetInfo);
//                targetDataLine.open(audioFormat);
//                micThread.start();
//
//                long startTime = System.currentTimeMillis();
//
//                while (true) {
//
//                    long estimatedTime = System.currentTimeMillis() - startTime;
//
//                    if (estimatedTime >= STREAMING_LIMIT) {
//
//                        clientStream.closeSend();
//                        referenceToStreamController.cancel(); // remove Observer
//
//                        if (resultEndTimeInMS > 0) {
//                            finalRequestEndTime = isFinalEndTime;
//                        }
//                        resultEndTimeInMS = 0;
//
//                        lastAudioInput = null;
//                        lastAudioInput = audioInput;
//                        audioInput = new ArrayList<ByteString>();
//
//                        restartCounter++;
//
//                        if (!lastTranscriptWasFinal) {
//                            System.out.print('\n');
//                        }
//
//                        newStream = true;
//
//                        clientStream = client.streamingRecognizeCallable().splitCall(responseObserver);
//
//                        request =
//                                StreamingRecognizeRequest.newBuilder()
//                                        .setStreamingConfig(streamingRecognitionConfig)
//                                        .build();
//
//                        System.out.println(YELLOW);
//                        System.out.printf("%d: RESTARTING REQUEST\n", restartCounter * STREAMING_LIMIT);
//
//                        startTime = System.currentTimeMillis();
//
//                    } else {
//
//                        if ((newStream) && (lastAudioInput.size() > 0)) {
//                            // if this is the first audio from a new request
//                            // calculate amount of unfinalized audio from last request
//                            // resend the audio to the speech client before incoming audio
//                            double chunkTime = STREAMING_LIMIT / lastAudioInput.size();
//                            // ms length of each chunk in previous request audio arrayList
//                            if (chunkTime != 0) {
//                                if (bridgingOffset < 0) {
//                                    // bridging Offset accounts for time of resent audio
//                                    // calculated from last request
//                                    bridgingOffset = 0;
//                                }
//                                if (bridgingOffset > finalRequestEndTime) {
//                                    bridgingOffset = finalRequestEndTime;
//                                }
//                                int chunksFromMs =
//                                        (int) Math.floor((finalRequestEndTime - bridgingOffset) / chunkTime);
//                                // chunks from MS is number of chunks to resend
//                                bridgingOffset =
//                                        (int) Math.floor((lastAudioInput.size() - chunksFromMs) * chunkTime);
//                                // set bridging offset for next request
//                                for (int i = chunksFromMs; i < lastAudioInput.size(); i++) {
//                                    request =
//                                            StreamingRecognizeRequest.newBuilder()
//                                                    .setAudioContent(lastAudioInput.get(i))
//                                                    .build();
//                                    clientStream.send(request);
//                                }
//                            }
//                            newStream = false;
//                        }
//
//                        tempByteString = ByteString.copyFrom(sharedQueue.take());
//
//                        request =
//                                StreamingRecognizeRequest.newBuilder().setAudioContent(tempByteString).build();
//
//                        audioInput.add(tempByteString);
//                    }
//
//                    clientStream.send(request);
//                }
//            } catch (Exception e) {
//                System.out.println(e);
//            }
//        }
//    }
//
//
//
//}
