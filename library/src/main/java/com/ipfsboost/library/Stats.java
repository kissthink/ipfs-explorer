package com.ipfsboost.library;

import com.ipfsboost.library.entity.Stats_bw;
import com.ipfsboost.library.service.CommandService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;

public class Stats {
    Retrofit retrofit;

    public Stats(Retrofit retrofit) {
        this.retrofit = retrofit;
    }

    public void bitswap() {
    }

    public void bw(Callback<Stats_bw> callback) {
        CommandService.stats stats = retrofit.create(CommandService.stats.class);
        Call<Stats_bw> bw = stats.bw();
        bw.enqueue(callback);

    }

    public void repo() {
    }


}
