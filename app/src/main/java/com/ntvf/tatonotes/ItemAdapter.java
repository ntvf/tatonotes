package com.ntvf.tatonotes;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.icu.util.Calendar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {

    private List<DatabaseHelper.Item> itemList;
    private Context context;
    private DatabaseHelper databaseHelper;

    public ItemAdapter(List<DatabaseHelper.Item> itemList, Context context, DatabaseHelper databaseHelper) {
        this.itemList = itemList;
        this.context = context;
        this.databaseHelper = databaseHelper;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_view_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DatabaseHelper.Item item = itemList.get(position);
        holder.textView.setText(item.getText().length() > 100 ? item.getText().substring(0, 100) + "..." : item.getText());

        if (item.getTimestamp() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            String formattedDate = sdf.format(new Date(item.getTimestamp()));
            holder.timestampView.setText(formattedDate);
        } else {
            holder.timestampView.setText(context.getText(R.string.no_date_set));
        }

        long now = System.currentTimeMillis();
        if (item.getAlarmTimestamp() > 0 && item.getAlarmTimestamp() >= now) {
            holder.alarmButton.setColorFilter(ContextCompat.getColor(context, R.color.green));
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            String alarmDate = sdf.format(new Date(item.getAlarmTimestamp()));
            holder.alarmTimestampView.setText(alarmDate);
        } else {
            holder.alarmButton.setColorFilter(ContextCompat.getColor(context, R.color.black));
            holder.alarmTimestampView.setText("");
        }

        holder.itemView.setOnClickListener(v -> showEditDialog(item, position));

        holder.alarmButton.setOnClickListener(v -> showDateTimePickerDialog(item, holder));

    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public void addItem(DatabaseHelper.Item item) {
        itemList.add(0, item);
        notifyItemInserted(0);
    }

    private void showEditDialog(DatabaseHelper.Item item, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getText(R.string.edit));

        View viewInflated = LayoutInflater.from(context).inflate(R.layout.edit_text_dialog, (ViewGroup) null, false);
        final EditText input = viewInflated.findViewById(R.id.input);
        input.setText(item.getText());

        builder.setView(viewInflated);

        builder.setNegativeButton(context.getText(R.string.cancel), (dialog, which) -> dialog.cancel());

        builder.setNeutralButton(context.getText(R.string.delete_alarm), (dialog, which) -> {
            databaseHelper.deleteItem(item.getId());
            itemList.remove(position);
            notifyItemRemoved(position);
        });

        AlertDialog dialog = builder.create();

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                String updatedText = s.toString();
                Button button = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

                if (!updatedText.equals(item.getText())) {
                    button.setText(context.getText(R.string.save));
                    button.setOnClickListener(v -> {
                        item.setText(updatedText);
                        databaseHelper.updateItem(item.getId(), updatedText, item.getTimestamp());
                        notifyItemChanged(position);
                        dialog.dismiss();
                    });
                } else {
                    button.setText(context.getText(R.string.cancel));
                    button.setOnClickListener(v -> dialog.dismiss());
                }
            }
        });

        dialog.show();
    }

    private void showDateTimePickerDialog(DatabaseHelper.Item item, ViewHolder holder) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(context, (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            TimePickerDialog timePickerDialog = new TimePickerDialog(context, (view1, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);

                long timestamp = calendar.getTimeInMillis();
                item.setAlarmTimestamp(timestamp);
                databaseHelper.updateItemAlarm(item.getId(), timestamp);
                setAlarm(context, item, timestamp);
                notifyItemChanged(holder.getAdapterPosition());

            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);

            timePickerDialog.show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        if(item.getAlarmTimestamp() >= System.currentTimeMillis()) {
            datePickerDialog.setButton(DialogInterface.BUTTON_NEUTRAL, context.getText(R.string.delete_alarm), (dialog, which) -> {
                deleteAlarm(context, item.getId());

                databaseHelper.updateItemAlarm(item.getId(), 0);
                holder.alarmTimestampView.setText("");
                holder.alarmButton.setColorFilter(ContextCompat.getColor(context, R.color.black));

            });
        }

        datePickerDialog.show();
    }

    private void deleteAlarm(Context context, long itemId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, (int) itemId, intent, PendingIntent.FLAG_IMMUTABLE);

        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
    }

    private void setAlarm(Context context, DatabaseHelper.Item item, long timestamp) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("item_id", item.getId());
        intent.putExtra("item_text", item.getText());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, item.getId(), intent, PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, timestamp, pendingIntent);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        TextView timestampView;
        ImageButton alarmButton;
        TextView alarmTimestampView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.reminderTextContent);
            timestampView = itemView.findViewById(R.id.reminderCreatedAtText);
            alarmButton = itemView.findViewById(R.id.alarmButton);
            alarmTimestampView = itemView.findViewById(R.id.alarmTimestamp);
        }
    }
}
