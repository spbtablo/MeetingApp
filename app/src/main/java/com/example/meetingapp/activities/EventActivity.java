package com.example.meetingapp.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.example.meetingapp.R;
import com.example.meetingapp.adapters.EventsAdapter;
import com.example.meetingapp.api.UserClient;
import com.example.meetingapp.models.Event;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class EventActivity extends AppCompatActivity {

    static final String BASE_URL = "http://10.0.2.2:8000/";

    private TextView textViewEventId;
    private TextView textViewEventName;
    private Event event;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);

        textViewEventId = findViewById(R.id.textViewEventId);
        textViewEventName = findViewById(R.id.textViewEventName);

        loadEvent();
    }

    private void loadEvent() {
        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create());

        Retrofit retrofit = builder.build();
        UserClient userClient = retrofit.create(UserClient.class);

        String pk = getIntent().getStringExtra("eventId");
        Call<Event> call = userClient.getEvent(pk, "Token 9ba875f0b1b909484e327292bd5d01be30c75791");

        call.enqueue(new Callback<Event>() {
            @Override
            public void onResponse(Call<Event> call, Response<Event> response) {
                event = response.body();
                putEvent();
            }

            @Override
            public void onFailure(Call<Event> call, Throwable t) {

            }
        });
    }

    private void putEvent(){
        textViewEventId.setText(String.valueOf(event.getId()));
        textViewEventName.setText(event.getName());
    }
}