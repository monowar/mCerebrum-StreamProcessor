package org.md2k.streamprocessor;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;

import org.md2k.datakitapi.DataKitAPI;
import org.md2k.datakitapi.datatype.DataType;
import org.md2k.datakitapi.datatype.DataTypeDoubleArray;
import org.md2k.datakitapi.exception.DataKitException;
import org.md2k.datakitapi.messagehandler.OnReceiveListener;
import org.md2k.datakitapi.source.METADATA;
import org.md2k.datakitapi.source.datasource.DataSourceBuilder;
import org.md2k.datakitapi.source.datasource.DataSourceClient;
import org.md2k.datakitapi.source.datasource.DataSourceType;
import org.md2k.datakitapi.source.platform.PlatformBuilder;
import org.md2k.datakitapi.source.platform.PlatformId;
import org.md2k.datakitapi.source.platform.PlatformType;
import org.md2k.streamprocessor.output.Activity;
import org.md2k.streamprocessor.output.ECGQuality;
import org.md2k.streamprocessor.output.Output;
import org.md2k.streamprocessor.output.PuffLabel;
import org.md2k.streamprocessor.output.PuffProbability;
import org.md2k.streamprocessor.output.RIPQuality;
import org.md2k.streamprocessor.output.SmokingEpisode;
import org.md2k.streamprocessor.output.StressActivity;
import org.md2k.streamprocessor.output.StressEpisode;
import org.md2k.streamprocessor.output.StressLabel;
import org.md2k.streamprocessor.output.StressProbability;
import org.md2k.streamprocessor.output.StressRIPLabel;
import org.md2k.streamprocessor.output.StressRIPProbability;
import org.md2k.streamprocessor.output.cStressFeatureVector;
import org.md2k.streamprocessor.output.cStressRIPFeatureVector;
import org.md2k.streamprocessor.output.puffMarkerFeatureVector;
import org.md2k.utilities.Report.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import md2k.mcerebrum.CSVDataPoint;
import md2k.mcerebrum.cstress.StreamConstants;
import md2k.mcerebrum.cstress.autosense.AUTOSENSE;
import md2k.mcerebrum.cstress.autosense.PUFFMARKER;

