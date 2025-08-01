package com.lionido.dreams_track.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lionido.dreams_track.R;
import com.lionido.dreams_track.model.Dream;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DreamAdapter extends RecyclerView.Adapter<DreamAdapter.DreamViewHolder> {

    private List<Dream> dreamsList;
    private OnDreamClickListener listener;

    public interface OnDreamClickListener {
        void onDreamClick(Dream dream);
    }

    public interface OnDreamDeleteListener {
        void onDreamDelete(Dream dream, int position);
    }
    private OnDreamDeleteListener deleteListener;
    public void setOnDreamDeleteListener(OnDreamDeleteListener listener) {
        this.deleteListener = listener;
    }

    public DreamAdapter(List<Dream> dreamsList, OnDreamClickListener listener) {
        this.dreamsList = dreamsList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DreamViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dream, parent, false);
        return new DreamViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DreamViewHolder holder, int position) {
        Dream dream = dreamsList.get(position);
        holder.bind(dream);
        holder.itemView.setOnLongClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDreamDelete(dream, position);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return dreamsList.size();
    }

    public void removeDream(int position) {
        dreamsList.remove(position);
        notifyItemRemoved(position);
    }

    class DreamViewHolder extends RecyclerView.ViewHolder {
        private TextView tvDreamText;
        private TextView tvDreamDate;

        public DreamViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDreamText = itemView.findViewById(R.id.tv_dream_text);
            tvDreamDate = itemView.findViewById(R.id.tv_dream_date);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onDreamClick(dreamsList.get(position));
                }
            });
        }

        public void bind(Dream dream) {
            // Отображаем первые 50 символов текста сна
            String displayText = dream.getText();
            if (displayText.length() > 50) {
                displayText = displayText.substring(0, 50) + "...";
            }
            tvDreamText.setText(displayText);

            // Форматируем дату
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            String dateText = sdf.format(new Date(dream.getTimestamp()));
            tvDreamDate.setText(dateText);
        }
    }
}