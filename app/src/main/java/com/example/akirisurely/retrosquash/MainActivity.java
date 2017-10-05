package com.example.akirisurely.retrosquash;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.SoundPool;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import java.util.Random;

public class MainActivity extends AppCompatActivity {
    //canvas for drawing
    Canvas canvas;
    SquashCourtView courtView;
    //sound variables
    private SoundPool soundPool;

    int sample1 = -1;
    int sample2 = -1;
    int sample3 = -1;
    int sample4 = -1;
    //screen
    Display display;
    int screenWidth;
    int screenHeight;

    //game objects
    int racketWidth;
    int racketHeight;
    Point racketPosition;

    //point contains x and y coordinates
    int ballWidth;
    Point ballPostion;

    //racket movement
    boolean racketIsMovingLeft;
    boolean racketIsMovingRight;
    //ball movement
    boolean ballIsMovingLeft;
    boolean ballIsMovingRight;
    boolean ballIsMovingUp;
    boolean ballIsMovingDown;

    //keep score
    long lastFrameTime;
    int fps; //frame per second
    int score;
    int speed=5;
    int lives;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        courtView = new SquashCourtView(this);
        setContentView(courtView);
        //init sound variables
        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        sample1 = soundPool.load(this, R.raw.sample1, 0);
        sample2 = soundPool.load(this, R.raw.sample2, 0);
        sample3 = soundPool.load(this, R.raw.sample3, 0);
        sample4 = soundPool.load(this, R.raw.sample4, 0);

        //init display
        display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;

        //position game objects
        racketPosition = new Point();
        racketPosition.x = screenWidth / 2;
        racketPosition.y = screenHeight - 50;
        racketWidth = screenWidth / 8;
        racketHeight = 10;

        ballWidth = screenWidth / 35;//any division here
        ballPostion = new Point();
        ballPostion.x = screenWidth / 2;
        ballPostion.y = 1 + ballWidth;

        lives = 3;
        score = 0;
    }


    protected void onResume()
    {
        super.onResume();
        courtView.resume();
    }
    protected void onPause()
    {
        super.onPause();
        courtView.pause();
    }

    class SquashCourtView extends SurfaceView implements Runnable {
        Thread logicThread;
        SurfaceHolder ourHolder;
        volatile boolean playSquash; //JVM takes variable and stores it in main memory instead of processor's cache
        Paint paint;

        SquashCourtView(Context context) {
            super(context);
            ourHolder = getHolder();
            paint = new Paint();
            ballIsMovingDown = true;

            Random run = new Random();
            int direction = run.nextInt();
            switch (direction) {
                case 0:
                    ballIsMovingLeft = false;
                    ballIsMovingRight = false;
                    break;
                case 1:
                    ballIsMovingLeft = true;
                    ballIsMovingRight = false;
                    break;

                case 2:
                    ballIsMovingLeft = false;
                    ballIsMovingRight = true;
                    break;

            }
        }

        public void run() {
            while (playSquash) {
                updateCourt();
                drawCourt();
                controlFPS();

            }
        }

        private void drawCourt() {
            if (!ourHolder.getSurface().isValid()) {
                return;
            }
            canvas = ourHolder.lockCanvas();//prevent screen from painting at once.

            canvas.drawColor(Color.BLACK);

            paint.setColor(Color.WHITE);
            paint.setTextSize(25);
            String title = "Score" + score + "Lives" + lives + "fps" + fps;
            canvas.drawText(title, 20, 20, paint);

            //racket
            int bottom = racketPosition.y + (racketHeight / 2);
            int top = racketPosition.y - (racketHeight / 2);
            int left = racketPosition.x - (racketWidth / 2);
            int right = racketPosition.x + (racketWidth / 2);
            canvas.drawRect(left, top, right, bottom, paint);

//ball
            canvas.drawCircle(ballPostion.x, ballPostion.y, ballWidth, paint);

            ourHolder.unlockCanvasAndPost(canvas);


        }

        private void updateCourt() {
            if (racketIsMovingRight) {
                if (racketPosition.x + (racketWidth/2)< screenWidth) {
                    racketPosition.x = racketPosition.x + 10;
                }
            }
            if (racketIsMovingLeft) {
                if (racketPosition.x - (racketWidth/2)> 0) {

                    racketPosition.x = racketPosition.x - 10;
                }
            }
            //detect collision
            if (ballPostion.y + ballWidth >= (racketPosition.y - racketHeight / 2)) {
                int halfRacket = racketWidth / 2;
                if (ballPostion.x + ballWidth > (racketPosition.x - halfRacket)
                        && ballPostion.x - ballWidth < (racketPosition.x + halfRacket)) {
                    //rebound the ball vertically and play a sound
                    soundPool.play(sample3, 1, 1, 0, 0, 1);
                    score++;
                    ballIsMovingUp = true;
                    ballIsMovingDown = false;

                    //now decide how to rebound the ball horizontally
                    if (ballPostion.x > racketPosition.x) {
                        ballIsMovingRight = true;
                        ballIsMovingLeft = false;
                    } else {
                        ballIsMovingRight = false;
                        ballIsMovingLeft = true;
                    }
                }
            }


            //right
            if (ballPostion.x +ballWidth > screenWidth){
                ballIsMovingLeft=true;
                ballIsMovingRight= false;

                soundPool.play(sample1, 1f,1f,0,0,1);
            }
            //left
            if (ballPostion.x < 0) {
                ballIsMovingRight = true;
                ballIsMovingLeft = false;
                soundPool.play(sample2, 1, 1, 0, 0, 1);
            }
//bottom
            if (ballPostion.y > screenHeight - ballWidth) {
                lives = lives - 1;
                if (lives == 0) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this,"FAILED",Toast.LENGTH_SHORT).show();
                        }
                    });
                    lives = 3;
                    score = 0;
                    soundPool.play(sample4, 1, 1, 0, 0, 1);
                }
                //return ball to top of screen
                ballPostion.y = 1 + ballWidth;
