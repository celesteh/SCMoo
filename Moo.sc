Moo {
	var index, objects, users, <api, <semaphore, <pronouns, <host, lobby, <me;

	*new{|netAPI, json|
		^super.new.load(netAPI)
	}

	*login{|netAPI|
		^super.new.init_remote(netAPI)
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


	load {|net, json|

		var root, user_update_action;

		this.init(net);

		objects = [];
		index = 0;
		users = IdentityDictionary();

		host = true;

		json.isNil.if({
			{
				//MooObject(this, "dummy");
				root = MooRoot(this, "Root");
				lobby = MooRoom(this, "Lobby", root);
				lobby.arrive(root);


				//objects = [MooRoom(this, "Lobby", objects[0])];
				//index = 2;
			}.fork;
		}, {
			// There needs to be a way to load this from JSON objects
		});

		//listen for new users
		user_update_action {|buser|
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


	add { |obj|

		var obj_index, name, should_add= true;

		semaphore.wait;

		obj.isKindOf(MooPlayer).if({
			name = obj.name.asSymbol;
			users.includes(name).not({
				users.add(name);
			} , {
				// this user already exists
				obj_index = users.at(name).id;
				should_add = false;
				MooDuplicateError("MooPlayer % exists as %".format(name, obj_index)).throw;
			});
		});

		should_add.if({

			obj_index = objects.size;
			objects = objects.add(obj);
			index = index + 1;
		});

		semaphore.signal;

		^obj_index;
	}

	delete {| obj |

		var obj_index;

		obj.isInteger.if({
			obj_index = obj;
		} , {
			obj_index = objects.indexOf(obj);
		});

		obj_index.notNil.if({

			objects[obj_index] = nil; // Don't RENUM!!!!
		});
	}

	at {|ind|

		^objects.at(ind);
	}

}




MooError : Error {}
MooVerbError : MooError {}
MooCompileError : MooVerbError {}
MooDuplicateError : MooError{}

MooReservedWordError : MooVerbError {
	var <>badStrings;
	 *new {|msg, strings|
		^super.new.init(msg, strings);
	}
	init{|msg, strings|
		super.init(msg);
		badStrings = strings;
	}
}



MooObject  {

	var <moo,  <owner, <id, <aliases, verbs, <location,  <properties, <>immobel,
	playableEnv;


	*new { |moo, name, maker|

		^super.new.init(moo, name, maker);
	}

	init {|imoo, iname, maker|

		var name;

		moo = imoo;
		owner = maker;

		//super.make_init(moo.api, nil, {});
		playableEnv = NetworkGui.make(moo.api, nil, {});
		playableEnv.know = true;


		id = moo.add(this);
		name = iname ? id.asString;
		aliases = [];
		verbs = IdentityDictionary();
		properties = IdentityDictionary();
		immobel = false;

		this.property_(\description, "You see nothing special.", true);
		this.property_(\name, name, true);


		this.verb_(\look, \this, \none,
			"""
			{|dobj, iobj, caller|
dobj.description.postln;
                caller.post(dobj.description.value);
			}
            """
		);

		this.verb_(\describe, \this, \any,
			"""
			{|dobj, iobj, caller|

				(caller == dobj.owner).if({
					dobj.description.value = iobj.asString;
				});
			}
            """
		);

		this.verb_(\drop, \this, \none,
			"""
			{|dobj, iobj, caller|

				caller.contents.remove(this);
				caller.location.announce(\"% dropped %\".format(caller, dobj));
				caller.location.contents = caller.location.contents.add(this);
			}
         """
		);
	}



	alias {|new_alias|

		aliases = aliases.add(new_alias);
	}


	property_ {|key, ival, publish = true|

		var shared;

		key = key.asSymbol;

		((properties.includesKey(key)) || verbs.includesKey(key)).if({
			MooError("% name already in use by %".format(key, this.name)).throw;
		}, {

			shared = SharedResource(ival);
			properties.put(key, shared);
			publish.if({
				playableEnv.addShared("%/%".format(key, id).asSymbol, shared);
			});
		});

		^shared;
	}

	verb_ {|key, dobj, iobj, func, publish=false|

		var newV;

		key = key.asSymbol;

		((properties.includesKey(key)) || verbs.includesKey(key)).if({
			MooError("% name already in use by %".format(key, this.name)).throw;
		}, {
			newV = MooVerb(key, dobj, iobj, func, this, publish);

			verbs.put(key, newV);
		});
	}

	getVerb {|key|

		^verbs.at(key.asSymbol)
	}

	isPlayer{ ^false }


	doesNotUnderstand { arg selector ... args;
		var verb, property, func;

		// first try moo stuff

		verb = verbs[selector];
		if (verb.notNil) {
			//^func.functionPerformList(\value, this, args);
			^verb.invoke(args[0], args[1], args[2]);
		};

		if (selector.isSetter) {
			selector = selector.asGetter;
			if(this.respondsTo(selector)) {
				warn(selector.asCompileString
					+ "exists as a method name, so you can't use it as a pseudo-method.")
			};
			^properties[selector].value = args[0];
		};

		property = properties[selector];
		property.notNil.if({
			^property.value
		});

		^nil;

	}


	move {|newLocation|

		var oldLocation, moved = false;

		immobel.not.if({
			this.isPlayer.not.if({
				oldLocation.remove(this);
				newLocation.addObject(this);

			} , {
				//name = this.property(\name);
				//oldLocation.player.remove(this);
				oldLocation.depart(this, oldLocation, this);
				newLocation.arrive(this, newLocation, this);
			});

			this.location = newLocation;
		})
	}


	match{|key|

		var matches = false;

		matches = (key == this.name);

		matches.not.if({

			matches = aliases.includes(key)
		});

		^matches;
	}

}





/*
MooMusicalObject : MooObject {

	var playableEnv;

	*new { |moo, name, maker|

		^super.new.init(moo, name, maker);
	}

	init {| moo, name, maker|

		super.init(moo, name, maker);

		playableEnv = NetworkGui.make(moo.api, nil, {});
		playableEnv.know = true;

	}

}
*/


MooClock : MooObject {

	var <stage, <clock;

	*new { |moo, name, maker, stage|
		var clock = super.new(moo, name, maker);
		^clock.init(moo, name, maker, stage);
	}

	init {| moo, name, maker, istage|

		var sharedTempo;

		super.init(moo, name, maker);

		stage = istage;

		istage.notNil.if({
			stage = istage;
			location = stage.location;
			location.add(this);
		});


		clock = TempoClock();

		verbs = IdentityDictionary();


		// share tempo
		sharedTempo = this.property_(\tempo, 1).changeFunc_({|old, tempo|
			var changed = (old != tempo);
			clock.tempo = tempo;
			^changed;
		}).spec(\tempo, 1);

		this.verb_(\set, \this, \any,
			"""
			{|dobj, iobj, caller|

              sharedTempo.value = iobj.asFloat;
		    }
            """
		); //uses a property so doesn't need to be published

		this.verb_(\play, \this, \any,
			"""
			{|dobj, iobj, caller|

				clock.play
			}
            """
			, true);

		this.verb_(\stop, \this, \any,
			"""
			{|dobj, iob, caller|

				clock.stop
			}
            """
			, true);

		//desc = "The clock attached to %".format(stage.name);
		stage.notNil.if({
			this.description = "The clock attached to %".format(stage.name);
		} , {
			this.description = "A clock";
		});

	}


}

MooStage : MooObject {

	var players, clock, speakers;

	*new { |moo, name, maker|
		var stage = super.new(moo,name, maker);
		^stage.init(moo, name, maker);
	}

	init {| moo, name, maker|

		super.init(moo, name, maker);
		players = [];
		speakers = [];
		location = maker.location;
		immobel = true;

		clock = MooClock(moo, "%_clock".format(name), maker);

		verb_(\add, \any, \this,

			"""
			{|dobj, iobj, caller|

				(caller == owner).if({
					dobj.isKindOf(MooPlayer).if({
						players.includes(dobj).not ({
							players = players.add(dobj);
						})
					})
				})
			}
            """
		);

	}
}



MooRoom : MooObject {

	var contents, players, exits, semaphore;

		*new { |moo, name, maker|

		^super.new.init(moo, name, maker);
	}

	init {| moo, name, maker|

		super.init(moo, name, maker);
		semaphore = Semaphore(1);
		players = [];
		contents = [];
		aliases = ["here"];
		exits = IdentityDictionary();

		this.verb_(\announce, \any, \this,
			// announce "blah" to here
			"""
			{|dobj, iobj, caller|

				iobj.announce(dobj);
			}
            """
		);

		this.verb(\arrive, \any, \this,
			"""
			{|dobj, iobj, caller|
				caller.isPlayer.if({

					iobj.announce(\"With a dramatic flourish, % enters\".format(caller.name));
					//players = players.add(caller);
                    iobj.addPlayer(caller);
					caller.location = this
				});
			}
            """
		);

		this.verb(\depart, \any, \this,
			"""
			{|dobj, iobj, caller|
				caller.isPlayer.if({

					//players.remove(caller);
                    iobj.removePlayer(caller);
					iobj.announce(\"With a dramatic flounce, % departs\".format(caller.name));

				});
			}
            """
		);
	}

	announce {|str|

		var tell;

		players.do({|player|
			player.post(str);
		});

		contents.do ({|thing|
			thing.verbs.includesKey(\tell).if({
				tell = thing.verbs.at(\tell);
				tell.invoke(thing, str);
			});
		});
	}

	announceExcluding{|excluded, str|
		var tell;

		players.do({|player|
			(player != excluded).if({
				player.post(str);
			});
		});

		contents.do ({|thing|
			thing.verbs.includesKey(\tell).if({
				tell = thing.verbs.at(\tell);
				tell.invoke(thing, str);
			});
		});


	}


	remove {|item|

		semaphore.wait;
		contents.remove(item);
		playableEnv.remove(item);
		semaphore.signal

	}

	addObject {|item|

		semaphore.wait;
		contents = contents.add(item);
		playableEnv.put(item.name.asSymbol, item);
		semaphore.signal

	}

	removePlayer {|player|
		semaphore.wait;
		players.remove(player);
		playableEnv.remove(player);
		semaphore.signal
	}

	addPlayer{|player|
		semaphore.wait;
		players = players.add(players);
		playableEnv.put(players.name.asSymbol, players);
		semaphore.signal
	}

	exit{|key|
		^exits[\key]
	}

	addExit{|key, room|

		exits.put(key, room);
	}

	findObj {|key|

		var found, search;
		key = key.asSymbol;

		search = {|arr|

			arr.do({|obj|


				obj.matches(key).if({

					found = obj;
				})
			});

			found;
		};

		found = search.(contents);
		found.isNil.if({
			found = search.(players);
		});

		^found;
	}

	env {

		^playableEnv
	}


	doesNotUnderstand { arg selector ... args;
		var found;

		found = super.doesNotUnderstand(selector, *args);

		found.isNil.if({
			found = this.findObj(selector);
			found.isNil.if({
				found = playableEnv[selector];
			});
		});

		^found;
	}

	remoteDesc { arg user;


	}
}
