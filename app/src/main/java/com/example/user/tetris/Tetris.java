package com.example.user.tetris;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import java.util.LinkedList;
import java.util.Random;

public class Tetris extends AppCompatActivity {

    Intent intent;

    class FieldView extends SurfaceView implements SurfaceHolder.Callback{


        final private class Matrix{
            public int[][] m;

            public Matrix(int[][] ints) {
            }

            public void setMatrix(int[][] arg) {
                this.m = arg;
            }
        }

        final class Block {
            public Matrix[] rotation;
//            public int color;
            private int direction = 0;
            public int initX;
            public int initY;

            private Block() {
                super();
            }

            public Block(int[][][] rotation, int initX, int initY) {
                int c = rotation.length;
                this.rotation = new Matrix[c];
                for (int i = 0; i < c; i++) {
                    this.rotation[i] = new Matrix(rotation[i]);
                }
                this.initX = initX;
                this.initY = initY;
            }

            public Block clone(){
                Block b = new Block();
                b.rotation = this.rotation;
                b.initX = this.initX;
                b.initY = this.initY;
                b.direction = 0;
                return b;
            }

            public Matrix getMatrix(){
                return rotation[direction];
            }

            public void rotationLeft(){
                if (++direction >= rotation.length){
                    direction = 0;
                }
            }

            public void rotationRight(){
                if (--direction < 0){
                    direction = rotation.length -1;
                }
            }
        }
        int[][][] normal = {
                {
                        {1,1},
                        {0,1},
                        {0,1},
                },
                {
                        {1,1},
                        {1,0},
                        {1,0}
                },
                {
                        {1,1},
                        {1,1}
                },
                {
                        {1,0},
                        {1,1},
                        {1,0}
                },
                {
                        {1,0},
                        {1,1},
                        {0,1}
                },
                {
                        {0,1},
                        {1,1},
                        {1,0}
                },
                {
                        {1},
                        {1},
                        {1},
                        {1}
                }
        };

        Block normalBlock = new Block(normal,5,-1);

        //使用される全てのブロック
        //ここからランダムに選択し、cloneにてコピーした新しいブロックインスタンスを用いる
        Random mRand = new Random(System.currentTimeMillis());

        LinkedList<Block> blocks;

        Block block;

//        int[][] block = blocks[mRand.nextInt(blocks.length)];
        int posx, posy;
        static final private int FRAME_SIZE = 5;
        static final private int BLOCK_SIZE = 45;
        static final private int BLOCK_BLANK_SIZE = 40;
        static final int mapWidth  = 10 * 2 + 4;
        static final int mapHeight = 15 * 2 + 4;
        int[][] map = new int[mapHeight][];
        private SurfaceHolder mSurfaceHolder;
        private int KeyState;
        private int lineCounter = 0;
        private int gravity = 1;
        private long lastDropTime;
        private long lastMoveTime;

        class MyThread extends Thread{
            private boolean mRun = true;
            private int frameCount = 0;
            private final long startTime = System.currentTimeMillis();

            private int fps;

            @Override
            public void run() {
                lastDropTime = System.currentTimeMillis();
                lastMoveTime = lastDropTime;
                while (mRun){
                    Canvas c = null;
                    long currentTime = System.currentTimeMillis();
                    frameCount++;

                    if (currentTime - lastMoveTime > 100){
                        doMove();
                        lastMoveTime = currentTime;
                    }

                    fps = (int)(frameCount * 1000 / (currentTime - startTime));

                    if (currentTime - lastDropTime > 500){
                        doDrop();
                        //lastMoveTime = currentTime;
                    }

                    try {
                        c = mSurfaceHolder.lockCanvas();

                        synchronized (mSurfaceHolder) {
                            doDraw(c);
                        }
                    }finally {
                        if (c != null){
                            mSurfaceHolder.unlockCanvasAndPost(c);
                        }
                    }
                }
            }

            public void quit(){
                mRun = false;
            }

            public int getFPS(){
                return fps;
            }
        }

        final private MyThread myThread = new MyThread();

        private void doDrop(){
            if (!check( block.getMatrix().m, posx, posy + 1)){
                mergeMatrix(block.getMatrix().m, posx, posy);
                clearRows();

                Block tmp = blocks.remove(mRand.nextInt(blocks.size()));
                futureBlocks.add(tmp);
                blocks.add(block.clone());

                posx = block.initX;
                posy = block.initY;

                if (map[0][5] != 0){
                    Message m = mHandler.obtainMessage();
                    mHandler.sendMessage(m);
                }

                return;
            }

            int tg = gravity -1;
            posy++;

            while (tg-- > 0){
                if (check(block.getMatrix().m, posx, posy + 1)){
                    posy++;
                }else{
                    return;
                }
            }
        }

