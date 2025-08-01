package com.lionido.dreams_track.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lionido.dreams_track.R;
import com.lionido.dreams_track.model.Symbol;

import java.util.List;

public class SymbolAdapter extends RecyclerView.Adapter<SymbolAdapter.SymbolViewHolder> {

    private List<Symbol> symbolsList;

    public SymbolAdapter(List<Symbol> symbolsList) {
        this.symbolsList = symbolsList;
    }

    @NonNull
    @Override
    public SymbolViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_symbol, parent, false);
        return new SymbolViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SymbolViewHolder holder, int position) {
        Symbol symbol = symbolsList.get(position);
        holder.bind(symbol);
    }

    @Override
    public int getItemCount() {
        return symbolsList.size();
    }

    static class SymbolViewHolder extends RecyclerView.ViewHolder {
        private TextView tvSymbolKeyword;
        private TextView tvSymbolInterpretation;

        public SymbolViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSymbolKeyword = itemView.findViewById(R.id.tv_symbol_keyword);
            tvSymbolInterpretation = itemView.findViewById(R.id.tv_symbol_interpretation);
        }

        public void bind(Symbol symbol) {
            tvSymbolKeyword.setText("🔍 " + symbol.getKeyword());
            tvSymbolInterpretation.setText(symbol.getInterpretation());
        }
    }
}