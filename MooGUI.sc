MooGUI : BileChat {

	var moo, me;

	*new {|moo, callback, show=true|

		^super.new(moo.api, show).mooInit(moo, callback);
	}

	mooInit {|imoo, callback|

		var key;

		moo = imoo;
		me = moo.me;
		moo.me.me = true;

		key = Moo.formatKey(me.id, \post);

		//moo.api.sendMsg("post/%".format(this.id).asSymbol, this.id, str);

		api.add(key, { arg id, str;

			"Posting: %".format(str).debug(this);

			AppClock.sched(0, {
				(win.isClosed.not && exists).if ({
					//disp.string = disp.string ++ "\n buh?";
					//disp.string = disp.string ++ "\n" + user ++">"+ blah;
					//string = disp.string;
					str = str.asString.replace("\\n", "\n");
					//this.add(""++ user ++ ">" + blah);
					this.growlnotify("", str);
					this.add(str);
					//[user, blah].postln;
				}, {
					api.remove(key);
				});
				nil;
			});
		}, "For moo ouput. Usage: %, id, text".format(key).asSymbol);


		inputWidget.keyDownAction_({ arg view, char, modifiers, unicode, keycode;

			var string;

			(char == 13.asAscii).if({
				string = inputWidget.string;
				//blah = blah.stripRTF;
				//.escapeChar(13.asAscii).escapeChar(10.asAscii);
				//.tr(13.asAscii, $ ).tr(10.asAscii, $ ).replace("  ", " ");
				string = string.stripWhiteSpace;
				//blah = blah.replace(""++13.asAscii, "\\n");
				//blah = blah.replace(""++10.asAscii, "\\n");
				inputWidget.string = "";
				//this.say(blah);

				MooParser(me, string);

			});


			inputWidget.keyDownAction(view, char, modifiers, unicode, keycode);
		});

		disp.keyDownAction_({|doc, char, mod, unicode, keycode |
			var string;
			var returnVal = nil;
			var altArrow, altLeft, altPlus, altMinus;
			//[mod, keycode, unicode].postln;

			altArrow = Platform.case(
				\osx, { ((keycode==124)||(keycode==123)||(keycode==125)
					||(keycode==126)||(keycode==111)||(keycode==113)||
					(keycode==114)||(keycode==116)||(keycode==37)||
					(keycode==38)||(keycode==39)||(keycode==40))
				},
				\linux, {((keycode>=65361) && (keycode <=65364))},
				\windows, // I don't know, so this is a copy of the mac:
				{ ((keycode==124)||(keycode==123)||(keycode==125)
					||(keycode==126)||(keycode==111)||(keycode==113)||
					(keycode==114)||(keycode==116)||(keycode==37)||
					(keycode==38)||(keycode==39)||(keycode==40))
				}
			);

			altLeft = Platform.case(
				\osx, {((keycode==123) || (keycode==37))},
				\linux,{(keycode==65361)},
				\windows, // I don't know, so here's a copy of osx
				{((keycode==123) || (keycode==37))}
			);

			if( mod.isAlt && altArrow.value,
				{ // alt + left or up or right or down arrow keys
					"eval".debug(this);
					string = doc.selectedString;
					MooParser(me, string);

				}
			);
		});

		this.name = "Moo";

		// this is not a proper callback function;
		AppClock.sched(0.1, {
			//"ready for callback".debug(this);
			callback.notNil.if({
				//"callback".debug(this);
				callback.value(this);
			});
			nil;
		});

	}

}