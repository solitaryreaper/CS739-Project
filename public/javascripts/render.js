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
    
    // State variables
    var color = 'blue';
    var size = 5;
    
    var SESSION_MGR_API_URL = "http://54.201.156.52:8080/CollabDraw/serverOps?";

    // Send the painter name in each event to identify the client to the server.
    var user_id = $("#painter_name").text();
    console.log("Created painter " + user_id);
    
    var preferred_ip_address = $("#preferred_ip_address").text();
    console.log("Server IP address : " + preferred_ip_address);
    console.log("Location Host : " + location.host);

    /**
     *	Open a Websocket connection here that would be responsible for handle real-time communication
     *  of events between client and the preferred server.
     *  
     *  Example websocket URL : http://127.0.0.1:9000/stream?paintroom=India
     **/
    var paint_room_name = value = $("#paint_room_name").text();
    var primary_sock = new WebSocket("ws://" + location.host + "/stream?paintroom=" + paint_room_name);
    console.log("Opening a new websocket connection : " + location.host + " at client for paintroom " + paint_room_name);
    var is_connected = false;

    /* Websocket event handlers/callbacks */
    primary_sock.onopen = function () {
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

    primary_sock.onclose = function () {
        console.log("Websocket disconnected ..");
        is_connected = false;
        
        // Check if this is a normal client disconnect or preferred server failure
        var is_normal_client_disconnect = checkIfNormalClientDisconnect();
        
        /**
         * In case, this is not a normal client disconnect then need to figure out if there are any
         * other worker servers to which this client can be redirected. If yes, redirect to that
         * worker server, else operate in the disconnected mode.
         */
        if(is_normal_client_disconnect == false) {
        	var is_disconnected_mode = checkIfDisconnectedMode();
        	// Preferred server is down. Migrate the client to a new worker server
        	if(!is_disconnected_mode) {
        		console.log("Preferred server down. Migrating client to a new worker server ..");
        		handlePreferredServerDown();
        	}
        	// All worker servers are down. Operate in disconnected mode
        	else {
        		console.log("All worker servers down. Operating in disconnected mode ..");
        		handleDisconnectedMode();
        	}
        }
        else {
        	console.log("Client has ended its session ..");
        }
    }

    /*---------- Replicate Worker Server websocket for replication ----------------*/
    var replicate_ip_addresss_html = $("#replicate_ip_address").text();
    var replicate_ip_address = "";
    if( Object.prototype.toString.call(replicate_ip_address) == '[object HTMLLabelElement]' ) {
    	replicate_ip_addresss = replicate_ip_address_html.innerHTML;
    	console.log(replicate_ip_address_html.innerHTML);
    }
    else {
    	replicate_ip_addresss = replicate_ip_addresss_html;
    }
    console.log("Replicate IP address : " + replicate_ip_address);
    
    var replicate_sock = new WebSocket("ws://" + replicate_ip_address + ":9000" + "/synchronize?paintroom=" + paint_room_name);
    var is_replicate_connected = false;
    console.log("Opened a replicate socket connection to " + replicate_ip_address + " ... ");
    
    /* Websocket event handlers/callbacks for replicate server */
    replicate_sock.onopen = function () {
        console.log("Connected via websocket to replicate server ..");
        is_replicate_connected = true;
    }

    replicate_sock.onclose = function () {
        console.log("Websocket disconnected to replicate server ..");
        is_replicate_connected = false;
    }
    
    /**
     * Checks if the websocket disconnect happened because the client ended its session or
     * because the preferred server went down.
     */
    function checkIfNormalClientDisconnect()
    {
    	var is_normal_client_disconnect = false;
    	var api_result = "";
    	
    	console.log("Invoking a synchronous AJAX call to check for normal client disconnect ..");
        $.ajax({
            type: "GET",
            url: SESSION_MGR_API_URL,
            data: "operation=getServerStatus&serverIP=" + preferred_ip_address,            
    	    async:false,            
            success: function(response) {
            	console.log("API Result : " + response);
            	api_result = response;
            },
            error: function( req, status, err ) {
                console.log( 'something went wrong', status, err );
            }             
        });
        
        console.log("API Result for normal client disconnect mode determination is " + api_result);
        if(api_result == "reachable") {
        	is_normal_client_disconnect = true;
        }
        
    	return is_normal_client_disconnect;
    }
    
    /**
     * Checks if all the worker servers are down and the client is operating in disconnected mode.
     */
    function checkIfDisconnectedMode()
    {
    	var is_disconnected_mode = false;
    	var api_result = "";
    	
    	console.log("Invoking a synchronous AJAX call to check for disconnected mode ..");
        $.ajax({
            type: "GET",
            url: SESSION_MGR_API_URL,
            data: "operation=getWorkerServer&sessionId=" + paint_room_name + "&userId=" + user_id,
            success: function(response) {
            	console.log("API Result : " + response);
            	api_result = response;
            },
    	    async:false
        });
        
        console.log("API Result for disconnected mode determination is " + api_result);
        if(api_result == "DISCONNECTED") {
        	is_disconnected_mode = true;
        }
        
    	return is_disconnected_mode;
    }
    
    /**
     * Function that is invoked when the preferred server goes down
     */
    function handlePreferredServerDown()
    {
    	// Show the hidden form that would aid user in navigating to a new server
    	$("#failure_handler").show();
    }
    
    /**
     * Function that is invoked when the client is operating in disconnected mode.
     */
    function handleDisconnectedMode()
    {
    	
    }
    
    // Handle incoming messages/events from the server via websocket duplex connection
    primary_sock.onmessage = function (msg) {
        // Consume events from server. Specifically, replays all events on client to simulate
        // real-time synchronization between distributed clients
        var data = JSON.parse(msg.data);
        draw(data.start_x, data.start_y, data.end_x, data.end_y);
    }

    // Send local client messages/events to the server via websocket duplex connection
    function sendMsgToServer(msg) {
        console.log("Sending a message to server on websocket ..");
        if (is_connected) {
            primary_sock.send(JSON.stringify(msg));
            replicate_sock.send(JSON.stringify(msg));
        }
    }

    /* Mouse Capturing Work */
    canvas.addEventListener('mousemove', function (e) {
        last_mouse.x = curr_mouse.x;
        last_mouse.y = curr_mouse.y;

        var o = $(canvas).offset();
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
