MooGUI {

	var moo, me, <view, <string, disp, >notify, <exists, <color, inputWidget, codeText, openSpace;

	*asDoc {|moo, callback|
		^this.asDocument(moo, callback);
	}

	*asDocument{|moo, callback|
		var doc;
		doc = Document("Moo", "// Code Space\n\n// Put moo objects in the environment with the following:\n\n(Moo.default.me ++ Moo.default.me.location).push;\n\n// You will need to re-evaluate on creating any new object.\n\n// To evaluate Moo code, use Ctrl >\n\n", (moo.me ++ moo.me.location));
		this.setKeys(doc, moo.me);
		callback.notNil.if({ { callback.value(doc) }.defer; });
		^doc;
	}

	*new{|moo, callback, view, show=true|

		^super.newCopyArgs(moo, moo.me, view).init(show, callback);
	}

	*setKeys{|widget, me|

		widget.keyDownAction_({|doc, char, mod, unicode, keycode |
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

			((mod.isAlt && altArrow.value) || (mod.isCtrl && (char == $\>))).if({
				 // alt + left or up or right or down arrow keys
					//"eval".debug(this);
					string = doc.selectedString;
					MooParser(me, string);

			});
		});

		^widget
	}


	init {|show, callback|

		var panel, font, key = Moo.formatKey(me.id, \post);

		// Yes, this is happening!
		exists = true;

		// set up the graphical layout

		color = color ? BileTools.colour;

		view  = view ? Window(nil, 1000@1000).name_("Moo").view;
		view.background_(color);

		view.respondsTo(\minWidth_).if({
			view.minWidth_(180);
		});
		view.respondsTo(\minHeight_).if({
			view.minHeight_(130);
		});

		// font
		font = Font.monospace(24, bold: false, italic: false, usePointSize: false);

		disp = TextView().editable = false;
		disp.resize_(5);
		disp.hasVerticalScroller = true;
		disp.autohidesScrollers_(true);
		disp.font = font;

		codeText = TextView().editable = true;
		codeText.resize_(5);
		codeText.hasVerticalScroller = true;
		codeText.autohidesScrollers_(true);
		codeText.enterInterpretsSelection = true;
		codeText.string_("// Code Space\n\n(Moo.default.me ++ Moo.default.me.location).push;\n\n");
		codeText.syntaxColorize;  // it would be nice if this worked
		codeText.tabWidth_(15);
		codeText.font = font;

		panel = SplitHPanel(leftPanel:disp, rightPanel:codeText);

		inputWidget = TextView()//(view,480@30)
		.focus(true)
		.autohidesScrollers_(true);
		inputWidget.resize_(8).maxHeight_(30);
		inputWidget.font = font;

		openSpace = HLayout();

		view.layout = VLayout(
			openSpace,
			[panel, s:5],
			inputWidget
		);

		this.name = "Moo";
		this.show(show);


		// add functionality

		this.class.setKeys(codeText, me);


		moo.api.add(key, { arg id, str;

			"Posting: %".format(str).debug(this);


			(view.isClosed.not && exists).if ({
				this.append(str);
			}, {
				moo.api.remove(key);
			});

			//AppClock.sched(0, {
			//	(view.isClosed.not && exists).if ({
			//		str = str.asString.replace("\\n", "\n");
			//		this.append(str);
			//	}, {
			//		moo.api.remove(key);
			//	});
				//nil;
			//});
		}, "For moo ouput. Usage: %, id, text".format(key).asSymbol);


		inputWidget.keyDownAction_({ arg view, char, modifiers, unicode, keycode;

			var string;

			(char == 13.asAscii).if({
				string = inputWidget.string;
				string = string.stripWhiteSpace;
				inputWidget.string = "";

				{ MooParser(me, string); }.fork;

			});


			inputWidget.keyDownAction(view, char, modifiers, unicode, keycode);
		});



		// handle window closing

		view.onClose_({
			//api.remove('msg');
			moo.api.remove(key);
			disp = nil;
			view = nil;
			this.release;
			"should be gone".debug(this);
			exists = false;
		});


		this.callback(callback);


	}

	callback {|func|

		AppClock.sched(0.1, {
			//"ready for callback".debug(this);
			func.notNil.if({
				//"callback".debug(this);
				func.value(this);
			});
			nil;
		});
	}

	color_ {|c|
		color = c;
		view.background_(color);
	}


	show { |doit = true|

		if (doit, {
			//view.front;
			this.win.front;
		});
	}

	win { // get the top level View
		var lineage = view.parents;

		lineage.isNil.if({
			^view
		});
		^lineage.last;
	}

	name_{|name|

		name.notNil.if({
			//win.name = name.asString;
			this.win.name = name.asString;
		});
	}


	title_{|title|
		this.name_(title);
	}

	asView {
		^view
	}

	isClosed{
		^view.isClosed
	}

	front { arg ...args;
		^view.front(*args);
	}

	add {|widget, stretch, align|

		openSpace.add(widget, stretch, align);
	}


	append { |text|

		var str, bounds, scrolled;

		"append %".format(text).debug(this);


		text = text.asString.replace("\\n", "\n");

		str ="";

		text.as(Array).do({|c|
			(c.ascii >= 0).if ({

				str = str++ c;
			})
		});


		AppClock.sched(0, {
			//disp.string = disp.string ++ "\n" ++ str;
			/*
			(disp.string.size > 6000).if({
				"truncate".debug(this);
				disp.string = disp.string.copyRange((disp.string.size - 5000).max(0), disp.string.size +1);
			});
			*/

			//disp.respondsTo('setString').if ({
			//	disp.setString("\n"++str, disp.string.size, 0);
			//} , {
				disp.string = disp.string ++ "\n" ++ str; // this may not be as efficient, but it scrolls
			//});

			string = disp.string;

			scrolled = false;

			disp.respondsTo('innerBounds').if({
				bounds = disp.innerBounds;
				//"bottom is at %\n".postf(bounds.bottom);
				disp.respondsTo('visibleOrigin').if ({
					disp.visibleOrigin = Point(0,bounds.bottom);
					//"Set point at %\n".postf(bounds.bottom);
					scrolled = true;
				});
			});

			scrolled.not.if({
				disp.respondsTo('select').if ({
					str = disp.selectedString;
					str.notNil.if({
						(str.size < 1).if ({ // don't wipe out the user's selection
							disp.select(disp.string.size-2, 1);
							disp.select(disp.string.size+1, 0);
						});
					});
				})
			});


			nil
		});


	}


	/*

	mooInit {|imoo, callback|

		var key;

		moo = imoo;
		me = moo.me;
		//moo.me.me = true;

		key = Moo.formatKey(me.id, \post);

		//moo.api.sendMsg("post/%".format(this.id).asSymbol, this.id, str);

		api.add(key, { arg id, str;

			"Posting: %".format(str).debug(this);

			AppClock.sched(0, {
				(view.isClosed.not && exists).if ({
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
	*/



}