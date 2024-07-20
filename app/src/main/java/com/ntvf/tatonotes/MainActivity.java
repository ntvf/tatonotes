package com.ntvf.tatonotes;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.floatingActionButton);
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.main_list_item);

        try (DatabaseHelper databaseHelper = new DatabaseHelper(this)) {

            SharedPreferences prefs = getSharedPreferences("com.ntvf.tatonotes", MODE_PRIVATE);
            if (prefs.getBoolean("first_run", true)) {
                String initialReminder = getString(R.string.initial_reminder);
                long timestamp = System.currentTimeMillis();
                databaseHelper.addItem(initialReminder, timestamp);
                prefs.edit().putBoolean("first_run", false).apply();
            }

            List<DatabaseHelper.Item> itemList = databaseHelper.getAllItems();
            ItemAdapter itemAdapter = new ItemAdapter(itemList, this, databaseHelper);

            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(itemAdapter);

            DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this);
            recyclerView.addItemDecoration(dividerItemDecoration);



            floatingActionButton.setOnClickListener(v -> showInputDialog(databaseHelper, itemAdapter));

        }
    }


    private void showInputDialog(DatabaseHelper databaseHelper, ItemAdapter itemAdapter) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getText(R.string.enter_text));

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton(getText(R.string.ok), (dialog, which) -> {
            String text = input.getText().toString();
            if (!text.isEmpty()) {
                long timestamp = System.currentTimeMillis(); // Use current timestamp
                DatabaseHelper.Item newItem = new DatabaseHelper.Item();
                newItem.setText(text);
                newItem.setTimestamp(timestamp);
                databaseHelper.addItem(text, timestamp);
                itemAdapter.addItem(newItem);
            }
        });

        builder.setNegativeButton(getText(R.string.cancel), (dialog, which) -> dialog.cancel());

        builder.show();
    }

}