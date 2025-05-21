MooWebSocketResponder : WebSocketResponder {

	var <json, jsonSemaphore;


	getJSON {|action|

		// the console log indicates an incoming "OSC" message _or_ that the JSON is ready

		var oldResponder = this.jsResponder;

		jsonSemaphore.isNil.if({
			jsonSemaphore = Semaphore(1); // this method won't be called often, so this is hopefully ok
		});

		jsResponder = {

			AppClock.sched(0, {
				//"getMesg".debug(WebSocketResponder);
				var semaphore = this.class.threadSemaphore;
					semaphore.wait; // this is just traffic direction



				webview.runJavaScript("getJSON()", {|res|


					res.debug(this);

					jsonSemaphore.wait; // one at a time!!

					res.notNil.if({
						//res.postln;
						json = res;
						action.notNil.if({
							// ok, so we have to disocnnect BEFORE running the action in case it reconnects
							this.disconnect;
							action.value(res, this);
							// we only want to do the action once
							action = nil;
							//wasJoined.if({ // don't do this
							//	this.join();
							//});
							jsResponder = oldResponder;
							//webview.onJavaScriptMsg = { this.jsResponder.value };

						})
					});

					jsonSemaphore.signal;
				});

				semaphore.signal;
				nil;
			});
		};

		webview.onJavaScriptMsg = { this.jsResponder.value };
		webview.runJavaScript("requestJSON()", {|m| m.debug(this)});

	}


}