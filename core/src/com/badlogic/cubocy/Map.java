
package com.badlogic.cubocy;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.Array;

/**
 * 地图
 */
public class Map {
    static int EMPTY = 0;
    /**
     * 砖
     */
    static int TILE = 0xffffff;
    static int START = 0xff0000;
    static int END = 0xff00ff;
    static int DISPENSER = 0xff0100;
    /**
     * 钉子参数
     */
    static int SPIKES = 0x00ff00;
    static int ROCKET = 0x0000ff;
    static int MOVING_SPIKES = 0xffff00;
    static int LASER = 0x00ffff;

    int[][] tiles;
    /**
     * 主角
     */
    public Bob bob;
    /**
     * 立方体
     */
    Cube cube;
    Array<Dispenser> dispensers = new Array<Dispenser>();
    /**
     * 最近的传送点吧
     */
    Dispenser activeDispenser = null;
    /**
     * 火箭
     */
    Array<Rocket> rockets = new Array<Rocket>();
    /**
     * 钉
     */
    Array<MovingSpikes> movingSpikes = new Array<MovingSpikes>();
    /**
     * 激光
     */
    Array<Laser> lasers = new Array<Laser>();
    public EndDoor endDoor;

    public Map() {
        loadBinary();
    }

    private void loadBinary() {
        Pixmap pixmap = new Pixmap(Gdx.files.internal("data/levels.png"));
        // 图的像素大小240*160
        tiles = new int[pixmap.getWidth()][pixmap.getHeight()];
        for (int y = 0; y < 35; y++) {
            for (int x = 0; x < 150; x++) {
                int pix = (pixmap.getPixel(x, y) >>> 8) & 0xffffff;
                if (match(pix, START)) {
                    // 开始
                    Dispenser dispenser = new Dispenser(x, pixmap.getHeight() - 1 - y);
                    dispensers.add(dispenser);
                    activeDispenser = dispenser;
                    bob = new Bob(this, activeDispenser.bounds.x, activeDispenser.bounds.y);
                    bob.state = Bob.SPAWN;
                    cube = new Cube(this, activeDispenser.bounds.x, activeDispenser.bounds.y);
                    cube.state = Cube.DEAD;
                } else if (match(pix, DISPENSER)) {
                    // 最近传送点
                    Dispenser dispenser = new Dispenser(x, pixmap.getHeight() - 1 - y);
                    dispensers.add(dispenser);
                } else if (match(pix, ROCKET)) {
                    Rocket rocket = new Rocket(this, x, pixmap.getHeight() - 1 - y);
                    rockets.add(rocket);
                } else if (match(pix, MOVING_SPIKES)) {
                    movingSpikes.add(new MovingSpikes(this, x, pixmap.getHeight() - 1 - y));
                } else if (match(pix, LASER)) {
                    lasers.add(new Laser(this, x, pixmap.getHeight() - 1 - y));
                } else if (match(pix, END)) {
                    endDoor = new EndDoor(x, pixmap.getHeight() - 1 - y);
                } else {
                    tiles[x][y] = pix;
                }
            }
        }

        for (int i = 0; i < movingSpikes.size; i++) {
            movingSpikes.get(i).init();
        }
        for (int i = 0; i < lasers.size; i++) {
            lasers.get(i).init();
        }
    }

    boolean match(int src, int dst) {
        return src == dst;
    }

    /**
     * 地图有更新
     */
    public void update(float deltaTime) {
        bob.update(deltaTime);
        if (bob.state == Bob.DEAD) {
            // 死了复活
            bob = new Bob(this, activeDispenser.bounds.x, activeDispenser.bounds.y);
        }
        cube.update(deltaTime);
        if (cube.state == Cube.DEAD) {
            cube = new Cube(this, bob.bounds.x, bob.bounds.y);
        }
        // 接触到新的传送点，更新
        for (int i = 0; i < dispensers.size; i++) {
            if (bob.bounds.overlaps(dispensers.get(i).bounds)) {
                activeDispenser = dispensers.get(i);
            }
        }
        for (int i = 0; i < rockets.size; i++) {
            Rocket rocket = rockets.get(i);
            rocket.update(deltaTime);
        }
        for (int i = 0; i < movingSpikes.size; i++) {
            MovingSpikes spikes = movingSpikes.get(i);
            spikes.update(deltaTime);
        }
        for (int i = 0; i < lasers.size; i++) {
            lasers.get(i).update();
        }
    }

    public boolean isDeadly(int tileId) {
        return tileId == SPIKES;
    }
}
