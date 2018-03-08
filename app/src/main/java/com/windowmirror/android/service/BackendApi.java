package com.windowmirror.android.service;

import android.support.annotation.NonNull;

import com.windowmirror.android.model.service.Recording;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface BackendApi {

    @GET("/recording")
    Call<List<Recording>> getAllRecordings();

    @PATCH("/recording/{uuid}")
    Call<Recording> getRecording(@NonNull @Path("uuid") String uuid);

    @POST("/recording")
    Call<Recording> createRecording(@NonNull @Body Recording request);

    @PATCH("/recording/{uuid}")
    Call<Recording> updateRecording(@NonNull @Path("uuid") String uuid,
                                    @NonNull @Body Recording request);

    @DELETE("/recording/{uuid}")
    Call<Response<Void>> deleteRecording(@NonNull @Path("uuid") String uuid);
}
