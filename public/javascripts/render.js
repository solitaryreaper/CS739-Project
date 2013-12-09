/**
 * TODO :
 * 1) Add a color and size picker canvas to differentiate between various clients.
 * 2) Add a frame on the left with the list of active users for this session.
 * 3) Add proper error-handling for websocket lifecycle.
 * 4) Modify JSON object to contain color and size too.
 */
$(document).ready(function () {
	console.log("Setting canvas drawing pad ..");
    var canvas = document.getElementById('sketch_pad');
    var ctx = canvas.getContext('2d');

    var sketch = document.getElementById('sketch_container');
    var sketch_style = getComputedStyle(sketch);
    canvas.width = parseInt(sketch_style.getPropertyValue('width'));
    canvas.height = parseInt(sketch_style.getPropertyValue('height'));

    var curr_mouse = {
        x: 0,
        y: 0
    };
    var last_mouse = {
        x: 0,
        y: 0
    };

    // CONSTANTS
    var COLORS = ['red', 'blue', 'yellow', 'green', 'white'];
    var SIZES = [2, 5, 8, 10, 14];
    
    // State variables
    var color = COLORS[0];
    var size = SIZES[1];

    // Send the painter name in each event to identify the client to the server.
    var user_id = $("#painter_name").text();
    console.log("Created painter " + user_id);
    
    var server_ip_address = $("#ip_address").text();
    console.log("Server IP address : " + server_ip_address);
    console.log("Location Host : " + location.host);
    
    /**
     *	Open a Websocket connection here that would be responsible for handle real-time communication
     *  of events between client and server.
     *  
     *  Example websocket URL : http://127.0.0.1:9000/stream?paintroom=India
     **/
    var paint_room_name = value = $("#paint_room_name").text();
    var socket = new WebSocket("ws://" + server_ip_address + ":9000" + "/stream?paintroom=" + paint_room_name);
    console.log("Opening a new websocket connection : " + location.host + " at client for paintroom " + paint_room_name);
    is_connected = false;



    /* Websocket event handlers/callbacks */
    socket.onopen = function () {
        console.log("Connected via websocket ..");
        is_connected = true;
        // Bootstrap the canvas with prior events from the server ..
        if (is_connected) {
            console.log("Bootstrapping prior events for the current session on local client ..");
            dummy_initial_bootstrap();
        } else {
            console.log("Not connected via websocket yet during bootstrapping ..");
        }
    }

    socket.onclose = function () {
        console.log("Websocket disconnected ..");
        is_connected = false;
    }

    // Handle incoming messages/events from the server via websocket duplex connection
    socket.onmessage = function (msg) {
        // Consume events from server. Specifically, replays all events on client to simulate
        // real-time synchronization between distributed clients
        var data = JSON.parse(msg.data);
        draw(data.start_x, data.start_y, data.end_x, data.end_y);
    }

    // Send local client messages/events to the server via websocket duplex connection
    function sendMsgToServer(msg) {
        console.log("Sending a message to server on websocket ..");
        if (is_connected) {
            socket.send(JSON.stringify(msg))
        }
    }

    /* Mouse Capturing Work */
    canvas.addEventListener('mousemove', function (e) {
        last_mouse.x = curr_mouse.x;
        last_mouse.y = curr_mouse.y;

        curr_mouse.x = e.pageX - this.offsetLeft;
        curr_mouse.y = e.pageY - this.offsetTop;
    }, false);


    /* Drawing on Paint App */
    ctx.lineWidth = size;
    ctx.lineJoin = 'round';
    ctx.lineCap = 'round';
    ctx.strokeStyle = color;

    canvas.addEventListener('mousedown', function (e) {
        canvas.addEventListener('mousemove', onPaint, false);
    }, false);

    canvas.addEventListener('mouseup', function () {
        canvas.removeEventListener('mousemove', onPaint, false);
    }, false);

    /**
     * Function that draws locally based on brush movements and
     * disseminates local draw events to the server for broadcasting ..
     */
    var onPaint = function () {
        // Draw the changes locally and then broadcast it too all the other clients
        draw(last_mouse.x, last_mouse.y, curr_mouse.x, curr_mouse.y, size, color);

        // Compose a JSON event to send to server
        console.log("Broadcasting local change to all the connected clients ..");
        var msg = {
            start_x: last_mouse.x,
            start_y: last_mouse.y,
            end_x: curr_mouse.x,
            end_y: curr_mouse.y,
            name: user_id
        }
        sendMsgToServer(msg);
    };

    /**
     * Utility function that actually draws on the canvas between specified co-ordinates.
     */
    function draw(start_x, start_y, end_x, end_y, brush_size, brush_color) {
        console.log("Painting on the client .. => (" + start_x + "," + start_y + ") , " +
            "(" + end_x + "," + end_y + ")");
        ctx.beginPath();
        ctx.moveTo(start_x, start_y);
        ctx.lineTo(end_x, end_y);
        ctx.closePath();
        ctx.stroke();
    }

    /**
     * When the canvas loads at the client, to get all the "happened-before" events for this canvas
     * from the server send a dummy transparent brush event to the server. Before this we were getting
     * the past events only when the painter started drawing on the canvas because the new painter
     * determination logic is at the server end. This hack ensures that the client gets a fully
     * "baked" canvas when it is loaded on the client.
     */
    function dummy_initial_bootstrap() {
        var msg = {
            start_x: 0,
            start_y: 0,
            end_x: 0,
            end_y: 0,
            name: user_id,
            brush_size: 1,
            brush_color: 'white',
            event_type: 'dummy'
        }

        sendMsgToServer(msg);
        console.log("Sent dummy event for user " + user_id + ", message " + JSON.stringify(msg));
    }
    
});