var Pong = {

    UP: 38,
    DOWN: 40,
    /*global console */
    canvas: window.document.getElementById("pong"),
    ctx: window.document.getElementById("pong").getContext('2d'),
    WIDTH: undefined,
    HEIGHT: undefined,
    ballSpeed: undefined,
    playerSpeed: undefined,
    skin: {
        paddle: '#C4B9A9',
        ball: '#9FFCB9',
        field: '#2E3340'
    },
    keys: {
        release: function () {
            this.UP = false;
            this.DOWN = false
        },
        UP: false,
        DOWN: false
    },
    whoiam: undefined,
    frameId: undefined,
    lastServerUpdate: undefined,
    jitterbuf: [],

    keydownListener: function (event) {
        var key;
        if (event.keyCode === Pong.UP) key = "up";
        if (event.keyCode === Pong.DOWN) key = "down";

        if (key !== undefined) {
            if (Pong.keys[event.keyCode] === false) {
                // Store key stroke
                Pong.keys[event.keyCode] = true;

                // Notify server
                var keyStrokeUpdate = {};
                keyStrokeUpdate.msgType = "KEY_STROKE_UPDATE";
                keyStrokeUpdate.key = key;
                keyStrokeUpdate.event = "keydown";
                keyStrokeUpdate.millis = new Date().getTime();
                sendMsg(JSON.stringify(keyStrokeUpdate));
            }
        }
    },
    keyupListener: function (event) {
        var key;
        if (event.keyCode === Pong.UP) key = "up";
        if (event.keyCode === Pong.DOWN) key = "down";

        if (key !== undefined) {
            // Store key stroke
            Pong.keys[event.keyCode] = false;

            // Notify server
            var keyStrokeUpdate = {};
            keyStrokeUpdate.msgType = "KEY_STROKE_UPDATE";
            keyStrokeUpdate.key = key;
            keyStrokeUpdate.event = "keyup";
            keyStrokeUpdate.millis = new Date().getTime();
            sendMsg(JSON.stringify(keyStrokeUpdate));
        }
    },

    init: function (gameId, width, height, ballSpeed, playerSpeed) {
        "use strict";
        this.WIDTH = width;
        this.HEIGHT = height;
        this.ballSpeed = ballSpeed;
        this.playerSpeed = playerSpeed;
        this.initJitter();

        connectToWS(gameId);
        window.document.addEventListener("keydown", this.keydownListener);
        window.document.addEventListener("keyup", this.keyupListener);

        var loop = function () {
            Pong.lastPhy().draw();
            window.requestAnimationFrame(loop);
        };
        window.requestAnimationFrame(loop);
    },

    applyServerStatus: function () {
        var cmd = Pong.lastServerUpdate;
        if (cmd !== undefined) {
            Pong.frameId = Math.max(cmd.frameId, Pong.frameId);

            var serverFrameIdx;
            if (cmd.frameId < Pong.frameId) {
                serverFrameIdx = Pong.jitterbuf.findIndex(function (phy) {
                    return phy.frameId >= cmd.frameId
                    //return phy.millis >= cmd.time
                });
                Pong.jitterbuf.splice(0, serverFrameIdx);
            } else {
                Pong.jitterbuf.splice(0, Pong.jitterbuf.length - 1);
                Pong.jitterbuf[0].frameId = cmd.frameId;
                Pong.jitterbuf[0].phy.millis = cmd.time;
                Pong.jitterbuf[0].phy.ball.x = cmd.ball.x;
                Pong.jitterbuf[0].phy.ball.y = cmd.ball.y;
                Pong.jitterbuf[0].phy.ball.direction.x = cmd.ball.mov.x;
                Pong.jitterbuf[0].phy.ball.direction.y = cmd.ball.mov.y;
                Pong.jitterbuf[0].phy.ball.speed = cmd.ball.mov.speed;
            }

            // interpolate to the present
            var lastSyncSnap = Pong.jitterbuf[0];
            lastSyncSnap.phy.ball.speed = cmd.ball.mov.speed;
            lastSyncSnap.phy.ball.x = cmd.ball.x;
            lastSyncSnap.phy.ball.y = cmd.ball.y;
            lastSyncSnap.phy.ball.direction.x = cmd.ball.mov.x;
            lastSyncSnap.phy.ball.direction.y = cmd.ball.mov.y;
            var me;
            if (Pong.whoiam().num === 1) {
                me = lastSyncSnap.phy.p1;
                him = lastSyncSnap.phy.p2;
            } else {
                me = lastSyncSnap.phy.p2;
                him = lastSyncSnap.phy.p1;
            }
            me.x = cmd.me.x;
            me.y = cmd.me.y;
            me.speed = cmd.me.mov.speed;
            me.direction.x = cmd.me.mov.x;
            me.direction.y = cmd.me.mov.y;
            him.x = cmd.him.x;
            him.y = cmd.him.y;
            him.speed = cmd.him.mov.speed;
            him.direction.x = cmd.him.mov.x;
            him.direction.y = cmd.him.mov.y;

            for (i = 1; i < Pong.jitterbuf.length; i++) {
                // Interpolate each one of the client predictions
                Pong.jitterbuf[i].phy = Pong.jitterbuf[i - 1].phy.update(0.015);
            }
            if (Math.abs(cmd.ball.x - lastSyncSnap.phy.ball.x) > 10) {
                console.log("client: " + lastSyncSnap.phy.ball.x + ", server: " + cmd.ball.x);
            }
        }
    },

    lastPhy: function () {
        if (this.jitterbuf.length !== 0) {
            return this.jitterbuf[this.jitterbuf.length - 1].phy;
        }
    },

    initJitter: function () {
        Pong.lastPhisics = Pong.timestamp();
        this.jitterbuf = [{
            frameId: 0, phy: new Phy(new Ball(Pong.WIDTH / 2 - 5, Pong.HEIGHT / 2 - 5, {x: -1, y: 0}, Pong.ballSpeed),
                new Player(Pong.playerSpeed, 1, 10, 60, 20, Pong.HEIGHT / 2 - (60 / 2), {x: 0, y: 0}),
                new Player(Pong.playerSpeed, 2, 10, 60, Pong.WIDTH - 30, Pong.HEIGHT / 2 - (60 / 2), {x: 0, y: 0}))
        }];
    },

    startPhysicsLoop: function () {
        this.stopPhysicsLoop();
        this.initJitter();
        this.keys.release();
        this.frameId = 0;
        this.lastServerUpdate = undefined;
        this.lastPhisics = Pong.timestamp();
        this.physicsLoopId = setInterval(function () {
            Pong.frameId += 1;
            Pong.processInput();
            Pong.applyServerStatus();
            var dt = Math.min((Pong.timestamp() - Pong.lastPhisics) / 1000, 0.015);
            Pong.lastPhisics = Pong.timestamp();
            Pong.jitterbuf.push({frameId: Pong.frameId, millis: Pong.lastPhisics, phy: Pong.lastPhy().update(dt)});
        }.bind(this), 15);
        console.log("Started physics loop " + this.physicsLoopId + ", ");
    },
    stopPhysicsLoop: function () {
        if (Pong.physicsLoopId !== undefined) {
            console.log("Stopping physics loop " + Pong.physicsLoopId);
            clearInterval(Pong.physicsLoopId);
        }
    },

    processInput: function () {
        var direction;
        if (this.keys[Pong.UP]) {
            direction = -1;
        } else {
            direction = 0;
        }

        if (this.keys[Pong.DOWN]) {
            direction = 1;
        } else {
            direction = 0;
        }

        this.whoiam().direction.y = direction;
    },
    timestamp: function () {
        return window.performance && window.performance.now ? window.performance.now() : new Date().getTime();
    },

    score: function (p1, p2) {
        "use strict";
        var div = document.getElementById('scoreboard');
        div.innerText = p1 + ' - ' + p2;
    },
};

