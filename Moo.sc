Moo {
	classvar <>default;
	var index, objects, users, <api, <semaphore, <pronouns, <host, <>lobby, <me, <genericObject, <genericRoom, <genericPlayer;

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
			doc = TextView.new(Window.new("", Rect(100, 100, 600, 700)).front, Rect(0, 0, 600, 700)).resize_(5);

			moo.me.me = true;
			moo.gui(doc);

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

		host = true;

		json.notNil.if({
			this.fromJSON(json, nil, loadType);
		});

		((objects.size == 0) || (json.isNil)).if({

			"json is nil".postln;
			//moo.dump;
			//api.dump;
			//this.dump;

			//hack = this;

			//MooObject(this, "dummy");
			"make root".debug(this);
			root = MooRoot(this, "Root");
			"made root".debug(this);
			genericObject = MooObject(this, "object", root, -1);
			"make a generic player".debug(this);
			genericPlayer = MooPlayer(this, "player", nil);
			"made generic player, %".format(genericPlayer.name).debug(this);
			genericPlayer.dump;
			root.parent = genericPlayer;

			genericRoom = MooRoom(this, "room", root, genericObject);
			genericRoom.description_("An unremarkable place.");
			lobby = MooRoom(this, "Lobby", root, genericRoom);
			lobby.arrive(root,lobby, root, lobby);

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

		should_add = objects.includes(obj).not;
		obj.isKindOf(MooPlayer).if({
			name = name ? obj.name;
			name = name.asSymbol;
			should_add = users[name].isNil;
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

		^objects.at(ind);
	}


	gui {|doc|

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
					"eval".postln;
					string = doc.selectedString;
					MooParser(me, string);

				}
			);

		});


		^doc;


	}


	toJSON{|converter|

		var encoder, synths;

		converter.isNil.if({
			encoder = MooCustomEncoder();
			//^MooJSONConverter.convertToJSON(objects, encoder);
			converter = MooJSONConverter(customEncoder:encoder);
		});

		synths = "\"SynthDefs\" : % ".format(converter.convertToJSON(api.synthDefs));

		^"{\n\"class\": \"Moo\",\n%,\n\"Objects\": %\n}\n".format(synths, converter.convertTree(objects, this));


	}


	fromJSON{|obj, converter, loadType=\parseFile|

		var encoder, objects;


		converter.isNil.if({
			encoder = MooCustomDecoder();
			{
				^MooJSONConverter.perform(loadType, obj, encoder);
			}.try ({
				^MooJSONConverter.convertToSC( obj, encoder);
			});
		});

		obj.isKindof(String).if({
			{
				^MooJSONConverter.perform(loadType, obj, encoder);
			}.try ({
				^MooJSONConverter.convertToSC( obj, encoder);
			});
		});

		semaphore.wait;

		obj.isKindOf(Dictionary).if({
			objects = obj.atIgnoreCase("Objects");
			objects = converter.restoreMoo(objects, this);
			objects = objects.sort({|a, b|
				a.atIgnoreCase("index").asInteger < b.atIgnoreCase("index").asInteger
			});
			objects = Array(objects.last.atIgnoreCase("index").asInteger+1);  // allocate the right size array

			// put everything back at the right index
			objects.do({|o|
				objects[o.atIgnoreCase("index").asInteger] = o;
			});


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



