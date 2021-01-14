package com.example.bubbleshooterfp;

import android.graphics.Bitmap;
import android.graphics.RectF;

public class Bubble {
    Bitmap bitmap;
    RectF rect;
    float x;
    float y;
    float xV;
    float yV;
    int colorIndex;
    boolean active;
    float radius;
    boolean impulse;

    public Bubble(Bitmap inputBitmap, int colorInd, boolean isActive) {
        bitmap = inputBitmap;
        radius = (float) ((inputBitmap.getWidth() * Math.sqrt(2)) + 1) / 4;
        active = isActive;
        impulse = false;
        colorIndex = -1;
        if (isActive) {
            colorIndex = colorInd;
            rect = new RectF(x - radius, y - radius, x + radius, y + radius);
        }
    }

    public Bubble(float x, float y, Bitmap bitmap, int colInd, boolean isActive) {
        this.bitmap = bitmap;
        radius = (float) ((bitmap.getWidth() * Math.sqrt(2)) + 1) / 4;
        this.x = x;
        this.y = y;
        active = isActive;
        impulse = false;
        colorIndex = -1;
        if (isActive) {
            colorIndex = colInd;
            rect = new RectF(x - radius, y - radius, x + radius, y + radius);
        }
    }

    public Bubble(Bubble bubble, float posX, float posY) {
        this.x = posX;
        this.y = posY;
        this.bitmap = bubble.bitmap;
        this.colorIndex = bubble.colorIndex;
        active = true;
        this.impulse = bubble.impulse;
        rect = new RectF(x - radius, y - radius, x + radius, y + radius);
    }

    public void invertXSpeed() {
        xV = -xV;
    }

    public void invertYSpeed() {
        yV = -yV;
    }

    public void BallUpdate(long fps) {
        if ((xV != 0) & (yV != 0f)) {
            rect.left = rect.left + (xV / fps);
            rect.top = rect.top + (yV / fps);
            rect.right = rect.left + (radius * 2);
            rect.bottom = rect.top + (radius * 2);
            x = x + (xV / fps);
            y = y + (yV / fps);
        }
    }

    public RectF getRect() {
        return this.rect;
    }

    public void shoot(Line line) {
        float disX = (line.xEnd - line.xStart);
        float disY = (line.yEnd - line.yStart);
        xV = disX;
        yV = disY;
        this.impulse = true;
    }
}

