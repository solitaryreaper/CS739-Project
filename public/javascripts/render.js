/**
 * TODO : 
 * 1) Add a color and size picker canvas to differentiate between various clients.
 * 2) Add a frame on the left with the list of active users for this session.
 * 3) Add proper error-handling for websocket lifecycle.
 * 4) Modify JSON object to contain color and size too.
 */
$(document).ready(function () {
	var canvas = document.querySelector('#sketch_pad');
	var ctx = canvas.getContext('2d');
	
	var sketch = document.querySelector('#sketch_container');
	var sketch_style = getComputedStyle(sketch);
	canvas.width = parseInt(sketch_style.getPropertyValue('width'));
	canvas.height = parseInt(sketch_style.getPropertyValue('height'));

	var curr_mouse = {x: 0, y: 0};
	var last_mouse = {x: 0, y: 0};
	
	/**
	 *	Open a Websocket connection here that would be responsible for handle real-time communication
	 *  of events between client and server.
	 * 
	 **/
	console.log("Opening a new websocket connection : " + location.host);
	var socket = new WebSocket("ws://" + location.host + "/stream");
	console.log("Opened a new websocket connection ..");
	is_connected = false;

	var user_id = Math.random().toString(36).slice(2);
	console.log("Created user id " + user_id);
	
	/* Websocket event handlers/callbacks */
	console.log("Before websocket open ..");
	socket.onopen = function() {
		console.log("Connected via websocket ..");
		is_connected = true;
	}
	console.log("After websocket open ..");
	socket.onclose = function() {
		console.log("Websocket disconnected ..");
		is_connected = false;
	}
	
	// Handle incoming messages/events from the server via websocket duplex connection
	socket.onmessage = function(msg) {
		// Consume events from server. Specifically, replays all events on client to simulate
		// real-time synchronization between distributed clients
		var data = JSON.parse(msg.data);
		console.log("Recieved a message from server on websocket from client " + data.user_id);
		
		draw(data.start_x, data.start_y, data.end_x, data.end_y);
	}
	
	// Send local client messages/events to the server via websocket duplex connection
	function sendMsgToServer(msg) {
		console.log("Sending a message to server on websocket ..");
		if(is_connected) {
			socket.send(JSON.stringify(msg))
		}
	}
	
	/* Mouse Capturing Work */
	canvas.addEventListener('mousemove', function(e) {
		last_mouse.x = curr_mouse.x;
		last_mouse.y = curr_mouse.y;
		
		curr_mouse.x = e.pageX - this.offsetLeft;
		curr_mouse.y = e.pageY - this.offsetTop;
	}, false);
	
	
	/* Drawing on Paint App */
	ctx.lineWidth = 5;
	ctx.lineJoin = 'round';
	ctx.lineCap = 'round';
	ctx.strokeStyle = 'blue';
	
	canvas.addEventListener('mousedown', function(e) {
		canvas.addEventListener('mousemove', onPaint, false);
	}, false);
	
	canvas.addEventListener('mouseup', function() {
		canvas.removeEventListener('mousemove', onPaint, false);
	}, false);
	
	var onPaint = function() {
		// Draw the changes locally and then broadcast it too all the other clients
		draw(last_mouse.x, last_mouse.y, curr_mouse.x, curr_mouse.y);
		
		// Compose a JSON event to send to server
		console.log("Broadcasting local change to all the connected clients ..");
		var msg = {start_x : last_mouse.x, start_y : last_mouse.y, end_x : curr_mouse.x, end_y : curr_mouse.y, id : user_id}
		sendMsgToServer(msg);
	};

	/**
	 * Utility function that actually draws on the canvas between specified co-ordinates.
	 */
	function draw(start_x, start_y, end_x, end_y)
	{
		console.log("Painting on the client .. => (" + start_x + "," + start_y + ") , (" + end_x + "," + end_y + ")");
		ctx.beginPath();
		ctx.moveTo(start_x, start_y);
		ctx.lineTo(end_x, end_y);
		ctx.closePath();
		ctx.stroke();
	}
});
