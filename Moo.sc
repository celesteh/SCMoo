Moo {
	classvar <>default;
	var index, objects, users, <api, <semaphore, <pronouns, <host, <>lobby, <me, <generics;

	*new{|netAPI, json|
		^super.new.load(netAPI, json)
	}

	*login{|netAPI|
		^super.new.init_remote(netAPI)
	}

	*bootstrap {|api, json, loadType|
		var doc, moo;

		//doc = Document();

		moo = Moo(api, json, loadType);

		AppClock.sched(0, {
			//doc = TextView.new(Window.new("", Rect(100, 100, 600, 700)).front, Rect(0, 0, 600, 700)).resize_(5);

			//moo.me.me = true;
			moo.gui({
				moo.lobby.arrive(moo.me,moo.lobby, moo.me, moo.lobby);

			});

			nil;
		});

		api.dump;

		^moo
	}

	*fromJSON{|json, api, loadType|
		var order;
		json.isKindOf(NetAPI).if({
			order = api;
			api = json;
			json = order;
		});

		default.notNil.if({
			^default.fromJSON(json);
		});

		^bootstrap(api, json, loadType)
	}


	*load{|json, api|

		var order;
		json.isKindOf(NetAPI).if({
			order = api;
			api = json;
			json = order;
		});

		default.notNil.if({
			^default.fromJSON(json, \parseFile);
		});

		^bootstrap(api, json, \parseFile)
	}



	init {|net|

		api = net ? NetAPI.default;

		api.isNil.if({
			Error("You must open a NetAPI first").throw;
		});

		semaphore = Semaphore(1);
		pronouns = IdentityDictionary[ \masc -> IdentityDictionary[
			\sub -> "He", \ob-> "Him", \pa-> "His", \po -> "His", \ref -> "Himself" ];
		];

		host = false;
	}


	load {|net, json, loadType|

		var root, user_update_action;

		Moo.default = this;

		this.init(net);

		api.dump;

		objects = IdentityDictionary();//[];
		index = 0;
		users = IdentityDictionary();
		generics = IdentityDictionary();

		host = true;

		json.notNil.if({
			this.fromJSON(json, nil, nil, loadType);
		});

		((objects.size == 0) || (json.isNil)).if({

			"json is nil".debug(this);
			//moo.dump;
			//api.dump;
			//this.dump;

			//hack = this;

			//ok, the roo ID is always \0

			//MooObject(this, "dummy");
			"make root".debug(this);
			root = MooRoot(this, "Root");
			"made root".debug(this);

			generics[\object] = MooObject(this, "object", root, -1);
			//MooParser.reserveWord(\object, genericObject);


			"make a generic player".debug(this);
			generics[\player] = MooPlayer(this, "player", nil);
			"made generic player, %".format(generics[\player].name).debug(this);
			//genericPlayer.dump;
			root.parent = generics[\player];
			//MooParser.reserveWord(\player, genericPlayer);

			generics[\container] = MooContainer(this, "bag", root, generics[\object]);

			generics[\room] = MooRoom(this, "room", root, generics[\container]);
			generics[\room].description_("An unremarkable place.");
			generics[\room].verb_(\look, \this, \none,

				{|dobj, iobj, caller, object|
					var stuff, others, last, exits;
					//object.description.postln;
					caller.postUser(object.name.asString +"\n" + object.description.value);
					(object == caller.location).if({
						stuff = object.contents;
						(stuff.size > 0).if({
							stuff = stuff.collect({|o| Moo.refToObject(o).name });
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
						exits = object.exits.keys;
						(exits.size == 1).if({
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
			//MooParser.reserveWord(\room, genericRoom);

			lobby = MooRoom(this, "Lobby", root, generics[\room]);
			me = root;



			//objects = [MooRoom(this, "Lobby", objects[0])];
			//index = 2;


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


	add { |obj, name, id|

		var obj_index, should_add, count;//= true;

		semaphore.wait;

		obj.isKindOf(MooRoot).if({
			id = \0;
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

		should_add.if({

			count = 0;
			{objects[id].notNil}. while ({
				"find an unused id, %".format(id).debug(this);
				id = (id ++ count).asSymbol;
			});

			//obj_index = objects.size;
			//objects = objects.add(obj);
			objects.put(id, obj);
			//obj_index = objects.size;
			//index = index + 1;
			obj.isKindOf(MooPlayer).if({
				"name is %".format(name).debug(obj);
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
		(obj.isKindOf(Symbol) || obj.isKindOf(String)).if({
			obj_index = obj.asSymbol;
		} , {
			obj_index = objects.findKeyForValue(obj);
		});

		obj_index.notNil.if({

			objects[obj_index] = nil; // Don't RENUM!!!!
		});
	}

	at {|ind|

		(index.isKindOf(String) || index.isKindOf(Symbol)).not.if({
			index = index.asString;
		});
		index = index.asSymbol;

		^objects.at(ind);
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

		var encoder, synths, json_generics, reserved, obs;

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

		obs = "\"Objects\": % ".format(converter.convertTree(objects, this));

		^"{\n\"class\": \"Moo\",\n%,\n%,\n%,\n%\n}\n".format(
			obs,
			synths,
			json_generics,
			reserved
		);
	}


	// jsonClass.fromJSON(input, converter, moo);
	fromJSON{|obj, converter, linkToThis, loadType=\parseFile|

		var decoder, objs, json_generics, ref, reserved, key, value;

		loadType = loadType ? \parseFile;

		"fromJSON % converter % %".format(loadType, converter, obj).debug(this);

		obj.isKindOf(String).if({
			"its' a string".debug(this);
			decoder = MooCustomDecoder();
			{
				^MooJSONConverter.perform(loadType, obj, decoder, false, true, this);
			}.try ({
				^MooJSONConverter.convertToSC( obj, decoder, false, true, this);
			});
		});

		"fromJSON not recursing".format(obj).debug(this);

		semaphore.wait;

		obj.isKindOf(Dictionary).if({

			converter.isNil.if({
				decoder = MooCustomDecoder();
				converter = MooJSONConverter(true, false, nil, decoder).moo_(this);
			});

			// First grab all the objects

			objs = obj.atIgnoreCase("Objects");
			objs = converter.restoreMoo(objects, this);
			//objects = objects.sort({|a, b|
			//	a.atIgnoreCase("index").asInteger < b.atIgnoreCase("index").asInteger
			//});
			//objects = Array(objects.last.atIgnoreCase("index").asInteger+1);  // allocate the right size array

			// put everything back at the right index
			//objects.do({|o|
			//	objects[o.atIgnoreCase("index").asInteger] = o;
			//});
			objs.do({|o|
				objects.put(o.id.asSymbol, o);
			});

			// Then get the generics
			json_generics =  obj.atIgnoreCase("Generics");
			//ref = generics.atIgnoreCase("Object");
			//ref.notNil.if({
			//	genericObject = objects.at(ref.atIgnoreCase("id").asSymbol);
			//});
			//ref = generics.atIgnoreCase("Player");
			//ref.notNil.if({
			//	genericPlayer = objects.at(ref.atIgnoreCase("id").asSymbol);
			//});
			//ref = generics.atIgnoreCase("Room");
			//ref.notNil.if({
			//	genericRoom = objects.at(ref.atIgnoreCase("id").asSymbol);
			//});
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
			reserved = obj.atIgnoreCase("Reserved");
			MooParser.fromJSON(converter, this, reserved);

			// finally, the synthdefs, which idk


			converter.finish;

		});


		semaphore.signal;



		//^"{ \"class\": \"Moo\", \n\"Contents\": % }\n".format(converter.prConvertTree(objects));


	}


}




MooError : Error {}
MooTypeError : MooError {}
MooVerbError : MooError {}
MooCompileError : MooVerbError {}
MooDuplicateError : MooError{}

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



