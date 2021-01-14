package com.example.bubbleshooterfp;

public class Line {
    float xStart;
    float xEnd;
    float yStart;
    float yEnd;


    public Line(float xStart, float yStart, float endX, float endY)
    {
        this.xStart = xStart;
        this.yStart = yStart;
        this.xEnd = endX;
        this.yEnd = endY;
    }

    public void update(float endX, float endY)
    {
        this.xEnd = endX;
        this.yEnd = endY;
    }
}
