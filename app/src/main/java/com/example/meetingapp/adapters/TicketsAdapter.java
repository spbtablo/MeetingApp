package com.example.meetingapp.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.amar.library.ui.presenter.StickyScrollPresenter;
import com.example.meetingapp.R;
import com.example.meetingapp.activities.TicketActivity;
import com.example.meetingapp.models.Ticket;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;

public class TicketsAdapter extends RecyclerView.Adapter<TicketsAdapter.ViewHolder> {

    private List<Ticket> tickets;
    private Context context;
    private List<Integer> ticketsIds;

    public TicketsAdapter(Context context, List<Ticket> tickets) {
        this.tickets = tickets;
        this.context = context;

        ticketsIds = new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.ticket_item, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final Ticket ticket = tickets.get(position);

        holder.textName.setText(ticket.getName());
        holder.textPlace.setText(ticket.getAddress());
        holder.textPrice.setText(ticket.getPrice() + " ₽");
        holder.textDate.setText(parseDate(ticket.getDate()));
        holder.textTime.setText(parseTime(ticket.getTime()));

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, TicketActivity.class);
            intent.putExtra("EXTRA_TICKET_ID", String.valueOf(ticket.getId()));

            context.startActivity(intent);
        });

//        holder.buttonSendRequest.setOnClickListener(v -> {
//            sendRequest(String.valueOf(ticket.getCreator().getId()), ticket.getId());
//            removeItemAfterRequest(position);
//        });
    }

    private String parseTime(String time) {
        @SuppressLint("SimpleDateFormat") DateFormat format = new SimpleDateFormat("hh:mm:ss");
        Date date = null;
        try {
            date = format.parse(time);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        @SuppressLint("SimpleDateFormat") String newDate = new SimpleDateFormat("hh:mm").format(Objects.requireNonNull(date));
        return newDate;
    }

    private String parseDate(String dateString) {
        @SuppressLint("SimpleDateFormat") DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Date date = null;
        try {
            date = format.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        @SuppressLint("SimpleDateFormat") String newDate = new SimpleDateFormat("dd MMMM", new Locale("RU")).format(Objects.requireNonNull(date));
        return newDate;
    }


    private void sendRequest(String toUser, long ticket) {

    }

    private void removeItemAfterRequest(int position) {
        tickets.remove(position);
        ticketsIds = new ArrayList<>();

        notifyItemRemoved(position);
        notifyItemRangeChanged(position, getItemCount());
    }


    @Override
    public int getItemCount() {
        return tickets.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.name)
        TextView textName;

        @BindView(R.id.place)
        TextView textPlace;

        @BindView(R.id.price)
        TextView textPrice;

        @BindView(R.id.date)
        TextView textDate;

        @BindView(R.id.time)
        TextView textTime;

        ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