//drop in random position
                Random run = new Random();
                int direction = run.nextInt();
                switch (direction) {
                    case 0:
                        ballIsMovingLeft = false;
                        ballIsMovingRight = false;
                        break;
                    case 1:
                        ballIsMovingLeft = true;
                        ballIsMovingRight = false;
                        break;

                    case 2:
                        ballIsMovingLeft = false;
                        ballIsMovingRight = true;
                        break;

                }
            }
            //top
            if(ballPostion.y <= 0){
                ballIsMovingDown = true;
                ballIsMovingUp = false;
                ballPostion.y = 1;
                soundPool.play(sample3, 1f, 1f, 0, 0, 1);
            }

            // depending on the direction we should be moving in, adjust X and Y positions
            if (ballIsMovingDown) {
                ballPostion.y += 6;
            }

            if (ballIsMovingUp) {
                ballPostion.y -= 10;
            }

            if (ballIsMovingLeft) {
                ballPostion.x -= speed;
            }

            if (ballIsMovingRight) {
                ballPostion.x += speed;
            }
            }

        private void controlFPS() {
            long timeThisFrame = (System.currentTimeMillis() - lastFrameTime);
            long timeToSleep = 15 - timeThisFrame;
            if (timeThisFrame > 0) {
                fps = (int) (1000 / timeThisFrame);
            }
            if (timeToSleep > 0) {
                try {
                    logicThread.sleep(timeToSleep);
                } catch (InterruptedException e) {

                }
            }
            lastFrameTime = System.currentTimeMillis();

        }

        public void resume() {
            playSquash = true;
            logicThread = new Thread(this);
            logicThread.start();
        }

        public void pause() {
            playSquash = false;
            try {
                logicThread.join();//allows logicthread work accessible to main thread
            } catch (InterruptedException e) {

            }
        }
        @Override
        public boolean onTouchEvent(MotionEvent motionEvent){
            switch(motionEvent.getAction() & MotionEvent.ACTION_MASK){
                case MotionEvent.ACTION_DOWN:
                    if(motionEvent.getX() >= screenWidth/2){
                        racketIsMovingRight = true;
                        racketIsMovingLeft = false;
                    }else{
                        racketIsMovingLeft = true;
                        racketIsMovingRight = false;
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    racketIsMovingRight = false;
                    racketIsMovingLeft = false;
                    break;
            }
            return true;
        }
    }


    }

