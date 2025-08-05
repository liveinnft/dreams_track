package com.lionido.dreams_track.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.lionido.dreams_track.R;
import com.lionido.dreams_track.model.Symbol;
import java.util.List;

public class SymbolsAdapter extends RecyclerView.Adapter<SymbolsAdapter.SymbolViewHolder> {

    private List<Symbol> symbols;
    private Context context;

    public SymbolsAdapter(Context context, List<Symbol> symbols) {
        this.context = context;
        this.symbols = symbols;
    }

    @NonNull
    @Override
    public SymbolViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_symbol, parent, false);
        return new SymbolViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SymbolViewHolder holder, int position) {
        Symbol symbol = symbols.get(position);
        holder.bind(symbol);
    }

    @Override
    public int getItemCount() {
        return symbols != null ? symbols.size() : 0;
    }

    public void updateSymbols(List<Symbol> newSymbols) {
        this.symbols = newSymbols;
        notifyDataSetChanged();
    }

    static class SymbolViewHolder extends RecyclerView.ViewHolder {
        private TextView tvSymbolKeyword;
        private TextView tvEmotionBadge;
        private TextView tvArchetype;
        private TextView tvInterpretation;

        public SymbolViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSymbolKeyword = itemView.findViewById(R.id.textKeyword);
            tvEmotionBadge = itemView.findViewById(R.id.textEmotion);
            tvArchetype = itemView.findViewById(R.id.textSymbol);
            tvInterpretation = itemView.findViewById(R.id.textInterpretation);
        }

        public void bind(Symbol symbol) {
            // Устанавливаем ключевое слово
            tvSymbolKeyword.setText(capitalizeFirst(symbol.getKeyword()));

            // Устанавливаем эмоцию
            tvEmotionBadge.setText(symbol.getEmotionDisplayName());
            tvEmotionBadge.setBackgroundResource(getEmotionBackgroundResource(symbol.getEmotion()));

            // Устанавливаем архетип если есть
            if (symbol.hasArchetype()) {
                tvArchetype.setText("Архетип: " + symbol.getArchetypeDisplayName());
                tvArchetype.setVisibility(View.VISIBLE);
            } else {
                tvArchetype.setVisibility(View.GONE);
            }

            // Устанавливаем интерпретацию
            tvInterpretation.setText(symbol.getInterpretation());
        }

        private String capitalizeFirst(String text) {
            if (text == null || text.isEmpty()) {
                return text;
            }
            return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
        }

        private int getEmotionBackgroundResource(String emotion) {
            switch (emotion.toLowerCase()) {
                case "fear":
                case "anxiety":
                    return R.drawable.emotion_badge_fear;
                case "joy":
                case "empowerment":
                    return R.drawable.emotion_badge_joy;
                case "sadness":
                case "despair":
                    return R.drawable.emotion_badge_sadness;
                case "anger":
                    return R.drawable.emotion_badge_anger;
                case "surprise":
                    return R.drawable.emotion_badge_surprise;
                case "calm":
                    return R.drawable.emotion_badge_calm;
                case "love":
                    return R.drawable.emotion_badge_love;
                case "shame":
                    return R.drawable.emotion_badge_shame;
                case "confusion":
                    return R.drawable.emotion_badge_confusion;
                default:
                    return R.drawable.emotion_badge_neutral;
            }
        }
    }
}