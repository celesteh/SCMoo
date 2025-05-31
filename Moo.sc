Moo {
	classvar <>default;
	var index, objects, users, <api, <semaphore, <pronouns, <host, <>lobby, <me, <generics, gui, <>rest;


	*formatKey{|id, key|
		^"%/%".format(id, key).asSymbol;
	}

	*new{|netAPI, json, loadType, isHost=true, rest= 0.01, fontSize=24|
		"new".debug(this);
		^super.new.load(netAPI, json, loadType, isHost, rest,fontSize)
	}

	*login{|netAPI, json, loadType, isHost=true, rest= 0.01, fontSize=24|
		var moo;
		"login".debug(this);
		moo = super.new.load(netAPI, json, loadType, isHost, rest);
		//^super.new.init_remote(netAPI)
		moo.toJSON.postln;
		AppClock.sched(0, {
			moo.gui({
				"arriving".debug(this);
				//newLocation.arrive(this, newLocation, this, newLocation);
				//moo.lobby.arrive(moo.me,moo.lobby, moo.me, moo.lobby);
				moo.lobby.getVerb(\arrive).invoke(moo.me, moo.lobby, moo.me, moo.lobby);
				//moo.me.login(moo.me, moo.lobbj, moo.me, moo.me);
			});
		}, nil);
		^moo;
	}

	*bootstrap {|api, json, loadType, isHost=true, rest= 0.01, fontSize=24|
		var doc, moo, startGui;

		"bootstrap".debug(this);

		//doc = Document();

		startGui = true;//json.isNil;

		moo = Moo(api, json, loadType, isHost);

		startGui.if({
			AppClock.sched(0, {
				//doc = TextView.new(Window.new("", Rect(100, 100, 600, 700)).front, Rect(0, 0, 600, 700)).resize_(5);

				//moo.me.me = true;
				//moo.me = moo.me ? moo.at(0);
				moo.login(0);
				//"moo.me %".format(moo.me.class).debug(this);
				//moo.me.me = true;
				moo.gui({
					moo.lobby.arrive(moo.me,moo.lobby, moo.me, moo.lobby);

				});

				nil;
			});
		});

		//api.dump;

		^moo
	}

	*fromJSON{|json, api, loadType, isHost=true, rest= 0.01, fontSize=24|
		var arg1, arg2;
		var moo, player;

		"fromJSON".debug(this);


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

			"new moo".debug(this);
			moo = Moo(api, json, loadType, isHost);

			//moo.me = moo.user(api.nick);//moo.users.atIgnoreCase(api.nick);
			//moo.me.isNil.if({
			//	moo.me = MooPlayer(moo, api.nick, nil, true);
			//});
			moo.login(api);

		});

		//AppClock.sched(0, {

		//	"in the AppClock in *fromJSON %".format(moo.me).debug(this);

		//	moo.login(moo.me ? 0);
			//moo.me.me = true;

		//	moo.gui({
		//		moo.lobby.arrive(moo.me,moo.lobby, moo.me, moo.lobby);

		//	});

		//	nil;
		//});

		//api.dump;

		^moo

	}


	*load{|json, api, loadType = \parseFile, isHost=true, rest= 0.01|
		"load".debug(this);
		^this.fromJSON(json, api, loadType, isHost, rest);
	}



	init {|net|

		"init".debug(this);

		rest = rest ? 0.01;
		api = net ? NetAPI.default;

		api.isNil.if({
			Error("You must open a NetAPI first").throw;
		});

		// For Barcelona
		api.synthDefs.subscribeRemote = true;
		//

		semaphore = Semaphore(1);
		pronouns = IdentityDictionary[ \he -> IdentityDictionary[
			\sub -> "He", \ob-> "Him", \pa-> "His", \po -> "His", \ref -> "Himself" ];
		 \she -> IdentityDictionary[
			\sub -> "She", \ob-> "Her", \pa-> "Her", \po -> "Her", \ref -> "Herself" ];
		];

		//host = false;
		//api.add('msg', { arg user, blah;
		//api.add('login', { arg username;

		//	var newuser;

		//	newuser = user(username);
		//});


		api.add('newObject', {arg id, name, class, parent, owner, sender, location;

			var obj, user;

			("api.add %, % % % % % %").format(sender, api.nick, id, name, class, parent, owner).debug(this);

			(sender != api.nick).if({

				user = this.user(owner);
				user.isNil.if({
					user = this.at(owner);
				});

				// moo, name, maker, parent
				(class.asString.compare("MooPlayer") != 0).if({ // we should notmake users this way

					obj = class.asSymbol.asClass.new(this, name, user, this.at(parent), false, id, location);
					//this.add(obj, name, id, false);
					obj.networking;
				});
			});
		});

		//"/MOO/User", 2980, "Les", "Les", 4246
		api.add('User', {arg id, name, nick, location;
			var player, loc;

			"api User % % % %".format(id, name, nick, location).debug(this);

			(nick.asString.compare(api.nick.asString) != 0).if({
				player = users.atIgnoreCase(name);
				loc = MooObject.mooObject(location, this);

				player.isNil.if({
					loc.isKindOf(MooRoom).not.if({ // if the location is borked, then the lobby
						loc = lobby;
					});
					//*new { |moo, name, user, self=false, parent, local, id, location|
					player = MooPlayer(moo:this, name:name, user:api.getUser(nick), self:false,
						parent:nil, local:false, id:id, location:loc);

					//loc = MooObject.mooObject(location, this);

					//loc.isKindOf(MooRoom).not.if({
					//	loc = lobby;
					//});
					//
					//loc.getVerb(\arrive).invoke(player, loc, player, loc); // this will announce arrival n-1 times for N users....., so needs fixing
					me.postUser("% is logged in.".format(name));
				});

				loc.isKindOf(MooRoom).if({ // don't ever move to nil
					"adding % to %".format(name, location).debug("api.add User");
					player.location_(loc, api);
					loc.addPlayer(player);
					//player.move(loc, api);
				}, {
					"% is not a location".format(location).debug("api.add User");
				});
			});
		});

		api.add(\reqPlayers, {
			api.sendMsg(\User, me.id, me.name, api.nick, me.property(\location).value.value.value.value);
		});

		{
			api.sendMsg(\reqPlayers);
			2.wait;
			api.sendMsg(\reqPlayers);
		}.fork;
	}


	load {|net, json, loadType, isHost=true, loadRest=0.01|

		var root, user_update_action;

		"load".debug(this);
		rest = loadRest ? 0.01;

		Moo.default = this;

		this.init(net);


		//api.dump;

		objects = Dictionary();//IdentityDictionary();//[];
		index = 0;
		users = IdentityDictionary();
		generics = Dictionary();

		host = isHost;

		//net.silence = isHost.not; // Don't advertise everything unless we're in charge

		json.notNil.if({
			this.fromJSON(json, nil, nil, loadType);
			(objects.size==0).if({
				//"Program should halt".debug(this);
				Error("Load failed").throw;
			});
		});

		((objects.size == 0) || (json.isNil)).if({

			//"json is nil".debug(this);
			MooInit.initAll(this);
			me = root;

		});

		this.login(api);

		// This won't work. We need IDs

		//listen for new users
		//user_update_action = {|buser|
		//	var name, muser, idle;

		//	name = buser.nick.asSymbol;
		//	muser = users.at(name);
		//	muser.isNil.if({
				//muser = MooPlayer(this, name, buser);
		//		muser = MooPlayer(this, name, buser, self:false, parent, local, id, location|
		//	});

		//	muser.location.isNil.if({
		//		lobby.arrive(muser); // this is actually going to be a problem
		//	});
		//};
		//api.add_user_update_listener(this, user_update_action );

	}

	init_remote{|net|
		var root;

		"init_remote".debug(this);

		this.init(net);

	}

	login {|player|

		var oldme;

		"login".debug(this);

		this.host.if({

			"we're a hosT of % %".format(player, player.class).debug(this);

			oldme = me;

			player.isKindOf(NetAPI).if({

				me = this.user(player.nick);//moo.users.atIgnoreCase(api.nick);
				me.isNil.if({
					"new player".debug(this);
					me = MooPlayer(this, player.nick, player, true, nil, true);


					//Error().throw;


				});
			}, {
				player.isKindOf(MooPlayer).if({
					me = player;
				} , {
					// something else
					me = this.user(player)
				});
			});

			(me.notNil && ( oldme != me)).if({
				oldme.isKindOf(MooPlayer).if({
					oldme.isSelf = false;
				});
				//me. me = true;
				me.isSelf = true;
			});


			// new user announcement goes here
			//api.add('User', {arg id, name, nick, location;
			api.sendMsg('User', me.id, me.name, api.nick, me.location);

		}, {
			// not yet written, but probably an auotmatic effect of NetAPI
		});

		^me;
	}




	add { |obj, name, id|

		var obj_index, should_add, count, parent, owner;//= true;

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

	gui {|callback, fontSize|

		gui.notNil.if({
			gui.exists.if({
				//"we have a gui".debug(this);
				^gui.fontSize_(fontSize).callback(callback);
			})
		});

		//"no gui".debug(this);
		gui = MooGUI(this,callback, fontSize:fontSize);
		^gui;

	}

	doc {|callback|

		^MooGUI.asDoc(this, callback);
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

		rest.wait; // the system needs to breathe

		obj.isKindOf(Dictionary).if({


			converter.isNil.if({
				decoder = MooCustomDecoder();
				converter = MooJSONConverter(true, false, nil, decoder).moo_(this);
			});

			// First grab all the objects

			objs = obj.atIgnoreCase("Objects");
			//objs.debug(this);
			objs = converter.restoreMoo(objs, this, rest);

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



