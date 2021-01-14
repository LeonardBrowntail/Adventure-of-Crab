package com.example.bubbleshooterfp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.Random;
import java.util.Stack;

public class GameScreen extends Activity {
    BubbleShooting bubbleShooting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bubbleShooting = new BubbleShooting(this);
        setContentView(bubbleShooting);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bubbleShooting.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        bubbleShooting.pause();
    }

    class BubbleShooting extends SurfaceView implements Runnable {

        Thread gameThread = null;

        SurfaceHolder ourHolder;
        volatile boolean playing;

        // Win - lose condition
        // States:
        // paused = -1
        // running = 0
        // end = 1
        int state = -1;
        boolean win = false;

        Canvas canvas;
        Paint paint;

        long fps;
        long timeThisFrame;

        //Screen Size
        float screenWidth;
        float screenHeight;

        //Grid
        float vGrid;
        float hGrid;

        //Ready-to-shoot bubble default position
        float startX;
        float startY;

        //To detect whether the shotBubble is moving or not
        boolean isMoving = false;

        //Player's score
        int score;
        Stack<Vector2> destroyStack = new Stack<>();

        //SFX
        SoundPool sfx;
        int hitWall = -1;
        int hitBubble = -1;
        int bubblePop = -1;
        int shoot = -1;

        //player controlled Bubble
        Bubble mainBubble;

        //Bubbles array
        Bubble[][] bubbles = new Bubble[10][20];
        //Bubbles bitmaps
        Bitmap[] bubblesDef = new Bitmap[3];
        //Background bitmap
        Bitmap background;
        //Number of active bubbles
        int activeBubbles;

        //Current bubble cord
        int selectColor;

        //Bubble Size
        int bubbleSize;
        float bubbleRadius;

        //Line
        Line line = null;

        //============= Main Menu ================//
        Button title, play, restart, credit, exit, pause;
        Bitmap pens, gameTech, crab;
        boolean showCredit = false;

        public BubbleShooting(Context context) {
            super(context);
            //========= Game =========//
            ourHolder = getHolder();
            paint = new Paint();

            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);

            screenWidth = size.x;
            screenHeight = size.y;

            startX = screenWidth / 2.0f;
            startY = screenHeight * 0.93f;

            vGrid = screenWidth / 10;
            hGrid = screenHeight / 20;
            background = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.bg), (int) screenWidth, (int) screenHeight, false);
            bubblesDef[0] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.bubble0), (int) vGrid, (int) vGrid, false);
            bubblesDef[1] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.bubble1), (int) vGrid, (int) vGrid, false);
            bubblesDef[2] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.bubble2), (int) vGrid, (int) vGrid, false);

            bubbleSize = bubblesDef[0].getWidth();
            bubbleRadius = (float) bubbleSize / 2;

            vGrid = screenWidth / 10;
            hGrid = screenHeight / 20;

            mainBubble = new Bubble(bubblesDef[0], 0, false);
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 20; j++) {
                    bubbles[i][j] = new Bubble(bubblesDef[0], 0, false);
                }
            }

            //Sound handler
            sfx = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
            try {
                AssetManager assetManager = context.getAssets();
                AssetFileDescriptor descriptor;

                descriptor = assetManager.openFd("bubblepop.ogg");
                bubblePop = sfx.load(descriptor, 0);

                descriptor = assetManager.openFd("hitwall.ogg");
                hitWall = sfx.load(descriptor, 0);

                descriptor = assetManager.openFd("hitbubble.ogg");
                hitBubble = sfx.load(descriptor, 0);

                descriptor = assetManager.openFd("shoot.ogg");
                shoot = sfx.load(descriptor, 0);

            } catch (IOException e) {
                Log.e("Error", "Failed to load sound files");
            }

            //========= Main Menu =========//
            Bitmap pauseButton = BitmapFactory.decodeResource(this.getResources(), R.drawable.back);
            play = new Button(BitmapFactory.decodeResource(this.getResources(), R.drawable.play), (int) screenWidth / 2, (int) screenHeight / 2);
            title = new Button(BitmapFactory.decodeResource(this.getResources(), R.drawable.judul), (int) screenWidth / 2, (int) (screenHeight * 0.25f));
            restart = new Button(BitmapFactory.decodeResource(this.getResources(), R.drawable.restrt), (int) screenWidth / 2, play.y + 150);
            credit = new Button(BitmapFactory.decodeResource(this.getResources(), R.drawable.credits), (int) screenWidth / 2, play.y + 300);
            exit = new Button(BitmapFactory.decodeResource(this.getResources(), R.drawable.exit), (int) screenWidth / 2, play.y + 450);
            pause = new Button(pauseButton, (int) screenWidth - pauseButton.getWidth() / 2, (int) screenHeight - pauseButton.getHeight());
            gameTech = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.gt), 100, 100, false);
            pens = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.ps), 100, 100, false);
            crab = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.crab), 400, 300, false);
            Start();
        }

        @Override
        public void run() {
            while (playing) {
                long startFrameTime = System.currentTimeMillis();
                switch (state) {
                    case -1: //paused
                        DrawMenu();
                        break;
                    case 0: //playing
                        Update();
                        Draw();
                        break;
                    case 1: //win or lost
                        Draw();
                        break;
                }

                timeThisFrame = System.currentTimeMillis() - startFrameTime;
                if (timeThisFrame >= 1) {
                    fps = 1000 / timeThisFrame;
                }
            }
        }

        public void resume() {
            playing = true;
            gameThread = new Thread(this);
            gameThread.start();
        }

        public void pause() {
            playing = false;
            try {
                gameThread.join();
            } catch (InterruptedException e) {
                Log.e("Error:", "Joining Thread");
            }
        }

        public void Start() {
            win = false;
            state = -1;
            isMoving = false;
            selectColor = -1;
            int num;
            //Delete everything
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 20; j++) {
                    bubbles[i][j] = new Bubble((vGrid * (i + 1) - bubbleRadius), (hGrid * (j + 1) - bubbleRadius), bubblesDef[0], 0, false);
                }
            }
            //activating the first rows of bubbles
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    num = new Random().nextInt(3);
                    bubbles[i][j] = new Bubble((vGrid * (i + 1) - bubbleRadius), (hGrid * (j + 1) - bubbleRadius), bubblesDef[num], num, true);
                }
            }
            //Background music

            //Giving a new ready-to-shoot bubble
            num = new Random().nextInt(3);
            mainBubble = new Bubble(startX, startY, bubblesDef[num], num, true);

            //Resetting score to 0
            score = 0;
        }

        public void Update() {
            //Hits a bubble
            activeBubbles = 0;
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 20; j++) {
                    if (bubbles[i][j].active & mainBubble.active) {
                        if (RectF.intersects(mainBubble.getRect(), bubbles[i][j].getRect())) //Detect collision
                        {
                            //play hit sound
                            sfx.play(hitBubble, 1, 1, 0, 0, 1);

                            destroyStack.clear();
                            selectColor = mainBubble.colorIndex;
                            float distX = mainBubble.x - bubbles[i][j].x;
                            float distY = mainBubble.y - bubbles[i][j].y;
                            float distance = (float) Math.sqrt((distX * distX) + (distY * distY));
                            if (distance <= bubbleSize) {
                                if (mainBubble.y <= bubbles[i][j].y) //if above
                                {
                                    if (j == 0) //if top
                                    {
                                        if (mainBubble.x >= bubbles[i][j].x) //if right
                                        {
                                            if (i != 9) //if not corner
                                            {
                                                if (!bubbles[i + 1][j].active) //if right not active
                                                {
                                                    bubbles[i + 1][j] = new Bubble(mainBubble, bubbles[i][j].x + vGrid, bubbles[i][j].y); //set right
                                                    destroyStack.push(new Vector2(i + 1, j));
                                                }
                                            }
                                        } else //if left
                                        {
                                            if (i != 0) //if not corner
                                            {
                                                if (!bubbles[i - 1][j].active) //if left not active
                                                {
                                                    bubbles[i - 1][j] = new Bubble(mainBubble, bubbles[i][j].x - vGrid, bubbles[i][j].y); //Set Left
                                                    destroyStack.push(new Vector2(i - 1, j));
                                                }
                                            }
                                        }
                                    } else //if not top
                                    {
                                        if (bubbles[i][j - 1].active) //if above active
                                        {
                                            if (mainBubble.x >= bubbles[i][j].x) //if right
                                            {
                                                if (i != 9) //if not right corner
                                                {
                                                    if (bubbles[i + 1][j].active) //if right active
                                                    {
                                                        if (!bubbles[i + 1][j - 1].active) //if above-right diagonal not active
                                                        {
                                                            bubbles[i + 1][j - 1] = new Bubble(mainBubble, bubbles[i][j].x + vGrid, bubbles[i][j].y - hGrid); //set above-right diagonal
                                                            destroyStack.push(new Vector2(i + 1, j - 1));
                                                        }
                                                    } else //if right not active
                                                    {
                                                        bubbles[i + 1][j] = new Bubble(mainBubble, bubbles[i][j].x + vGrid, bubbles[i][j].y); //set right
                                                        destroyStack.push(new Vector2(i + 1, j));
                                                    }
                                                }
                                            } else //if left
                                            {
                                                if (i != 0) //if not left corner
                                                {
                                                    if (bubbles[i - 1][j].active) //if left active
                                                    {
                                                        if (!bubbles[i - 1][j - 1].active) //if above-left diagonal not active
                                                        {
                                                            bubbles[i - 1][j - 1] = new Bubble(mainBubble, bubbles[i][j].x - vGrid, bubbles[i][j].y - hGrid); //set above-left diagonal
                                                            destroyStack.push(new Vector2(i - 1, j - 1));
                                                        }
                                                    } else //if left not active
                                                    {
                                                        bubbles[i - 1][j] = new Bubble(mainBubble, bubbles[i][j].x - vGrid, bubbles[i][j].y); //set left
                                                        destroyStack.push(new Vector2(i - 1, j));
                                                    }
                                                }
                                            }
                                        } else //if above not active
                                        {
                                            if (mainBubble.x >= bubbles[i][j].x) //if right
                                            {
                                                if (distX < -distY) //if near above
                                                {
                                                    bubbles[i][j - 1] = new Bubble(mainBubble, bubbles[i][j].x, bubbles[i][j].y - hGrid); //set above
                                                    destroyStack.push(new Vector2(i, j - 1));
                                                } else //if near right
                                                {
                                                    if (i != 9) //if not corner
                                                    {
                                                        if (bubbles[i + 1][j].active) //if right active
                                                        {
                                                            if (!bubbles[i + 1][j - 1].active) //if above-right diagonal not active
                                                            {
                                                                bubbles[i + 1][j - 1] = new Bubble(mainBubble, bubbles[i][j].x + vGrid, bubbles[i][j].y - hGrid); //set above-right diagonal
                                                                destroyStack.push(new Vector2(i + 1, j - 1));
                                                            }
                                                        } else {
                                                            bubbles[i + 1][j] = new Bubble(mainBubble, bubbles[i][j].x + vGrid, bubbles[i][j].y); //set right
                                                            destroyStack.push(new Vector2(i + 1, j));
                                                        }
                                                    }
                                                }
                                            } else //if left
                                            {
                                                if (distX > distY) //if near above
                                                {
                                                    bubbles[i][j - 1] = new Bubble(mainBubble, bubbles[i][j].x, bubbles[i][j].y - hGrid); //set above
                                                    destroyStack.push(new Vector2(i, j - 1));
                                                } else //if near left
                                                {
                                                    if (i != 0) //if not corner
                                                    {
                                                        if (bubbles[i - 1][j].active) //if left active
                                                        {
                                                            if (!bubbles[i - 1][j - 1].active) //if above-left diagonal not active
                                                            {
                                                                bubbles[i - 1][j - 1] = new Bubble(mainBubble, bubbles[i][j].x - vGrid, bubbles[i][j].y - hGrid); //set above-left diagonal
                                                                destroyStack.push(new Vector2(i - 1, j - 1));
                                                            }
                                                        } else //if left not active
                                                        {
                                                            bubbles[i - 1][j] = new Bubble(mainBubble, bubbles[i][j].x - vGrid, bubbles[i][j].y); //set left
                                                            destroyStack.push(new Vector2(i - 1, j));
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else //if below
                                {
                                    if (j != 19) //if not bottom
                                    {
                                        if (bubbles[i][j + 1].active) //if below active
                                        {
                                            if (mainBubble.x >= bubbles[i][j].x) //if right
                                            {
                                                if (i != 9) //if not right corner
                                                {
                                                    if (bubbles[i + 1][j].active) //if right active
                                                    {
                                                        if (!bubbles[i + 1][j + 1].active) //if below-right diagonal not active
                                                        {
                                                            bubbles[i + 1][j + 1] = new Bubble(mainBubble, bubbles[i][j].x + vGrid, bubbles[i][j].y + hGrid); //set below-right diagonal
                                                            destroyStack.push(new Vector2(i + 1, j + 1));
                                                        }
                                                    } else //if right not active
                                                    {
                                                        bubbles[i + 1][j] = new Bubble(mainBubble, bubbles[i][j].x + vGrid, bubbles[i][j].y); //set right
                                                        destroyStack.push(new Vector2(i + 1, j));
                                                    }
                                                }
                                            } else //if left
                                            {
                                                if (i != 0) //if not left corner
                                                {
                                                    if (bubbles[i - 1][j].active) //if left active
                                                    {
                                                        if (!bubbles[i - 1][j + 1].active) //if below-left diagonal not active
                                                        {
                                                            bubbles[i - 1][j + 1] = new Bubble(mainBubble, bubbles[i][j].x - vGrid, bubbles[i][j].y + hGrid); //set below-left diagonal
                                                            destroyStack.push(new Vector2(i - 1, j + 1));
                                                        }
                                                    } else //if left not active
                                                    {
                                                        bubbles[i - 1][j] = new Bubble(mainBubble, bubbles[i][j].x - vGrid, bubbles[i][j].y); //set left
                                                        destroyStack.push(new Vector2(i - 1, j));
                                                    }
                                                }
                                            }
                                        } else //if below not active
                                        {
                                            if (mainBubble.x >= bubbles[i][j].x) //if right
                                            {
                                                if (distX > -distY) //if near below
                                                {
                                                    bubbles[i][j + 1] = new Bubble(mainBubble, bubbles[i][j].x, bubbles[i][j].y + hGrid); //set below
                                                    destroyStack.push(new Vector2(i, j + 1));
                                                } else //if near right
                                                {
                                                    if (i != 9) //if not corner
                                                    {
                                                        if (bubbles[i + 1][j].active) //if right active
                                                        {
                                                            if (!bubbles[i + 1][j + 1].active) //if below-right diagonal not active
                                                            {
                                                                bubbles[i + 1][j + 1] = new Bubble(mainBubble, bubbles[i][j].x + vGrid, bubbles[i][j].y + hGrid); //set below-right diagonal
                                                                destroyStack.push(new Vector2(i + 1, j + 1));
                                                            }
                                                        } else {
                                                            bubbles[i + 1][j] = new Bubble(mainBubble, bubbles[i][j].x + vGrid, bubbles[i][j].y); //set right
                                                            destroyStack.push(new Vector2(i + 1, j));
                                                        }
                                                    }
                                                }
                                            } else //if left
                                            {
                                                if (distX < distY) //if near below
                                                {
                                                    bubbles[i][j + 1] = new Bubble(mainBubble, bubbles[i][j].x, bubbles[i][j].y + hGrid); //set below
                                                    destroyStack.push(new Vector2(i, j + 1));
                                                } else //if near left
                                                {
                                                    if (i != 0) //if not corner
                                                    {
                                                        if (bubbles[i - 1][j].active) //if left active
                                                        {
                                                            if (!bubbles[i - 1][j + 1].active) //if below-left diagonal not active
                                                            {
                                                                bubbles[i - 1][j + 1] = new Bubble(mainBubble, bubbles[i][j].x - vGrid, bubbles[i][j].y + hGrid); //set below-left diagonal
                                                                destroyStack.push(new Vector2(i - 1, j + 1));
                                                            }
                                                        } else //if left not active
                                                        {
                                                            bubbles[i - 1][j] = new Bubble(mainBubble, bubbles[i][j].x - vGrid, bubbles[i][j].y); //set left
                                                            destroyStack.push(new Vector2(i - 1, j));
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Stack<Vector2> lastStack;
                            do {
                                lastStack = destroyStack;
                                for (int rep = 0; rep < 5; rep++) {
                                    for (int y = 0; y < 20; y++) {
                                        for (int x = 0; x < 10; x++) {
                                            if (bubbles[x][y].active & !bubbles[x][y].impulse & bubbles[x][y].colorIndex == selectColor) {
                                                //detect top
                                                if (y > 0) {
                                                    if (bubbles[x][y - 1].impulse & bubbles[x][y - 1].colorIndex == selectColor & !bubbles[x][y].impulse) {
                                                        bubbles[x][y].impulse = true;
                                                        destroyStack.push(new Vector2(x, y));
                                                        continue;
                                                    }
                                                }
                                                //detect below
                                                if (y < 19) {
                                                    if (bubbles[x][y + 1].impulse & bubbles[x][y + 1].colorIndex == selectColor & !bubbles[x][y].impulse) {
                                                        bubbles[x][y].impulse = true;
                                                        destroyStack.push(new Vector2(x, y));
                                                        continue;
                                                    }
                                                }
                                                //detect left
                                                if (x > 0) {
                                                    if (bubbles[x - 1][y].impulse & bubbles[x - 1][y].colorIndex == selectColor & !bubbles[x][y].impulse) {
                                                        bubbles[x][y].impulse = true;
                                                        destroyStack.push(new Vector2(x, y));
                                                        continue;
                                                    }
                                                }
                                                //detect right
                                                if (x < 9) {
                                                    if (bubbles[x + 1][y].impulse & bubbles[x + 1][y].colorIndex == selectColor & !bubbles[x][y].impulse) {
                                                        bubbles[x][y].impulse = true;
                                                        destroyStack.push(new Vector2(x, y));
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    for (int x = 0; x < 10; x++) {
                                        for (int y = 0; y < 20; y++) {
                                            if (bubbles[x][y].active & !bubbles[x][y].impulse & bubbles[x][y].colorIndex == selectColor) {
                                                //detect top
                                                if (y > 0) {
                                                    if (bubbles[x][y - 1].impulse & bubbles[x][y - 1].colorIndex == selectColor & !bubbles[x][y].impulse) {
                                                        bubbles[x][y].impulse = true;
                                                        destroyStack.push(new Vector2(x, y));
                                                        continue;
                                                    }
                                                }
                                                //detect below
                                                if (y < 19) {
                                                    if (bubbles[x][y + 1].impulse & bubbles[x][y + 1].colorIndex == selectColor & !bubbles[x][y].impulse) {
                                                        bubbles[x][y].impulse = true;
                                                        destroyStack.push(new Vector2(x, y));
                                                        continue;
                                                    }
                                                }
                                                //detect left
                                                if (x > 0) {
                                                    if (bubbles[x - 1][y].impulse & bubbles[x - 1][y].colorIndex == selectColor & !bubbles[x][y].impulse) {
                                                        bubbles[x][y].impulse = true;
                                                        destroyStack.push(new Vector2(x, y));
                                                        continue;
                                                    }
                                                }
                                                //detect right
                                                if (x < 9) {
                                                    if (bubbles[x + 1][y].impulse & bubbles[x + 1][y].colorIndex == selectColor & !bubbles[x][y].impulse) {
                                                        bubbles[x][y].impulse = true;
                                                        destroyStack.push(new Vector2(x, y));
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } while (lastStack.size() != destroyStack.size());
                            //Destroy bubbles if more than 2 is connected
                            if (destroyStack.size() >= 3) {
                                sfx.play(bubblePop, 1, 1, 0, 0, 1);
                                score += (destroyStack.size() * 100) + (((destroyStack.size() / 3) - 1) * 50);
                                Vector2[] coords = new Vector2[destroyStack.size()];
                                for (int k = 0; k < coords.length; k++) {
                                    coords[k] = destroyStack.pop();
                                    assert coords[k] != null;
                                    bubbles[coords[k].x][coords[k].y].active = false;
                                }
                            }
                            //Reverting back the impulses
                            for (int a = 0; a < 10; a++) {
                                for (int b = 0; b < 20; b++) {
                                    bubbles[a][b].impulse = false;
                                }
                            }
                            int num = new Random().nextInt(3);
                            mainBubble = new Bubble(startX, startY, bubblesDef[num], num, true);
                            isMoving = false;
                            selectColor = -1;
                        }
                    }
                    if (bubbles[i][j].active) //Detect how many active bubbles there are
                    {
                        activeBubbles++;
                    }
                }
            }

            //If bubble wall is too low = lose
            for (int i = 0; i < 10; i++) {
                if (bubbles[i][17].active) {
                    state = 1;
                    break;
                }
            }

            //if active bubbles reaches zero = win
            if (activeBubbles == 0) {
                win = true;
                state = 1;
            }

            //Hits left wall
            if (mainBubble.x - bubbleRadius < 0) {
                sfx.play(hitWall, 1, 1, 0, 0, 1);
                mainBubble.invertXSpeed();
            }
            //Hits right wall
            if (mainBubble.x + bubbleRadius > screenWidth) {
                sfx.play(hitWall, 1, 1, 0, 0, 1);
                mainBubble.invertXSpeed();
            }
            //Hits ceiling
            if (mainBubble.y - bubbleRadius < 0) {
                isMoving = false;
                int num = new Random().nextInt(3);
                mainBubble = new Bubble(startX, startY, bubblesDef[num], num, true);
            }
            //goes below bubble starting point
            if (mainBubble.y > startY + 10) {
                mainBubble.invertYSpeed();
            }
            //update ball
            mainBubble.BallUpdate(fps);
        }

        public void DrawMenu() {
            if (ourHolder.getSurface().isValid()) {
                canvas = ourHolder.lockCanvas();

                //Draw background
                paint.setColor(Color.argb(255, 255, 255, 255));
                canvas.drawBitmap(background, 0, 0, paint);

                if (showCredit) {
                    paint.setTextSize(50);
                    canvas.drawRect(0, screenHeight * 0.3f, screenWidth, screenHeight * 0.7f, new Paint(Color.argb(128, 128, 128, 128)));
                    canvas.drawText("Lecturer: Mr. Zulhaidar Fairozal Akbar", screenWidth * 0.10f, screenHeight * 0.35f, paint);
                    canvas.drawText("Programmer: Aydin Ihsan Ibrahim Nurdin", screenWidth * 0.10f, screenHeight * 0.45f, paint);
                    canvas.drawText("Artist & Sound Designer: Hikmatul Ulya", screenWidth * 0.10f, screenHeight * 0.55f, paint);
                    canvas.drawText("Artist & UI: Siti Julekhah", screenWidth * 0.10f, screenHeight * 0.65f, paint);
                    canvas.drawBitmap(pause.bitmap, screenWidth - pause.bitmap.getWidth(), 0, paint);

                } else {
                    canvas.drawBitmap(title.bitmap, title.left, title.top, paint);
                    canvas.drawBitmap(play.bitmap, play.left, play.top, paint);
                    canvas.drawBitmap(restart.bitmap, restart.left, restart.top, paint);
                    canvas.drawBitmap(credit.bitmap, credit.left, credit.top, paint);
                    canvas.drawBitmap(exit.bitmap, exit.left, exit.top, paint);
                }
                canvas.drawBitmap(gameTech, screenWidth - gameTech.getWidth() - 50, screenHeight - gameTech.getHeight() - 75, paint);
                canvas.drawBitmap(pens, screenWidth - gameTech.getWidth() - pens.getWidth() - 75, screenHeight - pens.getHeight() - 75, paint);
                canvas.drawBitmap(crab, 0, screenHeight - crab.getHeight(), paint);

                ourHolder.unlockCanvasAndPost(canvas);
            }
        }

        public void Draw() {
            if (ourHolder.getSurface().isValid()) {
                canvas = ourHolder.lockCanvas();

                //Draw background
                paint.setColor(Color.argb(255, 255, 255, 255));
                canvas.drawBitmap(background, 0, 0, paint);

                //Draw crab
                canvas.drawBitmap(crab, startX - 200, startY - 150, paint);
                //Draw line
                if (line != null) {
                    canvas.drawLine(line.xStart, line.yStart, line.xEnd, line.yEnd, paint);
                }

                //Draw bubbles
                for (int i = 0; i < 10; i++) {
                    for (int j = 0; j < 20; j++) {
                        if (bubbles[i][j].active) {
                            canvas.drawBitmap(bubbles[i][j].bitmap, bubbles[i][j].x - bubbleRadius, bubbles[i][j].y - bubbleRadius, paint);
                            //canvas.drawRect(bubbles[i][j].getRect(), paint);
                        }
                    }
                }

                //Texts
                paint.setTextSize(75);

                //Win and lose screen
                if (state == 0) {
                    canvas.drawText("Score: " + score, 0, screenHeight - 100, paint);
                    if (mainBubble.active) {
                        canvas.drawBitmap(mainBubble.bitmap, mainBubble.x - bubbleRadius, mainBubble.y - bubbleRadius, paint);
                        //canvas.drawRect(mainBubble.getRect(), paint);
                    }
                    //Draw pause button
                    canvas.drawBitmap(pause.bitmap, pause.left, pause.top, paint);
                }
                if (state == 1 & win) //if won
                {
                    canvas.drawRect(0, screenHeight * 0.3f, screenWidth, screenHeight * 0.6f, new Paint(Color.argb(128, 0, 0, 0)));
                    canvas.drawText("Congratulation, You Won!", screenWidth * 0.10f, screenHeight * 0.35f, paint);
                    canvas.drawText("Score: " + score, screenWidth * 0.325f, screenHeight * 0.4f, paint);
                    canvas.drawBitmap(restart.bitmap, restart.left, restart.top - 200, paint);
                    canvas.drawBitmap(exit.bitmap, exit.left, restart.top - 50, paint);
                } else if (state == 1 & !win) //if lost
                {
                    canvas.drawRect(0, screenHeight * 0.3f, screenWidth, screenHeight * 0.6f, new Paint(Color.argb(128, 0, 0, 0)));
                    canvas.drawText("Unfortunately, you lost...", screenWidth * 0.15f, screenHeight * 0.35f, paint);
                    canvas.drawText("Score: " + score, screenWidth * 0.325f, screenHeight * 0.4f, paint);
                    canvas.drawBitmap(restart.bitmap, restart.left, restart.top - 200, paint);
                    canvas.drawBitmap(exit.bitmap, exit.left, restart.top - 50, paint);
                }

                //Draw Debug
                //canvas.drawText("FPS = " + fps, 0, 50, paint);
                //canvas.drawText("selected color: " + selectColor, 0, 100, paint);
                //canvas.drawText("isMoving = " + isMoving, 0, 100, paint);
                //canvas.drawText("Bubble xV = " + mainBubble.xV + ", yV = " + mainBubble.yV, 0, 150, paint);
                //canvas.drawText("Bubble posX = " + mainBubble.x + ", posY = " + mainBubble.y, 0, 200, paint);
                ourHolder.unlockCanvasAndPost(canvas);
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float touchX;
            float touchY;
            switch (state) {
                case -1: {
                    if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
                        touchX = event.getX();
                        touchY = event.getY();
                        //Play Button
                        if (touchX >= play.left & touchX <= play.right & touchY >= play.top & touchY <= play.bottom) {
                            state = 0;
                            return true;
                        }
                        //Restart Button
                        if (touchX >= restart.left & touchX <= restart.right & touchY >= restart.top & touchY <= restart.bottom) {
                            bubbleShooting.Start();
                            state = 0;
                            return true;
                        }
                        //Credits Button
                        if (touchX >= credit.left & touchX <= credit.right & touchY >= credit.top & touchY <= credit.bottom) {
                            showCredit = true;
                            return true;
                        }
                        //Exit Button
                        if (touchX >= exit.left & touchX <= exit.right & touchY >= exit.top & touchY <= exit.bottom) {
                            finish();
                            System.exit(0);
                        }
                        if (showCredit) {
                            Button back = new Button(pause.bitmap, (int) screenWidth - (pause.bitmap.getWidth() / 2), pause.bitmap.getHeight() / 2);
                            if (touchX >= back.left & touchX <= back.right & touchY >= back.top & touchY <= back.bottom) {
                                showCredit = false;
                            }
                        }
                    }
                    break;
                }
                case 0:
                    if (!isMoving) {
                        switch (event.getAction() & MotionEvent.ACTION_MASK) {
                            case MotionEvent.ACTION_DOWN:
                                touchX = event.getX();
                                touchY = event.getY();

                                line = new Line(startX, startY, touchX, touchY);
                                break;

                            case MotionEvent.ACTION_MOVE:
                                touchX = event.getX();
                                touchY = event.getY();
                                if (line != null) line.update(touchX, touchY);
                                break;

                            case MotionEvent.ACTION_UP:
                                if (line != null) {
                                    mainBubble.shoot(line);
                                    sfx.play(shoot, 1, 1, 0, 0, 1);
                                    isMoving = true;
                                    line = null;
                                }
                        }
                    }
                    touchX = event.getX();
                    touchY = event.getY();
                    if (touchX >= pause.left & touchX <= pause.right & touchY >= pause.top & touchY <= pause.bottom) {
                        line = null;
                        state = -1;
                    }
                    break;
                case 1:
                    if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
                        Button restartCase1 = new Button(restart.bitmap, restart.x, restart.y - 200);
                        Button exitCase1 = new Button(exit.bitmap, exit.x, restart.y - 50);
                        touchX = event.getX();
                        touchY = event.getY();
                        if (touchX >= restartCase1.left & touchX <= restartCase1.right & touchY >= restartCase1.top & touchY <= restartCase1.bottom) {
                            bubbleShooting.Start();
                            state = 0;
                            return true;
                        }
                        if (touchX >= exitCase1.left & touchX <= exitCase1.right & touchY >= exitCase1.top & touchY <= exitCase1.bottom) {
                            finish();
                            System.exit(0);
                        }
                    }
            }
            return true;
        }
    }
}