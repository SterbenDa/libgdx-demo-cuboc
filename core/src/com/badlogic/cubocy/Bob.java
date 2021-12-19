
package com.badlogic.cubocy;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/**
 * 主角
 */
public class Bob {
    /**
     * 无事可做的
     */
    static final int IDLE = 0;
    static final int RUN = 1;
    static final int JUMP = 2;
    static final int SPAWN = 3;
    static final int DYING = 4;
    static final int DEAD = 5;
    static final int LEFT = -1;
    static final int RIGHT = 1;
    /**
     * 加速
     */
    static final float ACCELERATION = 20f;
    /**
     * 跳跃速度
     */
    static final float JUMP_VELOCITY = 10;
    /**
     * 重力
     */
    static final float GRAVITY = 20.0f;
    /**
     * 最大速度
     */
    static final float MAX_VEL = 6f;
    /**
     * 阻尼
     */
    static final float DAMP = 0.90f;

    /**
     * 坐标
     */
    Vector2 pos = new Vector2();
    /**
     * 加速度a x代表水平，y代表竖直
     */
    Vector2 accel = new Vector2();
    /**
     * 速度 v=v0+at
     */
    Vector2 vel = new Vector2();

    /**
     * 人物矩形
     */
    public Rectangle bounds = new Rectangle();

    /**
     * 状态
     */
    int state = SPAWN;
    float stateTime = 0;
    /**
     * 方向
     */
    int dir = LEFT;
    Map map;
    /**
     * 是否接触地面
     */
    boolean grounded = false;

    public Bob(Map map, float x, float y) {
        this.map = map;
        pos.x = x;
        pos.y = y;
        bounds.width = 0.6f;
        bounds.height = 0.8f;
        bounds.x = pos.x + 0.2f;
        bounds.y = pos.y;
        state = SPAWN;
        stateTime = 0;
    }

    /**
     * 主角更新
     */
    public void update(float deltaTime) {
        processKeys();

        // 竖直方向重力加速度
        accel.y = -GRAVITY;
        // 计算当前变化帧的速度a*t
        accel.scl(deltaTime);
        // 当前速度v=v0+at
        vel.add(accel.x, accel.y);
        if (accel.x == 0) {
            // 水平没加速度，有阻尼
            vel.x *= DAMP;
        }
        if (vel.x > MAX_VEL) {
            // 限速
            vel.x = MAX_VEL;
        }
        if (vel.x < -MAX_VEL) {
            vel.x = -MAX_VEL;
        }
        // 根据当前速度和时间 算出移动距离x=vt
        vel.scl(deltaTime);
        tryMove();
        // 尝试理解：1帧 客观世界1秒 也就是当前实际速度 因为有限速，否则会越来越快
        vel.scl(1.0f / deltaTime);

        if (state == SPAWN) {
            if (stateTime > 0.4f) {
                state = IDLE;
            }
        }

        if (state == DYING) {
            if (stateTime > 0.4f) {
                state = DEAD;
            }
        }

        stateTime += deltaTime;
    }

    private void processKeys() {
        if (map.cube.state == Cube.CONTROLLED || state == SPAWN || state == DYING) {
            // 正在控制立方体或出生或死亡状态，不处理
            return;
        }

        float x0 = (Gdx.input.getX(0) / (float) Gdx.graphics.getWidth()) * 480;
        float x1 = (Gdx.input.getX(1) / (float) Gdx.graphics.getWidth()) * 480;
        float y0 = 320 - (Gdx.input.getY(0) / (float) Gdx.graphics.getHeight()) * 320;

        boolean leftButton = (Gdx.input.isTouched(0) && x0 < 70) || (Gdx.input.isTouched(1) && x1 < 70);
        boolean rightButton = (Gdx.input.isTouched(0) && x0 > 70 && x0 < 134) || (Gdx.input.isTouched(1) && x1 > 70 && x1 < 134);
        boolean jumpButton = (Gdx.input.isTouched(0) && x0 > 416 && x0 < 480 && y0 < 64)
                || (Gdx.input.isTouched(1) && x1 > 416 && x1 < 480 && y0 < 64);

        if ((Gdx.input.isKeyPressed(Keys.W) || jumpButton) && state != JUMP) {
            // 设置跳跃状态
            state = JUMP;
            // 设置向上跳跃速度
            vel.y = JUMP_VELOCITY;
            grounded = false;
        }

        if (Gdx.input.isKeyPressed(Keys.A) || leftButton) {
            // 左移
            if (state != JUMP) {
                state = RUN;
            }
            dir = LEFT;
            accel.x = ACCELERATION * dir;
        } else if (Gdx.input.isKeyPressed(Keys.D) || rightButton) {
            // 右移
            if (state != JUMP) {
                state = RUN;
            }
            dir = RIGHT;
            accel.x = ACCELERATION * dir;
        } else {
            // 没事做
            if (state != JUMP) {
                state = IDLE;
            }
            accel.x = 0;
        }
    }