/** Ball */
function Ball(x, y, direction, speed) {
    "use strict";

    this.x = x;
    this.y = y;
    this.direction = direction;
    this.speed = speed;
}

Ball.prototype.update = function (delta) {
    "use strict";

    var x = this.x + this.direction.x * this.speed * delta;
    var y = this.y + this.direction.y * this.speed * delta;
    var direction = this.direction;
    if (this.y < 0 || (this.y + 10) > Pong.HEIGHT) {
        direction.y *= -1;
    }

    var normalized;
    var angle;
    if (this.x < 0 || this.x > Pong.WIDTH) {
        direction = {x: 0, y: 0};
    } else if (this.y >= (Pong.lastPhy().p1.y - 10) &&      // p1 intersects
        this.y <= (Pong.lastPhy().p1.y + Pong.lastPhy().p1.height) &&
        Math.abs(this.x - Pong.lastPhy().p1.x) < 10) {
        x = Pong.lastPhy().p1.x + (Pong.lastPhy().p1.width) + 1;
        normalized = (this.y - Pong.lastPhy().p1.y) / ((Pong.lastPhy().p1.x + Pong.lastPhy().p1.height - 10) - Pong.lastPhy().p1.x);
        angle = 0.25 * Math.PI * (2 * normalized - 1);
        direction.x = Math.cos(angle);
        direction.y = Math.sin(angle);
    } else if (this.y >= (Pong.lastPhy().p2.y - 10) &&      // p2 intersects
        this.y <= (Pong.lastPhy().p2.y + Pong.lastPhy().p2.height) &&
        Math.abs(this.x - Pong.lastPhy().p2.x) < 10) {
        x = Pong.lastPhy().p2.x - 10 - 1;
        normalized = (this.y - Pong.lastPhy().p2.y) / ((Pong.lastPhy().p2.x + Pong.lastPhy().p2.height - 10) - Pong.lastPhy().p2.x);
        angle = 0.25 * Math.PI * (2 * normalized - 1);
        direction.x = -Math.cos(angle);
        direction.y = Math.sin(angle);
    }

    return new Ball(x, y, direction, this.speed);
};

