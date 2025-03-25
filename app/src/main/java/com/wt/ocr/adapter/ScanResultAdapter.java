package com.wt.ocr.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.wt.ocr.R;
import com.wt.ocr.data.ScannedImage;
import java.util.List;

public class ScanResultAdapter extends RecyclerView.Adapter<ScanResultAdapter.ViewHolder> {
    private List<ScannedImage> scanResults;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView textView;

        public ViewHolder(View view) {
            super(view);
            textView = view.findViewById(R.id.resultText);
        }
    }

    public ScanResultAdapter(List<ScannedImage> scanResults) {
        this.scanResults = scanResults;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.scan_result_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ScannedImage result = scanResults.get(position);
        holder.textView.setText(result.toString());
    }

    @Override
    public int getItemCount() {
        return scanResults.size();
    }

    public void updateResults(List<ScannedImage> newResults) {
        this.scanResults = newResults;
        notifyDataSetChanged();
    }
} 