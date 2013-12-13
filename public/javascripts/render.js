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
    var color = 'red';
    var size = 5;
    
    var SESSION_MGR_IP_ADDRESS = "54.201.156.52";
    var SESSION_MGR_API_URL = "http://" + SESSION_MGR_IP_ADDRESS + ":8080/CollabDraw/serverOps?";

    // Events stored in localstorage during disconnected mode.
    var local_events_ctr = 0;
    
    // Send the painter name in each event to identify the client to the server.
    var user_id = $("#painter_name").text();
    console.log("Created painter " + user_id);
    
    var preferred_ip_address = $("#preferred_ip_address").text();
    console.log("Server IP address : " + preferred_ip_address);
    console.log("Location Host : " + location.host);

    var paint_room_name = value = $("#paint_room_name").text();
    
    /**
     *	Open a Websocket connection here that would be responsible for handle real-time communication
     *  of events between client and the preferred server.
     *  
     *  Example websocket URL : http://127.0.0.1:9000/stream?paintroom=India
     **/

    var primary_sock;
    var replicate_sock;
    var is_connected = false;
    var is_replicate_connected = false;
    
    /**
     * Opens the primary websocket connection andn initializes all the event handlers.
     */
    function init_primary_websocket()
    {
        try {
        	primary_sock = new WebSocket("ws://" + location.host + "/stream?paintroom=" + paint_room_name);
            console.log("Opening a new websocket connection : " + location.host + " at client for paintroom " + paint_room_name);        	
        }
        catch(e) {
        	console.log("Failed top open primary websocket connection . Reason : " + e.message);
        }

        /* Websocket event handlers */
        
        // OPEN event handler
        primary_sock.onopen = function () {
            console.log("Connected via websocket ..");
            is_connected = true;
            // Bootstrap the canvas with prior events from the server ..
            if (is_connected) {
                console.log("Bootstrapping session storage events after offline mode ..");
                load_localstorage_events();  
                
                console.log("Bootstrapping prior events for the current session on local client ..");                	
                dummy_initial_bootstrap();                	
            } else {
                console.log("Not connected via websocket yet during bootstrapping ..");
            }
        }

        // ONCLOSE event handler
        primary_sock.onclose = function () {
            console.log("Websocket disconnected ..");
            is_connected = false;
            
            // Check if this is a normal client disconnect or preferred server failure
            var is_normal_client_disconnect = checkIfNormalClientDisconnect();
            console.log("Normal disconnect ? " + is_normal_client_disconnect);
            
            /**
             * In case, this is not a normal client disconnect then need to figure out if there are any
             * other worker servers to which this client can be redirected. If yes, redirect to that
             * worker server, else operate in the disconnected mode.
             */
            if(is_normal_client_disconnect == false) {
            	console.log("Unregistering the client with paintroom ..");
        		unregisterClientSession();

        		var is_disconnected_mode = checkIfDisconnectedMode();
            	console.log("Is disconnected ? " + is_disconnected_mode);
            	// Preferred server is down. Migrate the client to a new worker server
            	if(is_disconnected_mode == false) {
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
            	sessionStorage.clear();
            }
        }
        
        // Handle incoming messages/events from the server via websocket duplex connection
        primary_sock.onmessage = function (msg) {
            // Consume events from server. Specifically, replays all events on client to simulate
            // real-time synchronization between distributed clients
            var data = JSON.parse(msg.data);
            draw(data.start_x, data.start_y, data.end_x, data.end_y);
        }        
    }
    
    /*---------- Replicate Worker Server websocket for replication ----------------*/    
    function init_replicate_websocket()
    {
         try {
             var replicate_ip_address_html = $("#replicate_ip_address").text();
             console.log("Replicate HTML : " + replicate_ip_address_html);
             var replicate_ip_address = "";
             if( Object.prototype.toString.call(replicate_ip_address) == '[object HTMLLabelElement]' ) {
             	replicate_ip_address = replicate_ip_address_html.innerHTML;
             	console.log(replicate_ip_address_html.innerHTML);
             }
             else {
             	replicate_ip_address = replicate_ip_address_html;
             }
             console.log("Replicate IP address : " + replicate_ip_address);
             
        	replicate_sock = new WebSocket("ws://" + replicate_ip_address + ":9000" + "/synchronize?paintroom=" + paint_room_name);
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
        }
        catch(e) {
        	console.log("Replicate websocket connection establishment failed with " + replicate_ip_address + ". Reason : " + e.message);
        }    	
    }
    
    init_primary_websocket();
    init_replicate_websocket();

    /**
     * Checks if the specified worker server is up or not.
     */
    function checkIfServerIsUp(server_ip_address)
    {
    	var is_server_up = false;
    	console.log("Invoking a synchronous AJAX call to check for server's status ..");
    	try {
            $.ajax({
                type: "GET",
                url: SESSION_MGR_API_URL,
                data: "operation=getServerStatus&serverIP=" + server_ip_address,            
        	    async:false,            
                success: function(response) {
                	console.log("API Result : " + response);
                	api_result = response;
                },
                error: function( req, status, err ) {
                    console.log( 'something went wrong', status, err );
                }             
            });    		
    	}
    	catch(e) {
    		console.log("Failed to determine if server is up. Reason : " + e.message);
    	}

        
        console.log("API Result for normal client disconnect mode determination is " + api_result);
        api_result = $.trim(api_result);
        if(api_result == "reachable") {
        	is_server_up = true;
        }    	
        
    	return is_server_up;
    }
    
    /**
     * Checks if the websocket disconnect happened because the client ended its session or
     * because the preferred server went down.
     */
    function checkIfNormalClientDisconnect()
    {
    	var is_server_up = checkIfServerIsUp(preferred_ip_address);
    	console.log("Normal client disconnect : " + is_server_up);
    	if(is_server_up == true) {
    		return true;
    	}
    	
    	return false;
    }
    
    /**
     * Checks if all the worker servers are down and the client is operating in disconnected mode.
     */
    function checkIfDisconnectedMode()
    {
    	var is_disconnected_mode = false;
    	var api_result = "";
    	
    	try {
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
    	}
    	catch(e) {
    		console.log("Failed to determine if client is in disconnected state. Reason : " + e.message);
    	}

        
        console.log("API Result for disconnected mode determination is " + api_result + "..");
        api_result = $.trim(api_result);
        if(api_result == "DISCONNECTED") {
        	console.log("YES disconnected !!");
        	is_disconnected_mode = true;
        }
        else {
        	console.log("Not disconnected " + api_result + "..")
        }
        
    	return is_disconnected_mode;
    }
    
    /**
     * Establishes a session for this paintroom and client with the session manager.
     * 
     * This is required when the client is either operating in the failover or the disconnected
     * mode.
     */
    function registerClientSessionInFailoverMode()
    {
    	console.log("Invoking a synchronous AJAX call for client registration ..");
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
        console.log("API Result for client registration is " + api_result + "..");
    }
    
    /**
     * Unregsiters a client from a session with the session manager.
     */
    function unregisterClientSession()
    {
    	console.log("Invoking a synchronous AJAX call to unregister a client ..");
        $.ajax({
            type: "GET",
            url: SESSION_MGR_API_URL,
            data: "operation=unregisterUser&sessionId=" + paint_room_name + "&userId=" + user_id,
            success: function(response) {
            	console.log("API Result : " + response);
            	api_result = response;
            },
    	    async:false
        });
        console.log("API Result for client unregistration is " + api_result + "..");
    }
    
    /**
     * Function that is invoked when the preferred server goes down
     */
    function handlePreferredServerDown()
    {
    	// Show the hidden form that would aid user in navigating to a new server
    	$("#failure_handler").show();
    	$("#sketch_container").css({ opacity: 0.25 });    	
    	
    	// register session for this client with the new failover server.
    	console.log("Registering the client to paintroom with failover server ..");
		registerClientSessionInFailoverMode();
    }
    
    /**
     * Function that is invoked when the client is operating in disconnected mode.
     */
    function handleDisconnectedMode()
    {
    	// Show the hidden symbol to suggest that the user is operating in disconnected/offline mode.
    	$("#disconnected_handler").show();
    	$("#connected_handler").hide();
    }
    
    // Send local client messages/events to the server via websocket duplex connection
    function sendMsgToServer(msg) {
        console.log("Sending a message to server on websocket ..");
        var msg_str = JSON.stringify(msg);
        if (is_connected) {
            primary_sock.send(msg_str);
            if(is_replicate_connected) {
                replicate_sock.send(msg_str);            	
            }
        }
    }

    // Canvas event handlers
    function init_canvas_events()
    {
        /* Mouse Capturing Work */
        canvas.addEventListener('mousemove', function (e) {
            last_mouse.x = curr_mouse.x;
            last_mouse.y = curr_mouse.y;

            var o = $(canvas).offset();
            curr_mouse.x = e.pageX - this.offsetLeft;
            curr_mouse.y = e.pageY - this.offsetTop;
        }, false);


        canvas.addEventListener('mousedown', function (e) {
            canvas.addEventListener('mousemove', onPaint, false);
        }, false);

        canvas.addEventListener('mouseup', function () {
            canvas.removeEventListener('mousemove', onPaint, false);
        }, false);    	
    }

    function init_canvas_meta()
    {
        /* Drawing on Paint App */
        ctx.lineWidth = size;
        ctx.lineJoin = 'round';
        ctx.lineCap = 'round';
        ctx.strokeStyle = color;    	
    }
    
    init_canvas_events();
    init_canvas_meta();
    
    var new_sock_checker = -1;
    
    /**
     * Function that draws locally based on brush movements and
     * disseminates local draw events to the server for broadcasting ..
     */
    var onPaint = function () {
        // Draw the changes locally and then broadcast it too all the other clients
        draw(last_mouse.x, last_mouse.y, curr_mouse.x, curr_mouse.y, size, color);

        // Compose a JSON event to send to server
        var msg = {
            start_x: last_mouse.x,
            start_y: last_mouse.y,
            end_x: curr_mouse.x,
            end_y: curr_mouse.y,
            name: user_id
        }
        
        // If connected, send event to the server
        if(is_connected) {
            console.log("Broadcasting local change to all the connected clients ..");
            sendMsgToServer(msg);
        }
        // If in disconnected mode, store the events locally in HTML5 localstorage
        else {
        	console.log("Storing events locally in HTML5 storage ..");
        	sessionStorage.setItem(local_events_ctr.toString(), JSON.stringify(msg));
        	local_events_ctr = local_events_ctr + 1;
        	
        	// This interval function should be set only once and called periodically to check
        	// if the disconnected server is up.
        	if(new_sock_checker < 0) {
        		console.log("Starting a new interval clock ..");
            	new_sock_checker = setInterval(function(){renew_server_socket()}, 100);
        	}

        	/*
        	// check if the server was in disconnected state and is now connected
        	var is_server_up = false;
        	if(sessionStorage.length % 5 == 0) {
        		console.log("Checking server status ..");
        		is_server_up = checkIfServerIsUp(preferred_ip_address);
        		init_canvas_meta();
        	}
        	if(is_server_up == true) {
        		if(new_primary_sock_conn_initiated == false) {
            		console.log("Restoring primary websocket connection after disconnect mode ..");
            		init_primary_websocket(true);
            		new_primary_sock_conn_initiated = true;
            	    
            	    $("#disconnected_handler").hide();
            	    $("#connected_handler").show();
        		}
        	}
        	*/
        }
    };

    /**
     * Function that is repeatedly called to check if the disconnected server is up now.
     */
    function renew_server_socket()
    {
    	// check if the server was in disconnected state and is now connected
    	var is_server_up = checkIfServerIsUp(preferred_ip_address);
    	if(is_server_up == true) {
    		console.log("Restoring primary websocket connection after disconnect mode ..");
    		init_primary_websocket();
    	    
    	    $("#disconnected_handler").hide();
    	    $("#connected_handler").show();
    	    
    	    // clear this polling function since the server is up now
    	    console.log("Clearing the interval function ..");
    	    clearInterval(new_sock_checker);
    	    
    	    // setup a session for this client with the session manager
    	    registerClientSessionInFailoverMode();
    	}    	
    }
    
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
    
    /**
     * Loads all the events stored in localstorage when the client was operating in the disconnected
     * or offline mode.
     */
    function load_localstorage_events() {
    	console.log("Loading local storage elements ..");
    	for(var event in sessionStorage) {
    		sendMsgToServer(JSON.parse(sessionStorage[event]));
    	}
    	
    	console.log("Emptying local storage events ..");
    	sessionStorage.clear();
    	local_events_ctr = 0;
    }
    
});
