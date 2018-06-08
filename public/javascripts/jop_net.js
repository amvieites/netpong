var myWebSocket;

function sendMsg(msg) {
    myWebSocket.send(msg);
}
function closeConn() {
    myWebSocket.close();
}


function connectToWS(gameId) {
    var endpoint = "ws://" + window.location.host + "/ws/positions?id=" + gameId;
    if (myWebSocket !== undefined) {
        myWebSocket.close()
    }
    myWebSocket = new WebSocket(endpoint);
    myWebSocket.onmessage = function (event) {
        handleWsMessage(event);
    };
    myWebSocket.onopen = function (evt) {
        console.log("onopen.");
    };
    myWebSocket.onclose = function (evt) {
        console.log("onclose.");
    };
    myWebSocket.onerror = function (evt) {
        console.log("Error!");
    };
}

function handleWsMessage(event) {
    var leng;
    if (event.data.size === undefined) {
        leng = event.data.length
    } else {
        leng = event.data.size
    }
    if (leng > 0) {
        //                            console.log("Event received: " + event.data)
        var cmd = JSON.parse(event.data);
        if (cmd.msgType === "GAME_UPDATE") {
            Pong.lastServerUpdate = cmd;
        } else if (cmd.msgType === "WELCOME") {
            console.log("onmessage. size: " + leng + ", content: " + event.data);
            if (cmd.player_no === 1) {
                Pong.whoiam = function () {
                    return Pong.lastPhy().p1
                };
            } else {
                Pong.whoiam = function () {
                    return Pong.lastPhy().p2
                };
            }
            Pong.whoiam().moveUp = Pong.UP;
            Pong.whoiam().moveDown = Pong.DOWN;
        } else if (cmd.msgType === "COUNTDOWN") {
            console.log(cmd);
            if (Pong.countdown !== undefined) {
                Pong.countdown(cmd.seconds);
            }
        } else if (cmd.msgType === "POINT_START") {
            console.log("Starting point " + cmd.pointNumber);
            Pong.startPhysicsLoop();
        } else if (cmd.msgType === "PLAYER_SCORED") {
            Pong.stopPhysicsLoop();
            if (Pong.scoring !== undefined) {
                console.log(cmd);
                if (Pong.whoiam().num === 1) {
                    Pong.scoring(cmd.me, cmd.him);
                } else {
                    Pong.scoring(cmd.him, cmd.me);
                }
            }
        } else if (cmd.msgType === "GAME_STATE") {
            if (cmd.state === 'AWAITING_PLAYERS' && Pong.gameEnd !== undefined) {
                Pong.gameEnd();
            } else if (cmd.state === 'PLAYING' && Pong.gameStart !== undefined) {
                Pong.gameStart();
            } else {
                console.log("Unhandled game state: " + cmd.state);
            }
        } else {
            console.log("Unrecognized message: " + event.data)
        }
    }
}