/*
 * Copyright (c) 2015, The University of Memphis, MD2K Center
 * - Syed Monowar Hossain <monowar.hossain@gmail.com>
 * - Timothy Hnat <twhnat@memphis.edu>
 * - Nazir Saleheen <nsleheen@memphis.edu>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
public class DataKitManager {
    protected Context context;
    protected DataKitAPI dataKitAPI;
    protected StreamProcessorWrapper streamProcessorWrapper;
    protected HashMap<String, Integer> dataSourceTypeTOChannel;
    protected boolean active;
    protected HashMap<String, Output> outputHashMap;

    DataKitManager(Context ccontext) {
        this.context = ccontext;
        dataKitAPI = DataKitAPI.getInstance(context);
        createDataSourceTypeTOChannel();
        streamProcessorWrapper = new StreamProcessorWrapper(new org.md2k.streamprocessor.OnReceiveListener() {
            @Override
            public void onReceived(String s, final DataType value) {
                if (s.equals(StreamConstants.ORG_MD2K_PUFFMARKER_PUFFLABEL)) {
                    String directory = Environment.getExternalStorageDirectory().getAbsolutePath() + "/puffResponces/";
                    String filename = "puffResponses.txt";
                    writeResponse(directory, filename, "yes/no ::" + value.getDateTime() + "," + System.currentTimeMillis() + "\n");

/*                    AlertDialogs.AlertDialog(context, "Puff", "Puff detected", R.drawable.ic_info_teal_48dp, "Yes", "No", null, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String response = "No";
                            if (which == DialogInterface.BUTTON_POSITIVE) {
                                response = "Yes";
                            }
                            String directory = Environment.getExternalStorageDirectory().getAbsolutePath() + "/puffResponces/";
                            String filename = "puffResponses.txt";
                            writeResponse(directory, filename, response + "::" + value.getDateTime() + "," + System.currentTimeMillis() + "\n");
                        }
                    });
*/
                    Log.d("puffMarker", s + " : " + value.toString());
                }
                try {
                    outputHashMap.get(s).insert(value);
                } catch (DataKitException e) {
                    LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Constants.INTENT_STOP));
                }
                Log.d("Stream Processor", s + " : " + value.toString());
            }
        });
        active = false;
    }

    private void writeResponse(String directory, String filename, String s) {
        File dir = new File(directory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try {
            FileWriter writer = new FileWriter(directory + filename, true);
            writer.write(s);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void createDataSourceTypeTOChannel() {
        dataSourceTypeTOChannel = new HashMap<>();
        dataSourceTypeTOChannel.put(DataSourceType.RESPIRATION, AUTOSENSE.CHEST_RIP);
        dataSourceTypeTOChannel.put(DataSourceType.ECG, AUTOSENSE.CHEST_ECG);
        dataSourceTypeTOChannel.put(DataSourceType.ACCELEROMETER_X, AUTOSENSE.CHEST_ACCEL_X);
        dataSourceTypeTOChannel.put(DataSourceType.ACCELEROMETER_Y, AUTOSENSE.CHEST_ACCEL_Y);
        dataSourceTypeTOChannel.put(DataSourceType.ACCELEROMETER_Z, AUTOSENSE.CHEST_ACCEL_Z);

        dataSourceTypeTOChannel.put(PlatformId.LEFT_WRIST + "_" + DataSourceType.ACCELEROMETER_X, PUFFMARKER.LEFTWRIST_ACCEL_X);
        dataSourceTypeTOChannel.put(PlatformId.RIGHT_WRIST + "_" + DataSourceType.ACCELEROMETER_X, PUFFMARKER.RIGHTWRIST_ACCEL_X);
        dataSourceTypeTOChannel.put(PlatformId.LEFT_WRIST + "_" + DataSourceType.ACCELEROMETER_Y, PUFFMARKER.LEFTWRIST_ACCEL_Y);
        dataSourceTypeTOChannel.put(PlatformId.RIGHT_WRIST + "_" + DataSourceType.ACCELEROMETER_Y, PUFFMARKER.RIGHTWRIST_ACCEL_Y);
        dataSourceTypeTOChannel.put(PlatformId.LEFT_WRIST + "_" + DataSourceType.ACCELEROMETER_Z, PUFFMARKER.LEFTWRIST_ACCEL_Z);
        dataSourceTypeTOChannel.put(PlatformId.RIGHT_WRIST + "_" + DataSourceType.ACCELEROMETER_Z, PUFFMARKER.RIGHTWRIST_ACCEL_Z);

        dataSourceTypeTOChannel.put(PlatformId.LEFT_WRIST + "_" + DataSourceType.GYROSCOPE_X, PUFFMARKER.LEFTWRIST_GYRO_X);
        dataSourceTypeTOChannel.put(PlatformId.RIGHT_WRIST + "_" + DataSourceType.GYROSCOPE_X, PUFFMARKER.RIGHTWRIST_GYRO_X);
        dataSourceTypeTOChannel.put(PlatformId.LEFT_WRIST + "_" + DataSourceType.GYROSCOPE_Y, PUFFMARKER.LEFTWRIST_GYRO_Y);
        dataSourceTypeTOChannel.put(PlatformId.RIGHT_WRIST + "_" + DataSourceType.GYROSCOPE_Y, PUFFMARKER.RIGHTWRIST_GYRO_Y);
        dataSourceTypeTOChannel.put(PlatformId.LEFT_WRIST + "_" + DataSourceType.GYROSCOPE_Z, PUFFMARKER.LEFTWRIST_GYRO_Z);
        dataSourceTypeTOChannel.put(PlatformId.RIGHT_WRIST + "_" + DataSourceType.GYROSCOPE_Z, PUFFMARKER.RIGHTWRIST_GYRO_Z);

    }

    protected void start() throws DataKitException {
        outputHashMap = new HashMap<>();
        active = true;

        subscribe(PlatformType.AUTOSENSE_CHEST, DataSourceType.RESPIRATION);
        subscribe(PlatformType.AUTOSENSE_CHEST, DataSourceType.ECG);
        subscribe(PlatformType.AUTOSENSE_CHEST, DataSourceType.ACCELEROMETER_X);
        subscribe(PlatformType.AUTOSENSE_CHEST, DataSourceType.ACCELEROMETER_Y);
        subscribe(PlatformType.AUTOSENSE_CHEST, DataSourceType.ACCELEROMETER_Z);

        try {
            subscribe(PlatformId.LEFT_WRIST);
        } catch (Exception e) {
            Log.w("StreamProcessor", "Left wrist not available");
            e.printStackTrace();
        }

        try {
            subscribe(PlatformId.RIGHT_WRIST);
        } catch (Exception e) {
            Log.w("StreamProcessor", "Right wrist not available");
            e.printStackTrace();
        }

        addListener(DataSourceType.STRESS_PROBABILITY);
        addListener(DataSourceType.STRESS_LABEL);
        addListener(DataSourceType.STRESS_ACTIVITY);
        addListener(DataSourceType.CSTRESS_FEATURE_VECTOR);
        addListener(DataSourceType.ORG_MD2K_CSTRESS_DATA_ECG_QUALITY);
        addListener(DataSourceType.ORG_MD2K_CSTRESS_DATA_RIP_QUALITY);
        addListener(DataSourceType.ORG_MD2K_CSTRESS_STRESS_EPISODE_CLASSIFICATION);
        addListener(DataSourceType.CSTRESS_FEATURE_VECTOR_RIP);
        addListener(DataSourceType.STRESS_RIP_PROBABILITY);
        addListener(DataSourceType.STRESS_RIP_LABEL);
        addListener(DataSourceType.ACTIVITY);
        addListener(DataSourceType.PUFF_PROBABILITY);
        addListener(DataSourceType.PUFF_LABEL);
        addListener(DataSourceType.PUFFMARKER_FEATURE_VECTOR);
        addListener(DataSourceType.PUFFMARKER_SMOKING_EPISODE);

    }

    public boolean isActive() {
        return active;
    }

    protected void stop() {
        active = false;
        try {
            unsubscribe(PlatformType.AUTOSENSE_CHEST, DataSourceType.RESPIRATION);
            unsubscribe(PlatformType.AUTOSENSE_CHEST, DataSourceType.ECG);
            unsubscribe(PlatformType.AUTOSENSE_CHEST, DataSourceType.ACCELEROMETER_X);
            unsubscribe(PlatformType.AUTOSENSE_CHEST, DataSourceType.ACCELEROMETER_Y);
            unsubscribe(PlatformType.AUTOSENSE_CHEST, DataSourceType.ACCELEROMETER_Z);
        } catch (DataKitException e) {
            e.printStackTrace();
        }
        try {
            unsubscribe(PlatformId.LEFT_WRIST);
        } catch (DataKitException e) {
            e.printStackTrace();
        }
        try {
            unsubscribe(PlatformId.RIGHT_WRIST);

        } catch (DataKitException e) {
            e.printStackTrace();
        }
    }

    public void addListener(String dataSourceType) {
        Output output;
        switch (dataSourceType) {
            case DataSourceType.STRESS_PROBABILITY:
                output = new StressProbability(context);
                output.register();
                outputHashMap.put(StreamConstants.ORG_MD2K_CSTRESS_PROBABILITY, output);
                streamProcessorWrapper.streamProcessor.registerCallbackDataStream(StreamConstants.ORG_MD2K_CSTRESS_PROBABILITY);
                break;

            case DataSourceType.STRESS_LABEL:
                output = new StressLabel(context);
                output.register();
                outputHashMap.put(StreamConstants.ORG_MD2K_CSTRESS_STRESSLABEL, output);
                streamProcessorWrapper.streamProcessor.registerCallbackDataStream(StreamConstants.ORG_MD2K_CSTRESS_STRESSLABEL);
                break;

            case DataSourceType.STRESS_RIP_PROBABILITY:
                output = new StressRIPProbability(context);
                output.register();
                outputHashMap.put(StreamConstants.ORG_MD2K_CSTRESS_RIP_PROBABILITY, output);
                streamProcessorWrapper.streamProcessor.registerCallbackDataStream(StreamConstants.ORG_MD2K_CSTRESS_RIP_PROBABILITY);
                break;

            case DataSourceType.STRESS_RIP_LABEL:
                output = new StressRIPLabel(context);
                output.register();
                outputHashMap.put(StreamConstants.ORG_MD2K_CSTRESS_RIP_STRESSLABEL, output);
                streamProcessorWrapper.streamProcessor.registerCallbackDataStream(StreamConstants.ORG_MD2K_CSTRESS_RIP_STRESSLABEL);
                break;

            case DataSourceType.STRESS_ACTIVITY:
                output = new StressActivity(context);
                output.register();
                outputHashMap.put(StreamConstants.ORG_MD2K_CSTRESS_DATA_ACCEL_ACTIVITY, output);
                streamProcessorWrapper.streamProcessor.registerCallbackDataStream(StreamConstants.ORG_MD2K_CSTRESS_DATA_ACCEL_ACTIVITY);
                break;

            case DataSourceType.CSTRESS_FEATURE_VECTOR:
                output = new cStressFeatureVector(context);
                output.register();
                outputHashMap.put(StreamConstants.ORG_MD2K_CSTRESS_FV, output);
                streamProcessorWrapper.streamProcessor.registerCallbackDataArrayStream(StreamConstants.ORG_MD2K_CSTRESS_FV);
                break;

            case DataSourceType.CSTRESS_FEATURE_VECTOR_RIP:
                output = new cStressRIPFeatureVector(context);
                output.register();
                outputHashMap.put(StreamConstants.ORG_MD2K_CSTRESS_FV_RIP, output);
                streamProcessorWrapper.streamProcessor.registerCallbackDataArrayStream(StreamConstants.ORG_MD2K_CSTRESS_FV_RIP);
                break;

            case DataSourceType.ORG_MD2K_CSTRESS_DATA_RIP_QUALITY:
                output = new RIPQuality(context);
                output.register();
                outputHashMap.put(StreamConstants.ORG_MD2K_CSTRESS_DATA_RIP_QUALITY, output);
                streamProcessorWrapper.streamProcessor.registerCallbackDataStream(StreamConstants.ORG_MD2K_CSTRESS_DATA_RIP_QUALITY);
                break;

            case DataSourceType.ORG_MD2K_CSTRESS_DATA_ECG_QUALITY:
                output = new ECGQuality(context);
                output.register();
                outputHashMap.put(StreamConstants.ORG_MD2K_CSTRESS_DATA_ECG_QUALITY, output);
                streamProcessorWrapper.streamProcessor.registerCallbackDataStream(StreamConstants.ORG_MD2K_CSTRESS_DATA_ECG_QUALITY);
                break;

            case DataSourceType.ORG_MD2K_CSTRESS_STRESS_EPISODE_CLASSIFICATION:
                output = new StressEpisode(context);
                output.register();
                outputHashMap.put(StreamConstants.ORG_MD2K_CSTRESS_STRESS_EPISODE_CLASSIFICATION, output);
                streamProcessorWrapper.streamProcessor.registerCallbackDataStream(StreamConstants.ORG_MD2K_CSTRESS_STRESS_EPISODE_CLASSIFICATION);
                break;

            case DataSourceType.ACTIVITY:
                output = new Activity(context);
                output.register();
                outputHashMap.put(StreamConstants.ORG_MD2K_CSTRESS_DATA_ACCEL_WINDOWED_MAGNITUDE_STDEV, output);
                streamProcessorWrapper.streamProcessor.registerCallbackDataStream(StreamConstants.ORG_MD2K_CSTRESS_DATA_ACCEL_WINDOWED_MAGNITUDE_STDEV);
                break;

            case DataSourceType.PUFF_PROBABILITY:
                output = new PuffProbability(context);
                output.register();
                outputHashMap.put(StreamConstants.ORG_MD2K_PUFFMARKER_PROBABILITY, output);
                streamProcessorWrapper.streamProcessor.registerCallbackDataStream(StreamConstants.ORG_MD2K_PUFFMARKER_PROBABILITY);
                break;

            case DataSourceType.PUFF_LABEL:
                output = new PuffLabel(context);
                output.register();
                outputHashMap.put(StreamConstants.ORG_MD2K_PUFFMARKER_PUFFLABEL, output);
                streamProcessorWrapper.streamProcessor.registerCallbackDataStream(StreamConstants.ORG_MD2K_PUFFMARKER_PUFFLABEL);
                break;

            case DataSourceType.PUFFMARKER_FEATURE_VECTOR:
                output = new puffMarkerFeatureVector(context);
                output.register();
                outputHashMap.put(StreamConstants.ORG_MD2K_PUFFMARKER_FV, output);
                streamProcessorWrapper.streamProcessor.registerCallbackDataArrayStream(StreamConstants.ORG_MD2K_PUFFMARKER_FV);
                break;

            case DataSourceType.PUFFMARKER_SMOKING_EPISODE:
                output = new SmokingEpisode(context);
                output.register();
                outputHashMap.put(StreamConstants.ORG_MD2K_PUFFMARKER_SMOKING_EPISODE, output);
                streamProcessorWrapper.streamProcessor.registerCallbackDataArrayStream(StreamConstants.ORG_MD2K_PUFFMARKER_SMOKING_EPISODE);
                break;

            default:
                break;
        }
    }

    public void subscribe(String platformType, final String dataSourceType) throws DataKitException {
        DataSourceClient dataSourceClient = findDataSourceClient(platformType, dataSourceType);
        dataKitAPI.subscribe(dataSourceClient, new OnReceiveListener() {
            @Override
            public void onReceived(DataType dataType) {
                try {
                    DataTypeDoubleArray dataTypeDoubleArray = (DataTypeDoubleArray) dataType;
                    CSVDataPoint csvDataPoint = new CSVDataPoint(dataSourceTypeTOChannel.get(dataSourceType), dataTypeDoubleArray.getDateTime(), dataTypeDoubleArray.getSample()[0]);
                    streamProcessorWrapper.addDataPoint(csvDataPoint);
                } catch (Exception ignored) {

                }
            }
        });

    }

    private void unsubscribe(final String platformId) throws DataKitException {
        DataSourceClient dataSourceClientMBAccel = findDataSourceClient(PlatformType.MICROSOFT_BAND, platformId, DataSourceType.ACCELEROMETER);
        DataSourceClient dataSourceClientMBGyro = findDataSourceClient(PlatformType.MICROSOFT_BAND, platformId, DataSourceType.GYROSCOPE);

        DataSourceClient dataSourceClientMSAccel = findDataSourceClient(PlatformType.MOTION_SENSE, platformId, DataSourceType.ACCELEROMETER);
        DataSourceClient dataSourceClientMSGyro = findDataSourceClient(PlatformType.MOTION_SENSE, platformId, DataSourceType.GYROSCOPE);

        DataSourceClient dataSourceClientAW = findDataSourceClient(PlatformType.AUTOSENSE_WRIST, platformId, null);

        if (dataSourceClientMBAccel != null && dataSourceClientMBGyro != null) {
            dataKitAPI.unsubscribe(dataSourceClientMBAccel);
            dataKitAPI.unsubscribe(dataSourceClientMBGyro);

        } else if (dataSourceClientAW != null) {
            unsubscribe(PlatformType.AUTOSENSE_WRIST, platformId, DataSourceType.ACCELEROMETER_X);
            unsubscribe(PlatformType.AUTOSENSE_WRIST, platformId, DataSourceType.ACCELEROMETER_Y);
            unsubscribe(PlatformType.AUTOSENSE_WRIST, platformId, DataSourceType.ACCELEROMETER_Z);
            unsubscribe(PlatformType.AUTOSENSE_WRIST, platformId, DataSourceType.GYROSCOPE_X);
            unsubscribe(PlatformType.AUTOSENSE_WRIST, platformId, DataSourceType.GYROSCOPE_Y);
            unsubscribe(PlatformType.AUTOSENSE_WRIST, platformId, DataSourceType.GYROSCOPE_Z);

        } else if (dataSourceClientMSAccel != null && dataSourceClientMSGyro != null) {
            dataKitAPI.unsubscribe(dataSourceClientMSAccel);
            dataKitAPI.unsubscribe(dataSourceClientMSGyro);

        }
    }

    private void subscribe(final String platformId) throws DataKitException {
        String wrist = PUFFMARKER.LEFT_WRIST;
        if (PlatformId.RIGHT_WRIST.equals(platformId))
            wrist = PUFFMARKER.RIGHT_WRIST;

        DataSourceClient dataSourceClientMBAccel = findDataSourceClient(PlatformType.MICROSOFT_BAND, platformId, DataSourceType.ACCELEROMETER);
        DataSourceClient dataSourceClientMBGyro = findDataSourceClient(PlatformType.MICROSOFT_BAND, platformId, DataSourceType.GYROSCOPE);

        DataSourceClient dataSourceClientMSAccel = findDataSourceClient(PlatformType.MOTION_SENSE, platformId, DataSourceType.ACCELEROMETER);
        DataSourceClient dataSourceClientMSGyro = findDataSourceClient(PlatformType.MOTION_SENSE, platformId, DataSourceType.GYROSCOPE);

        DataSourceClient dataSourceClientAW = findDataSourceClient(PlatformType.AUTOSENSE_WRIST, platformId, null);

        if (dataSourceClientMBAccel != null && dataSourceClientMBGyro != null) {
            subscribeForThreeTuple(dataSourceClientMBAccel, platformId, DataSourceType.ACCELEROMETER, new int[]{1, 0, 2}, new int[]{-1, -1, -1});
            subscribeForThreeTuple(dataSourceClientMBGyro, platformId, DataSourceType.GYROSCOPE, new int[]{1, 0, 2}, new int[]{-1, -1, -1});

            streamProcessorWrapper.streamProcessor.settingWristFrequencies(wrist, Double.parseDouble(dataSourceClientMBAccel.getDataSource().getMetadata().get(METADATA.FREQUENCY)), Double.parseDouble(dataSourceClientMBGyro.getDataSource().getMetadata().get(METADATA.FREQUENCY)));

        } else if (dataSourceClientAW != null) {
            subscribe(PlatformType.AUTOSENSE_WRIST, platformId, DataSourceType.ACCELEROMETER_X);
            subscribe(PlatformType.AUTOSENSE_WRIST, platformId, DataSourceType.ACCELEROMETER_Y);
            subscribe(PlatformType.AUTOSENSE_WRIST, platformId, DataSourceType.ACCELEROMETER_Z);
            DataSourceClient dataSourceClientAWAccl = findDataSourceClient(PlatformType.AUTOSENSE_WRIST, platformId, DataSourceType.ACCELEROMETER_X);

            subscribe(PlatformType.AUTOSENSE_WRIST, platformId, DataSourceType.GYROSCOPE_X);
            subscribe(PlatformType.AUTOSENSE_WRIST, platformId, DataSourceType.GYROSCOPE_Y);
            subscribe(PlatformType.AUTOSENSE_WRIST, platformId, DataSourceType.GYROSCOPE_Z);
            DataSourceClient dataSourceClientAGyro = findDataSourceClient(PlatformType.AUTOSENSE_WRIST, platformId, DataSourceType.GYROSCOPE_X);

            streamProcessorWrapper.streamProcessor.settingWristFrequencies(wrist, Double.parseDouble(dataSourceClientAWAccl.getDataSource().getMetadata().get(METADATA.FREQUENCY)), Double.parseDouble(dataSourceClientAGyro.getDataSource().getMetadata().get(METADATA.FREQUENCY)));

        } else if (dataSourceClientMSAccel != null && dataSourceClientMSGyro != null) {
            subscribeForThreeTuple(dataSourceClientMSAccel, platformId, DataSourceType.ACCELEROMETER, new int[]{0, 1, 2}, new int[]{1, 1, 1});
            subscribeForThreeTuple(dataSourceClientMSGyro, platformId, DataSourceType.GYROSCOPE, new int[]{0, 1, 2}, new int[]{1, 1, 1});

            streamProcessorWrapper.streamProcessor.settingWristFrequencies(wrist, Double.parseDouble(dataSourceClientMSAccel.getDataSource().getMetadata().get(METADATA.FREQUENCY)), Double.parseDouble(dataSourceClientMSGyro.getDataSource().getMetadata().get(METADATA.FREQUENCY)));

        }
    }

    private void subscribe(String platformType, final String platformId, final String dataSourceType) throws DataKitException {
        DataSourceClient dataSourceClient = findDataSourceClient(platformType, platformId, dataSourceType);
        dataKitAPI.subscribe(dataSourceClient, new OnReceiveListener() {
            @Override
            public void onReceived(DataType dataType) {
                try {
                    DataTypeDoubleArray dataTypeDoubleArray = (DataTypeDoubleArray) dataType;
                    CSVDataPoint csvDataPoint = new CSVDataPoint(dataSourceTypeTOChannel.get(platformId + "_" + dataSourceType), dataTypeDoubleArray.getDateTime(), dataTypeDoubleArray.getSample()[0]);
                    streamProcessorWrapper.addDataPoint(csvDataPoint);
                } catch (Exception ignored) {

                }
            }
        });
    }

    private void subscribeForThreeTuple(DataSourceClient dataSourceClient, final String platformId, final String dataSourceId, final int[] convertedAxis, final int[] convertedSign) throws DataKitException {
        dataKitAPI.subscribe(dataSourceClient, new OnReceiveListener() {
            @Override
            public void onReceived(DataType dataType) {
                try {
                    DataTypeDoubleArray dataTypeDoubleArray = (DataTypeDoubleArray) dataType;
                    CSVDataPoint csvDataPointx = null;
                    CSVDataPoint csvDataPointy = null;
                    CSVDataPoint csvDataPointz = null;

                    if (DataSourceType.ACCELEROMETER.equals(dataSourceId)) {
                        csvDataPointx = new CSVDataPoint(dataSourceTypeTOChannel.get(platformId + "_" + DataSourceType.ACCELEROMETER_X), dataTypeDoubleArray.getDateTime(), convertedSign[0] * dataTypeDoubleArray.getSample()[convertedAxis[0]]);
                        csvDataPointy = new CSVDataPoint(dataSourceTypeTOChannel.get(platformId + "_" + DataSourceType.ACCELEROMETER_Y), dataTypeDoubleArray.getDateTime(), convertedSign[1] * dataTypeDoubleArray.getSample()[convertedAxis[1]]);
                        csvDataPointz = new CSVDataPoint(dataSourceTypeTOChannel.get(platformId + "_" + DataSourceType.ACCELEROMETER_Z), dataTypeDoubleArray.getDateTime(), convertedSign[2] * dataTypeDoubleArray.getSample()[convertedAxis[2]]);
                    } else if (DataSourceType.GYROSCOPE.equals(dataSourceId)) {
                        csvDataPointx = new CSVDataPoint(dataSourceTypeTOChannel.get(platformId + "_" + DataSourceType.GYROSCOPE_X), dataTypeDoubleArray.getDateTime(), convertedSign[0] * dataTypeDoubleArray.getSample()[convertedAxis[0]]);
                        csvDataPointy = new CSVDataPoint(dataSourceTypeTOChannel.get(platformId + "_" + DataSourceType.GYROSCOPE_Y), dataTypeDoubleArray.getDateTime(), convertedSign[1] * dataTypeDoubleArray.getSample()[convertedAxis[1]]);
                        csvDataPointz = new CSVDataPoint(dataSourceTypeTOChannel.get(platformId + "_" + DataSourceType.GYROSCOPE_Z), dataTypeDoubleArray.getDateTime(), convertedSign[2] * dataTypeDoubleArray.getSample()[convertedAxis[2]]);
                    }
                    streamProcessorWrapper.addDataPoint(csvDataPointx);
                    streamProcessorWrapper.addDataPoint(csvDataPointy);
                    streamProcessorWrapper.addDataPoint(csvDataPointz);
                } catch (Exception ignored) {

                }
            }
        });
    }


    public void unsubscribe(String platformType, final String dataSourceType) throws DataKitException {
        DataSourceClient dataSourceClient = findDataSourceClient(platformType, dataSourceType);
        if (dataSourceClient != null)
            dataKitAPI.unsubscribe(dataSourceClient);
    }

    public void unsubscribe(String platformType, final String platformId, final String dataSourceType) throws DataKitException {
        DataSourceClient dataSourceClient = findDataSourceClient(platformType, platformId, dataSourceType);
        if (dataSourceClient != null)
            dataKitAPI.unsubscribe(dataSourceClient);

    }


    protected DataSourceClient findDataSourceClient(String platformType, String dataSourceType) {
        PlatformBuilder platformBuilder = new PlatformBuilder().setType(platformType);
        DataSourceBuilder dataSourceBuilder = new DataSourceBuilder();
        dataSourceBuilder.setType(dataSourceType).setPlatform(platformBuilder.build());
        ArrayList<DataSourceClient> dataSourceClientArrayList = null;
        try {
            dataSourceClientArrayList = dataKitAPI.find(dataSourceBuilder);
        } catch (DataKitException e) {
            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Constants.INTENT_STOP));
            return null;
        }
        if (dataSourceClientArrayList.size() != 1) return null;
        return dataSourceClientArrayList.get(0);
    }

    protected DataSourceClient findDataSourceClient(String platformType, String platformId, String dataSourceType) {
        PlatformBuilder platformBuilder = new PlatformBuilder().setType(platformType).setId(platformId);
        DataSourceBuilder dataSourceBuilder = new DataSourceBuilder();
        if (dataSourceType != null && dataSourceType.length() != 1)
            dataSourceBuilder.setType(dataSourceType);
        dataSourceBuilder.setPlatform(platformBuilder.build());
        ArrayList<DataSourceClient> dataSourceClientArrayList = null;
        try {
            dataSourceClientArrayList = dataKitAPI.find(dataSourceBuilder);
        } catch (DataKitException e) {
            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Constants.INTENT_STOP));
            return null;
        }
        if (dataSourceClientArrayList.size() != 1)
            return null;
        return dataSourceClientArrayList.get(0);
    }
}
