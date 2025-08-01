package com.lionido.dreams_track.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WordCloudView extends View {
    public static class Word {
        public String text;
        public int frequency;
        public Word(String text, int frequency) {
            this.text = text;
            this.frequency = frequency;
        }
    }

    private List<Word> words = new ArrayList<>();
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Random random = new Random();

    public WordCloudView(Context context) {
        super(context);
    }
    public WordCloudView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public WordCloudView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setWords(List<Word> words) {
        this.words = words;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (words == null || words.isEmpty()) return;
        int width = getWidth();
        int height = getHeight();
        int n = words.size();
        for (int i = 0; i < n; i++) {
            Word word = words.get(i);
            float size = 32 + word.frequency * 8;
            paint.setTextSize(size);
            paint.setColor(getRandomColor(i));
            float x = random.nextInt(Math.max(1, width - (int)paint.measureText(word.text)));
            float y = 60 + random.nextInt(Math.max(1, height - 60));
            canvas.drawText(word.text, x, y, paint);
        }
    }

    private int getRandomColor(int seed) {
        int[] palette = {Color.BLUE, Color.RED, Color.GREEN, Color.MAGENTA, Color.CYAN, Color.DKGRAY, Color.BLACK, Color.rgb(255,140,0)};
        return palette[seed % palette.length];
    }
}