Ball.prototype.draw = function () {
    "use strict";
    Pong.ctx.fillStyle = Pong.skin.ball;
    Pong.ctx.fillRect(this.x, this.y, 10, 10);
};

/** Player */
function Player(playerSpeed, num, width, height, x, y, direction) {
    "use strict";
    this.num = num;
    this.speed = playerSpeed;
    this.width = width;
    this.height = height;
    this.x = x;
    this.y = y;
    this.direction = direction;
}
Player.prototype.update = function (delta) {
    "use strict";
    var increment = this.direction.y * this.speed * delta;
    var newY = Math.min(Math.max(0, this.y + increment), Pong.HEIGHT - this.height);
    return new Player(this.speed, this.num, this.width, this.height, this.x, newY, this.direction)
};
Player.prototype.draw = function () {
    "use strict";
    Pong.ctx.fillStyle = Pong.skin.paddle;
    Pong.ctx.fillRect(this.x, this.y, this.width, this.height);
};

/** PhysicalState */
function Phy(ball, p1, p2) {
    "use strict";
    this.ball = ball;
    this.p1 = p1;
    this.p2 = p2;
}

Phy.prototype.update = function (dt) {
    "use strict";
    return new Phy(this.ball.update(dt), this.p1.update(dt), this.p2.update(dt));
};

Phy.prototype.draw = function () {
    "use strict";
    Pong.ctx.fillStyle = Pong.skin.field;
    Pong.ctx.fillRect(0, 0, Pong.WIDTH, Pong.HEIGHT);

    this.ball.draw();
    this.p1.draw();
    this.p2.draw();

    Pong.ctx.restore();
};