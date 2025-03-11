Moo {
	classvar <>default;
	var index, objects, users, <api, <semaphore, <pronouns, <host, <>lobby, <me;

	*new{|netAPI, json|
		^super.new.load(netAPI, json)
	}

	*login{|netAPI|
		^super.new.init_remote(netAPI)
	}

	*bootstrap {|api, foo|
		var doc, moo;

		//doc = Document();

		moo = Moo(api, foo);

		AppClock.sched(0, {
			doc = TextView.new(Window.new("", Rect(100, 100, 600, 700)).front, Rect(0, 0, 600, 700)).resize_(5);

			moo.gui(doc);

			nil;
		});

		api.dump;

		^moo
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

		Moo.default = this;

		this.init(net);

		api.dump;

		objects = [];
		index = 0;
		users = IdentityDictionary();

		host = true;

		json.isNil.if({

			"json is nil".postln;
			//moo.dump;
			//api.dump;
			//this.dump;

			//hack = this;

			//MooObject(this, "dummy");
			root = MooRoot(this, "Jason");
			lobby = MooRoom(this, "Lobby", root);
			lobby.arrive(root,lobby, root);

			me = root;

			//objects = [MooRoom(this, "Lobby", objects[0])];
			//index = 2;

		}, {
			// There needs to be a way to load this from JSON objects
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


	add { |obj, name|

		var obj_index, should_add= true;

		semaphore.wait;

		obj.isKindOf(MooPlayer).if({
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
		"MooObject.new".postln;
		^super.new.initMooObj(moo, name, maker);
	}

	initMooObj {|imoo, iname, maker|

		var name;

		imoo.notNil.if({

			moo = imoo;
			owner = maker;

			//super.make_init(moo.api, nil, {});
			playableEnv = NetworkGui.make(moo.api);
			playableEnv.know = true;

			aliases = [];
			verbs = IdentityDictionary();
			properties = IdentityDictionary();
			immobel = false;

			id = moo.add(this, iname);
			name = iname ? id.asInteger.asString;

			this.property_(\description, "You see nothing special.", true);
			this.property_(\name, name, true).value.postln;


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

caller.contents.remove(dobj);
caller.location.announce(\"% dropped %\".format(caller, dobj));
caller.location.contents = caller.location.contents.add(dobj);
}
"""
			);
		});
	}



	alias {|new_alias|

		aliases = aliases.add(new_alias);
	}


	property_ {|key, ival, publish = true|

		var shared;

		key = key.asSymbol;

		"property_ % %".format(key, ival).postln;

		((properties.includesKey(key)) || verbs.includesKey(key)).if({
			MooError("% name already in use by %".format(key, this.name)).throw;
		}, {

			shared = SharedResource(ival);
			"shared %".format(shared.value).postln;
			properties.put(key, shared);
			publish.if({
				playableEnv.addShared("%/%".format(key, id).asSymbol, shared);
			});
		});

		properties.keys.postln;
		"saved as %".format(properties[key].value).postln;

		^shared;
	}

	verb_ {|key, dobj, iobj, func, publish=false|

		var newV;

		"New verb %".format(key).postln;

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

	verb {|key|
		^this.getVerb(key)
	}

	isPlayer{ ^false }


	doesNotUnderstand { arg selector ... args;
		var verb, property, func;

		selector = selector.asSymbol;


		// first try moo stuff
		"..\ndoesNotUnderstand %".format(selector).debug(this.id);
		verbs.keys.postln;
		properties.keys.postln;
		"..".postln;

		verb = verbs[selector];
		if (verb.notNil) {
			"verb %".format(selector).postln;
			//^func.functionPerformList(\value, this, args);
			^verb.invoke(args[0], args[1], args[2]);
		};

		"not a verb".postln;
		verbs.keys.postln;

		if (selector.isSetter) {
			selector = selector.asGetter;
			if(this.respondsTo(selector), {
				warn(selector.asCompileString
					+ "exists as a method name, so you can't use it as a pseudo-method.");
				this.dumpBackTrace;
				{
					this.perform(selector.asSetter, *args);
					"new result is %".format(this.perform(selector)).debug(this.id);
				}.try({|err| err.postln; });
			}, {
				^(properties[selector].value = args[0]);
			});
		};

		property = properties[selector];
		property.notNil.if({
			"porperty % %".format(selector, property.value).postln;
			^property.value
		});

		"not a property".postln;
		properties.keys.postln;

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
		^super.new.initClock(moo, name, maker, stage);
	}

	initClock {| moo, name, maker, istage|

		var sharedTempo;

		super.initMooObj(moo, name, maker);

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
		^super.new.initStagre(moo, name, maker);
	}

	initStage {| moo, name, maker|

		super.initMooObj(moo, name, maker);
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

		^super.new.initRoom(moo, name, maker);
	}

	initRoom {| moo, name, maker|

		super.initMooObj(moo, name, maker);
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

		this.verb_(\arrive, \any, \this,
			"""
{|dobj, iobj, caller|
\"arrive\".postln;
caller.isPlayer.if({

iobj.announce(\"With a dramatic flourish, % enters\".format(caller.name));
//players = players.add(caller);
caller.dumpStack;
iobj.addPlayer(caller);
caller.location = iobj;
});
}
"""
		);

		this.verb_(\depart, \any, \this,
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
		players = players.add(player);
		playableEnv.put(player.name.asSymbol, player);
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

		selector = selector.asSymbol;

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