        private LinkedList<Block> futureBlocks;


        public FieldView(Context context) {
            super(context);

            mSurfaceHolder = getHolder();
            mSurfaceHolder.addCallback(this);
            mSurfaceHolder.setSizeFromLayout();

            setFocusable(true);
            setFocusableInTouchMode(true);
            requestFocus();

        }

        //ブロックを回転させる処理
        int[][] rotate(final int[][] block) {
            int[][] rotated = new int[block[0].length][];
            for (int x = 0; x < block[0].length; x ++) {
                rotated[x] = new int[block.length];
                for (int y = 0; y < block.length; y ++) {
                    rotated[x][block.length - y - 1] = block[y][x];
                }
            }
            return rotated;
        }

        public void initGame() {
            for (int y = 0; y < mapHeight; y++) {
                map[y] = new int[mapWidth];
                for (int x = 0; x < mapWidth; x++) {
                    map[y][x] = 0;
                }
            }

            blocks = new LinkedList<Block>();
            blocks.add(normalBlock);

            Block tmp = blocks.remove(mRand.nextInt(blocks.size()));
            block = tmp.clone();
            posx = block.initX;
            posy = block.initY;
            futureBlocks = new LinkedList<Block>();
            for (int i = 0; i < 3; i++){
                Block tmp2 = blocks.remove(mRand.nextInt(blocks.size()));
                futureBlocks.add(tmp2);
            }

            lineCounter = 0;
            gravity = 1;
        }

        //テトリスのブロックの点画の大きさとブロックの隙間の調整
        private void commonPaint(Canvas canvas, ShapeDrawable rect, int[][] matrix, int offsetx, int offsety){
            int h = matrix.length;
            int w = matrix[0].length;

            for (int y = 0; y < h; y++){
                for (int x = 0; x < w; x++){
                    if (matrix[y][x] != 0){
                    //カラー選択があればここでif文
                    int px = (x + offsetx) * BLOCK_SIZE;
                    int py = (y + offsety) * BLOCK_SIZE;

                    rect.setBounds(py, px, px + BLOCK_BLANK_SIZE, py + BLOCK_BLANK_SIZE);
                    rect.draw(canvas);
                    }
                }
            }
        }


        private void paintMatrix(Canvas canvas, int[][] matrix, int offsetx, int offsety, int color) {
            ShapeDrawable rect = new ShapeDrawable(new RectShape());
            rect.getPaint().setColor(color);

            commonPaint(canvas, rect, matrix, offsetx, offsety);
        }

        // ブロックがマップ上のその位置に存在できるかどうかの確認
        // できるならtrue, できないならfalse
        boolean check(int[][] block, int offsetx, int offsety) {
            int blockWidth = block[0].length;
            int blockHeight = block.length;

            //ブロックの全ての点を舐める
            for (int y = 0; y < blockHeight; y++){
                int ry = y + offsety;

                //ブロックは上だけ画面外でもOK
                if(ry < 0) continue;
                for (int x = 0; x < blockWidth; x++){
                    int b = block[x][y];
                    if (b == 0) continue;
                    //ここから下はブロックに点有り

                    //ブロックの点が画面外にあるならアウト
                    if (ry >= mapHeight) return false;

                    int rx = x + offsetx;
                    if (rx < 0 || rx >= mapWidth){
                        return false;
                    }

                    //ブロックがの点が画面内の瓦礫と重なるならアウト
                    int m = map[ry][rx];
                    if (m != 0){
                        return false;
                    }
                }
            }
            return true;
        }


        void mergeMatrix(int[][] block, int offsetx, int offsety) {
            for (int y = 0; y < block.length; y++) {
                int ry = y + offsety;
                if (ry < 0) continue;
                for (int x = 0; x < block[0].length; x++) {
                    if (block[y][x] != 0){
                        map[ry][offsetx + x] = block[x][y] | 0x10;
                    }
                }
            }
        }

        void clearRows() {
            // 埋まった行は消す。nullで一旦マーキング
            for (int y = 0; y < mapHeight; y ++) {
                boolean full = true;
                for (int x = 0; x < mapWidth; x ++) {
                    if (map[y][x] == 0) {
                        full = false;
                        break;
                    }
                }

                if (full) map[y] = null;
            }

            // 新しいmapにnull以外の行を詰めてコピーする
            int[][] newMap = new int[mapHeight][];
            int y2 = mapHeight - 1;
            for (int y = mapHeight - 1; y >= 0; y--) {
                if (map[y] == null) {
                    continue;
                } else {
                    newMap[y2--] = map[y];
                }
            }

            //消した行がない場合
            if (y2 == -1){
                map = newMap;
                return;
            }

            // 消えた行数分新しい行を追加する
            for (int i = 0; i <= y2; i++) {
                int[] newRow = new int[mapWidth];
                for (int j = 0; j < mapWidth; j ++) {
                    newRow[j] = 0;
                }
                newMap[i] = newRow;
            }
            map = newMap;
        }