    /**
     * 人物矩形4个点的碰撞体 和 跟随立方体的碰撞体
     */
    Rectangle[] r = {new Rectangle(), new Rectangle(), new Rectangle(), new Rectangle(), new Rectangle()};

    /**
     * 尝试移动
     */
    private void tryMove() {
        bounds.x += vel.x;
        fetchCollidableRects();
        for (int i = 0; i < r.length; i++) {
            Rectangle rect = r[i];
            if (bounds.overlaps(rect)) {
                // 重叠，意味着碰撞
                if (vel.x < 0) {
                    // 向左碰撞
                    bounds.x = rect.x + rect.width + 0.01f;
                } else {
                    bounds.x = rect.x - bounds.width - 0.01f;
                }
                // 水平速度没有了
                vel.x = 0;
            }
        }

        // 同理更新垂直方向
        bounds.y += vel.y;
        fetchCollidableRects();
        for (int i = 0; i < r.length; i++) {
            Rectangle rect = r[i];
            if (bounds.overlaps(rect)) {
                // 重叠，意味着碰撞
                if (vel.y < 0) {
                    // 向下碰撞，落地了
                    bounds.y = rect.y + rect.height + 0.01f;
                    grounded = true;
                    if (state != DYING && state != SPAWN) {
                        state = Math.abs(accel.x) > 0.1f ? RUN : IDLE;
                    }
                } else {
                    bounds.y = rect.y - bounds.height - 0.01f;
                }
                // 垂直速度没有了
                vel.y = 0;
            }
        }

        pos.x = bounds.x - 0.2f;
        pos.y = bounds.y;
    }

    /**
     * 抵达设置碰撞体
     */
    private void fetchCollidableRects() {
        /**
         * p4 ------------- p3
         *    |           |
         *    |           |
         * p1 ------------- p2
         */
        int p1x = (int) bounds.x;
        int p1y = (int) Math.floor(bounds.y);
        int p2x = (int) (bounds.x + bounds.width);
        int p2y = (int) Math.floor(bounds.y);
        int p3x = (int) (bounds.x + bounds.width);
        int p3y = (int) (bounds.y + bounds.height);
        int p4x = (int) bounds.x;
        int p4y = (int) (bounds.y + bounds.height);

        int[][] tiles = map.tiles;
        int tile1 = tiles[p1x][map.tiles[0].length - 1 - p1y];
        int tile2 = tiles[p2x][map.tiles[0].length - 1 - p2y];
        int tile3 = tiles[p3x][map.tiles[0].length - 1 - p3y];
        int tile4 = tiles[p4x][map.tiles[0].length - 1 - p4y];

        if (state != DYING && (map.isDeadly(tile1) || map.isDeadly(tile2) || map.isDeadly(tile3) || map.isDeadly(tile4))) {
            // 碰到钉子，给我死
            state = DYING;
            stateTime = 0;
        }

        if (tile1 == Map.TILE) {
            // 位置是砖，设置碰撞体
            r[0].set(p1x, p1y, 1, 1);
        } else {
            r[0].set(-1, -1, 0, 0);
        }
        if (tile2 == Map.TILE) {
            r[1].set(p2x, p2y, 1, 1);
        } else {
            r[1].set(-1, -1, 0, 0);
        }
        if (tile3 == Map.TILE) {
            r[2].set(p3x, p3y, 1, 1);
        } else {
            r[2].set(-1, -1, 0, 0);
        }
        if (tile4 == Map.TILE) {
            r[3].set(p4x, p4y, 1, 1);
        } else {
            r[3].set(-1, -1, 0, 0);
        }

        if (map.cube.state == Cube.FIXED) {
            // 跟随的立方体是固定状态，此时是可碰撞的，设置碰撞体
            r[4].x = map.cube.bounds.x;
            r[4].y = map.cube.bounds.y;
            r[4].width = map.cube.bounds.width;
            r[4].height = map.cube.bounds.height;
        } else {
            r[4].set(-1, -1, 0, 0);
        }
    }
}
