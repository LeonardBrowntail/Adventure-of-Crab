package com.example.bubbleshooterfp;

import android.graphics.Bitmap;

public class Button {
    int x;
    int y;
    int left;
    int top;
    int right;
    int bottom;
    Bitmap bitmap;

    public Button(Bitmap inBitmap, int xPos, int yPos) {
        x = xPos;
        y = yPos;
        bitmap = inBitmap;
        top = y - (bitmap.getHeight() / 2);
        left = x - (bitmap.getWidth() / 2);
        right = x + (bitmap.getWidth() / 2);
        bottom = y + (bitmap.getHeight() / 2);
    }
}
