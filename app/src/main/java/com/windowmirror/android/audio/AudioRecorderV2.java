package com.windowmirror.android.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;

import java.io.*;

/**
 * @author alliecurry
 */
public class AudioRecorderV2 {
    private static final int RECORDER_BPP = 16;//bit depth per sample
    private static final String OUTPUT_FOLDER = "WindowMirror";
    private static final String TEMP_FILE = "temp.raw";
    private static final int SAMPLE_RATE = 8000;
    private static final int CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord mAudioRecorder = null;
    private int bufferSize = 0;
    private Thread recordThread = null;
    public boolean isRecording = false;
    private String fileNameSaved = null;

    public AudioRecorderV2(final String fileName){
        bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNELS, AUDIO_ENCODING);
        fileNameSaved = fileName;
    }

    public String getTempFileName(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,OUTPUT_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }

        File tempFile = new File(filepath,TEMP_FILE);

        if(tempFile.exists())
            tempFile.delete();

        return (file.getAbsolutePath() + "/" + TEMP_FILE);
    }


    public void startRecording(){
        //Initialize mAudioRecorder object with InputSource, Sample rate, number of channel, audio encoding and buffer size of output stream
        mAudioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNELS, AUDIO_ENCODING, bufferSize );
        //Set to Recording state
        mAudioRecorder.startRecording();
        isRecording = true;
        recordThread = new Thread(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                writeRecordedDataToRawFile();
            }
        });
        recordThread.start();
    }//end of startRecording()

    public void writeRecordedDataToRawFile(){
        byte data[] = new byte[bufferSize];
        String fileName = getTempFileName();
        FileOutputStream fos = null;

        try{
            fos = new FileOutputStream(fileName);
        }catch(FileNotFoundException e){
            e.printStackTrace();
        }

        int read = 0;
        if(null!= fos){
            while(isRecording){
                read = mAudioRecorder.read(data, 0, bufferSize);
                if(AudioRecord.ERROR_INVALID_OPERATION != read){
                    try{
                        fos.write(data);
                    }catch(IOException e){
                        e.printStackTrace();
                    }
                }
            }
            try{
                fos.close();
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }//end of writeRecordedDataToRawFile()

    public void stopRecording(final AudioRecordThread.OnCompleteListener onCompleteListener){
        if(null!= mAudioRecorder){
            isRecording = false;
            mAudioRecorder.stop();
            mAudioRecorder.release();
            mAudioRecorder = null;
            recordThread = null;
        }

        saveFromTempToWavFile(getTempFileName(), fileNameSaved);
        onCompleteListener.onComplete(fileNameSaved);
        //deleteTempFile();
    }//end of stopRecording()

    public void saveFromTempToWavFile(String tempFile, String wavFile){
        FileInputStream fis = null;
        FileOutputStream fos = null;

        long totalAudioDataLen = 0;
        long totalDataLen = totalAudioDataLen + 36;
        long longSampleRate = SAMPLE_RATE;
        int numberOfChannel = 1;
        long byteRate = RECORDER_BPP*SAMPLE_RATE*numberOfChannel/8;

        byte[] data = new byte[bufferSize];

        try{
            fis = new FileInputStream(tempFile);
            fos = new FileOutputStream(wavFile);
            //get the size of audio data
            totalAudioDataLen = fis.getChannel().size();
            totalDataLen = totalAudioDataLen + 36;
            //create Wave File header
            createWavFile(fos, totalAudioDataLen, totalDataLen, longSampleRate, numberOfChannel, byteRate);
            //continue to write audio data after completing the header
            while(fis.read(data) != -1){
                fos.write(data);
            }

            fis.close();
            fos.close();
        }catch(FileNotFoundException e){
            e.printStackTrace();
        }catch(IOException e){
            e.printStackTrace();
        }
    }//end of saveFromTempToWavFile

    public void createWavFile(FileOutputStream fos, long totalAudioDataLen, long totalDataLen, long sampleRate, int numberOfChannel, long byteRate){
        byte[] mWaveFileHeader = new byte[44];

        mWaveFileHeader[0] = 'R';
        mWaveFileHeader[1] = 'I';
        mWaveFileHeader[2] = 'F';
        mWaveFileHeader[3] = 'F';
        mWaveFileHeader[4] = (byte)(totalDataLen & 0xff);
        mWaveFileHeader[5] = (byte)((totalDataLen >> 8) & 0xff);
        mWaveFileHeader[6] = (byte)((totalDataLen >> 16) & 0xff);
        mWaveFileHeader[7] = (byte)((totalDataLen >> 24) & 0xff);
        mWaveFileHeader[8] = 'W';
        mWaveFileHeader[9] = 'A';
        mWaveFileHeader[10] = 'V';
        mWaveFileHeader[11] = 'E';
        mWaveFileHeader[12] = 'f';
        mWaveFileHeader[13] = 'm';
        mWaveFileHeader[14] = 't';
        mWaveFileHeader[15] = ' ';
        mWaveFileHeader[16] = 16;//size of 'fmt ' chunk
        mWaveFileHeader[17] = 0;
        mWaveFileHeader[18] = 0;
        mWaveFileHeader[19] = 0;
        mWaveFileHeader[20] = 1;
        mWaveFileHeader[21] = 0;
        mWaveFileHeader[22] = (byte) numberOfChannel;
        mWaveFileHeader[23] = 0;

        mWaveFileHeader[24] = (byte)(sampleRate & 0xff);
        mWaveFileHeader[25] = (byte)((sampleRate >> 8) & 0xff);
        mWaveFileHeader[26] = (byte)((sampleRate >> 16) & 0xff);
        mWaveFileHeader[27] = (byte)((sampleRate >> 24) & 0xff);

        mWaveFileHeader[28] = (byte)(byteRate & 0xff);
        mWaveFileHeader[29] = (byte)((byteRate >> 8) & 0xff);
        mWaveFileHeader[30] = (byte)((byteRate >> 16) & 0xff);
        mWaveFileHeader[31] = (byte)((byteRate >> 24) & 0xff);

        mWaveFileHeader[32] = (byte)(numberOfChannel*RECORDER_BPP/8);
        mWaveFileHeader[33] = 0;
        mWaveFileHeader[34] = RECORDER_BPP;

        mWaveFileHeader[35] = 0;

        mWaveFileHeader[36] = 'd';
        mWaveFileHeader[37] = 'a';
        mWaveFileHeader[38] = 't';
        mWaveFileHeader[39] = 'a';

        mWaveFileHeader[40] = (byte)(totalAudioDataLen & 0xff);
        mWaveFileHeader[41] = (byte)((totalAudioDataLen >> 8) & 0xff);
        mWaveFileHeader[42] = (byte)((totalAudioDataLen >> 16) & 0xff);
        mWaveFileHeader[43] = (byte)((totalAudioDataLen >> 24) & 0xff);

        try {
            fos.write(mWaveFileHeader, 0, 44);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }//end of createWaveFile()

    public void deleteTempFile(){
        File file = new File(getTempFileName());
        file.delete();
    }
}
