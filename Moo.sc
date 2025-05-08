Moo {
	classvar <>default;
	var index, objects, users, <api, <semaphore, <pronouns, <host, <>lobby, <me, <generics;


	*formatKey{|id, key|
		^"%/%".format(id, key).asSymbol;
	}

	*new{|netAPI, json, loadType, isHost=true|
		^super.new.load(netAPI, json, loadType, isHost)
	}

	*login{|netAPI|
		^super.new.init_remote(netAPI)
	}

	*bootstrap {|api, json, loadType, isHost=true|
		var doc, moo, startGui;

		//doc = Document();

		startGui = json.isNil;

		moo = Moo(api, json, loadType, isHost);

		startGui.if({
			AppClock.sched(0, {
				//doc = TextView.new(Window.new("", Rect(100, 100, 600, 700)).front, Rect(0, 0, 600, 700)).resize_(5);

				//moo.me.me = true;
				//moo.me = moo.me ? moo.at(0);
				moo.login(moo.me ? 0);
				//"moo.me %".format(moo.me.class).debug(this);
				moo.me.me = true;
				moo.gui({
					moo.lobby.arrive(moo.me,moo.lobby, moo.me, moo.lobby);

				});

				nil;
			});
		});

		//api.dump;

		^moo
	}

	*fromJSON{|json, api, loadType, isHost=true|
		var arg1, arg2;
		var moo, player;

		// there's a reason I was worried about passing arguments in the wrong order
		arg1 = json; arg2 = api;
		arg1.isKindOf(NetAPI).if({
			api = arg1;
			json = arg2;
		});

		default.notNil.if({
			moo = default;
			moo.fromJSON(json);
		} , {

			moo = Moo(api, json, loadType, isHost);

			//moo.me = moo.user(api.nick);//moo.users.atIgnoreCase(api.nick);
			//moo.me.isNil.if({
			//	moo.me = MooPlayer(moo, api.nick, nil, true);
			//});
			moo.login(api.nick);

		});

		AppClock.sched(0, {

			moo.login(moo.me ? 0);
			moo.me.me = true;

			moo.gui({
				moo.lobby.arrive(moo.me,moo.lobby, moo.me, moo.lobby);

			});

			nil;
		});

		//api.dump;

		^moo

	}


	*load{|json, api, loadType = \parseFile, isHost=true|
		^this.fromJSON(json, api, loadType, isHost);
	}



	init {|net|

		api = net ? NetAPI.default;

		api.isNil.if({
			Error("You must open a NetAPI first").throw;
		});

		semaphore = Semaphore(1);
		pronouns = IdentityDictionary[ \he -> IdentityDictionary[
			\sub -> "He", \ob-> "Him", \pa-> "His", \po -> "His", \ref -> "Himself" ];
		 \she -> IdentityDictionary[
			\sub -> "She", \ob-> "Her", \pa-> "Her", \po -> "Her", \ref -> "Herself" ];
		];

		host = false;
	}


	load {|net, json, loadType, isHost=true|

		var root, user_update_action;

		Moo.default = this;

		this.init(net);

		api.dump;

		objects = Dictionary();//IdentityDictionary();//[];
		index = 0;
		users = IdentityDictionary();
		generics = Dictionary();

		host = isHost;

		net.silence = isHost.not; // Don't advertise everything unless we're in charge

		json.notNil.if({
			this.fromJSON(json, nil, nil, loadType);
			(objects.size==0).if({
				//"Program should halt".debug(this);
				Error("Load failed").throw;
			});
		});

		((objects.size == 0) || (json.isNil)).if({

			//"json is nil".debug(this);

			/*
			//ok, the roo ID is always \0

			//"make root".debug(this);
			root = MooRoot(this, "Root");
			//"made root".debug(this);

			generics[\object] = MooObject(this, "object", root, -1);
			generics[\object].description_("You see nothing special.");
			generics[\object].verb_(\get, \this, \none,
				{|dobj, iobj, caller, object|

					//"get".debug(object);

					object.immobel.not.if({

						object.location.remove(object);
						caller.addObject(object);
						caller.location.announceExcluding(caller, "% picked up %.".format(caller.name, object.name), caller);
						caller.postUser("You picked up %".format(object.name), caller);
					} , {
						caller.postUser("You cannot pick up %".format(object.name), caller);
					});
				}
			);
			generics[\object].verb_(\verbs, \this, \none,
				{|dobj, iobj, caller, object|

					var keys = object.verbs.keys.asList;

					(keys.size == 0).if({
						caller.postUser("% has no verbs".format(object.name));
					}, {
						(keys.size == 1).if({
							caller.postUser("% has a verb, %".format(object.name, keys.first));
						}, {
							// more than 1
							caller.postUser("% has the verbs %, and %".format(object.name, keys.copyRange(0, keys.size-2), keys.last));
						})
					})
				}
			);

			generics[\container] = MooContainer(this, "bag", root, generics[\object]);

			//"make a generic player".debug(this);
			generics[\player] = MooPlayer(this, "player", nil, false, generics[\container]);
			//"made generic player, %".format(generics[\player].name).debug(this);
			root.parent = generics[\player];


			generics[\room] = MooRoom(this, "room", root, generics[\container]);
			generics[\room].description_("An unremarkable place.");
			generics[\room].verb_(\look, \this, \none,

				{|dobj, iobj, caller, object|
					var stuff, others, last, exits;
					//object.description.postln;
					caller.postUser(object.name.asString +"\n" + object.description.value);
					(object == caller.location).if({
						stuff = object.contents;
						//"stuff".debug(object);
						(stuff.size > 0).if({
							stuff = stuff.collect({|o| MooObject.mooObject(o, object.moo).name });
							stuff = stuff.join(", ");
							caller.postUser("You see:" + stuff);
						});
						others = object.players.select({|player| player != caller });
						(others.size == 1).if({
							caller.postUser(Moo.refToObject(others[0]).name.asString + "is here.");
						}, {
							(others.size > 1).if({
								last = Moo.refToObject(others.pop);
								others = others.collect({|o|  Moo.refToObject(o).name  });
								others = others.join(", ");
								caller.postUser(others ++", and" + last.name + "are here.");
							});
						});
						exits = object.exits.keys.asList;
						(exits.size == 1).if({
							//"one exit %".format(exits.first).debug(object);
							caller.postUser("You can exit" + exits[0].asString);
						}, {
							(exits.size > 1).if({
								caller.postUser("Exits are:" + exits.join(", "));
							}, {
								(exits.size==0).if({
									caller.postUser("There is no way out.");
								});
							});
						});
					});
				}.asCompileString;

			);

			generics[\room].verb_(\inventory, \this, \none,

				{|dobj, iobj, caller, object|
					var str, last, skip = false, callerVerb;

					// Did the caller specify the room?
					dobj.isNil.if({
						// no, this was called by default
						(caller.location == object).if({
							// and the caller is in the room

							callerVerb = caller.verb(\inventory);
							callerVerb.notNil.if({
								skip = true;
								callerVerb.invoke(dobj, iobj, caller, caller);
							});
						});
					});

					// No, the user wants to see our contents
					skip.not.if({
						//object.description.postln;
						(object.contents.size == 0).if({
							str = "% is empty.".format(object.name);
						}, {
							(object.contents.size == 1).if({
								str = "% contains %.".format(object.name, object.contents[0].name);
							}, {
								(object.contents.size > 1).if({
									last = object.contents.last;
									str =  "% contains % and %.".format(object.name,
										object.contents.copyRange(0, object.contents.size-2)
										.collect({|c| c.name}).asList.join(", "),
										last.name);
								})
							})
						});
						str.notNil.if({
							//str.debug(object);
							caller.postUser(str);
						} , {
							"Should not be nil".warn;
						});
					});
				}.asCompileString;

			);
			//MooParser.reserveWord(\room, genericRoom);
			//generics[MooRoom] = generics[\room];

			lobby = MooRoom(this, "Lobby", root, generics[\room]);




			//objects = [MooRoom(this, "Lobby", objects[0])];
			//index = 2;

			MooParser.reserveWord(\l, \look);
			MooParser.reserveWord(\inv, \inventory);

			*/

			MooInit.initAll(this);
			me = root;

		});

		//listen for new users
		user_update_action = {|buser|
			var name, muser;

			name = buser.nick.asSymbol;
			muser = users.at(name);
			muser.isNil.if({
				muser = MooPlayer(this, name);
			});
			lobby.arrive(muser);

		};
		api.add_user_update_listener(this, user_update_action );

	}

	init_remote{|net|
		var root;

		this.init(net);

	}

	login {|player|

		var oldme;

		this.host.if({

			oldme = me;

			player.isKindOf(NetAPI).if({
				me = this.user(player.nick);//moo.users.atIgnoreCase(api.nick);
				me.isNil.if({
					me = MooPlayer(this, player.nick, nil, true);
				});
			}, {
				player.isKindOf(MooPlayer).if({
					me = player;
				} , {
					// something else
					me = this.user(player)
				});
			});

			me.notNil.if({
				oldme.isKindOf(MooPlayer).if({
					oldme.me = false;
				});
				me. me = true;
			});
		}, {
			// not yet written, but probably an auotmatic effect of NetAPI
		});

		^me;
	}




	add { |obj, name, id|

		var obj_index, should_add, count;//= true;

		//"add".debug(this);

		semaphore.wait;

		//"waited".debug(this);

		obj.isKindOf(MooRoot).if({
			id = 0;
			should_add = objects.at(id).isNil;
			should_add.not.if({
				MooDuplicateError("You already have a Root").throw;
			});
		} , {

			should_add = objects.includes(obj).not;
			obj.isKindOf(MooPlayer).if({
				name = name ? obj.name;
				name = name.asSymbol;
				should_add = users[name].isNil;
			});
		});


		/*
		(should_add && obj.isKindOf(MooPlayer)).if({
		name = name ? obj.name.asSymbol;
		users.includes(name).not({
		users.add(name);
		} , {
		// this user already exists
		obj_index = users.at(name).id;
		should_add = false;
		MooDuplicateError("MooPlayer % exists as %".format(name, obj_index)).throw;
		});
		});
		*/

		id.isKindOf(Symbol).if({
			id = id.asString;
		});
		id.isKindOf(String).if({
			id = id.select({|c| c.isDecDigit }).asInteger;
		});

		should_add.if({

			count = 0;
			{objects[id].notNil}. while ({
				//"find an unused id, %".format(id).debug(this);
				id = id + 1; //(id ++ count).asSymbol;
			});

			//obj_index = objects.size;
			//objects = objects.add(obj);
			objects.put(id, obj);
			//obj_index = objects.size;
			//index = index + 1;
			obj.isKindOf(MooPlayer).if({
				//"name is %".format(name).debug(obj);
				users.put(name, obj);
			});
		}, {
			obj_index = objects.findKeyForValue(obj);
		});

		semaphore.signal;

		// nothing below should lead to a race condition

		// no the index needs to be a unique number AND the user who created it because it needs to be unique

		//obj_index = objects.indexOf(obj);

		should_add.not.if({
			MooDuplicateError("% % exists as %".format(obj.class, name, obj_index)).throw;
		});


		//^obj_index;
		^id;
	}

	delete {| obj |

		var obj_index;

		//obj.isInteger.if({
		//(obj.isKindOf(Symbol) || obj.isKindOf(String)).if({
		//	obj_index = obj.asSymbol;
		//} , {
		obj.isKindOf(SimpleNumber).not.if({
			obj_index = objects.findKeyForValue(obj);
		});

		obj_index.notNil.if({

			objects[obj_index] = nil; // Don't RENUM!!!!
		});
	}

	at {|ind|

		//(index.isKindOf(String) || index.isKindOf(Symbol)).not.if({
		//	index = index.asString;
		//});
		//index = index.asSymbol;

		^objects.at(ind);
	}

	user {|who| // find a player

		var player;

		who = who.asString;

		player = users.atIgnoreCase(who.asSymbol);
		(player.isNil && who.isDecimal).if({
			player = this.at(who.asInteger);

			player.notNil.if({
				player.isKindOf (MooPlayer).not.if({
					player = nil
				})
			});
		});

		^player
	}

	genericObject {
		var obj = generics[\object];
		obj.notNil.if({
			obj.isKindOf(MooObject).not.if({
				obj = this.at(obj);
			});
		});
		^obj
	}

	genericRoom {
		var obj = generics[\room];
		obj.notNil.if({
			obj.isKindOf(MooObject).not.if({
				obj = this.at(obj);
			});
		});
		^obj
	}

	genericPlayer {
		var obj = generics[\player];
		obj.notNil.if({
			obj.isKindOf(MooObject).not.if({
				obj = this.at(obj);
			});
		});
		^obj
	}



	find{|name|
		var found, arr, obj;
		// if there is more than one answer, we pick randomly.

		found = MooParser.reservedWord(name);

		found.isNil.if({
			found = objects.values.select({|o|
				o.name.asString.compare(name.asString, true) == 0;
			}).asList.choose;
		});

		found.notNil.if({
			arr = objects.values.asList.scramble;
			{found.isNil && (arr.size > 0)}.while({
				obj = arr.pop;
				(obj.aliases.select({|a| a.asString.compare(name.asString, true) == 0 }).asList.size > 0).if({
					found = obj;
				});
			});
		});

		^found
	}

	gui {|callback|

		/*

		var string;

		doc.isNil.if({
			//doc = TextView.new(Window.new("", Rect(100, 100, 600, 700)).front, Rect(0, 0, 600, 700)).resize_(5);
			Error("nil").throw;
		});

		doc.keyDownAction_({|doc, char, mod, unicode, keycode |
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


		^doc;

		*/

		^MooGUI(this,callback);


	}


	toJSON{|converter|

		var encoder, synths, json_generics, reserved, obs, jlobby;

		converter.isNil.if({
			encoder = MooCustomEncoder();
			//^MooJSONConverter.convertToJSON(objects, encoder);
			converter = MooJSONConverter(customEncoder:encoder);
		});

		synths = "\"SynthDefs\" : % ".format(converter.convertToJSON(api.synthDefs));
		//generics = "\"Generics\" : { \"Object\": % , \"Player\": %, \"Room\": % }".format(
		//	converter.convertToJSON(genericObject),
		//	converter.convertToJSON(genericPlayer),
		//	converter.convertToJSON(genericRoom)
		//);
		json_generics =  generics.keys.collect({|key|
			"{ \"key\" : \"%\" ,  \"object\": % }".format(key, converter.convertToJSON(generics.at(key)));
		}).asList.join(", ");
		json_generics = "\"Generics\" : [ % ]".format(json_generics);

		reserved = "\"Reserved\": % ".format(converter.convertToJSON(MooParser));
		jlobby = "\"Lobby\": % ".format(converter.prConvertToJson(lobby));

		obs = "\"Objects\": % ".format(converter.convertTree(objects, this));

		^"{\n\"class\": \"Moo\",\n%\n}\n".format(
			[obs, synths, json_generics, reserved, jlobby].join(",\n")
		);
	}


	// jsonClass.fromJSON(input, converter, moo);
	fromJSON{|obj, converter, linkToThis, loadType=\parseFile|

		var decoder, objs, json_generics, ref, reserved, key, value;

		// dont' advertise stuff we're loading
		api.silence = true;

		loadType = loadType ? \parseFile;

		//"fromJSON % converter % %".format(loadType, converter, obj).debug(this);
		//obj.atIgnoreCase("Objects").debug(this);

		obj.isKindOf(String).if({
			//"its' a string".debug(this);
			decoder = MooCustomDecoder();
			(loadType != \parseText).if({
				File.exists(obj).if({
					^MooJSONConverter.parseFile(obj, decoder, false, true, this);
				});
				"File % does not exist".format(obj).warn;
			});
			// It must be a string of JSON
			^MooJSONConverter.convertToSC( obj, decoder, false, true, this);
		});


		//"fromJSON not recursing".format(obj).debug(this);

		//semaphore.wait;

		//"waited".debug(this);

		obj.isKindOf(Dictionary).if({


			converter.isNil.if({
				decoder = MooCustomDecoder();
				converter = MooJSONConverter(true, false, nil, decoder).moo_(this);
			});

			// First grab all the objects

			objs = obj.atIgnoreCase("Objects");
			//objs.debug(this);
			objs = converter.restoreMoo(objs, this);

			// put everything back at the right index
			//objects.do({|o|
			//	objects[o.atIgnoreCase("index").asInteger] = o;
			//});
			//objs.do({|o|
				//objects.put(o.id, o);
				//"Restoring %".format(o.id).debug(this);
				//this.add(o, o.name, o.id);
			//});

			// make sure all objects are actually restored
			semaphore.wait;
			objects = objects.collect({|o| MooObject.mooObject(o, this) });
			semaphore.signal;

			// Then get the generics
			json_generics =  obj.atIgnoreCase("Generics");

			json_generics.do({|item|
				key = item.atIgnoreCase("key");
				value = item.atIgnoreCase("object");
				value = MooObject.refToObject(value, converter, this);
				generics.put(key.asSymbol, value);
				// fix the generic problem
				value.isKindOf(MooObject).if({
					(value.class.generic.isKindOf(value.class)).not.if({
						value.class.generic = value;
					});
				});
			});


			// anything we've shoved in the parser.
			//"Now reserved words".debug(this);

			reserved = obj.atIgnoreCase("Reserved");
			//reserved.debug(this);

			MooParser.fromJSON(converter, this, reserved);

			// finally, the synthdefs, which idk
			//"synthdefs go here".debug(this);

			lobby = MooObject.refToObject(obj.atIgnoreCase("Lobby"), converter, this);

			converter.finish;

		});


		//semaphore.signal;



		//^"{ \"class\": \"Moo\", \n\"Contents\": % }\n".format(converter.prConvertTree(objects));

		MooInit.updateAll(this);
		api.silence =false;

	}


}




MooError : Error {}
MooTypeError : MooError {}
MooVerbError : MooError {}
MooCompileError : MooVerbError {}
MooDuplicateError : MooError{}
MooMalformedKeyError : MooError {}

MooReservedWordError : MooVerbError { // this has problems
	var <>badStrings;
	*new {|msg, strings|
		^super.new(msg).init(msg, strings);
	}
	init{|msg, strings|
		//super.init(msg);
		badStrings = strings;
	}
}