        /**
         * Draws the 2D layer.
         */
        void doDraw(Canvas canvas) {
            //next block
            canvas.drawColor(0xEFFFFFFF);
            Paint paint = new Paint();
            //ここにFPSの表示用処理を描く
            //ここにライン数表示処理を描く
            canvas.save();
            canvas.translate(FRAME_SIZE, BLOCK_SIZE * 2);
            paintNextBlock(canvas);
            canvas.restore();

            //field
            canvas.translate(0, 70);
            ShapeDrawable rect = new ShapeDrawable(new RectShape());
            Paint p = rect.getPaint();
            final int field_width = mapWidth * BLOCK_SIZE;
            final int field_height = mapHeight * BLOCK_SIZE;
            rect.setBounds(0, 0, field_width + FRAME_SIZE * 2, field_height + FRAME_SIZE * 2);
            p.setColor(0xFF000000);
            rect.draw(canvas);

            canvas.translate(FRAME_SIZE, FRAME_SIZE);
            rect.setBounds(0, 0, field_width, field_height);
            p.setColor(0xFFCCCCCC);
            rect.draw(canvas);

            int[][] tetra = block.getMatrix().m;
            paintMatrix(canvas, tetra, posx, posy, 0xFFFF0000);
            paintMatrix(canvas, map, 0, 0, 0xFF808080);
        }


        private void paintNextBlock(Canvas canvas){
            Block b = futureBlocks.getFirst();
            int[][] m = b.getMatrix().m;
            int last_width = m[0].length;
            paintMatrix(canvas, m, b.initX, b.initY, 0xFFFF0000);

            canvas.translate(BLOCK_SIZE * (b.initX + last_width + 0.5f), BLOCK_SIZE);
            canvas.scale(0.5f, 0.5f);

            for (int i = 1; i < futureBlocks.size(); i++){
                b = futureBlocks.get(i);

                paintMatrix(canvas, b.getMatrix().m, 0, b.initY, 0xFFFF0000);

                m = b.getMatrix().m;
                last_width = m[0].length + 1; //空白を１つ空ける

                if (last_width == 2){
                    last_width++;
                }
                canvas.translate(BLOCK_SIZE * last_width, 0);

            }
        }

        public void doMove() {
            // activity画面のタッチ操作を取得
            View frick = getWindow().getDecorView();

            //フリック操作を行ったときにFlickから帰ってくる操作で識別
            new Flick(frick) {
                @Override
                public void getFlick(int swipe) {
                    switch (swipe) {
                        case Flick.LEFT_FLICK:
                            if (check(block.getMatrix().m, posx - 1, posy)) {
                                posx = posx - 1;
                            }
                            break;

                        case Flick.RIGHT_FLICK:
                            if (check(block.getMatrix().m, posx + 1, posy)) {
                                posx = posx + 1;
                            }
                            break;

                        case Flick.Tap:
                            int[][] newBlock = rotate(block.getMatrix().m);
                            if (check(newBlock, posx, posy)) {
                                block.getMatrix().m = newBlock;
                            }
                            break;

                        case Flick.DOWN_FLICK:
                            int y = posy;
                            while (check(block.getMatrix().m, posx, y)) {
                                y++;
                            }
                            if (y > 0) posy = y - 1;
                            break;
                    }
                }
            };
        }



        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            myThread.start();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            holder.setFixedSize(width, height);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            boolean retry = true;
            myThread.quit();
            while (retry){
                try {
                    myThread.join();
                    retry = false;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Controls the animation using the message queue. Every time we receive an
         * INVALIDATE message, we redraw and place another message in the queue.
         */
        private final Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                startActivity(intent);
            }
        };
    }

    FieldView mFieldView;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        FrameLayout fl = new FrameLayout(this);
        mFieldView = new FieldView(this);
        fl.addView(mFieldView);
        RelativeLayout rl = new RelativeLayout(this);
        fl.addView(rl);
        setContentView(fl);

        intent = new Intent(this,GameOverScreen.class);

    }

    @Override
    protected void onResume() {
        super.onResume();
        mFieldView.initGame();
        Looper.myQueue().addIdleHandler(new Idler());
    }


    @Override
    protected void onPause() {
        super.onPause();
        mFieldView.myThread.quit();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mFieldView.myThread.quit();
    }


    // Allow the activity to go idle before its animation starts
    class Idler implements MessageQueue.IdleHandler {
        public Idler() {
            super();
        }

        public final boolean queueIdle() {
            return false;
        }
    }
}
