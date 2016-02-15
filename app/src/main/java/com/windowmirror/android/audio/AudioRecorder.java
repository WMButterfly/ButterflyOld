package com.windowmirror.android.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author alliecurry
 */
public class AudioRecorder {
    private static final String TAG = AudioRecorder.class.getSimpleName();
    private static final String OUTPUT_FOLDER = "WindowMirror";
    private static final String TEMP_FILE = "temp";
    private static final int SAMPLE_RATE = 16000;
    private static final int RECORDER_BPP = 16; // bit depth per sample
    private static final int CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    /** the maximum size of each file "chunk" generated, in bytes */
    private static final long CHUNK_SIZE = 3450000; // approximately 1 minute 45 seconds

    private AudioRecord audioRecord = null;
    private AudioListener listener;
    private Thread recordThread = null;

    private int bufferSize = 0;
    public boolean isRecording = false;
    private String fileNameTemp;
    private String fileNameSaved = null;

    public AudioRecorder(final String fileName, final AudioListener listener){
        bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNELS, AUDIO_ENCODING);
        fileNameSaved = fileName;
        this.listener = listener;
    }

    /** @return String full file path of the initial recorded raw file. */
    public String getTempFileName() {
        String fileName = TEMP_FILE + new Random().nextInt(10000) + ".raw";
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, OUTPUT_FOLDER);

        if (!file.exists()) {
            file.mkdirs();
        }

        File tempFile = new File(filepath, fileName);

        if (tempFile.exists()) {
            tempFile.delete();
        }

        return (file.getAbsolutePath() + "/" + fileName);
    }


    public void startRecording() {
        //Initialize audioRecord object with InputSource, Sample rate, number of channel, audio encoding and buffer size of output stream
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNELS, AUDIO_ENCODING, bufferSize);
        audioRecord.startRecording();
        isRecording = true;

        recordThread = new Thread(new Runnable() {
            @Override
            public void run() {
                writeRecordedDataToRawFile();
            }
        });
        recordThread.start();
    }

    public void writeRecordedDataToRawFile() {
        byte data[] = new byte[bufferSize];
        fileNameTemp = getTempFileName();
        FileOutputStream fos;

        try {
            fos = new FileOutputStream(fileNameTemp);
        } catch(FileNotFoundException e){
            Log.e(TAG, e.toString());
            if (listener != null) {
                listener.onAudioRecordError();
            }
            return;
        }

        int read;
        while(isRecording) {
            read = audioRecord.read(data, 0, bufferSize);
            if (read != AudioRecord.ERROR_INVALID_OPERATION){
                try {
                    fos.write(data);
                } catch(IOException e) {
                    Log.e(TAG, e.toString());
                }
            }
        }

        try {
            fos.close();
        } catch(IOException e) {
            Log.e(TAG, e.toString());
        }
    }

    public void stopRecording() {
        if (audioRecord != null) {
            isRecording = false;
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
            recordThread = null;
        }

        saveFromTempToWavFile(fileNameTemp, fileNameSaved); // Main wav file
        final List<String> chunks = getWavChunks(fileNameTemp, fileNameSaved); // "chunked" version
        listener.onAudioRecordComplete(fileNameSaved, chunks);
        deleteTempFile();
    }

    private void saveFromTempToWavFile(String tempFile, String wavFile){
        FileInputStream fis;
        FileOutputStream fos;

        long totalAudioDataLen = 0;
        long totalDataLen;
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
        } catch (FileNotFoundException e){
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public List<String> getWavChunks(final String tempFile, final String wavFile) {
        String newFileName = wavFile.substring(0, wavFile.indexOf(".wav"));
        try {
            return split(tempFile, newFileName);
        } catch (IOException e) {
            Log.e(TAG, "Error getting chunks: " + e.toString());
        }
        // "Chunking" failed... return only one chunk.
        final List<String> list = new ArrayList<>();
        list.add(wavFile);
        return list;
    }

    /**
     * split the file specified by filename into pieces, each of size
     * CHUNK_SIZE except for the last one, which may be smaller
     */
    private List<String> split(final String rawFile, final String filename) throws IOException {
        final List<String> chunkList = new ArrayList<>();
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(rawFile));

        // get the file length
        File f = new File(rawFile);
        long fileSize = f.length();

        // loop for each full chunk
        int subfile;
        for (subfile = 0; subfile < fileSize / CHUNK_SIZE; subfile++) {
            // open the output file
            final String chunkFileName = filename + "." + subfile + ".wav";
            final FileOutputStream fos = new FileOutputStream(chunkFileName);
            BufferedOutputStream out = new BufferedOutputStream(fos);
            createWavFile(fos, CHUNK_SIZE, CHUNK_SIZE + 36, SAMPLE_RATE, 1,
                    RECORDER_BPP * SAMPLE_RATE * 1 / 8);
            // write the right amount of bytes
            for (int currentByte = 0; currentByte < CHUNK_SIZE; currentByte++) {
                // load one byte from the input file and write it to the output file
                out.write(in.read());
            }

            // close the file
            out.close();
            chunkList.add(chunkFileName);
        }

        // close the file
        in.close();
        return chunkList;
    }

    private static void createWavFile(FileOutputStream fos, long totalAudioDataLen, long totalDataLen,
                                      long sampleRate, int numberOfChannel, long byteRate){
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
            Log.e(TAG, "Error writing wav header: " + e.toString());
        }
    }

    public void deleteTempFile() {
        if (fileNameTemp == null || fileNameTemp.isEmpty()) {
            return;
        }
        final File file = new File(fileNameTemp);
        file.delete();
    }

    public interface AudioListener {
        void onAudioRecordError();
        void onAudioRecordComplete(final String filePath, List<String> chunks);
    }
}
