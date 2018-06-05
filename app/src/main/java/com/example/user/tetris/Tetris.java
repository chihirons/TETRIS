package com.example.user.tetris;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.SurfaceView;

import java.util.Random;

public class Tetris extends AppCompatActivity {
    private class FieldView extends SurfaceView {

        Random mRand = new Random(System.currentTimeMillis());

        int[][][] blocks = {
                {
                        {1,1},
                        {0,1},
                        {0,1}
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

        int[][] block = blocks[mRand.nextInt(blocks.length)];
        int posx, posy;
        int mapWidth  = 10;
        int mapHeight = 20;
        int[][] map = new int[mapHeight][];

        public FieldView(Context context) {
            super(context);

            setBackgroundColor(0xFFFFFFFF);
            setFocusable(true);
            setFocusableInTouchMode(true);
            requestFocus();
        }

        public void initGame() {
            for (int y = 0; y < mapHeight; y++) {
                map[y] = new int[mapWidth];
                for (int x = 0; x < mapWidth; x++) {
                    map[y][x] = 0;
                }
            }
        }

        private void paintMatrix(Canvas canvas, int[][] matrix, int offsetx, int offsety, int color) {
            ShapeDrawable rect = new ShapeDrawable(new RectShape());
            rect.getPaint().setColor(color);
            int h = matrix.length;
            int w = matrix[0].length;

            for (int y = 0; y < h; y ++) {
                for (int x = 0; x < w; x ++) {
                    if (matrix[y][x] != 0) {
                        int px = (x + offsetx) * 20;
                        int py = (y + offsety) * 20;
                        rect.setBounds(px, py, px + 20, py + 20);
                        rect.draw(canvas);
                    }
                }
            }
        }


        boolean check(int[][] block, int offsetx, int offsety) {
            if (offsetx < 0 || offsety < 0 ||
                    mapHeight < offsety + block.length ||
                    mapWidth < offsetx + block[0].length) {
                return false;
            }
            for (int y = 0; y < block.length; y ++) {
                for (int x = 0; x < block[y].length; x ++) {
                    if (block[y][x] != 0 && map[y + offsety][x + offsetx] != 0) {
                        return false;
                    }
                }
            }
            return true;
        }

        void mergeMatrix(int[][] block, int offsetx, int offsety) {
            for (int y = 0; y < block.length; y ++) {
                for (int x = 0; x < block[0].length; x ++) {
                    if (block[y][x] != 0) {
                        map[offsety + y][offsetx + x] = block[y][x];
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
        @Override
        protected void onDraw(Canvas canvas) {
            ShapeDrawable rect = new ShapeDrawable(new RectShape());
            rect.setBounds(0, 0, 210, 410);
            rect.getPaint().setColor(0xFF000000);
            rect.draw(canvas);
            canvas.translate(5, 5);
            rect.setBounds(0, 0, 200, 400);
            rect.getPaint().setColor(0xFFFFFFFF);
            rect.draw(canvas);

            paintMatrix(canvas, block, posx, posy, 0xFFFF0000);
            paintMatrix(canvas, map, 0, 0, 0xFF808080);
        }

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

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_CENTER:
                    int[][] newBlock = rotate(block);
                    if (check(newBlock, posx, posy)) {
                        block = newBlock;
                    }
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if (check(block, posx + 1, posy)) {
                        posx = posx + 1;
                    }
                    break;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    if (check(block, posx - 1, posy)) {
                        posx = posx - 1;
                    }
                    break;
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    int y = posy;
                    while (check(block, posx, y)) { y++; }
                    if (y > 0) posy = y - 1;

                    break;
            }
            mHandler.sendEmptyMessage(INVALIDATE);
            return true;
        }

        public void startAnime() {
            mHandler.sendEmptyMessage(INVALIDATE);
            mHandler.sendEmptyMessage(DROPBLOCK);
        }

        public void stopAnime() {
            mHandler.removeMessages(INVALIDATE);
            mHandler.removeMessages(DROPBLOCK);
        }

        private static final int INVALIDATE = 1;
        private static final int DROPBLOCK = 2;

        /**
         * Controls the animation using the message queue. Every time we receive an
         * INVALIDATE message, we redraw and place another message in the queue.
         */
        private final Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case INVALIDATE:
                        invalidate();
                        break;
                    case DROPBLOCK:
                        if (check(block, posx, posy + 1)) {
                            posy++;
                        } else {
                            mergeMatrix(block, posx, posy);
                            clearRows();
                            posx = 0; posy = 0;
                            block = blocks[mRand.nextInt(blocks.length)];
                        }

                        invalidate();
                        Message massage = new Message();
                        massage.what = DROPBLOCK;
                        sendMessageDelayed(massage, 500);
                        break;
                }
            }
        };
    }

    FieldView mFieldView;

    private void setFieldView() {
        if (mFieldView == null) {
            mFieldView = new FieldView(getApplication());
            setContentView(mFieldView);
        }
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setFieldView();
        mFieldView.initGame();
        mFieldView.startAnime();
        Looper.myQueue().addIdleHandler(new Idler());
    }


    @Override
    protected void onPause() {
        super.onPause();
        mFieldView.stopAnime();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mFieldView.stopAnime();
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
