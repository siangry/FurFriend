package com.example.furfriend.screen.calendar;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.GridLayout;
import android.widget.Toast;
import androidx.core.content.FileProvider;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.furfriend.Constants;
import com.example.furfriend.R;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;
import com.prolificinteractive.materialcalendarview.OnRangeSelectedListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ExportPdfPage extends AppCompatActivity {
    private Button btnDaily, btnWeekly, btnMonthly, exportButton;
    private FrameLayout calendarContainer;
    private ImageButton backButton;
    private TextView exportPdfTitle, exportPdfSubtitle;
    private int selectedMode = 0; // 0: Daily, 1: Weekly, 2: Monthly
    private MaterialCalendarView calendarView;
    private List<CalendarDay> selectedRange = new ArrayList<>();
    private CalendarDay selectedDay;
    private int selectedMonth = -1;
    private int selectedYear = Calendar.getInstance().get(Calendar.YEAR);
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private SimpleDateFormat firestoreDateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export_pdf);

        btnDaily = findViewById(R.id.btnDaily);
        btnWeekly = findViewById(R.id.btnWeekly);
        btnMonthly = findViewById(R.id.btnMonthly);
        exportButton = findViewById(R.id.exportButton);
        calendarContainer = findViewById(R.id.calendarContainer);
        backButton = findViewById(R.id.backButton);
        exportPdfTitle = findViewById(R.id.exportPdfTitle);
        exportPdfSubtitle = findViewById(R.id.exportPdfSubtitle);

        btnDaily.setOnClickListener(v -> selectMode(0));
        btnWeekly.setOnClickListener(v -> selectMode(1));
        btnMonthly.setOnClickListener(v -> selectMode(2));

        backButton.setOnClickListener(v -> finish());

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        exportButton.setOnClickListener(v -> fetchAndExportActivities());

        selectMode(0); // Default to Daily
    }

    private void selectMode(int mode) {
        selectedMode = mode;
        // All buttons use the same background, selection handled by setSelected
        btnDaily.setBackgroundResource(R.drawable.rounded_orange_button);
        btnWeekly.setBackgroundResource(R.drawable.rounded_orange_button);
        btnMonthly.setBackgroundResource(R.drawable.rounded_orange_button);
        btnDaily.setSelected(mode == 0);
        btnWeekly.setSelected(mode == 1);
        btnMonthly.setSelected(mode == 2);
        btnDaily.setTextColor(getResources().getColor(mode == 0 ? android.R.color.white : R.color.black));
        btnWeekly.setTextColor(getResources().getColor(mode == 1 ? android.R.color.white : R.color.black));
        btnMonthly.setTextColor(getResources().getColor(mode == 2 ? android.R.color.white : R.color.black));
        calendarContainer.removeAllViews();
        if (mode == 0) {
            showDailyCalendar();
        } else if (mode == 1) {
            showWeeklyCalendar();
        } else {
            showMonthPicker();
        }
    }

    private void showDailyCalendar() {
        calendarView = new MaterialCalendarView(this);
        calendarView.setSelectionMode(MaterialCalendarView.SELECTION_MODE_SINGLE);
        calendarView.setTopbarVisible(true);
        calendarView.setShowOtherDates(MaterialCalendarView.SHOW_ALL);
        calendarView.setSelectionColor(getResources().getColor(R.color.orange));
        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            selectedDay = date;
        });
        calendarContainer.addView(calendarView);
    }

    private void showWeeklyCalendar() {
        calendarView = new MaterialCalendarView(this);
        calendarView.setSelectionMode(MaterialCalendarView.SELECTION_MODE_RANGE);
        calendarView.setTopbarVisible(true);
        calendarView.setShowOtherDates(MaterialCalendarView.SHOW_ALL);
        calendarView.setSelectionColor(getResources().getColor(R.color.orange));
        calendarView.setOnRangeSelectedListener((widget, dates) -> {
            selectedRange = dates;
        });
        calendarContainer.addView(calendarView);
    }

    private void showMonthPicker() {
        View monthPicker = LayoutInflater.from(this).inflate(R.layout.view_month_picker, calendarContainer, false);
        GridLayout monthGrid = monthPicker.findViewById(R.id.monthGrid);
        TextView tvYear = monthPicker.findViewById(R.id.tvYear);
        ImageButton btnPrevYear = monthPicker.findViewById(R.id.btnPrevYear);
        ImageButton btnNextYear = monthPicker.findViewById(R.id.btnNextYear);

        // Set initial year
        tvYear.setText(String.valueOf(selectedYear));

        btnPrevYear.setOnClickListener(v -> {
            selectedYear--;
            tvYear.setText(String.valueOf(selectedYear));
        });
        btnNextYear.setOnClickListener(v -> {
            selectedYear++;
            tvYear.setText(String.valueOf(selectedYear));
        });

        for (int i = 0; i < monthGrid.getChildCount(); i++) {
            TextView monthView = (TextView) monthGrid.getChildAt(i);
            int monthIndex = i;
            monthView.setOnClickListener(v -> {
                for (int j = 0; j < monthGrid.getChildCount(); j++) {
                    monthGrid.getChildAt(j).setSelected(false);
                }
                v.setSelected(true);
                selectedMonth = monthIndex;
            });
        }
        calendarContainer.addView(monthPicker);
    }

    private void fetchAndExportActivities() {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("activities")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<Map<String, Object>> filteredActivities = new ArrayList<>();
                int totalActivities = 0;
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    totalActivities++;
                    Map<String, Object> activity = document.getData();
                    activity.put("id", document.getId());
                    String dateStr = (String) activity.get("date");
                    try {
                        Date activityDate = firestoreDateFormat.parse(dateStr);
                        boolean shouldInclude = shouldIncludeActivity(activityDate);
                        if (shouldInclude) {
                            filteredActivities.add(activity);
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                if (filteredActivities.isEmpty()) {
                    Toast.makeText(this, "No activities found for selected date", Toast.LENGTH_SHORT).show();
                    return;
                }
                generatePdf(filteredActivities);
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to fetch activities", Toast.LENGTH_SHORT).show();
            });
    }

    private boolean shouldIncludeActivity(Date activityDate) {
        if (selectedMode == 0 && selectedDay != null) {
            // Daily: match exact date
            Calendar cal1 = Calendar.getInstance();
            cal1.setTime(activityDate);
            Calendar cal2 = Calendar.getInstance();
            cal2.set(selectedDay.getYear(), selectedDay.getMonth(), selectedDay.getDay());
            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                    && cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
                    && cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
        } else if (selectedMode == 1 && selectedRange != null && !selectedRange.isEmpty()) {
            // Weekly: in selected range
            Calendar cal = Calendar.getInstance();
            cal.setTime(activityDate);
            Calendar start = Calendar.getInstance();
            Calendar end = Calendar.getInstance();
            CalendarDay startDay = selectedRange.get(0);
            CalendarDay endDay = selectedRange.get(selectedRange.size() - 1);
            start.set(startDay.getYear(), startDay.getMonth(), startDay.getDay(), 0, 0, 0);
            end.set(endDay.getYear(), endDay.getMonth(), endDay.getDay(), 23, 59, 59);
            return !cal.before(start) && !cal.after(end);
        } else if (selectedMode == 2 && selectedMonth >= 0) {
            // Monthly: match month and year
            Calendar cal = Calendar.getInstance();
            cal.setTime(activityDate);
            // selectedMonth is 0-based from Grid, but Calendar.MONTH is also 0-based, so this is correct
            return cal.get(Calendar.YEAR) == selectedYear && cal.get(Calendar.MONTH) == selectedMonth;
        }
        return false;
    }

    private void generatePdf(List<Map<String, Object>> activities) {
        try {
            // First fetch the username
            String userId = mAuth.getCurrentUser().getUid();
            db.collection(Constants.USERS)
                .document(userId)
                .collection(Constants.PROFILE)
                .document(Constants.USER_DETAILS)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String username = "User";
                    if (documentSnapshot.exists()) {
                        username = documentSnapshot.getString("username");
                        if (username == null || username.isEmpty()) {
                            username = "User";
                        }
                    }
                    generatePdfWithUsername(activities, username);
                })
                .addOnFailureListener(e -> {
                    // If failed to get username, use default
                    generatePdfWithUsername(activities, "User");
                });
        } catch (Exception e) {
            Toast.makeText(this, "Error generating PDF", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void generatePdfWithUsername(List<Map<String, Object>> activities, String username) {
        try {
            int pageWidth = 595; // A4 size in points (approx 8.3in)
            int pageHeight = 842; // A4 size in points (approx 11.7in)
            int margin = 32;
            int y = margin;
            PdfDocument document = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            Paint paint = new Paint();

            // Use Lexend font if available
            Typeface lexend = null;
            try {
                lexend = Typeface.createFromAsset(getAssets(), "fonts/lexend_regular.ttf");
            } catch (Exception e) {
                lexend = Typeface.DEFAULT;
            }

            // Draw logo
            Bitmap logo = BitmapFactory.decodeResource(getResources(), R.drawable.ic_logo);
            int logoSize = 64;
            if (logo != null) {
                Bitmap scaledLogo = Bitmap.createScaledBitmap(logo, logoSize, logoSize, true);
                canvas.drawBitmap(scaledLogo, (pageWidth - logoSize) / 2f, y, paint);
            }
            y += logoSize + 24; // More space after logo

            // Draw header
            paint.setTypeface(Typeface.create(lexend, Typeface.BOLD));
            paint.setTextSize(18);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("FurFriend -- Where Every Pawprint Matter", pageWidth / 2f, y, paint);
            y += 80; // Double the space after header (was 40)

            // User info and summary date
            paint.setTypeface(lexend);
            paint.setTextSize(14);
            paint.setTextAlign(Paint.Align.LEFT);
            String summaryText = username + "'s summary at " + getSummaryDateText();
            canvas.drawText(summaryText, margin, y, paint);
            y += 24; // More space before table

            // Draw table header
            paint.setTypeface(Typeface.create(lexend, Typeface.BOLD));
            paint.setTextSize(13);
            paint.setStrokeWidth(1);
            int[] colWidths = {32, 90, 70, 90, 180, 80}; // No., Date, Time, Title, Description, Pet(s)
            String[] headers = {"No.", "Date", "Time", "Title", "Description", "Pet(s)"};
            int x = margin;
            int cellPadding = 6;
            // Draw top line for table header
            canvas.drawLine(margin, y, pageWidth - margin, y, paint);
            y += 16; // Add space after top line
            for (int i = 0; i < headers.length; i++) {
                paint.setTypeface(Typeface.create(lexend, Typeface.BOLD));
                canvas.drawText(headers[i], x + cellPadding, y, paint);
                x += colWidths[i];
            }
            y += 16;
            // Draw bottom line for table header
            canvas.drawLine(margin, y, pageWidth - margin, y, paint);
            y += 8;

            // Draw table rows
            for (Map<String, Object> activity : activities) {
                paint.setTypeface(lexend);
                paint.setTextSize(12);
                x = margin;
                String date = (String) activity.get("date");
                String time = (String) activity.get("time");
                String title = (String) activity.get("title");
                String desc = (String) activity.get("description");
                List<Map<String, String>> pets = (List<Map<String, String>>) activity.get("pets");
                StringBuilder petNames = new StringBuilder();
                if (pets != null) {
                    for (int i = 0; i < pets.size(); i++) {
                        if (i > 0) petNames.append(", ");
                        petNames.append(pets.get(i).get("petName"));
                    }
                }
                String[] row = {String.format("%02d", activities.indexOf(activity) + 1), date, time, title, desc, petNames.toString()};
                int maxCellHeight = 0;
                int[] cellHeights = new int[row.length];
                // Calculate max cell height for this row
                for (int i = 0; i < row.length; i++) {
                    cellHeights[i] = getMultilineTextHeight(row[i], colWidths[i] - 2 * cellPadding, paint) + 2 * cellPadding;
                    if (cellHeights[i] > maxCellHeight) maxCellHeight = cellHeights[i];
                }
                // Draw each cell with padding
                x = margin;
                for (int i = 0; i < row.length; i++) {
                    paint.setTypeface(lexend);
                    drawMultilineText(canvas, row[i], x + cellPadding, y + cellPadding, colWidths[i] - 2 * cellPadding, paint);
                    x += colWidths[i];
                }
                y += maxCellHeight;
                if (y > pageHeight - margin - 32) break; // Avoid overflow
            }

            document.finishPage(page);
            // Save PDF
            File pdfDir = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "furfriend_exports");
            if (!pdfDir.exists()) {
                boolean created = pdfDir.mkdirs();
                if (!created) {
                    Toast.makeText(this, "Failed to create PDF directory", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            String fileName = generatePdfFileName();
            File pdfFile = new File(pdfDir, fileName);
            FileOutputStream fos = new FileOutputStream(pdfFile);
            document.writeTo(fos);
            document.close();
            fos.close();
            
            Toast.makeText(this, "PDF saved successfully", Toast.LENGTH_SHORT).show();
            openPdf(pdfFile);
        } catch (IOException e) {
            Toast.makeText(this, "Failed to save PDF", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } catch (Exception e) {
            Toast.makeText(this, "Error generating PDF", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private int getMultilineTextHeight(String text, int width, Paint paint) {
        int lineHeight = (int) (paint.getTextSize() + 2);
        int start = 0;
        int end;
        int lines = 0;
        while (start < text.length()) {
            end = paint.breakText(text, start, text.length(), true, width, null) + start;
            lines++;
            start = end;
        }
        return Math.max(lineHeight * lines, lineHeight);
    }

    private void drawMultilineText(Canvas canvas, String text, float x, float y, int width, Paint paint) {
        int lineHeight = (int) (paint.getTextSize() + 2);
        int start = 0;
        int end;
        int yOffset = 0;
        while (start < text.length()) {
            end = paint.breakText(text, start, text.length(), true, width, null) + start;
            canvas.drawText(text.substring(start, end), x, y + yOffset, paint);
            yOffset += lineHeight;
            start = end;
        }
    }

    private String getSummaryDateText() {
        if (selectedMode == 0 && selectedDay != null) {
            return String.format(Locale.getDefault(), "%d %s %d", selectedDay.getDay(),
                    getMonthShortName(selectedDay.getMonth()), selectedDay.getYear());
        } else if (selectedMode == 1 && selectedRange != null && !selectedRange.isEmpty()) {
            CalendarDay start = selectedRange.get(0);
            CalendarDay end = selectedRange.get(selectedRange.size() - 1);
            return String.format(Locale.getDefault(), "%d %s %d - %d %s %d",
                    start.getDay(), getMonthShortName(start.getMonth()), start.getYear(),
                    end.getDay(), getMonthShortName(end.getMonth()), end.getYear());
        } else if (selectedMode == 2 && selectedMonth >= 0) {
            // selectedMonth is 0-based, so add 1 for display
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.MONTH, selectedMonth);
            return String.format(Locale.getDefault(), "%s %d",
                    getMonthShortName(cal.get(Calendar.MONTH)), selectedYear);
        }
        return "";
    }

    private String getMonthShortName(int month) {
        return new SimpleDateFormat("MMM", Locale.getDefault()).format(new Date(0, month, 1));
    }

    private void openPdf(File pdfFile) {
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", pdfFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to open PDF", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private String generatePdfFileName() {
        String prefix = "FurFriend_";
        String mode = "";
        String dateInfo = "";
        
        switch (selectedMode) {
            case 0: // Daily
                if (selectedDay != null) {
                    mode = "Daily_";
                    dateInfo = String.format(Locale.getDefault(), "%d_%s_%02d",
                            selectedDay.getYear(),
                            getMonthShortName(selectedDay.getMonth()),
                            selectedDay.getDay());
                }
                break;
            case 1: // Weekly
                if (selectedRange != null && !selectedRange.isEmpty()) {
                    mode = "Weekly_";
                    CalendarDay start = selectedRange.get(0);
                    CalendarDay end = selectedRange.get(selectedRange.size() - 1);
                    dateInfo = String.format(Locale.getDefault(), "%d_%s_%02d_to_%s_%02d",
                            start.getYear(),
                            getMonthShortName(start.getMonth()),
                            start.getDay(),
                            getMonthShortName(end.getMonth()),
                            end.getDay());
                }
                break;
            case 2: // Monthly
                if (selectedMonth >= 0) {
                    mode = "Monthly_";
                    dateInfo = String.format(Locale.getDefault(), "%d_%s",
                            selectedYear,
                            getMonthShortName(selectedMonth));
                }
                break;
        }
        
        return prefix + mode + dateInfo + ".pdf";
    }
} 